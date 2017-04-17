/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.io.IOException;
import java.util.Collection;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ActionExecutionException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.OperationFailedException;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.StatusWriter;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.deploy.DeploymentServer;
import net.kwatee.agiledeployment.core.deploy.PlatformService;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptor;
import net.kwatee.agiledeployment.core.deploy.descriptor.PackageDescriptorFactory;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class TaskBase extends Thread implements StatusWriter {

	static private long threadId = 0;

	final private DeploymentOperation deploymentOperation;
	final private ServerInstance instance;

	private Conduit conduit;

	final protected org.slf4j.Logger log;

	/**
	 * @param name
	 * @param deploymentOperation
	 * @param instance
	 */
	protected TaskBase(String name, DeploymentOperation deploymentOperation, ServerInstance instance) {
		super(deploymentOperation.group(), name + '-' + (++threadId));
		this.instance = instance;
		this.log = org.slf4j.LoggerFactory.getLogger(this.getClass());
		this.log.debug("create thread [{}]", getInstance().toString());
		this.deploymentOperation = deploymentOperation;
	}

	/**
	 * 
	 */
	public void run() {
		this.deploymentOperation.enter();
		this.log.debug("thread [{}] initiate", getInstance().toString());
		boolean acquiredPoolSemaphore = false;
		try {
			this.deploymentOperation.readyToRun(getServer().getName());
			acquiredPoolSemaphore = true;
			this.log.debug("thread [{}] start", getInstance().toString());
			doRun(this.deploymentOperation.userName());
		} catch (InterruptedException e) {
			// Will only affect status codes that are in progress or pending
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				setItemStatusCode(DeployStatus.STATUS_CANCELED, artifactVersion.getArtifact());
			}
			setItemStatusCode(DeployStatus.STATUS_CANCELED);
		} catch (Exception e) {
			this.log.error("thread [{}]", getInstance().toString(), e);
			// Will only affect status codes that are in progress or pending
			for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
				setItemStatusCode(DeployStatus.STATUS_CANCELED, artifactVersion.getArtifact());
			}
			statusMessage(e.getMessage());
			setItemStatusCode(DeployStatus.STATUS_FAILED);
		} finally {
			if (acquiredPoolSemaphore) {
				this.deploymentOperation.runCompleted(getServer().getName());
			}
			try {
				DeployTaskServices.getConduitService().evictServerConduit(getInstance());
			} catch (ConduitException e) {}
		}
		this.log.debug("thread [{}] exit", getInstance().toString());
		this.deploymentOperation.leave();
	}

	/**
	 * 
	 * @param userName
	 * @throws InterruptedException
	 * @throws ActionExecutionException
	 * @throws ConduitException
	 */
	abstract protected void doRun(String userName) throws InterruptedException, ActionExecutionException, ConduitException;

	public void statusMessage(String message) {
		batchStatusMessage(message, null);
	}

	/**
	 * @param message
	 * @param serverArtifact
	 * @throws InterruptedException
	 */
	protected void batchStatusMessage(String message, String artifactName) {
		if (StringUtils.isNotEmpty(message)) {
			String currentServerName = this.instance.getInstanceName();
			for (DeployStatus status : this.deploymentOperation.status()) {
				if (status.matchesReferences(currentServerName, artifactName)) {
					status.appendMessage(message);
					break;
				}
			}
		}
	}

	protected void setItemStatusCode(int code) {
		setItemStatusCode(code, null);
	}

	/**
	 * @param code
	 * @param serverArtifact
	 */
	protected void setItemStatusCode(int code, String artifactName) {
		String currentServerName = this.instance.getInstanceName();
		for (DeployStatus status : this.deploymentOperation.status()) {
			if (status.matchesReferences(currentServerName, artifactName)) {
				if (status.getStatus() >= DeployStatus.STATUS_INPROGRESS) {
					status.setStatus(code);
				}
				break;
			}
		}
	}

	/**
	 * @param userName
	 * @param accessLevel
	 * @param installAgent
	 * @return true if successful
	 * @throws InterruptedException
	 * @throws ConduitException
	 */
	protected boolean openConduit(String userName, AccessLevel accessLevel, boolean installAgent) throws InterruptedException, ConduitException {
		this.log.debug("openConduit");
		statusMessage("Establishing server connection...");
		setItemStatusCode(DeployStatus.STATUS_INPROGRESS);

		DeployCredentials credentials = DeployTaskServices.getDeployService().getCredentials(
				getDeployment().getEnvironmentName(),
				userName,
				getServer().getName(),
				accessLevel);
		if (credentials == null) {
			credentials = null;
			this.log.debug("credentials inexistent or insufficient");
		}

		String errorMessage;
		try {
			this.log.debug("openConduit getConduit");
			String conduitType = getServer().getConduitType();
			VariableResolver resolver = new DeploymentVariableResolver(getDeployment(), getInstance());
			String rootDir = DeployTaskServices.getVariableService().fetchVariableValue(Constants.REMOTE_ROOT_DIR, resolver);
			this.conduit = DeployTaskServices.getConduitService().getConduit(getInstance(), conduitType, rootDir);
			this.conduit.setVariableResolver(resolver);
			boolean newlyCreated = this.conduit.newlyCreated();
			this.log.debug("openConduit open");
			this.conduit.open(this.deploymentOperation.ref(), credentials);
			statusMessage("Connection established");
			if (newlyCreated) {
				try {
					if (credentials.getAccessLevel() == AccessLevel.SRM)
						installAgent = true;
					checkSiteStructure(installAgent);
				} catch (MissingVariableException | OperationFailedException ke) {
					this.conduit.close();
					throw ke;
				}
			}
			this.log.debug("openConduit succeeded");
			return true;
		} catch (IOException e) {
			this.log.debug("openConduit", e);
			errorMessage = e.getMessage();
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (credentials != null && credentials.interactivelyObtained()) {
				DeployTaskServices.getDeployService().removeInteractiveCredentials(
						getDeployment().getEnvironmentName(),
						userName,
						getServer().getName());
			}
			errorMessage = "Authentication error: " + e.getMessage();
			this.log.debug(errorMessage);
		} catch (MissingVariableException | ConduitAuthenticationFailedException | OperationFailedException e) {
			this.log.debug("openConduit", e);
			errorMessage = e.getMessage();
		}
		for (ArtifactVersionDto artifactVersion : getServerArtifacts()) {
			setItemStatusCode(DeployStatus.STATUS_FAILED, artifactVersion.getArtifact());
		}
		statusMessage(errorMessage);
		setItemStatusCode(DeployStatus.STATUS_FAILED);
		this.log.debug("openConduit failed");
		return false;
	}

	protected void closeConduit() {
		if (this.conduit != null) {
			this.conduit.close();
			this.conduit = null;
		}
	}

	/**
	 * Usually called right after the establishment of a conduit connection to check whether an agent is installed and
	 * up-to-date
	 * 
	 * @param installAgent
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws OperationFailedException
	 * @throws ConduitException
	 */
	private void checkSiteStructure(boolean installAgent) throws InterruptedException, MissingVariableException, OperationFailedException, ConduitException {
		this.log.debug("checkSiteStructure on {}", getInstance().toString());
		String agentVersion = this.conduit.retrieveRemoteAgentVersion();
		if (!PlatformService.requiredAgentVersion().equals(agentVersion)) {
			if (!installAgent) {
				if (agentVersion == null) {
					this.log.error("checkSiteStructure no agent");
					throw new OperationFailedException("No agent installed");
				} else {
					this.log.error("checkSiteStructure agent {} out of date", agentVersion);
					throw new OperationFailedException("Agent out of date");
				}
			}
			if (agentVersion == null) {
				this.log.warn("{} no agent installed", getInstance().toString());
			} else {
				this.log.warn("{} agent {} out of date", getInstance().toString(), agentVersion);
			}
			this.conduit.installAgent();
			checkSiteStructure(false);
		}
		this.log.debug("checkSiteStructure ok");
	}

	/**
	 * Checks the integrity of artifact <code>artifactFile</code> on <code>server</code> against local descriptor using
	 * agent.
	 * 
	 * @param serverName
	 * @param version
	 * @param artifactVersion
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws ConduitException
	 */
	protected void checkIntegrity(VersionDto version, ArtifactVersionDto artifactVersion) throws InterruptedException, MissingVariableException, ConduitException {
		PackageDescriptor descriptor = PackageDescriptorFactory.loadFromArtifact(getDeployment(), getInstance(), artifactVersion);
		this.conduit.checkIntegrity(
				descriptor.getDeploymentName(),
				descriptor.getArtifactName(),
				getDeployment().skipIntegrityCheck() ? null : descriptor.getSignature(),
				BooleanUtils.isTrue(getServer().getServer().getUseSudo()));
	}

	/**
	 * @param action
	 * @param artifactName
	 * @return the runtime status
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws ActionExecutionException
	 * @throws ConduitException
	 */
	protected int executeRemoteAction(String action, String artifactName) throws InterruptedException, MissingVariableException, ActionExecutionException, ConduitException {
		int exitCode = this.conduit.executeRemoteAction(
				action,
				getDeployment().getActionParams(),
				getDeployment().getEnvironmentName(),
				artifactName,
				getDeployment().skipIntegrityCheck(),
				BooleanUtils.isTrue(getServer().getServer().getUseSudo()));
		String response = this.conduit.getLastCommandOutput();
		if (!response.isEmpty())
			batchStatusMessage(response, artifactName);
		switch (exitCode) {
			case Conduit.KWATEE_RESULT_RUNNING:
				batchStatusMessage("> running", artifactName);
			break;
			case Conduit.KWATEE_RESULT_STOPPED:
				batchStatusMessage("> stopped", artifactName);
			break;
			case Conduit.KWATEE_RESULT_WAS_RUNNING:
				batchStatusMessage("> was already running", artifactName);
			break;
			case Conduit.KWATEE_RESULT_WAS_STOPPED:
				batchStatusMessage("> was already stopped", artifactName);
			break;
			case Conduit.KWATEE_RESULT_OK:
			break;
			case Conduit.KWATEE_RESULT_UNDEFINED:
			break;
			default:
				throw new ActionExecutionException(">ERROR: " + response);
		}
		return exitCode;
	}

	protected Object getOperationParam(String param) {
		return this.deploymentOperation.getOperationParam(param);
	}

	protected Collection<ArtifactVersionDto> getServerArtifacts() {
		ServerArtifactsDto serverArtifactsDto = ServerArtifactsDto.findByName(this.deploymentOperation.getDeployment().getRelease().getServers(), this.getServer().getName());
		return serverArtifactsDto.getArtifacts();
	}

	protected Conduit getConduit() {
		return this.conduit;
	}

	protected DeploymentServer getServer() {
		return (DeploymentServer) getInstance().getParent();
	}

	protected Deployment getDeployment() {
		return this.deploymentOperation.getDeployment();
	}

	protected ServerInstance getInstance() {
		return this.instance;
	}
}
