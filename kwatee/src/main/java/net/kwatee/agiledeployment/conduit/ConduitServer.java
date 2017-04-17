/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.Map;

public interface ConduitServer {

	/**
	 * 
	 * @return the id of the conduit server
	 */
	Long getId();

	/**
	 * 
	 * @return the name of the conduit server
	 */
	String getName();

	/**
	 * 
	 * @return the conduit type of the conduit server
	 */
	String getConduitType();

	/**
	 * 
	 * @return the pool type of the conduit server
	 */

	String getPoolType();

	/**
	 * 
	 * @return the concurency level of the pool
	 */
	int getPoolConcurrency();

	/**
	 * 
	 * @return the platform type of the pool
	 */
	Integer getPlatform();

	/**
	 * 
	 * @return the ip address of the server
	 */
	String getIPAddress();

	/**
	 * 
	 * @return the port of the server
	 */
	int getPort();

	/**
	 * 
	 * @return the properties of the pool type
	 */
	Map<String, String> getServerProperties();
}
