/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.deploy.PlatformService;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

abstract class AbstractShellConduit implements Conduit {

	final protected PlatformService platformService;
	final protected ServerInstance server;
	final protected String rootDir;
	private org.slf4j.Logger log;
	protected String lastOutput;
	private long normalCommandTimeout;
	private long slowCommandTimeout;
	protected DeployCredentials credentials;
	private boolean justCreated;
	private VariableResolver resolver;

	/**
	 * @param server
	 */
	protected AbstractShellConduit(ServerInstance server, String rootDir) {
		this.server = server;
		this.rootDir = rootDir;
		this.lastOutput = StringUtils.EMPTY;
		this.justCreated = true;
		this.platformService = PlatformService.getInstance();
		this.log = org.slf4j.LoggerFactory.getLogger(this.getClass());
	}

	@Override
	public void setVariableResolver(VariableResolver resolver) {
		this.resolver = resolver;
	}

	protected void setNormalCommandTimeout(long commandTimeout) {
		this.normalCommandTimeout = commandTimeout;
	}

	protected long getNormalCommandTimeout() {
		return this.normalCommandTimeout;
	}

	protected void setSlowCommandTimeout(long slowCommandTimeout) {
		this.slowCommandTimeout = slowCommandTimeout;
	}

	protected long getSlowCommandTimeout() {
		return this.slowCommandTimeout;
	}

	@Override
	public ServerInstance getServer() {
		return this.server;
	}

	@Override
	public void open(String ref, DeployCredentials credentials) throws IOException, ConduitAuthenticationFailedException, InterruptedException {
		if (credentials == null || credentials.getPassword() == null) {
			this.log.error("Missing or insufficient credentials");
			throw new ConduitAuthenticationPromptPasswordException(
					ref,
					this.server.getName(),
					credentials == null ? null : credentials.getLogin(),
					credentials == null ? null : credentials.getAccessLevel().toString(),
					"missing or insufficient credentials");
		}
		this.credentials = credentials;
	}

	@Override
	public boolean newlyCreated() {
		return this.justCreated;
	}

	protected int sendCommand(String command) throws InterruptedException, ConduitException {
		return sendCommand(command, false);
	}

	protected int sendCommand(String command, boolean useSudo) throws InterruptedException, ConduitException {
		if (useSudo)
			command = "sudo " + command;
		int exitCode = executeCommand(command, this.normalCommandTimeout);
		log.debug(command + " / " + exitCode);
		return exitCode;
	}

	@Override
	public String getLastCommandOutput() {
		return this.lastOutput;
	}

	abstract protected int executeCommand(String command, long timeout) throws InterruptedException, ConduitException;

	protected int saveOutputAndExtractCode(StringBuilder response) {
		Pattern p = Pattern.compile(KWATEE_RESULT_PATTERN);
		// Create a matcher with an input string
		Matcher m = p.matcher(response);
		int exitCode;
		String code = null;
		if (m.find()) {
			code = m.group(1);
			response.delete(m.start(), m.end() + 1);
			int idx = response.indexOf("\n", m.start());
			if (idx >= 0) {
				response.setLength(idx);
			}
		}
		if (code != null) {
			if (code.equalsIgnoreCase("error")) {
				exitCode = Conduit.KWATEE_ERROR;
			} else if (code.equalsIgnoreCase("ok")) {
				exitCode = Conduit.KWATEE_RESULT_OK;
			} else if (code.equalsIgnoreCase("running")) {
				exitCode = Conduit.KWATEE_RESULT_RUNNING;
			} else if (code.equalsIgnoreCase("was_running")) {
				exitCode = Conduit.KWATEE_RESULT_WAS_RUNNING;
			} else if (code.equalsIgnoreCase("stopped")) {
				exitCode = Conduit.KWATEE_RESULT_STOPPED;
			} else if (code.equalsIgnoreCase("was_stopped")) {
				exitCode = Conduit.KWATEE_RESULT_WAS_STOPPED;
			} else if (code.equalsIgnoreCase("no_descriptor_error")) {
				exitCode = Conduit.KWATEE_NO_DESCRIPTOR_ERROR;
			} else if (code.equalsIgnoreCase("tampering_error")) {
				exitCode = Conduit.KWATEE_TAMPERING_ERROR;
			} else if (code.equalsIgnoreCase("out_of_date_error")) {
				exitCode = Conduit.KWATEE_OUT_OF_DATE_ERROR;
			} else {
				try {
					int c = Integer.parseInt(code);
					exitCode = c == 0 ? Conduit.KWATEE_RESULT_OK : Conduit.KWATEE_ERROR;
				} catch (NumberFormatException e) {
					exitCode = Conduit.KWATEE_RESULT_UNDEFINED;
				}
			}
		} else {
			exitCode = Conduit.KWATEE_RESULT_UNDEFINED;
		}
		this.lastOutput = StringUtils.trim(response.toString().replaceAll("\r(\n)?", "\n"));
		return exitCode;
	}

	@Override
	public String remoteDiagnostics(DeployCredentials credentials) throws InterruptedException, IOException, ConduitAuthenticationFailedException, ConduitException {
		this.log.debug("remoteDiagnostics");
		open(null, credentials);
		String result;
		try {
			String command = this.platformService.getVerifyPlatformCommands(
					this.server.getPlatform(),
					this.resolver);
			if (command == null) {
				result = "No diagnostics on this platform";
			} else {
				sendCommand(command);
				result = this.lastOutput;
			}
		} finally {
			close();
		}
		this.log.debug("remoteDiagnostics ok");
		return result;
	}

	@Override
	public void installAgent() throws InterruptedException, MissingVariableException, ConduitException {
		makeSystemDir(this.rootDir);
		String localAgentUrl = this.platformService.getPlatformAgentUrl(this.server.getPlatform(), true);
		// HACK: include resource size in url
		long size = this.platformService.getAgentFileSize(this.server.getPlatform());
		localAgentUrl = localAgentUrl.replaceAll("jar:", "jar(size=" + size + "):");
		try {
			sendFile(localAgentUrl, this.rootDir, null);
		} catch (IOException ioe) {
			this.log.error("checkSiteStructure(sendFile)", ioe);
			throw new ConduitException("Failed to update agent: " + ioe.getMessage());
		}
		String command = this.platformService.getMakeAgentExecutableCommand(this.server.getPlatform(), this.resolver);
		if (StringUtils.isNotEmpty(command)) {
			if (sendCommand(command) != Conduit.KWATEE_RESULT_OK) {
				this.log.error("checkSiteStructure make agent executable failed");
				throw new ConduitException("Failed to make agent executable " + this.lastOutput);
			}
		}
	}

	private String sendDescriptorFile(String type, String xml) throws InterruptedException, ConduitException {
		File descriptorFile = null;
		OutputStream out = null;
		try {
			descriptorFile = File.createTempFile("kwatee", "." + type);
			out = new FileOutputStream(descriptorFile);
			IOUtils.write(xml, out, Charsets.UTF_8);
			IOUtils.closeQuietly(out);
			out = null;
			sendFile(descriptorFile.getAbsolutePath(), this.rootDir, descriptorFile.getName());
			return descriptorFile.getName();
		} catch (IOException ioe) {
			IOUtils.closeQuietly(out);
			this.log.error("installDescriptor", ioe);
			throw new ConduitException("Failed to deploy descriptor: " + ioe.getMessage());
		} finally {
			if (descriptorFile != null)
				descriptorFile.delete();
		}
	}

	@Override
	public void updateDeploymentDescriptor(String deploymentName, String descriptorXml) throws InterruptedException, MissingVariableException, ConduitException {
		String name = sendDescriptorFile("deployment", descriptorXml);
		String command = this.platformService.getAgentUpdateDescriptorCommand(
				this.server.getPlatform(),
				deploymentName,
				null,
				name,
				this.resolver);
		int exitCode = sendCommand(command);
		if (exitCode != Conduit.KWATEE_RESULT_OK) {
			throw new ConduitException("Failed to deploy descriptors: " + this.lastOutput);
		}
	}

	@Override
	public void updatePackageDescriptor(String deploymentName, String packageName, String descriptorXml) throws InterruptedException, MissingVariableException, ConduitException {
		String name = sendDescriptorFile("artifact", descriptorXml);
		String command = this.platformService.getAgentUpdateDescriptorCommand(
				this.server.getPlatform(),
				deploymentName,
				packageName,
				name,
				this.resolver);
		int exitCode = sendCommand(command);
		if (exitCode != Conduit.KWATEE_RESULT_OK) {
			throw new ConduitException("Failed to deploy descriptors: " + this.lastOutput);
		}
	}

	@Override
	/**
	 * Invokes the agent on <code>server</code> to determine its version
	 * 
	 * @throws InterruptedException
	 */
	public String retrieveRemoteAgentVersion() throws InterruptedException, MissingVariableException, ConduitException {
		String command = this.platformService.getAgentVersionCommand(
				this.server.getPlatform(),
				this.resolver);
		if (sendCommand(command) == Conduit.KWATEE_RESULT_OK) {
			int idx = this.lastOutput.lastIndexOf('\n');
			return idx < 0 ? this.lastOutput : this.lastOutput.substring(idx + 1);
		}
		return null;
	}

	@Override
	public String getRemoteDescriptor(String deploymentName, String packageName) throws InterruptedException, MissingVariableException, ConduitException {
		String command = this.platformService.getAgentDescriptorCommand(
				this.server.getPlatform(),
				deploymentName,
				packageName,
				this.resolver);
		int exitCode = sendCommand(command);
		if (exitCode != Conduit.KWATEE_RESULT_OK && exitCode != Conduit.KWATEE_NO_DESCRIPTOR_ERROR && exitCode != Conduit.KWATEE_TAMPERING_ERROR) {
			this.log.error("retrieveRemoteDescriptor failed");
			throw new ConduitException("Failed to retrieve remote descriptor: " + this.lastOutput);
		}
		if (exitCode == Conduit.KWATEE_RESULT_OK) {
			return this.lastOutput;
		}
		if (exitCode == KWATEE_NO_DESCRIPTOR_ERROR)
			return StringUtils.EMPTY;
		return null;
	}

	@Override
	public void checkIntegrity(String deploymentName, String packageName, String signature, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException {
		String command = this.platformService.getAgentIntegrityCheckCommand(
				this.server.getPlatform(),
				deploymentName,
				packageName,
				signature,
				this.resolver);
		int exitCode = sendCommand(command, useSudo);
		if (exitCode != Conduit.KWATEE_RESULT_OK) {
			throw new ConduitException("Integrity check failed: " + this.lastOutput);
		}
	}

	@Override
	public void deployPackage(String deploymentName, String packageName, String deployArchiveFile, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException {
		String remoteArchiveName = deploymentName + ".kwatee";
		try {
			sendFile(deployArchiveFile, this.rootDir, remoteArchiveName);
		} catch (IOException ioe) {
			throw new ConduitException("ERROR - failed to upload archive: " + ioe.getMessage());
		}

		// uncompress package
		String command = this.platformService.getAgentDeployCommand(
				this.server.getPlatform(),
				deploymentName,
				this.server.getName(),
				packageName,
				this.resolver);
		int exitCode = sendCommand(command, useSudo);
		if (exitCode != Conduit.KWATEE_RESULT_OK) {
			throw new ConduitException("Failed to install package: " + this.lastOutput);
		}
	}

	@Override
	public int undeployPackage(String deploymentName, String packageName, boolean skipIntegrityCheck, boolean force, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException {
		String command = platformService.getAgentUndeployCommand(
				this.server.getPlatform(),
				deploymentName,
				packageName,
				skipIntegrityCheck,
				force,
				this.resolver);
		int exitCode = sendCommand(command, useSudo);
		return exitCode;
	}

	@Override
	public int executeRemoteAction(String action, String actionParams, String deploymentName, String packageName, boolean skipIntegrityCheck, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException {
		String command = platformService.getRemoteActionCommand(
				this.server.getPlatform(),
				action,
				actionParams,
				deploymentName,
				packageName,
				skipIntegrityCheck,
				this.resolver);
		int exitCode = sendCommand(command, useSudo);
		return exitCode;
	}

	private void makeSystemDir(String dir) throws InterruptedException, MissingVariableException, ConduitException {
		String command = this.platformService.getMakeSystemDirCommand(
				this.server.getPlatform(),
				dir,
				this.resolver);
		if (sendCommand(command) != Conduit.KWATEE_RESULT_OK) {
			throw new ConduitException("Failed to create remote kwatee directory " + dir + " (" + this.lastOutput + ')');
		}
	}
}
