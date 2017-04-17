/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.IOException;

import net.kwatee.agiledeployment.conduit.ServerInstance;

public class SshConduitFactory extends AbstractShellConduitFactory {

	final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

	static public String getId() {
		return "ssh";
	}

	static public String getDisplayName() {
		return "Secure Shell / scp";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduitFactory#createNewInstance(net.kwatee.agiledeployment.server.poolserver.PoolServerInstance)
	 */
	protected AbstractShellConduit createNewInstance(ServerInstance server, String rootDir) throws IOException, InterruptedException {
		this.log.debug("Request ssh conduit for server " + server);
		return new SshConduit(server, rootDir);
	}
}
