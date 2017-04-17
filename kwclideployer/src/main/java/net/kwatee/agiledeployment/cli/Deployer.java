/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.core.deploy.DeployService;
import net.kwatee.agiledeployment.core.deploy.entity.Deployment;
import net.kwatee.agiledeployment.core.deploy.entity.DeploymentArtifact;
import net.kwatee.agiledeployment.core.deploy.entity.DeploymentServer;
import net.kwatee.agiledeployment.core.deploy.status.DeployStatus;
import net.kwatee.agiledeployment.core.deploy.status.DeploymentStatus;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public class Deployer {

	final private static String DEFAULT_USER = "deployer";

	@Autowired
	private DeployService deployService;
	@Resource(name = "authenticationManager")
	private AuthenticationManager authenticationManager;

	private SignalHandler oldSigInt;

	private String login;
	private String password;

	/**
	 * 
	 * @throws AuthenticationException
	 * @throws KwateeException
	 */

	@PostConstruct
	public void initialize() throws AuthenticationException, KwateeException {
		this.oldSigInt = Signal.handle(new Signal("INT"),
				new SignalHandler() {

					public void handle(Signal signal) {
						System.out.println("\nOperation canceled by user");
						try {
							cancel();
						} catch (KwateeException e) {}
						if (Deployer.this.oldSigInt != null) {
							Deployer.this.oldSigInt.handle(signal);
						}
					}
				});
		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SRM");
		Collection<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(authority);
		String password = "dummy";
		UserDetails user = new User(DEFAULT_USER, password, authorities);
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user, password, null);
		Authentication authentication = this.authenticationManager.authenticate(token);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	Deployment loadDeployment(String bundleXml, String configurationXml, String serverName, String artifactName, String forcedConduitType) throws KwateeException {
		Deployment deployment = this.deployService.loadDeployment(bundleXml, configurationXml, serverName, artifactName, true);
		if (forcedConduitType != null && deployment.servers().size() == 1)
			deployment.servers().iterator().next().setConduitType(forcedConduitType);
		return deployment;
	}

	/**
	 * 
	 * @throws KwateeException
	 */
	void deploy(Deployment deployment) throws KwateeException {
		try {
			long startTime = System.currentTimeMillis();
			try {
				String ref = this.deployService.deploy(deployment, DEFAULT_USER);
				waitForCompletion(ref, startTime);
			} catch (ConduitAuthenticationPromptPasswordException e) {
				if (e.getEnvironmentName() == null) {
					throw new ConduitAuthenticationPromptPasswordException(
							deployment.getEnvironmentName(),
							e.getServerName(),
							e.getLogin(),
							e.getAccessLevel(),
							e.getMessage());
				} else {
					throw e;
				}
			}
		} catch (ConduitAuthenticationPromptPasswordException cae) {
			authenticate(deployment, cae.getServerName(), cae.getLogin(), cae.getMessage());
		}
	}

	/**
	 * 
	 * @param force
	 * @throws KwateeException
	 */
	void undeploy(Deployment deployment, boolean force) throws KwateeException {
		try {
			long startTime = System.currentTimeMillis();
			try {
				String ref = this.deployService.undeploy(deployment, force, DEFAULT_USER);
				waitForCompletion(ref, startTime);
			} catch (ConduitAuthenticationPromptPasswordException e) {
				if (e.getEnvironmentName() == null) {
					throw new ConduitAuthenticationPromptPasswordException(
							deployment.getEnvironmentName(),
							e.getServerName(),
							e.getLogin(),
							e.getAccessLevel(),
							e.getMessage());
				} else {
					throw e;
				}
			}
		} catch (ConduitAuthenticationPromptPasswordException cae) {
			authenticate(deployment, cae.getServerName(), cae.getLogin(), cae.getMessage());
		}
	}

	/**
	 * 
	 * @throws KwateeException
	 */
	void check(Deployment deployment) throws KwateeException {
		try {
			long startTime = System.currentTimeMillis();
			try {
				String ref = this.deployService.checkIntegrity(deployment, DEFAULT_USER);
				waitForCompletion(ref, startTime);
			} catch (ConduitAuthenticationPromptPasswordException e) {
				if (e.getEnvironmentName() == null) {
					throw new ConduitAuthenticationPromptPasswordException(
							deployment.getEnvironmentName(),
							e.getServerName(),
							e.getLogin(),
							e.getAccessLevel(),
							e.getMessage());
				} else {
					throw e;
				}
			}

		} catch (ConduitAuthenticationPromptPasswordException cae) {
			authenticate(deployment, cae.getServerName(), cae.getLogin(), cae.getMessage());
		}
	}

	/**
	 * @throws KwateeException
	 */
	void start(Deployment deployment) throws KwateeException {
		try {
			long startTime = System.currentTimeMillis();
			try {
				String ref = this.deployService.start(deployment, DEFAULT_USER);
				waitForCompletion(ref, startTime);
			} catch (ConduitAuthenticationPromptPasswordException e) {
				if (e.getEnvironmentName() == null) {
					throw new ConduitAuthenticationPromptPasswordException(
							deployment.getEnvironmentName(),
							e.getServerName(),
							e.getLogin(),
							e.getAccessLevel(),
							e.getMessage());
				} else {
					throw e;
				}
			}
		} catch (ConduitAuthenticationPromptPasswordException cae) {
			authenticate(deployment, cae.getServerName(), cae.getLogin(), cae.getMessage());
		}
	}

	/**
	 * @throws KwateeException
	 */
	void stop(Deployment deployment) throws KwateeException {
		try {
			long startTime = System.currentTimeMillis();
			try {
				String ref = this.deployService.stop(deployment, DEFAULT_USER);
				waitForCompletion(ref, startTime);
			} catch (ConduitAuthenticationPromptPasswordException e) {
				if (e.getEnvironmentName() == null) {
					throw new ConduitAuthenticationPromptPasswordException(
							deployment.getEnvironmentName(),
							e.getServerName(),
							e.getLogin(),
							e.getAccessLevel(),
							e.getMessage());
				} else {
					throw e;
				}
			}
		} catch (ConduitAuthenticationPromptPasswordException cae) {
			authenticate(deployment, cae.getServerName(), cae.getLogin(), cae.getMessage());
		}
	}

	/**
	 * @throws KwateeException
	 */
	void status(Deployment deployment) throws KwateeException {
		try {
			long startTime = System.currentTimeMillis();
			try {
				String ref = this.deployService.status(deployment, DEFAULT_USER);
				waitForCompletion(ref, startTime);
			} catch (ConduitAuthenticationPromptPasswordException e) {
				if (e.getEnvironmentName() == null) {
					throw new ConduitAuthenticationPromptPasswordException(
							deployment.getEnvironmentName(),
							e.getServerName(),
							e.getLogin(),
							e.getAccessLevel(),
							e.getMessage());
				} else {
					throw e;
				}
			}
		} catch (ConduitAuthenticationPromptPasswordException cae) {
			authenticate(deployment, cae.getServerName(), cae.getLogin(), cae.getMessage());
		}
	}

	/**
	 * 
	 * @throws KwateeException
	 */
	void cancel() throws KwateeException {
		String batchRef = this.deployService.getOngoingRemoteOperation(DEFAULT_USER);
		if (batchRef != null) {
			this.deployService.cancelRemoteOperation(batchRef, false);
		}
	}

	private void waitForCompletion(String ref, long startTime) throws KwateeException {
		String[] statusLabel = new String[] {"OK", "FAILED", "DISABLED", "UNDETERMINED", "CANCELED", "RUNNING", "STOPPED", "INPROGRESS", "ABORTED"};

		DeploymentStatus status = this.deployService.remoteOperationProgress(ref);
		List<DeployStatus> previousStatusList = null;
		List<DeployStatus> statusList = status.getStatusList();
		do {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				String batchRef = this.deployService.getOngoingRemoteOperation(DEFAULT_USER);
				if (batchRef != null) {
					this.deployService.cancelRemoteOperation(batchRef, false);
				}
				return;
			}
			status = this.deployService.remoteOperationProgress(ref);
			statusList = status.getStatusList();
			for (DeployStatus ds : statusList) {
				if (ds.getReference2() != null) {
					printNewStatus(ds, previousStatusList);
				}
			}
			for (DeployStatus ds : statusList) {
				if (ds.getReference2() == null) {
					printNewStatus(ds, previousStatusList);
				}
			}
			previousStatusList = statusList;
		} while (status.getCompletionType() == DeploymentStatus.COMPLETION_INPROGRESS);
		long duration = System.currentTimeMillis() - startTime;
		System.out.println("\nOperation completed in " + duration + " milliseconds");

		System.out.println("\n===========================================================");
		for (DeployStatus ds : statusList) {
			String statusEl = ds.getDisplayName();
			String statusVal = statusLabel[ds.getStatus()];
			System.out.println(statusVal + "\t" + statusEl);
		}
	}

	private void printNewStatus(DeployStatus status, List<DeployStatus> previousStatusList) {
		String statusRef = status.getDisplayName();
		DeployStatus previousStatus = null;
		if (previousStatusList != null) {
			for (DeployStatus s : previousStatusList) {
				if (s.getDisplayName().equals(statusRef)) {
					previousStatus = s;
					break;
				}
			}
		}
		String newMessage = null;
		if (previousStatus == null) {
			newMessage = status.getMessages();
		} else if (status.getMessages().length() > previousStatus.getMessages().length()) {
			newMessage = status.getMessages().substring(previousStatus.getMessages().length());
		}
		if (newMessage != null) {
			for (String line : newMessage.split("\\n")) {
				if (!line.isEmpty()) {
					System.out.println(statusRef + ": " + line);
				}
			}
		}
	}

	void showContents(Deployment deployment) throws KwateeException {
		for (DeploymentServer server : deployment.servers()) {
			System.out.println(server.getName());
			for (DeploymentArtifact artifact : server.artifacts()) {
				System.out.println("   " + artifact.toString());
			}
		}
	}

	private void authenticate(Deployment deployment, final String serverName, String originalLogin, String message) throws KwateeException {
		System.out.println(message);
		String password = null;
		do {
			String login = originalLogin;
			System.out.println();
			if (login == null) {
				do {
					System.out.println("Enter login for server " + serverName);
					System.out.print("Login");
					if (this.login != null) {
						System.out.print(" [" + this.login + "]");
					}
					System.out.print(": ");
					login = System.console().readLine();
					if (login == null || login.isEmpty()) {
						login = this.login;
					}
				} while (login == null || login.isEmpty());
			}
			do {
				System.out.println("Enter password for user " + login + " on server " + serverName);
				System.out.print("Password");
				if (this.password != null) {
					System.out.print(" [same as before]");
				}
				System.out.print(": ");
				password = System.console().readLine();
				if (password == null || password.isEmpty()) {
					password = this.password;
				}
			} while (password == null || password.isEmpty());
			try {
				DeploymentServer server = (DeploymentServer) CollectionUtils.find(deployment.servers(), new Predicate() {

					@Override
					public boolean evaluate(Object object) {
						return serverName.equals(((DeploymentServer) object).getServerName());
					}

				});
				if (server == null)
					throw new ObjectNotExistException(ObjectNotExistException.SERVER, serverName);
				DeployCredentials creds = new DeployCredentials(
						AccessLevel.SRM,
						login,
						password,
						null,
						true);
				this.deployService.testConnection(server, creds);
				this.deployService.updateInteractiveCredentials(deployment.getEnvironmentName(), DEFAULT_USER, serverName, creds);
				this.login = login;
				this.password = password;
				return;
			} catch (ConduitAuthenticationFailedException e) {
				System.out.println(e.getMessage());
			} catch (KwateeException e1) {
				e1.printStackTrace();
			}
		} while (true);
	}
}
