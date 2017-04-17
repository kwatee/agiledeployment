/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.util.Map;

public class ServerUtils {

	static public Map<String, String> getServerProperties(Object parent, String prefix) {
		Map<String, String> properties = new java.util.HashMap<String, String>();
		for (String propName : ((ConduitServer) parent).getServerProperties().keySet()) {
			if (prefix == null || propName.startsWith(prefix)) {
				properties.put(propName, ((ConduitServer) parent).getServerProperties().get(propName));
			}
		}
		return properties;
	}

	static public int getPort(Object parent) {
		return ((ConduitServer) parent).getPort();
	}
}
