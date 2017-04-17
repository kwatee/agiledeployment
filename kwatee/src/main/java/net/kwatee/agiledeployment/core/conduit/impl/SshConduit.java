/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.IOException;
import java.io.InputStream;

import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.utils.PathUtils;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.conduit.ConduitException;

import org.apache.commons.lang3.StringUtils;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.ServerHostKeyVerifier;
import ch.ethz.ssh2.Session;

class SshConduit extends AbstractShellConduit {

	final private org.slf4j.Logger log;
	private Connection connection;

	/**
	 * @param server
	 */
	public SshConduit(ServerInstance server, String rootDir) {
		super(server, rootDir);
		this.log = org.slf4j.LoggerFactory.getLogger(SshConduit.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduit#open(java.lang.String, net.kwatee.agiledeployment.core.model.ServerCredentials)
	 */
	@Override
	public void open(String ref, final DeployCredentials credentials) throws IOException, InterruptedException, ConduitAuthenticationFailedException {
		super.open(ref, credentials);
		this.log.debug("connect to {}", this.server);

		if (this.connection == null) {
			this.connection = new Connection(this.server.getIPAddress(), this.server.getPort());
			this.connection.connect(new ServerHostKeyVerifier() {

				public boolean verifyServerHostKey(String arg0, int arg1, String arg2, byte[] arg3) throws Exception {
					return true;
				}
			}, 5000, 8000);
			try {
				this.log.trace("authenticate");

				Thread.sleep(1); // give opportunity to be interrupted
				boolean authenticated = false;
				if (credentials.hasPem()) {
					if (this.connection.isAuthMethodAvailable(credentials.getLogin(), "publickey")) {
						authenticated = this.connection.authenticateWithPublicKey(credentials.getLogin(), credentials.getPem().toCharArray(), credentials.getPassword());
						if (!authenticated)
							this.log.error("publickey authentication failed");
					} else
						this.log.error("publickey authentication method not supported by host {}", this.server);
				} else {
					if (this.connection.isAuthMethodAvailable(credentials.getLogin(), "password")) {
						/**
						 * Create a PasswordAuthenticationClient instance, set the properties
						 * and pass to the SessionClient to authenticate
						 */
						authenticated = this.connection.authenticateWithPassword(credentials.getLogin(), credentials.getPassword());
						if (!authenticated)
							this.log.error("password authentication failed");
					} else if (this.connection.isAuthMethodAvailable(credentials.getLogin(), "keyboard-interactive")) {
						authenticated = this.connection.authenticateWithKeyboardInteractive(credentials.getLogin(), new InteractiveCallback() {

							public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception {
								Thread.sleep(1); // give opportunity to be interrupted
								String prompts[] = new String[numPrompts];
								for (int i = 0; i < numPrompts; i++) {
									prompts[i] = credentials.getPassword();
								}
								return prompts;
							}

						});
						if (!authenticated) {
							this.log.error("keyboard-interactive authentication failed. Partial={}", connection.isAuthenticationPartialSuccess());
						}
					} else {
						this.log.error("Password or keyboard-interactive authentication not supported by server {}", server);
						throw new IOException("Password or keyboard-interactive authentication not supported by server " + server.getName());
					}
				}

				if (!authenticated) {
					this.log.error("authentication failed");
					throw new ConduitAuthenticationPromptPasswordException(ref, this.server.getName(), credentials.getLogin(), credentials.getAccessLevel().toString(), "login failed.");
				}

				if (!connection.isAuthenticationComplete() || connection.isAuthenticationPartialSuccess()) {
					this.log.error("connect: partial or incomplete authentication");
					throw new ConduitAuthenticationPromptPasswordException(ref, this.server.getName(), credentials.getLogin(), credentials.getAccessLevel().toString(), "login incomplete, cannot proceed");
				}
			} catch (IOException e) {
				this.connection.close();
				throw e;
			} catch (InterruptedException e) {
				this.connection.close();
				throw e;
			} catch (ConduitAuthenticationPromptPasswordException e) {
				this.connection.close();
				throw e;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerConduit#close()
	 */
	@Override
	public void close() {
		if (this.connection != null) {
			this.log.debug("disconnect");
			this.connection.close();
			this.connection = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduit#executeCommand(java.lang.String, long)
	 */
	@Override
	protected int executeCommand(String command, long inactivityTimeout) throws InterruptedException, ConduitException {
		this.log.debug("executeCommand {}", command);
		this.lastOutput = StringUtils.EMPTY;
		int exitCode;
		if (StringUtils.isEmpty(command)) {
			throw new ConduitException("No command defined for this operation");
		}
		Thread.sleep(1); // give opportunity to be interrupted
		Session session = null;
		try {
			session = this.connection.openSession();
			this.log.trace("executeCommand execCommand");
			session.execCommand(command);
			this.log.trace("executeCommand read response");
			StringBuilder response = new StringBuilder();
			InputStream stdout = session.getStdout();
			InputStream stderr = session.getStderr();
			byte[] buffer = new byte[1024];
			while (true) {
				Thread.sleep(1); // give opportunity to be interrupted
				if (stdout.available() == 0 && stderr.available() == 0) {
					int conditions = session.waitForCondition(ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA | ChannelCondition.EOF, inactivityTimeout);
					if ((conditions & ChannelCondition.TIMEOUT) != 0) {
						this.log.error("executeCommand ssh read timeout");
						response.append("*** console read timeout ***");
						break;
						// throw new ConduitException("ssh read timeout");
					}
					if ((conditions & (ChannelCondition.EOF | ChannelCondition.STDOUT_DATA | ChannelCondition.STDERR_DATA)) == ChannelCondition.EOF) {
						break;
					}
				}
				if (stderr.available() > 0) {
					int len = session.getStderr().read(buffer);
					if (len < 0) {
						throw new InternalErrorException("ssh read error");
					}
					this.log.trace("executeCommand partial-read stderr='{}'", new String(buffer, 0, len));
					response.append(new String(buffer, 0, len));
				}
				if (stdout.available() > 0) {
					int len = session.getStdout().read(buffer);
					if (len < 0) {
						throw new InternalErrorException("ssh read error");
					}
					this.log.trace("executeCommand partial-read stdout='{}'", new String(buffer, 0, len));
					response.append(new String(buffer, 0, len));
				}
			}
			session.close();
			if (response.length() == 0) {
				this.log.debug("executeCommand no response data");
			}
			Thread.sleep(1); // give opportunity to be interrupted
			exitCode = saveOutputAndExtractCode(response);
		} catch (IOException e) {
			throw new InternalErrorException(e);
		} finally {
			if (session != null) {
				session.close();
			}
		}
		this.log.debug("executeCommand ok");
		return exitCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.Conduit#sendFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void sendFile(String localFileUrl, String remoteDir, String remoteName) throws IOException, InterruptedException {
		Thread.sleep(1); // give opportunity to be interrupted
		this.log.debug("sendFile {}", localFileUrl);
		SCPClient scp = connection.createSCPClient();
		this.log.debug("sendFile put");
		int pathType = this.platformService.getPathType(getServer().getPlatform());
		String remotePlatformDir = PathUtils.quotePathIfNeeded(PathUtils.platformPath(remoteDir, pathType));
		if (remoteName == null)
			scp.put(localFileUrl, remotePlatformDir, "0644");
		else {
			scp.put(localFileUrl, PathUtils.quotePathIfNeeded(remoteName), remotePlatformDir, "0644");
		}
		this.log.debug("sendFile ok");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.Conduit#retrieveFile(java.lang.String, java.lang.String)
	 */
	@Override
	public void retrieveFile(String localDir, String remoteFile) throws IOException {
		this.log.debug("scpGet {}", remoteFile);
		SCPClient scp = this.connection.createSCPClient();
		scp.get(PathUtils.quotePathIfNeeded(remoteFile), localDir);
		this.log.debug("scpGet ok");
	}
}
