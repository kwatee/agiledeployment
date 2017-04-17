/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.util.Collection;

import net.kwatee.agiledeployment.common.exception.ActionExecutionException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptorFactory;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.ExecutableDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This class implements the thread that checks the runtime status
 * 
 */
public class TaskStatus extends TaskBase {

	/**
	 * @param deploymentOperation
	 * @param instance
	 */
	public TaskStatus(DeploymentOperation deploymentOperation, ServerInstance instance) {
		super("RuntimeStatus", deploymentOperation, instance);
	}

	@Override
	protected void doRun(String userName) throws InterruptedException, ActionExecutionException, ConduitException {

		if (!openConduit(userName, AccessLevel.VIEWER, false))
			return;

		try {
			statusMessage("Retrieving artifact executables status");
			int serverStatus = DeployStatus.STATUS_DISABLED;
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				Thread.sleep(50); // give opportunity to be interrupted
				int status = getArtifactStatus(artifactVersion);
				switch (status) {
					case DeployStatus.STATUS_FAILED:
						serverStatus = DeployStatus.STATUS_FAILED;
					break;
					case DeployStatus.STATUS_RUNNING:
						if (serverStatus == DeployStatus.STATUS_DISABLED || serverStatus == DeployStatus.STATUS_OK)
							serverStatus = DeployStatus.STATUS_RUNNING;
						else if (serverStatus == DeployStatus.STATUS_STOPPED)
							serverStatus = -1;
					break;
					case DeployStatus.STATUS_STOPPED:
						if (serverStatus == DeployStatus.STATUS_DISABLED || serverStatus == DeployStatus.STATUS_OK)
							serverStatus = DeployStatus.STATUS_STOPPED;
						else if (serverStatus == DeployStatus.STATUS_RUNNING)
							serverStatus = -1;
					break;
					case DeployStatus.STATUS_OK:
						if (serverStatus == DeployStatus.STATUS_DISABLED)
							serverStatus = DeployStatus.STATUS_OK;
					break;
					case DeployStatus.STATUS_UNDETERMINED:
						if (serverStatus == DeployStatus.STATUS_DISABLED || serverStatus == DeployStatus.STATUS_OK || serverStatus == DeployStatus.STATUS_RUNNING || serverStatus == DeployStatus.STATUS_STOPPED || serverStatus == -1)
							serverStatus = DeployStatus.STATUS_UNDETERMINED;
					break;
				}
			}
			if (serverStatus == -1)
				serverStatus = DeployStatus.STATUS_OK;
			if (serverStatus == DeployStatus.STATUS_FAILED)
				statusMessage("Failed due to artifact errors");
			else
				statusMessage("Done");
			setItemStatusCode(serverStatus);
		} finally {
			closeConduit();
		}
	}

	/**
	 * @param artifactVersion
	 * @throws InterruptedException
	 * @throws ActionExecutionException
	 * @throws ConduitException
	 */
	private int getArtifactStatus(ArtifactVersionDto artifactVersion) throws InterruptedException, ActionExecutionException, ConduitException {
		String artifactName = artifactVersion.getArtifact();
		try {
			int status;
			VersionDto version = getDeployment().findVersion(artifactVersion);
			Collection<ExecutableDto> executables = version.getExecutables();
			if (CollectionUtils.isEmpty(executables)) {
				batchStatusMessage("No executables", artifactName);
				status = DeployStatus.STATUS_DISABLED;
			} else {
				getConduit().setVariableResolver(new DeploymentVariableResolver(getDeployment(), version, getInstance()));
				setItemStatusCode(DeployStatus.STATUS_INPROGRESS, artifactName);
				int runningCount = 0;
				int stoppedCount = 0;
				int undeterminedCount = 0;
				int noActionCount = 0;
				boolean hasError = false;
				for (ExecutableDto executable : executables) {
					String action = executable.getStatusAction();
					if (StringUtils.isEmpty(action))
						noActionCount++;
					else {
						this.log.debug("Getting runtime status of executable {}:{}", artifactVersion, executable.getName());
						batchStatusMessage("Status action [" + executable + "]...", artifactName);
						int resultCode = executeRemoteAction(
								PackageDescriptorFactory.ACTION_STATUS_PREFIX + executable.getName(),
								artifactName);
						switch (resultCode) {
							case Conduit.KWATEE_ERROR:
								batchStatusMessage("> ERROR", artifactName);
								hasError = true;
							break;
							case Conduit.KWATEE_RESULT_RUNNING:
								runningCount++;
							break;
							case Conduit.KWATEE_RESULT_STOPPED:
								stoppedCount++;
							break;
							default:
								undeterminedCount++;
							break;
						}
					}
				}
				if (hasError)
					status = DeployStatus.STATUS_FAILED;
				else if (runningCount > 0 && stoppedCount == 0 && undeterminedCount == 0)
					status = DeployStatus.STATUS_RUNNING;
				else if (stoppedCount > 0 && runningCount == 0 && undeterminedCount == 0)
					status = DeployStatus.STATUS_STOPPED;
				else if (noActionCount > 0 && undeterminedCount == 0)
					status = DeployStatus.STATUS_DISABLED;
				else
					status = DeployStatus.STATUS_UNDETERMINED;
			}
			setItemStatusCode(status, artifactName);
			return status;
		} catch (MissingVariableException ke) {
			this.log.error("Get runtime status failed ({})", ke.getMessage());
			batchStatusMessage(ke.getMessage(), artifactName);
			setItemStatusCode(DeployStatus.STATUS_FAILED, artifactName);
			return DeployStatus.STATUS_FAILED;
		}
	}
}
