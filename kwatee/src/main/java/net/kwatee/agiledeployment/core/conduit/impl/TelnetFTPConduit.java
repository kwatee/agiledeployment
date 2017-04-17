/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.regex.Pattern;

import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.utils.PathUtils;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.conduit.ConduitException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;

class TelnetFTPConduit extends AbstractShellConduit {

	private TelnetClient telnetConnection;
	private InputStream telnetIn;
	private OutputStream telnetOut;
	final private StringBuilder lastBuffer;

	final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

	/**
	 * @param server
	 */
	public TelnetFTPConduit(ServerInstance server, String rootDir) {
		super(server, rootDir);
		this.lastBuffer = new StringBuilder();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduit#open(java.lang.String, net.kwatee.agiledeployment.core.conduit.DeployCredentials)
	 */
	@Override
	public void open(String ref, DeployCredentials credentials) throws IOException, InterruptedException, ConduitAuthenticationFailedException {
		super.open(ref, credentials);
		this.log.debug("connect to {}", this.server);
		this.telnetConnection = new TelnetClient();
		TelnetOptionHandler echoopt = new EchoOptionHandler(true, false, false, false);
		TelnetOptionHandler ttopt = new TerminalTypeOptionHandler("tty", true, false, false, false);
		try {
			this.telnetConnection.addOptionHandler(echoopt);
			this.telnetConnection.addOptionHandler(ttopt);
		} catch (InvalidTelnetOptionException e1) {}
		this.telnetConnection.setConnectTimeout(3000);
		this.telnetConnection.connect(this.server.getIPAddress(), this.server.getPort());
		this.telnetConnection.setKeepAlive(true);
		this.telnetIn = this.telnetConnection.getInputStream();
		this.telnetOut = this.telnetConnection.getOutputStream();
		String loginPrompt = getServer().getProperty("login_prompt");
		if (loginPrompt == null) {
			loginPrompt = "login:";
		}
		log.debug("login prompt '{}'", loginPrompt);
		String passwordPrompt = getServer().getProperty("password_prompt");
		if (passwordPrompt == null) {
			passwordPrompt = "password:";
		}
		log.debug("password prompt '{}'", passwordPrompt);

		try {
			this.log.debug("authenticate");
			waitForPrompt(loginPrompt, 0);
			if (!loginPrompt.isEmpty() && this.lastBuffer.indexOf(loginPrompt) < 0) {
				throw new IOException("No login prompt");
			}
			sendData(credentials.getLogin());
			waitForPrompt(passwordPrompt, 0);
			if (!passwordPrompt.isEmpty() && this.lastBuffer.indexOf(passwordPrompt) < 0) {
				throw new IOException("No password prompt");
			}
			sendData(credentials.getPassword());
			waitForPrompt(StringUtils.EMPTY, 0);
			if (!loginPrompt.isEmpty() && this.lastBuffer.toString().contains(loginPrompt)) {
				// prompting for password again, there is an error
				throw new IOException("Invalid credentials");
			}
		} catch (IOException e) {
			this.telnetConnection.disconnect();
			this.telnetConnection = null;
			throw e;
		} catch (InterruptedException e) {
			this.telnetConnection.disconnect();
			this.telnetConnection = null;
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.Conduit#close()
	 */
	@Override
	public void close() {
		if (this.telnetConnection != null) {
			try {
				this.telnetConnection.disconnect();
			} catch (IOException e) {}
			this.telnetConnection = null;
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
		command = command.replaceAll("(?<!\r)\n", "\r\n"); // replace \n not preceded by \r by \r\n
		Thread.sleep(1); // give opportunity to be interrupted
		try {
			if (command.startsWith("\"\"") && command.endsWith("\"")) {
				command = command.substring(1, command.length() - 1);
			}
			this.log.debug("executeCommand execute:", command);
			sendData(command);
			this.log.debug("executeCommand read response");
			waitForPrompt(KWATEE_RESULT_PATTERN, command.length());
			this.lastBuffer.delete(0, command.length() + 2); // delete echoed command line (including crlf
			exitCode = saveOutputAndExtractCode(this.lastBuffer);
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
		this.log.debug("executeCommand ok");
		return exitCode;
	}

	private boolean waitForPrompt(String promptOk, int offset) throws InterruptedException, IOException {
		Pattern patternOk = promptOk == null ? null : Pattern.compile(promptOk);
		long timeout = System.currentTimeMillis() + getNormalCommandTimeout();
		log.debug("timeout={}", Long.toString(getNormalCommandTimeout()));
		this.lastBuffer.setLength(0);
		byte buffer[] = new byte[1000];
		while (true) {
			Thread.sleep(100);
			if (this.telnetIn.available() == 0) {
				if (System.currentTimeMillis() > timeout) {
					if (this.lastBuffer.length() > 0) {
						return false;
					}
					throw new IOException("timeout " + promptOk);
				}
			} else {
				int len = this.telnetIn.read(buffer);
				if (len < 0) {
					throw new IOException("telnet read error");
				}
				String newData = new String(buffer);
				log.debug("Received data:\n{}", newData);
				this.lastBuffer.append(newData);
				if (promptOk != null && promptOk.isEmpty()) {
					return true;
				}
				if (offset == 0) {
					if (patternOk != null && patternOk.matcher(this.lastBuffer).find()) {
						return true;
					}
				} else {
					len = this.lastBuffer.length();
					if (len > offset) {
						if (patternOk != null && patternOk.matcher(this.lastBuffer.subSequence(offset, len)).find()) {
							return true;
						}
					}
				}
				timeout = System.currentTimeMillis() + getNormalCommandTimeout();
			}
		}
	}

	private void sendData(String data) throws IOException {
		this.telnetOut.write((data + "\r\n").getBytes());
		this.telnetOut.flush();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.Conduit#sendFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void sendFile(String localFile, String remoteDir, String remoteName) throws IOException, InterruptedException {
		this.log.debug("sendFile {}", localFile);
		int port;
		try {
			port = Integer.parseInt(getServer().getProperty("ftp_port"));
		} catch (NumberFormatException e) {
			throw new IOException("Bad FTP port");
		}
		FTPClient ftp;
		if (getServer().getProperty("ftps") == null) {
			ftp = new FTPClient();
		} else {
			ftp = new FTPSClient();
		}
		try {
			ftp.connect(this.server.getIPAddress(), port);
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				throw new IOException("FTP server refused connection.");
			}
			if (!ftp.login(this.credentials.getLogin(), this.credentials.getPassword())) {
				throw new IOException("Failed to login to FTP server");
			}
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			ftp.enterLocalPassiveMode();
			InputStream in;
			String fileName;
			if (localFile.startsWith("jar:")) {
				// localFile is a resource within a jar
				in = new URL(localFile).openStream();
				fileName = localFile.substring(localFile.lastIndexOf('/') + 1);
			} else {
				in = new FileInputStream(localFile);
				fileName = new File(localFile).getName();
			}
			String remoteFilePath;
			if (remoteName == null) {
				remoteFilePath = PathUtils.platformPath(remoteDir + '/' + fileName, 0/* PlatformUtils.getPathType(getServer().getPlatform()) */);
			} else {
				remoteFilePath = PathUtils.platformPath(remoteDir + '/' + remoteName, 0/* PlatformUtils.getPathType(getServer().getPlatform()) */);
			}
			int idx = remoteFilePath.indexOf(':');
			if (idx >= 0) {
				remoteFilePath = remoteFilePath.substring(idx + 1);
			}
			boolean success = ftp.storeFile(remoteFilePath, in);
			in.close();
			if (!success) {
				throw new IOException("FTP upload failed: " + remoteFilePath);
			}
		} finally {
			ftp.noop();
			ftp.logout();
			if (ftp.isConnected()) {
				ftp.disconnect();
			}
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
		int port;
		try {
			port = Integer.parseInt(getServer().getProperty("ftp_port"));
		} catch (NumberFormatException e) {
			throw new IOException("Bad FTP port");
		}
		FTPClient ftp;
		if (getServer().getProperty("ftps") == null) {
			ftp = new FTPClient();
		} else {
			ftp = new FTPSClient();
		}
		try {
			ftp.connect(this.server.getIPAddress(), port);
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				throw new IOException("FTP server refused connection.");
			}
			if (!ftp.login(this.credentials.getLogin(), this.credentials.getPassword())) {
				throw new IOException("Failed to login to FTP server");
			}
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			ftp.enterLocalPassiveMode();
			String fileName = new File(remoteFile).getName();
			OutputStream out = new FileOutputStream(localDir + '/' + fileName);
			int pathType = this.platformService.getPathType(getServer().getPlatform());
			boolean success = ftp.retrieveFile(PathUtils.platformPath(remoteFile, pathType), out);
			out.close();
			if (!success) {
				throw new IOException("FTP download failed");
			}
		} finally {
			try {
				ftp.noop();
				ftp.logout();
				if (ftp.isConnected()) {
					ftp.disconnect();
				}
			} catch (IOException e) {}
		}
	}
}