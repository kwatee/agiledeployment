/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.io.IOException;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.core.conduit.ConduitException;

public interface Conduit {

	String KWATEE_RESULT_PATTERN = "\\[kwatee_([0-9A-Za-z_]+)\\]";

	int KWATEE_SYSTEM_ERROR = -666;
	int KWATEE_RESULT_OK = 0;
	int KWATEE_ERROR = 4200;
	int KWATEE_NO_DESCRIPTOR_ERROR = KWATEE_ERROR + 1;
	int KWATEE_TAMPERING_ERROR = KWATEE_NO_DESCRIPTOR_ERROR + 1;
	int KWATEE_OUT_OF_DATE_ERROR = KWATEE_TAMPERING_ERROR + 1;

	int KWATEE_RESULT_UNDEFINED = 4240;
	int KWATEE_RESULT_RUNNING = 4243;
	int KWATEE_RESULT_WAS_RUNNING = 4244;
	int KWATEE_RESULT_STOPPED = 4245;
	int KWATEE_RESULT_WAS_STOPPED = 4246;

	/**
	 * Open the connection using the provided {@link DeployCredentials}
	 * 
	 * @param ref
	 *            A batch operation reference or null
	 * @param credentials
	 *            {@link DeployCredentials} provided to establish the connection
	 * @throws IOException
	 * @throws ConduitAuthenticationException
	 * @throws InterruptedException
	 */
	void open(String ref, DeployCredentials credentials) throws IOException, ConduitAuthenticationFailedException, InterruptedException;

	// void setReleaseContext(Object releaseContext);

	// void setServerContext(Object serverContext);

	void setVariableResolver(VariableResolver resolver);

	/**
	 * Close the connection
	 */
	void close();

	/**
	 * @return true if the session was just created, false if it is retrieved from an active cache
	 */
	boolean newlyCreated();

	/**
	 * 
	 * @return the output of the last command
	 */
	// String lastCommandOutput();

	/**
	 * Send a file over the conduit
	 * 
	 * @param localFile
	 *            to be sent
	 * @param remoteDir
	 *            to which the file will be sent
	 * @param remoteName
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void sendFile(String localFile, String remoteDir, String remoteName) throws IOException, InterruptedException;

	/**
	 * Retrieve a file over the conduit
	 * 
	 * @param localDir
	 *            in which to store the file
	 * @param remoteFile
	 *            to download
	 * @throws IOException
	 */
	void retrieveFile(String localDir, String remoteFile) throws IOException;

	/**
	 * Returns the underlying {@link ServerInstance}
	 * 
	 * @return The underlying {@link ServerInstance}
	 */
	ServerInstance getServer();

	String getLastCommandOutput();

	String remoteDiagnostics(DeployCredentials credentials) throws InterruptedException, IOException, ConduitAuthenticationFailedException, ConduitException;

	void installAgent() throws InterruptedException, MissingVariableException, ConduitException;

	void updateDeploymentDescriptor(String deploymentName, String deploymentDescriptorXml) throws InterruptedException, MissingVariableException, ConduitException;

	void updatePackageDescriptor(String deploymentName, String packageName, String packageDescriptorXml) throws InterruptedException, MissingVariableException, ConduitException;

	String retrieveRemoteAgentVersion() throws InterruptedException, MissingVariableException, ConduitException;

	/**
	 * 
	 * @param deploymentName
	 * @param packageName
	 * @return The remote deployment descriptor in xml. Empty string if no descriptor is found
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws ConduitException
	 */
	String getRemoteDescriptor(String deploymentName, String packageName) throws InterruptedException, MissingVariableException, ConduitException;

	/**
	 * Returns only if the artifact is properly installed without tampering
	 * 
	 * @param deploymentName
	 * @param packageName
	 * @param signature
	 * @param useSudo
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws ConduitException
	 */
	void checkIntegrity(String deploymentName, String packageName, String signature, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException;

	void deployPackage(String deploymentName, String packageName, String packageFile, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException;

	int undeployPackage(String deploymentName, String packageName, boolean skipIntegrityCheck, boolean force, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException;

	/**
	 * 
	 * @param action
	 * @param actionParams
	 * @param deploymentName
	 * @param packageName
	 * @param skipIntegrityCheck
	 * @param useSudo
	 * @return exit code of remote action
	 * @throws InterruptedException
	 * @throws MissingVariableException
	 * @throws ConduitException
	 */
	int executeRemoteAction(String action, String actionParams, String deploymentName, String packageName, boolean skipIntegrityCheck, boolean useSudo) throws InterruptedException, MissingVariableException, ConduitException;
}
