/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

public interface ServerInstance {

	// String SERVER_PLATFORM = "kwatee_server_platform";
	String SERVER_INSTANCE = "kwatee_server_instance";
	String SERVER_NAME = "kwatee_server_name";
	String SERVER_IP = "kwatee_server_ip";

	/**
	 * Returns the underlying server common to all instances as an opaque object
	 * 
	 * @return parent
	 */
	ConduitServer getParent();

	void setParent(ConduitServer parent);

	/**
	 * Returns the name of the underlying server common to all instances
	 * 
	 * @return The name of the underlying server common to all instances
	 */
	String getName();

	/**
	 * The conduit type of the underlying server common to all instances
	 * 
	 * @return The conduit type of the underlying server common to all instances
	 */
	String getConduitType();

	/**
	 * Returns the platform of the underlying server common to all instances
	 * 
	 * @return The platform of the underlying server common to all instances
	 */
	Integer getPlatform();

	/**
	 * Returns an instance property and if not found a server property
	 * 
	 * @param name
	 * @return the property <code>name</code> or null if not found
	 */
	String getProperty(String name);

	/**
	 * Returns the name of this instance
	 * 
	 * @return the name of this instance
	 */
	String getInstanceName();

	/**
	 * Returns the IP address of this instance
	 * 
	 * @return the IP address of this instance
	 */
	String getIPAddress();

	/**
	 * Returns the port of this instance
	 * 
	 * @return the port of this instance
	 */
	int getPort();

	/**
	 * Provides the opportunity to the server instance to
	 * execute an optional action before the start of any
	 * artifact deploy on this instance
	 * 
	 * @param statusWriter
	 *            {@link net.kwatee.agiledeployment.conduit.StatusWriter} instance for writing optional UI status messages
	 * @throws ServerInstanceException
	 */
	void preSetup(StatusWriter statusWriter) throws ServerInstanceException;

	/**
	 * Provides the opportunity to the server instance to
	 * execute an optional action after the end of all
	 * artifact deploy on this instance
	 * 
	 * @param statusWriter
	 *            {@link net.kwatee.agiledeployment.conduit.StatusWriter} instance for writing optional UI status messages
	 * @throws ServerInstanceException
	 */
	void postSetup(StatusWriter statusWriter) throws ServerInstanceException;

	/**
	 * Provides the opportunity to the server instance to
	 * execute an optional action before the start of any
	 * artifact undeploy on this instance
	 * 
	 * @param statusWriter
	 *            {@link net.kwatee.agiledeployment.conduit.StatusWriter} instance for writing optional UI status messages
	 * @throws ServerInstanceException
	 */
	void preCleanup(StatusWriter statusWriter) throws ServerInstanceException;

	/**
	 * Provides the opportunity to the server instance to
	 * execute an optional action after the end of all
	 * artifact undeploy on this instance
	 * 
	 * @param statusWriter
	 *            {@link net.kwatee.agiledeployment.conduit.StatusWriter} instance for writing optional UI status messages
	 * @throws ServerInstanceException
	 */
	void postCleanup(StatusWriter statusWriter) throws ServerInstanceException;
}
