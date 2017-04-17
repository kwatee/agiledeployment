/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.utils.PathUtils;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.deploy.PlatformService;

import org.apache.commons.io.FileUtils;
import org.springframework.util.StringUtils;

class FileSystemConduit implements Conduit {

	final private org.slf4j.Logger log;
	final private PlatformService platformService;
	final protected ServerInstance server;
	final private String rootDir;
	protected String lastOutput;

	/**
	 * @param server
	 */
	public FileSystemConduit(ServerInstance server, String rootDir) {
		this.server = server;
		this.rootDir = rootDir;
		this.platformService = PlatformService.getInstance();
		this.log = org.slf4j.LoggerFactory.getLogger(FileSystemConduit.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduit#open(java.lang.String, net.kwatee.agiledeployment.core.model.ServerCredentials)
	 */
	@Override
	public void open(String ref, final DeployCredentials credentials) throws IOException, InterruptedException, ConduitAuthenticationFailedException {
		this.log.debug("connect to {}", this.server);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerConduit#close()
	 */
	@Override
	public void close() {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduit#executeCommand(java.lang.String, long)
	 */
	private int executeCommandInDir(List<String[]> commands, File dir, long inactivityTimeout) throws InterruptedException {
		this.lastOutput = "";
		int exitCode;
		Thread.sleep(1); // give opportunity to be interrupted
		Process shell = null;
		StringBuilder response = new StringBuilder();
		try {
			for (String[] command : commands) {
				if (this.log.isDebugEnabled()) {
					StringBuilder s = new StringBuilder("Execute command:");
					for (String c : command) {
						s.append(' ');
						s.append(c);
					}
					this.log.debug(s.toString());
				}
				ProcessBuilder pb = new ProcessBuilder(command);
				pb.directory(dir);
				shell = pb.start();
				try {
					shell.waitFor();
				} catch (InterruptedException e) {
					this.log.error("executeCommand read timeout");
					response.append("*** console read timeout ***");
				}
				this.log.debug("executeCommand read response");
				InputStream stdout = shell.getInputStream();
				InputStream stderr = shell.getErrorStream();
				byte[] buffer = new byte[1024];
				while (stderr.available() > 0) {
					int len = stderr.read(buffer);
					if (len < 0) {
						throw new InternalErrorException("executeCommand read error stderr");
					}
					this.log.debug("executeCommand partial-read stderr='{}'", new String(buffer, 0, len));
					response.append(new String(buffer, 0, len));
				}
				while (stdout.available() > 0) {
					int len = stdout.read(buffer);
					if (len < 0) {
						this.log.error("executeCommand read error");
						throw new InternalErrorException("executeCommand read error stdout");
					}
					this.log.debug("executeCommand partial-read stdout='{}'", new String(buffer, 0, len));
					response.append(new String(buffer, 0, len));
				}
				if (response.length() == 0) {
					this.log.debug("executeCommand no response data");
				}
			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		} finally {
			if (shell != null) {
				shell.destroy();
			}
		}
		exitCode = saveOutputAndExtractCode(response);
		this.log.debug("executeCommand ok");
		return exitCode;
	}

	private int saveOutputAndExtractCode(StringBuilder response) {
		Pattern p = Pattern.compile(Conduit.KWATEE_RESULT_PATTERN);
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
		this.lastOutput = StringUtils.trimWhitespace(response.toString().replaceAll("\r(\n)?", "\n"));
		return exitCode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.Conduit#sendFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void sendFile(String localFileUrl, String remoteDir, String remoteName) throws IOException, InterruptedException {
		this.log.debug("sendFile {}", localFileUrl);
		File localFile = new File(localFileUrl);
		int pathType = this.platformService.getPathType(getServer().getPlatform());
		String remotePlatformDir = PathUtils.platformPath(remoteDir, pathType);
		if (remoteName == null) {
			remoteName = localFile.getName();
		}
		File remoteFile = new File(new File(remotePlatformDir), remoteName);
		FileUtils.copyFile(localFile, remoteFile);
		remoteFile.setReadable(true);
		remoteFile.setWritable(true);
		this.log.debug("sendFile ok");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.Conduit#retrieveFile(java.lang.String, java.lang.String)
	 */
	@Override
	public void retrieveFile(String localDir, String remotePath) throws IOException {
		this.log.debug("scpGet {}", remotePath);
		File remoteFile = new File(remotePath);
		FileUtils.copyFile(remoteFile, new File(new File(localDir), remoteFile.getName()));
		this.log.debug("scpGet ok");
	}

	@Override
	public void setVariableResolver(VariableResolver variableResolver) {}

	@Override
	public boolean newlyCreated() {
		return true;
	}

	@Override
	public ServerInstance getServer() {
		return this.server;
	}

	@Override
	public String getLastCommandOutput() {
		return this.lastOutput;
	}

	@Override
	public String remoteDiagnostics(DeployCredentials credentials) throws InterruptedException, IOException {
		return "Ok";
	}

	@Override
	public String getRemoteDescriptor(String deploymentName, String packageName) throws InterruptedException, MissingVariableException, ConduitException {
		File agentFile = getAgentFile();
		String[] command = {
				agentFile.getAbsolutePath(),
				"get-descriptor",
				"-d",
				deploymentName,
				"-a",
				packageName
		};
		List<String[]> commands = new ArrayList<>(1);
		commands.add(command);
		int exitCode = executeCommandInDir(commands, null, 0);
		if (exitCode != Conduit.KWATEE_RESULT_OK && exitCode != Conduit.KWATEE_NO_DESCRIPTOR_ERROR && exitCode != Conduit.KWATEE_TAMPERING_ERROR) {
			throw new ConduitException("Failed to retrieve descriptor");
		}
		if (exitCode == Conduit.KWATEE_RESULT_OK) {
			return this.lastOutput;
		}
		return null;
	}

	@Override
	public void installAgent() throws InterruptedException, MissingVariableException {
		File agentFile = getAgentFile();
		makeSystemDir(agentFile.getParent());
		makeSystemDir(this.rootDir);
		String localAgentUrl = this.platformService.getPlatformAgentUrl(this.server.getPlatform(), true);
		try {
			URL url = new URL(localAgentUrl);
			FileUtils.copyURLToFile(url, agentFile, 2000, 2000);
			agentFile.setExecutable(true, true);
		} catch (IOException ioe) {
			throw new InternalErrorException(ioe);
		}
	}

	private void updateDescriptor(String dir, String name, String xml) throws InterruptedException, ConduitException {
		File descriptorFile = new File(new File(this.rootDir, dir), name);
		this.log.debug("updateDescriptor: " + descriptorFile.getAbsolutePath());
		try {
			FileUtils.write(descriptorFile, xml);
		} catch (IOException ioe) {
			this.log.error("updateDescriptor", ioe);
			throw new ConduitException("Failed to update descriptor: " + ioe.getMessage());
		}
	}

	@Override
	public void updateDeploymentDescriptor(String deploymentName, String descriptorXml) throws InterruptedException, ConduitException {
		updateDescriptor(deploymentName, deploymentName + ".deployment", descriptorXml);
	}

	@Override
	public void updatePackageDescriptor(String deploymentName, String packageName, String descriptorXml) throws InterruptedException, ConduitException {
		updateDescriptor(deploymentName, packageName + ".artifact", descriptorXml);
	}

	private void makeSystemDir(String dir) throws InterruptedException {
		new File(dir).mkdirs();
	}

	@Override
	public String retrieveRemoteAgentVersion() throws InterruptedException, MissingVariableException, ConduitException {
		File agentFile = getAgentFile();
		if (!agentFile.exists()) {
			return null;
		}
		String[] command = {agentFile.getAbsolutePath(), "--version"};// { agentFile.getAbsolutePath(), "--version" };
		List<String[]> commands = new ArrayList<>(1);
		commands.add(command);
		if (Conduit.KWATEE_RESULT_OK != executeCommandInDir(commands, agentFile.getParentFile(), 0)) {
			throw new ConduitException("Failed to get version");
		}
		return this.lastOutput;
	}

	@Override
	public void deployPackage(String deploymentName, String packageName, String packageFile, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException {
		File agentFile = getAgentFile();
		String[] command = {
				agentFile.getAbsolutePath(),
				"deploy",
				"-s",
				this.server.getName(),
				"-d",
				deploymentName,
				"-a",
				packageName,
				"-f",
				packageFile
		};
		List<String[]> commands = new ArrayList<>(1);
		commands.add(command);
		if (Conduit.KWATEE_RESULT_OK != this.executeCommandInDir(commands, null, 0)) {
			throw new ConduitException("Failed to deploy package");
		}
	}

	@Override
	public void checkIntegrity(String deploymentName, String packageName, String signature, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException {
		File agentFile = getAgentFile();
		String[] command = {
				agentFile.getAbsolutePath(),
				"check-integrity",
				"-d",
				deploymentName,
				"-a",
				packageName,
				"--signature",
				signature
		};
		List<String[]> commands = new ArrayList<>(1);
		commands.add(command);
		int exitCode = this.executeCommandInDir(commands, null, 0);
		if (exitCode != Conduit.KWATEE_RESULT_OK) {
			throw new ConduitException("Failed to check package integrity");
		}
	}

	@Override
	public int executeRemoteAction(String action, String ignoredActionParams, String deploymentName, String packageName, boolean skipIntegrityCheck, boolean useSudo) throws InterruptedException, MissingVariableException {
		File agentFile = getAgentFile();
		String[] command;
		if (packageName != null) {
			command = new String[] {
					agentFile.getAbsolutePath(),
					"action",
					"--id",
					action,
					"-d",
					deploymentName,
					"-a",
					packageName,
					"--noIntegrityCheck",
					skipIntegrityCheck ? "true" : "false",
			};
		} else {
			command = new String[] {
					agentFile.getAbsolutePath(),
					"action",
					"--id",
					action,
					"-d",
					deploymentName
			};
		}
		List<String[]> commands = new ArrayList<>(1);
		commands.add(command);
		int exitCode = this.executeCommandInDir(commands, null, 0);
		return exitCode;
	}

	@Override
	public int undeployPackage(String deploymentName, String packageName, boolean skipIntegrityCheck, boolean force, boolean useSudo) throws InterruptedException, MissingVariableException {
		File agentFile = getAgentFile();
		String[] command = {
				agentFile.getAbsolutePath(),
				"undeploy",
				"-d",
				deploymentName,
				"-a",
				packageName,
				"--force",
				force ? "true" : "false",
				"--noIntegrityCheck",
				skipIntegrityCheck ? "true" : "false"
		};
		List<String[]> commands = new ArrayList<>(1);
		commands.add(command);
		return executeCommandInDir(commands, null, 0);
	}

	private File getAgentFile() throws MissingVariableException {
		String agentName = this.platformService.getAgentExecutableName(this.server.getPlatform());
		return new File(this.rootDir, agentName);
	}
}
