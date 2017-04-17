/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.OperationFailedException;
import net.kwatee.agiledeployment.common.exception.OperationInProgressException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ConduitFactory;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.core.conduit.PlainServerInstance;
import net.kwatee.agiledeployment.core.conduit.impl.DeployCredentialsStore;
import net.kwatee.agiledeployment.core.deploy.task.DeployStatus;
import net.kwatee.agiledeployment.core.deploy.task.DeploymentOperation;
import net.kwatee.agiledeployment.core.deploy.task.DeploymentStatus;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.DeploymentVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ServerDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeployService {

	private DeployCredentialsStore deployCredentialsStore = new DeployCredentialsStore();

	private final ArrayList<DeploymentOperation> deploymentOperations = new ArrayList<>();

	@Value("${kwatee.deploy.concurrency:10}")
	private int maxConcurrency;
	@Autowired
	private ConduitService conduitService;
	@Autowired
	private VariableService variableService;

	static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DeployService.class);

	/**
	 * Checks the status of the operation in progress
	 * 
	 * @param ref
	 *            An operation in progress reference
	 * @return The status of the operation in progress
	 * @throws ObjectNotExistException
	 */
	public DeploymentStatus remoteOperationProgress(String ref) throws ObjectNotExistException {
		DeploymentOperation deploymentOperation = findDeploymentOperation(ref);
		if (deploymentOperation == null) {
			throw new ObjectNotExistException(ObjectNotExistException.OPERATION, ref);
		}
		DeploymentStatus progress = new DeploymentStatus();
		progress.setDeploymentOperationRef(ref);
		progress.setOperation(deploymentOperation.operation());
		progress.setCompletionType(deploymentOperation.getCompletionType());
		progress.getStatusList().addAll(deploymentOperationStatus(deploymentOperation));
		return progress;
	}

	/**
	 * Cancels the operation in progress
	 * 
	 * @param ref
	 *            An operation in progress reference
	 */
	public boolean cancelRemoteOperation(String ref, boolean dontClear) {
		DeploymentOperation deploymentOperation = findDeploymentOperation(ref);
		if (deploymentOperation != null)
			if (deploymentOperation.getCompletionType() >= 2) {
				if (dontClear) {
					return false;
				}
				removeDeploymentOperation(ref);
			} else {
				deploymentOperation.interrupt();
				return false;
			}
		return true;
	}

	/**
	 * Retrieve the reference of the operation in progress
	 * 
	 * @param userName
	 *            A kwatee user name
	 * @return An operation in progress reference. Null if none found.
	 */
	public String getOngoingRemoteOperation(String userName) {
		return findUserDeploymentOperation(userName);
	}

	/**
	 * Initiates a deployment integrity check operation. This operation will connect to
	 * each server in the release and check all its artifacts integrity.
	 * 
	 * @param deployment
	 *            A kwatee deployment
	 * @param userName
	 * @return An operation in progress reference
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	public String checkIntegrity(Deployment deployment, String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		return initiateDeploymentOperations(
				deployment,
				DeploymentStatus.OP_CHECK,
				null,
				userName);
	}

	/**
	 * Initiates a deployment deploy operation. This operation will connect to
	 * each server in the release and deploy its artifacts
	 * 
	 * @param deployment
	 *            A kwatee deployment
	 * @return An operation in progress reference
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	public String deploy(Deployment deployment, String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		return initiateDeploymentOperations(
				deployment,
				DeploymentStatus.OP_DEPLOY,
				null,
				userName);
	}

	/**
	 * Initiates a deployment undeploy operation. This operation will connect to
	 * each server in the release and undeploy its artifacts
	 * 
	 * @param deployment
	 *            A kwatee deployment
	 * @param forceUndeploy
	 *            attempt to remove release even if integrity check does not match or artifact scripts fail
	 * @return An operation in progress reference
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	public String undeploy(Deployment deployment, boolean forceUndeploy, String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		Properties params = new Properties();
		params.put("forceUndeploy", Boolean.valueOf(forceUndeploy));
		return initiateDeploymentOperations(
				deployment,
				DeploymentStatus.OP_UNDEPLOY,
				params,
				userName);
	}

	/**
	 * Initiates a deployment status operation. This operation will connect to
	 * each server in the release and check the runtime status of its artifacts
	 * 
	 * @param deployment
	 *            A kwatee deployment
	 * @return An operation in progress reference
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	public String status(Deployment deployment, String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		return initiateDeploymentOperations(
				deployment,
				DeploymentStatus.OP_STATUS,
				null,
				userName);
	}

	/**
	 * Initiates a deployment start operation. This operation will connect to
	 * each server in the release and start its artifacts
	 * 
	 * @param deployment
	 *            A kwatee deployment
	 * @return An operation in progress reference
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	public String start(Deployment deployment, String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		return initiateDeploymentOperations(
				deployment,
				DeploymentStatus.OP_START,
				null,
				userName);
	}

	/**
	 * Initiates a deployment stop operation. This operation will connect to
	 * each server in the release and stop its artifacts
	 * 
	 * @param deployment
	 *            A kwatee deployment
	 * @return An operation in progress reference
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	public String stop(Deployment deployment, String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		return initiateDeploymentOperations(
				deployment,
				DeploymentStatus.OP_STOP,
				null,
				userName);
	}

	/**
	 * Start a new deployment operation (deploy, undeploy, checkintegrity, start, stop, status) by instantiating one
	 * thread per server
	 * 
	 * @param deployment
	 * @param op
	 * @param operationParams
	 * @param onlyServerName
	 * @param onlyArtifactName
	 * @param forcedConduitType
	 * @return
	 * @throws OperationFailedException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws ConduitException
	 */
	private String initiateDeploymentOperations(
			Deployment deployment,
			int op,
			Properties operationParams,
			String userName) throws OperationFailedException, ConduitAuthenticationPromptPasswordException, MissingVariableException, ConduitException {
		if (deployment == null)
			throw new OperationFailedException("Nothing to do");
		LOG.debug("prepareDeployment");
		String ref = findUserDeploymentOperation(userName);
		if (ref != null) {
			DeploymentOperation deploymentOperation = findDeploymentOperation(ref);
			if (deploymentOperation.getCompletionType() >= 2) {
				removeDeploymentOperation(ref);
			} else {
				throw new OperationInProgressException(ref);
			}
		}

		AccessLevel accessLevel;
		switch (op) {
			case DeploymentStatus.OP_DEPLOY:
			case DeploymentStatus.OP_UNDEPLOY:
				accessLevel = AccessLevel.SRM;
			break;
			case DeploymentStatus.OP_CHECK:
				accessLevel = AccessLevel.OPERATOR;
			break;
			case DeploymentStatus.OP_START:
			case DeploymentStatus.OP_STOP:
			case DeploymentStatus.OP_STATUS:
				accessLevel = AccessLevel.OPERATOR;
			break;
			default:
				throw new InternalErrorException("Unknown operation");
		}
		for (DeploymentServer deploymentServer : deployment.getServers()) {
			checkDeployCredentials(deployment, deploymentServer.getServer(), accessLevel, userName);
		}

		ref = deployment.getEnvironmentName() + ":" + System.currentTimeMillis();
		DeploymentOperation deploymentOperation = new DeploymentOperation(deployment, userName, ref, op, operationParams, this.maxConcurrency);
		addDeploymentOperation(deploymentOperation);
		deploymentOperation.createOperationThreads();
		deploymentOperation.start();
		return ref;
	}

	private void checkDeployCredentials(Deployment deployment, ServerDto server, AccessLevel accessLevel, String userName) throws ConduitAuthenticationPromptPasswordException, MissingVariableException {
		VariableResolver resolver = new DeploymentVariableResolver(deployment, server.getName(), server.getPlatform());
		String login = this.variableService.instantiateVariables(server.getCredentials().getLogin(), resolver);
		String password = this.variableService.instantiateVariables(CryptoUtils.decrypt(server.getCredentials().getPassword()), resolver);
		String pem = this.variableService.instantiateVariables(CryptoUtils.decrypt(server.getCredentials().getPem()), resolver);
		DeployCredentials deployCredentials = new DeployCredentials(
				AccessLevel.SRM,
				login,
				password,
				pem,
				false);
		this.deployCredentialsStore.updateServerCredentials(server.getName(), deployCredentials);

		String environmentName = deployment.getEnvironmentName();
		DeployCredentials credentials = this.deployCredentialsStore.getCredentials(environmentName, userName, server.getName(), accessLevel);
		if (credentials == null) {
			throw new ConduitAuthenticationPromptPasswordException(server.getName(), null, accessLevel.toString(), "insufficient credentials");
		}
		if (credentials.getPassword() == null) {
			throw new ConduitAuthenticationPromptPasswordException(deployment.getEnvironmentName(), server.getName(), credentials.getLogin(), accessLevel.toString(), "please enter your password");
		}
	}

	/**
	 * Find the deployment operations details corresponding to a deployment operation reference
	 * 
	 * @param ref
	 * @return
	 */
	private DeploymentOperation findDeploymentOperation(String ref) {
		if (ref != null) {
			synchronized (this.deploymentOperations) {
				for (DeploymentOperation deploymentOperation : this.deploymentOperations) {
					if (ref.equals(deploymentOperation.ref())) {
						return deploymentOperation;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Find the deployment operation details of the user <code>userId</code> (there can be only one per user for now)
	 * 
	 * @param userId
	 * @return
	 */
	private String findUserDeploymentOperation(String userName) {
		synchronized (this.deploymentOperations) {
			for (DeploymentOperation deploymentOperation : this.deploymentOperations) {
				if (deploymentOperation.userName().equals(userName)) {
					return deploymentOperation.ref();
				}
			}
			return null;
		}
	}

	/**
	 * Add a new deployment operation to the list
	 * 
	 * @param deploymentOperation
	 */
	private void addDeploymentOperation(DeploymentOperation deploymentOperation) {
		synchronized (this.deploymentOperations) {
			this.deploymentOperations.add(deploymentOperation);
		}
	}

	/**
	 * Remove a deployment operation from the list
	 * 
	 * @param ref
	 */
	private void removeDeploymentOperation(String ref) {
		synchronized (this.deploymentOperations) {
			for (DeploymentOperation deploymentOperation : this.deploymentOperations) {
				if (ref.equals(deploymentOperation.ref())) {
					if (deploymentOperation.getCompletionType() >= 2) {
						this.deploymentOperations.remove(deploymentOperation);
					}
					break;
				}
			}
		}
	}

	/**
	 * Retrieve the status of a deployment operation
	 * 
	 * @param deploymentOperation
	 * @return
	 */
	private Collection<DeployStatus> deploymentOperationStatus(DeploymentOperation deploymentOperation) {
		Collection<DeployStatus> status = new java.util.ArrayList<DeployStatus>();
		for (DeployStatus s : deploymentOperation.status()) {
			status.add((DeployStatus) s.clone());
		}
		return status;
	}

	/**
	 * Attempts a login on the server with the provided credentials
	 * 
	 * @param deploymentServer
	 *            A {@link net.kwatee.agiledeployment.webapp.ServerShortDto.ServerDto.Server} instance
	 * @param credentials
	 *            {@link DeployCredentials} to be tested
	 * @return the result of server diagnostics if login was successful
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationFailedException
	 * @throws ConduitException
	 */
	public String testConnection(DeploymentServer deploymentServer, DeployCredentials credentials) throws MissingVariableException, ConduitAuthenticationFailedException, ConduitException {
		PlainServerInstance instance = new PlainServerInstance();
		instance.setParent(deploymentServer);
		String rootDir = this.variableService.fetchVariableValue(Constants.REMOTE_ROOT_DIR, new DeploymentVariableResolver(null, instance));
		ConduitFactory conduitFactory = this.conduitService.getFactory(deploymentServer.getConduitType());
		try {
			Conduit conduit = conduitFactory.getNewInstance(instance, rootDir);
			return conduit.remoteDiagnostics(credentials);
		} catch (ConduitAuthenticationFailedException e) {
			throw e;
		} catch (Exception e) {
			throw new ConduitException(e.getMessage());
		} finally {
			conduitFactory.evictServerConnections(instance);
		}
	}

	public void updateInteractiveCredentials(String environmentName, String userName, String serverName, DeployCredentials credentials) {
		this.deployCredentialsStore.updateInteractiveCredentials(environmentName, userName, serverName, credentials);
	}

	public void removeCredentials(String userName) {
		this.deployCredentialsStore.remove(userName);
	}

	public void updateServerCredentials(String serverName, DeployCredentials credentials) {
		this.deployCredentialsStore.updateServerCredentials(serverName, credentials);
	}

	public DeployCredentials getCredentials(String environmentName, String userName, String serverName, AccessLevel accessLevel) {
		return this.deployCredentialsStore.getCredentials(environmentName, userName, serverName, accessLevel);
	}

	public void removeInteractiveCredentials(String environmentName, String userName, String serverName) {
		this.deployCredentialsStore.removeInteractiveCredentials(environmentName, userName, serverName);
	}
}