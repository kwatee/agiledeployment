/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.io.IOException;

import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ConduitFactory;
import net.kwatee.agiledeployment.conduit.ServerInstance;

abstract class AbstractShellConduitFactory implements ConduitFactory {

	final private static ShellConduitCache conduitCache = new ShellConduitCache();
	private long normalCommandTimeout = 300000L;
	private long slowCommandTimeout = 300000L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ConduitFactory#getNewInstance(net.kwatee.agiledeployment.core.conduit.ServerInstance)
	 */
	final public Conduit getNewInstance(final ServerInstance server, String rootDir) throws IOException, InterruptedException {
		AbstractShellConduit conduit = (AbstractShellConduit) conduitCache.get(server);
		if (conduit == null) {
			conduit = createNewInstance(server, rootDir);
			conduit.setNormalCommandTimeout(this.normalCommandTimeout);
			conduit.setSlowCommandTimeout(this.slowCommandTimeout);
			conduitCache.add(server, conduit);
		}
		return conduit;
	}

	protected abstract AbstractShellConduit createNewInstance(ServerInstance server, String rootDir) throws IOException, InterruptedException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.conduit.ConduitFactory#evictServerConnections(net.kwatee.agiledeployment.server.poolserver.PoolServerInstance)
	 */
	public void evictServerConnections(ServerInstance server) {
		conduitCache.evictServerConnections(server);
	}

	/**
	 * Bean property that defines the time (in seconds) before a 'normal' command will be aborted. Typically set in
	 * applicationContext.xml
	 * 
	 * @param timeout
	 */
	public void setCommandTimeout(final long timeout) {
		this.normalCommandTimeout = timeout;
	}

	/**
	 * Bean property that defines the time (in seconds) before a 'slow' command will be aborted. Typically set in
	 * applicationContext.xml
	 * 
	 * @param timeout
	 */
	public void setSlowCommandTimeout(final long timeout) {
		this.slowCommandTimeout = timeout;
	}
}
