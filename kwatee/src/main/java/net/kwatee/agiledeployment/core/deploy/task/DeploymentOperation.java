/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.deploy.task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.conduit.ServerInstanceFactory;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.conduit.PlainServerInstance;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.deploy.DeploymentServer;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;

public class DeploymentOperation {

	final private Deployment deployment;
	final private String userName;
	final private String ref;
	final private int op;
	final private Properties operationParams;
	final private int maxConcurrency;
	final private ThreadGroup threads;

	final private Collection<DeployStatus> status;
	final private Collection<Thread> taskThreads;
	final private Map<String, ServerPoolMonitor> serverPoolMonitors;

	private Semaphore concurrencySemaphore; // maximum number of concurrent server sessions
	private int count;
	private boolean canceling;

	/**
	 * Constructor
	 * 
	 * @param userName
	 * @param ref
	 * @param op
	 * @param operationParams
	 * @param maxConcurrency
	 */
	public DeploymentOperation(
			Deployment deployment,
			String userName,
			String ref,
			int op,
			Properties operationParams,
			int maxConcurrency) {
		this.deployment = deployment;
		this.userName = userName;
		this.ref = ref;
		this.op = op;
		this.operationParams = operationParams;
		this.maxConcurrency = maxConcurrency;
		this.threads = new ThreadGroup("DeploymentOperation " + ref);
		this.status = new ArrayList<>(0);
		this.taskThreads = new ArrayList<>();
		this.serverPoolMonitors = new HashMap<>(0);
	}

	/**
	 * 
	 * @return
	 */
	public Deployment getDeployment() {
		return this.deployment;
	}

	/**
	 * 
	 * @return
	 */
	public String userName() {
		return this.userName;
	}

	/**
	 * 
	 * @return
	 */
	public String ref() {
		return this.ref;
	}

	/**
	 * 
	 * @return
	 */
	public int operation() {
		return this.op;
	}

	/**
	 * 
	 * @param param
	 * @return
	 */
	public Object getOperationParam(String param) {
		return this.operationParams == null ? null : this.operationParams.get(param);
	}

	/**
	 * 
	 */
	public void interrupt() {
		this.canceling = true;
		this.threads.interrupt();
	}

	/**
	 * 
	 * @return
	 */
	public ThreadGroup group() {
		return this.threads;
	}

	/**
	 * 
	 */
	public synchronized void enter() {
		this.count++;
	}

	/**
	 * 
	 */
	public synchronized void leave() {
		this.count--;
	}

	/**
	 * 
	 * @return
	 */
	public synchronized int getCompletionType() {
		if (this.count > 0) {
			return this.canceling ? 1 : 0;
		}
		return this.canceling ? 3 : 2;
	}

	/**
	 * 
	 * @return
	 */
	public Collection<DeployStatus> status() {
		return this.status;
	}

	/**
	 * @throws ConduitException
	 * 
	 */
	public void createOperationThreads() throws ConduitException {
		/*
		 * Calculate deployment server instances and create status entries
		 */
		List<ServerInstance> instances = new ArrayList<>();
		for (ServerArtifactsDto serverArtifacts : this.deployment.getRelease().getServers()) {
			DeploymentServer deploymentServer = this.deployment.findDeploymentServer(serverArtifacts.getServer());
			Collection<ServerInstance> poolInstances = new ArrayList<>();
			if (deploymentServer.getPoolType() != null) {
				ServerInstanceFactory factory = DeployTaskServices.getInstanceService().getFactory(deploymentServer.getPoolType());
				try {
					poolInstances.addAll(factory.getServerPool(deploymentServer.getServerProperties()));
				} catch (ServerInstanceException e) {
					throw new InternalErrorException(e);
				}
			} else {
				ServerInstance instance = new PlainServerInstance();
				poolInstances.add(instance);
			}
			for (ServerInstance i : poolInstances) {
				i.setParent(deploymentServer);
				DeployStatus deployStatus = new DeployStatus();
				deployStatus.setReference(i.getInstanceName());
				this.status.add(deployStatus);
				for (ArtifactVersionDto artifactVersion : serverArtifacts.getArtifacts()) {
					DeployStatus artifactStatus = new DeployStatus();
					artifactStatus.setReference(i.getInstanceName(), artifactVersion.getArtifact());
					this.status.add(artifactStatus);
				}
			}
			instances.addAll(poolInstances);
		}

		/*
		 * Create threads
		 */
		boolean sequential;
		switch (this.op) {
			case DeploymentStatus.OP_UNDEPLOY:
			case DeploymentStatus.OP_DEPLOY:
				sequential = this.deployment.isSequential();
			break;
			case DeploymentStatus.OP_STOP:
			case DeploymentStatus.OP_START:
				sequential = this.deployment.isSequential();
			break;
			default:
				sequential = false;
			break;
		}
		if (sequential) {
			this.concurrencySemaphore = new Semaphore(1, true);
		}
		else {
			this.concurrencySemaphore = new Semaphore(this.maxConcurrency);
		}

		DeploymentServer lastServer = null;
		for (ServerInstance instance : instances) {
			// for (DeploymentServer deploymentServer : deployment.servers()) {
			if (instance.getParent() != lastServer) {
				lastServer = (DeploymentServer) instance.getParent();
				int concurrency = lastServer.getPoolConcurrency();
				if (concurrency <= 0) {
					concurrency = this.maxConcurrency;
				}
				this.serverPoolMonitors.put(lastServer.getServer().getName(), new ServerPoolMonitor(concurrency));
			}
			TaskBase thread;
			switch (this.op) {
				case DeploymentStatus.OP_DEPLOY:
					thread = new TaskDeploy(this, instance);
				break;
				case DeploymentStatus.OP_UNDEPLOY:
					thread = new TaskUndeploy(this, instance);
				break;
				case DeploymentStatus.OP_CHECK:
					thread = new TaskIntegrityCheck(this, instance);
				break;
				case DeploymentStatus.OP_START:
					thread = new TaskStart(this, instance);
				break;
				case DeploymentStatus.OP_STOP:
					thread = new TaskStop(this, instance);
				break;
				case DeploymentStatus.OP_STATUS:
					thread = new TaskStatus(this, instance);
				break;
				default:
					throw new InternalErrorException("Unknown operation " + op);
			}
			this.taskThreads.add(thread);
		}
	}

	/**
	 * 
	 */
	public void start() {
		/*
		 * Start all the threads
		 */
		for (Thread thread : this.taskThreads) {
			thread.start();
		}
	}

	/**
	 * 
	 * @param serverName
	 * @throws InterruptedException
	 */
	public void readyToRun(String serverName) throws InterruptedException {
		ServerPoolMonitor monitor = this.serverPoolMonitors.get(serverName);
		if (monitor.isFirstUse()) {
			synchronized (monitor) {
				if (monitor.isFirstUse()) {
					this.concurrencySemaphore.acquire();
					monitor.canStart();
				}
			}
		}
		monitor.acquire();
	}

	/**
	 * 
	 * @param serverName
	 */
	public void runCompleted(String serverName) {
		ServerPoolMonitor monitor = this.serverPoolMonitors.get(serverName);
		monitor.release();
		if (monitor.isEmpty()) {
			this.concurrencySemaphore.release();
		}
	}
}
