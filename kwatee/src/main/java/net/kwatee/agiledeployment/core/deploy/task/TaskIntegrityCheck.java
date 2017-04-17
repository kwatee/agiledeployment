/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.collections.CollectionUtils;

/**
 * This class implements the thread that performs an integrity check
 * 
 */
public class TaskIntegrityCheck extends TaskBase {

	/**
	 * @param deploymentOperation
	 * @param instance
	 */
	public TaskIntegrityCheck(DeploymentOperation deploymentOperation, ServerInstance instance) {
		super("CheckDeploymentIntegrity", deploymentOperation, instance);
	}

	@Override
	protected void doRun(String userName) throws InterruptedException, ConduitException {

		if (!openConduit(userName, AccessLevel.VIEWER, false))
			return;

		try {
			statusMessage("Checking artifacts integrity");
			boolean hasErrors = false;
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				Thread.sleep(50); // give opportunity to be interrupted
				if (!checkArtifactIntegrity(artifactVersion))
					hasErrors = true;
			}
			if (hasErrors)
				statusMessage("Failed due to artifact errors");
			else
				statusMessage("Done");
			setItemStatusCode(hasErrors ? DeployStatus.STATUS_FAILED : DeployStatus.STATUS_OK);
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
	private boolean checkArtifactIntegrity(ArtifactVersionDto artifactVersion) throws InterruptedException, ConduitException {
		String artifactName = artifactVersion.getArtifact();
		try {
			VersionDto version = getDeployment().findVersion(artifactVersion);
			if (version.getPackageInfo() == null && CollectionUtils.isEmpty(artifactVersion.getCustomFiles())) {
				batchStatusMessage("No files", artifactName);
				setItemStatusCode(DeployStatus.STATUS_DISABLED, artifactName);
			} else {
				getConduit().setVariableResolver(new DeploymentVariableResolver(getDeployment(), version, getInstance()));
				setItemStatusCode(DeployStatus.STATUS_INPROGRESS, artifactName);
				batchStatusMessage("Checking artifact...", artifactName);
				checkIntegrity(version, artifactVersion);
				batchStatusMessage("OK", artifactName);
				setItemStatusCode(DeployStatus.STATUS_OK, artifactName);
			}
			return true;
		} catch (MissingVariableException | ConduitException ke) {
			batchStatusMessage(ke.getMessage(), artifactName);
			setItemStatusCode(DeployStatus.STATUS_FAILED, artifactName);
			return false;
		}
	}
}
