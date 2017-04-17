/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.IOException;

import net.kwatee.agiledeployment.conduit.ServerInstance;

public class TelnetFTPConduitFactory extends AbstractShellConduitFactory {

	final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

	static public String getId() {
		return "telnetFTP";
	}

	static public String getDisplayName() {
		return "Telnet / FTP";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.impl.AbstractShellConduitFactory#createNewInstance(net.kwatee.agiledeployment.server.poolserver.PoolServerInstance)
	 */
	public AbstractShellConduit createNewInstance(ServerInstance server, String rootDir) throws IOException, InterruptedException {
		this.log.debug("Request telnet/ftp conduit for server " + server);
		return new TelnetFTPConduit(server, rootDir);
	}
}
