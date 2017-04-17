/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.IOException;

import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ConduitFactory;
import net.kwatee.agiledeployment.conduit.ServerInstance;

public class FileSystemConduitFactory implements ConduitFactory {

	final private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(this.getClass());

	static public String getId() {
		return "fileSystem";
	}

	static public String getDisplayName() {
		return "Local file system";
	}

	@Override
	public Conduit getNewInstance(ServerInstance server, String rootDir) throws IOException, InterruptedException {
		this.log.debug("Request file system conduit for server " + server);
		return new FileSystemConduit(server, rootDir);
	}

	@Override
	public void evictServerConnections(ServerInstance server) {}
}
