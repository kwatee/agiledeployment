/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit;

import java.util.Collection;
import java.util.Map;

import net.kwatee.agiledeployment.conduit.DefaultServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.conduit.ServerInstanceFactory;

public class ManualServerInstanceFactory implements ServerInstanceFactory {

	final protected static String PROPERTY_PREFIX = "manual.";

	static public String getId() {
		return "manual";
	}

	static public String getDisplayName() {
		return "Enumerated pool";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerInstanceFactory#getServerPool(java.lang.Object)
	 */
	@Override
	public Collection<ServerInstance> getServerPool(Map<String, String> properties) throws ServerInstanceException {
		Collection<ServerInstance> servers = new java.util.ArrayList<ServerInstance>();
		if (properties != null) {
			for (int i = 0; i < 10000; i++) {
				String instanceId = PROPERTY_PREFIX + "instance" + i + ".";
				if (!properties.containsKey(instanceId)) {
					break;
				}
				String[] instance = properties.get(instanceId).split(",");
				if (instance.length < 3) {
					break;
				}

				String elementName = instance[0];
				String ipAddress = instance[1];
				String port = instance[2];
				ServerInstance s = new DefaultServerInstance(elementName, ipAddress, Integer.parseInt(port));
				servers.add(s);
			}
		}
		return servers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerInstanceFactory#getUninstantiatedServers(java.util.Map)
	 */
	@Override
	public Map<String, String> getUninstantiatedServers(final Map<String, String> properties) throws ServerInstanceException {
		throw new ServerInstanceException("Not supported");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ServerInstanceFactory#getElementList(java.lang.String, java.util.Map)
	 */
	@Override
	public Map<String, String> getElementList(String listType, Map<String, String> properties) throws ServerInstanceException {
		return null;
	}
}
