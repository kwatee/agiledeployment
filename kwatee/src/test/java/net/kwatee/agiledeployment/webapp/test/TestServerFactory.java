/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import java.util.Collection;
import java.util.Map;

import net.kwatee.agiledeployment.conduit.DefaultServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.conduit.ServerInstanceFactory;

public class TestServerFactory implements ServerInstanceFactory {

	static public String getId() {
		return "test";
	}

	static public String getDisplayName() {
		return "Test pool";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerPoolFactory#getServerPool(java.Object)
	 */
	public Collection<ServerInstance> getServerPool(Map<String, String> properties) throws ServerInstanceException {
		final Collection<ServerInstance> servers = new java.util.ArrayList<ServerInstance>(5);
		for (int i = 1; i <= 5; i++) {
			final String name = "server_" + i;
			final String ipAddress = "ip" + i;
			final ServerInstance s = new DefaultServerInstance(name, ipAddress, 0);
			servers.add(s);
		}
		return servers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerPoolFactory#getUninstantiatedPoolServers(java.util.Map)
	 */
	public Map<String, String> getUninstantiatedServers(final Map<String, String> properties) throws ServerInstanceException {
		throw new ServerInstanceException("Not supported");
	}

	public Map<String, String> getElementList(String listType, Map<String, String> properties) throws ServerInstanceException {
		return null;
	}
}
