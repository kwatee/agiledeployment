/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.util.Collection;

import net.kwatee.agiledeployment.common.exception.ActionExecutionException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
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
 * This class implements the thread that performs process start
 * 
 */
public class TaskStart extends TaskBase {

	/**
	 * @param deploymentOperation
	 * @param instance
	 */
	public TaskStart(DeploymentOperation deploymentOperation, ServerInstance instance) {
		super("StartProcesses", deploymentOperation, instance);
	}

	@Override
	protected void doRun(String userName) throws InterruptedException, ConduitException {

		if (!openConduit(userName, AccessLevel.OPERATOR, false))
			return;

		try {
			statusMessage("Starting artifact executables");
			int serverStatus = DeployStatus.STATUS_DISABLED;
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				Thread.sleep(50); // give opportunity to be interrupted
				int status = startArtifact(artifactVersion);
				switch (status) {
					case DeployStatus.STATUS_FAILED:
						serverStatus = DeployStatus.STATUS_FAILED;
					break;
					case DeployStatus.STATUS_RUNNING:
						if (serverStatus == DeployStatus.STATUS_DISABLED || serverStatus == DeployStatus.STATUS_OK)
							serverStatus = DeployStatus.STATUS_RUNNING;
					break;
					case DeployStatus.STATUS_OK:
						if (serverStatus == DeployStatus.STATUS_DISABLED)
							serverStatus = DeployStatus.STATUS_OK;
					break;
					case DeployStatus.STATUS_UNDETERMINED:
						if (serverStatus == DeployStatus.STATUS_DISABLED || serverStatus == DeployStatus.STATUS_OK || serverStatus == DeployStatus.STATUS_RUNNING)
							serverStatus = DeployStatus.STATUS_UNDETERMINED;
					break;
				}
			}
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
	 * @throws ConduitException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 */
	private int startArtifact(ArtifactVersionDto artifactVersion) throws InterruptedException, ConduitException {
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
				int startedCount = 0;
				int undeterminedCount = 0;
				int noActionCount = 0;
				setItemStatusCode(DeployStatus.STATUS_INPROGRESS, artifactName);
				for (ExecutableDto executable : executables) {
					String action = executable.getStartAction();
					if (StringUtils.isEmpty(action)) {
						noActionCount++;
					} else {
						this.log.debug("Starting executable {}:{}", artifactVersion, executable.getName());
						int resultCode = executeRemoteAction(
								PackageDescriptorFactory.ACTION_START_PREFIX + executable.getName(),
								artifactName);
						switch (resultCode) {
							case Conduit.KWATEE_RESULT_WAS_RUNNING:
							case Conduit.KWATEE_RESULT_RUNNING:
							case Conduit.KWATEE_RESULT_OK:
								startedCount++;
							break;
							default:
								undeterminedCount++;
							break;
						}
					}
				}
				if (startedCount > 0 && undeterminedCount == 0)
					status = DeployStatus.STATUS_RUNNING;
				else if (noActionCount > 0 && undeterminedCount == 0)
					status = DeployStatus.STATUS_DISABLED;
				else
					status = DeployStatus.STATUS_UNDETERMINED;
			}
			setItemStatusCode(status, artifactName);
			return status;
		} catch (MissingVariableException | ActionExecutionException ke) {
			this.log.warn("Start {} failed ({})", artifactVersion, ke.getMessage());
			if (!(ke instanceof ActionExecutionException))
				batchStatusMessage(ke.getMessage(), artifactName);
			setItemStatusCode(DeployStatus.STATUS_FAILED, artifactName);
			return DeployStatus.STATUS_FAILED;
		}
	}
}
