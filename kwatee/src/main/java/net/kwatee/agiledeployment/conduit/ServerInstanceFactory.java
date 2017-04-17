/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.Collection;
import java.util.Map;

public interface ServerInstanceFactory {

	/**
	 * Returns a collection of server instances
	 * 
	 * @param properties
	 *            ServerShortDto properties
	 * @return the collection of {@link ServerInstance} children of the serverPool
	 * @throws ServerInstanceException
	 */
	Collection<ServerInstance> getServerPool(Map<String, String> properties) throws ServerInstanceException;

	/**
	 * Returns a map of instance information in the form of [instance name, ip address]. Used to verify the configuration in the user interface.
	 * 
	 * @param properties
	 *            The pool properties
	 * @return a map of instance information in the form of [instance name, ip address]
	 * @throws ServerInstanceException
	 */
	Map<String, String> getUninstantiatedServers(Map<String, String> properties) throws ServerInstanceException;

	/**
	 * Generic function to return pooltype-specific list of elements (e.g region names)
	 * 
	 * @param properties
	 *            The pool properties
	 * @return A map of elements in the form of [element_id, element_name] (element names may be null)
	 * @throws ServerInstanceException
	 */
	Map<String, String> getElementList(String listType, Map<String, String> properties) throws ServerInstanceException;
}
