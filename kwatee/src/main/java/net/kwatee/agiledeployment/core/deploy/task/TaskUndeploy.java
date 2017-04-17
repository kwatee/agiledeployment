/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import net.kwatee.agiledeployment.common.exception.ActionExecutionException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.OperationFailedException;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.deploy.descriptor.DeploymentDescriptorFactory;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptor;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptorFactory;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.lang3.BooleanUtils;

/**
 * This class implements the thread that performs an undeploy
 * 
 */
public class TaskUndeploy extends TaskBase {

	/**
	 * 
	 * @param deploymentOperation
	 * @param instance
	 */
	public TaskUndeploy(DeploymentOperation deploymentOperation, ServerInstance instance) {
		super("Undeployment", deploymentOperation, instance);
	}

	@Override
	protected void doRun(String userName) throws InterruptedException, ConduitException {
		Thread.sleep(50); // give opportunity to be interrupted

		if (!openConduit(userName, AccessLevel.SRM, true))
			return;

		try {
			Boolean b = (Boolean) getOperationParam("forceUndeploy");
			boolean forceUndeploy = b != null && b.booleanValue();
			int serverStatus = DeployStatus.STATUS_OK;
			if (!forceUndeploy) {
				try {
					getInstance().preCleanup(this);
				} catch (ServerInstanceException e) {
					statusMessage("Pre-cleanup failed: " + e.getMessage());
					setItemStatusCode(DeployStatus.STATUS_FAILED);
					return;
				}
				serverStatus = runDeploymentPreCleanupAction();
				if (serverStatus == DeployStatus.STATUS_FAILED) {
					setItemStatusCode(DeployStatus.STATUS_FAILED);
					return;
				}
			}
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				Thread.sleep(50); // give opportunity to be interrupted
				int artifactStatus = undeployArtifact(artifactVersion, forceUndeploy);
				if (serverStatus == DeployStatus.STATUS_OK || serverStatus == DeployStatus.STATUS_DISABLED || artifactStatus == DeployStatus.STATUS_FAILED)
					serverStatus = artifactStatus;
			}
			getConduit().setVariableResolver(new DeploymentVariableResolver(getDeployment(), getInstance()));
			if (!forceUndeploy) {
				if (serverStatus == DeployStatus.STATUS_FAILED)
					statusMessage("Failed due to artifact errors");
				else {
					int status = runDeploymentPostCleanupAction();
					if (status == DeployStatus.STATUS_FAILED || status == DeployStatus.STATUS_UNDETERMINED)
						serverStatus = status;
					if (serverStatus != DeployStatus.STATUS_FAILED)
						statusMessage("Done");
					setItemStatusCode(serverStatus);
				}
				try {
					getInstance().postCleanup(this);
					setItemStatusCode(serverStatus);
				} catch (ServerInstanceException e) {
					statusMessage("Post-cleanup failed: " + e.getMessage());
					setItemStatusCode(DeployStatus.STATUS_FAILED);
					return;
				}
			}
		} finally {
			closeConduit();
		}
	}

	/**
	 * @param artifactVersion
	 * @return
	 * @throws InterruptedException
	 * @throws ConduitException
	 */
	private int undeployArtifact(ArtifactVersionDto artifactVersion, boolean forceUndeploy) throws InterruptedException, ConduitException {
		String artifactName = artifactVersion.getArtifact();
		int status;
		try {
			VersionDto version = getDeployment().findVersion(artifactVersion);
			getConduit().setVariableResolver(new DeploymentVariableResolver(getDeployment(), version, getInstance()));
			PackageDescriptor descriptor = PackageDescriptorFactory.loadFromArtifact(getDeployment(), getInstance(), artifactVersion);
			statusMessage("Undeploy of " + artifactVersion);
			String response = getConduit().getRemoteDescriptor(descriptor.getDeploymentName(), descriptor.getArtifactName());
			if (response != null && response.isEmpty()) {
				batchStatusMessage("Nothing to undeploy (no descriptor)", artifactName);
				setItemStatusCode(DeployStatus.STATUS_OK, artifactName);
				return DeployStatus.STATUS_OK;
			}

			setItemStatusCode(DeployStatus.STATUS_INPROGRESS, artifactName);
			if (!undeployRemoteArtifact(descriptor, artifactName, forceUndeploy))
				status = DeployStatus.STATUS_UNDETERMINED;
			else
				status = DeployStatus.STATUS_OK;
			setItemStatusCode(status, artifactName);
		} catch (MissingVariableException | OperationFailedException ke) {
			this.log.warn("Undeployment of {} failed ({})", artifactVersion.toString(), ke.getMessage());
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
	private int runDeploymentPreCleanupAction() throws InterruptedException, ConduitException {
		int status;
		try {
			int resultCode = executeRemoteAction(DeploymentDescriptorFactory.ACTION_PRE_CLEANUP, null);
			if (resultCode == Conduit.KWATEE_RESULT_OK)
				status = DeployStatus.STATUS_OK;
			else
				status = DeployStatus.STATUS_UNDETERMINED;
		} catch (ActionExecutionException | MissingVariableException ke) {
			this.log.warn("ReleaseShortDto pre-cleanup action in {} failed ({})", getServer(), ke.getMessage());
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
	private int runDeploymentPostCleanupAction() throws InterruptedException, ConduitException {
		int status;
		try {
			int resultCode = executeRemoteAction(DeploymentDescriptorFactory.ACTION_POST_CLEANUP, null);
			if (resultCode == Conduit.KWATEE_RESULT_OK)
				status = DeployStatus.STATUS_OK;
			else
				status = DeployStatus.STATUS_UNDETERMINED;
		} catch (MissingVariableException | ActionExecutionException ke) {
			this.log.warn("ReleaseShortDto post-cleanup action in {} failed ({})", getServer(), ke.getMessage());
			if (!(ke instanceof ActionExecutionException))
				statusMessage(ke.getMessage());
			status = DeployStatus.STATUS_FAILED;
		}
		return status;
	}

	/**
	 * Undeploys the artifact <code>artifactFile</code> from <code>server</code> using agent
	 * 
	 * @param descriptor
	 * @param artifactName
	 * @param forceUndeploy
	 * @return
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws OperationFailedException
	 * @throws ConduitException
	 */
	private boolean undeployRemoteArtifact(PackageDescriptor descriptor, String artifactName, boolean forceUndeploy) throws InterruptedException, MissingVariableException, OperationFailedException, ConduitException {
		batchStatusMessage("Undeploying", artifactName);
		int exitCode = getConduit().undeployPackage(
				descriptor.getDeploymentName(),
				descriptor.getArtifactName(),
				getDeployment().skipIntegrityCheck(),
				forceUndeploy,
				BooleanUtils.isTrue(getServer().getServer().getUseSudo()));
		batchStatusMessage(getConduit().getLastCommandOutput(), artifactName);
		if (exitCode != Conduit.KWATEE_RESULT_OK && exitCode != Conduit.KWATEE_NO_DESCRIPTOR_ERROR) {
			if (forceUndeploy) {
				batchStatusMessage("Undeployment failed", artifactName);
				return false;
			}
			throw new OperationFailedException("Undeployment failed");
		}
		batchStatusMessage("Done", artifactName);
		return true;
	}
}
