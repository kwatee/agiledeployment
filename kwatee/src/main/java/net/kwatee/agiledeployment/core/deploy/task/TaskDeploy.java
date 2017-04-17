/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.io.File;
import java.util.ArrayList;

import net.kwatee.agiledeployment.common.exception.ActionExecutionException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.OperationFailedException;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.deploy.descriptor.DeploymentDescriptor;
import net.kwatee.agiledeployment.core.deploy.descriptor.DeploymentDescriptorFactory;
import net.kwatee.agiledeployment.core.deploy.descriptor.DescriptorDifferences;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptor;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptorFactory;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class implements the thread that performs an installation
 * 
 */
public class TaskDeploy extends TaskBase {

	/**
	 * @param deploymentOperation
	 * @param instance
	 */
	public TaskDeploy(DeploymentOperation deploymentOperation, ServerInstance instance) {
		super("InstallDeployment", deploymentOperation, instance);
	}

	@Override
	protected void doRun(String userName) throws InterruptedException, ConduitException {
		this.log.debug("TaskDeploy");
		Thread.sleep(50); // give opportunity to be interrupted
		if (!openConduit(userName, AccessLevel.SRM, true))
			return;

		try {
			int serverStatus = DeployStatus.STATUS_OK;
			DeploymentDescriptor deploymentDescriptor;
			this.log.debug("Update deployment descriptor on {}", getServer().toString());
			try {
				deploymentDescriptor = updateDeploymentDescriptor();
			} catch (MissingVariableException e) {
				this.log.warn("Failed to prepare deployment descriptor: {}", e.getMessage());
				batchStatusMessage("Failed to prepare deployment descriptor: " + e.getMessage(), null);
				setItemStatusCode(DeployStatus.STATUS_FAILED);
				return;
			}

			try {
				this.log.debug("server {} preSetup", getServer().toString());
				getInstance().preSetup(this);
			} catch (ServerInstanceException e) {
				this.log.warn("ServerShortDto Pre-setup failed {}", e.getMessage());
				statusMessage("Pre-setup failed: " + e.getMessage());
				setItemStatusCode(DeployStatus.STATUS_FAILED);
				return;
			}
			this.log.debug("release preSetup");
			serverStatus = runDeploymentPreSetupAction();
			if (serverStatus == DeployStatus.STATUS_FAILED) {
				setItemStatusCode(DeployStatus.STATUS_FAILED);
				return;
			}
			this.log.debug("deploy artifacts");
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				Thread.sleep(50); // give opportunity to be interrupted
				int artifactStatus = deployArtifact(artifactVersion, deploymentDescriptor);
				if (serverStatus == DeployStatus.STATUS_OK || serverStatus == DeployStatus.STATUS_DISABLED || artifactStatus == DeployStatus.STATUS_FAILED)
					serverStatus = artifactStatus;
			}
			this.getConduit().setVariableResolver(new DeploymentVariableResolver(getDeployment(), getInstance()));
			if (serverStatus == DeployStatus.STATUS_FAILED) {
				statusMessage("Failed due to artifact errors");
			} else {
				int status = runDeploymentPostSetupAction();
				if (status == DeployStatus.STATUS_FAILED || status == DeployStatus.STATUS_UNDETERMINED)
					serverStatus = status;
				if (serverStatus != DeployStatus.STATUS_FAILED)
					statusMessage("Deploy complete");
			}
			try {
				getInstance().postSetup(this);
				setItemStatusCode(serverStatus);
			} catch (ServerInstanceException e) {
				this.log.warn("Post-setup failed {}", e.getMessage());
				statusMessage("Post-setup failed: " + e.getMessage());
				setItemStatusCode(DeployStatus.STATUS_FAILED);
				return;
			}
		} finally {
			closeConduit();
		}
	}

	private DeploymentDescriptor updateDeploymentDescriptor() throws InterruptedException, MissingVariableException, ConduitException {
		DeploymentDescriptor deploymentDescriptor = DeploymentDescriptorFactory.loadFromServer(getDeployment(), getServer().getServer());
		// Always update all descriptors
		getConduit().updateDeploymentDescriptor(deploymentDescriptor.getDeploymentName(), deploymentDescriptor.toXml());
		return deploymentDescriptor;
	}

	/**
	 * @param artifactVersion
	 * @param deploymentDescriptor
	 * 
	 * @throws InterruptedException
	 * @throws ConduitException
	 */
	private int deployArtifact(ArtifactVersionDto artifactVersion, DeploymentDescriptor deploymentDescriptor) throws InterruptedException, ConduitException {
		String serverName = getServer().getName();
		this.log.debug("deploy {} on {}", artifactVersion.toString(), serverName);
		String artifactName = artifactVersion.getArtifact();
		setItemStatusCode(DeployStatus.STATUS_INPROGRESS, artifactName);
		int status;
		try {
			VersionDto version = getDeployment().findVersion(artifactVersion);
			getConduit().setVariableResolver(new DeploymentVariableResolver(getDeployment(), version, getInstance()));

			PackageDescriptor descriptor = PackageDescriptorFactory.loadFromArtifact(getDeployment(), getInstance(), artifactVersion);
			statusMessage("Deploy of " + artifactVersion.toString());
			batchStatusMessage("Retrieving remote artifact descriptor...", artifactName);
			PackageDescriptor remoteDescriptor = retrieveRemoteDescriptor(descriptor);
			DescriptorDifferences differences = PackageDescriptorFactory.diff(descriptor, remoteDescriptor);
			if (!differences.hasDifferences()) {
				// Update anyway in case actions were updated
				getConduit().updatePackageDescriptor(descriptor.getDeploymentName(), descriptor.getArtifactName(), descriptor.toXml());
				batchStatusMessage("Package files are up-to-date", artifactName);
				status = DeployStatus.STATUS_OK;
			} else {
				if (remoteDescriptor == null) {
					// Full installation
					remoteDescriptor = descriptor;
					batchStatusMessage("Full deployment needed", artifactName);
					differences = PackageDescriptorFactory.diff(descriptor, null);
					if (!differences.hasDifferences()) {
						batchStatusMessage("ArtifactShortDto file has been updated", artifactName);
						differences = null;
					}
				} else
					batchStatusMessage("Incremental deployment", artifactName);
				if (differences == null)
					status = DeployStatus.STATUS_OK;
				else {
					installIncrementalPackage(serverName, version, artifactVersion, deploymentDescriptor, descriptor, differences);
					status = DeployStatus.STATUS_OK;
				}
			}
			setItemStatusCode(status, artifactName);
			this.log.debug("deploy ok");
		} catch (MissingVariableException | OperationFailedException ke) {
			this.log.warn("Deploy of {} failed ({})", artifactVersion, ke.getMessage());
			batchStatusMessage(ke.getMessage(), artifactName);
			setItemStatusCode(DeployStatus.STATUS_FAILED, artifactName);
			status = DeployStatus.STATUS_FAILED;
		}
		return status;
	}

	/**
	 * @return
	 * @throws InterruptedException
	 * @throws ConduitException
	 */
	private int runDeploymentPreSetupAction() throws InterruptedException, ConduitException {
		int status;

		try {
			int resultCode = executeRemoteAction(DeploymentDescriptorFactory.ACTION_PRE_SETUP, null);
			if (resultCode == Conduit.KWATEE_RESULT_OK)
				status = DeployStatus.STATUS_OK;
			else
				status = DeployStatus.STATUS_UNDETERMINED;
		} catch (ActionExecutionException | MissingVariableException ke) {
			this.log.warn("Pre-setup script in {}@{} failed ({})", getDeployment(), getServer(), ke.getMessage());
			if (!(ke instanceof ActionExecutionException))
				statusMessage(ke.getMessage());
			status = DeployStatus.STATUS_FAILED;
		}
		return status;
	}

	/**
	 * @return
	 * @throws InterruptedException
	 * @throws ConduitException
	 */
	private int runDeploymentPostSetupAction() throws InterruptedException, ConduitException {
		int status;
		try {
			int resultCode = executeRemoteAction(DeploymentDescriptorFactory.ACTION_POST_SETUP, null);
			if (resultCode == Conduit.KWATEE_RESULT_OK)
				status = DeployStatus.STATUS_OK;
			else
				status = DeployStatus.STATUS_UNDETERMINED;
		} catch (ActionExecutionException | MissingVariableException ke) {
			this.log.warn("ReleaseShortDto post-setup action in {}@{} failed ({})", getDeployment(), getServer(), ke.getMessage());
			if (!(ke instanceof ActionExecutionException))
				statusMessage(ke.getMessage());
			status = DeployStatus.STATUS_FAILED;
		}
		return status;
	}

	/**
	 * @param serverName
	 * @param version
	 * @param artifactVersion
	 * @param deploymentDescriptor
	 * @param packageDescriptor
	 * @param differences
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws ConduitException
	 */
	private void installIncrementalPackage(String serverName, VersionDto version, ArtifactVersionDto artifactVersion, DeploymentDescriptor deploymentDescriptor, PackageDescriptor packageDescriptor, DescriptorDifferences differences) throws InterruptedException, MissingVariableException, ConduitException {
		Thread.sleep(50); // give opportunity to be interrupted
		ArrayList<FileDto> filesToPackage = new ArrayList<>();
		if (differences != null) {
			if (log.isDebugEnabled()) {
				log.debug("Differences:");
				for (FileDto f : differences.getAdditions())
					log.debug("  added {}", f.toString());
				for (FileDto f : differences.getChanges())
					log.debug("  changed {}", f.toString());
			}
			filesToPackage.addAll(differences.getAdditions());
			filesToPackage.addAll(differences.getChanges());
		}
		File deploymentFile = DeployTaskServices.getPackagerService().buildArtifactDeploymentBundle(
				getDeployment(),
				getServer().getServer(),
				artifactVersion,
				packageDescriptor,
				filesToPackage);

		try {
			batchStatusMessage("Deploying", artifactVersion.getArtifact());
			getConduit().deployPackage(
					packageDescriptor.getDeploymentName(),
					packageDescriptor.getArtifactName(),
					deploymentFile.getAbsolutePath(),
					BooleanUtils.isTrue(getServer().getServer().getUseSudo()));
		} finally {
			deploymentFile.delete();
		}
		batchStatusMessage(getConduit().getLastCommandOutput(), artifactVersion.getArtifact());
	}

	/**
	 * Retrieves the remote descriptor of <code>artifactFile</code>, i.e. the list of files deployed on
	 * <code>server</code> and their signatures using agent
	 * 
	 * @param descriptor
	 * @param artifact
	 * @return
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws OperationFailedException
	 * @throws ConduitException
	 */
	private PackageDescriptor retrieveRemoteDescriptor(PackageDescriptor descriptor) throws InterruptedException, MissingVariableException, OperationFailedException, ConduitException {
		this.log.debug("retrieveRemoteDescriptor");
		String response = getConduit().getRemoteDescriptor(descriptor.getDeploymentName(), descriptor.getArtifactName());
		if (StringUtils.isNotEmpty(response)) {
			int start = response.indexOf("<kwatee");
			int end = response.indexOf("</kwatee>");
			if (start == -1 || end == -1) {
				this.log.warn("retrieveRemoteDescriptor bad bundle");
				throw new OperationFailedException("Unknown error: " + response);
			}
			this.log.debug("retrieveRemoteDescriptor ok");
			String xmlDescriptor = response.substring(start, end + 9 /* length of </kwatee> */);
			return PackageDescriptorFactory.loadFromXml(xmlDescriptor);
		}
		this.log.warn("retrieveRemoteDescriptor exit code != 0");
		return null;
	}
}
