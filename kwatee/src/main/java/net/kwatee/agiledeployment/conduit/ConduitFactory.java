/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

import java.io.IOException;

public interface ConduitFactory {

	/**
	 * Returns a {@link Conduit} instance of the {@link ServerInstance}
	 * 
	 * @param server
	 *            the parent {@link ServerInstance}
	 * @param rootDir
	 *            the remote root directory where the agent gets installed and the metadata files are located
	 * @return A {@link Conduit} instance
	 * @throws IOException
	 * @throws KwateeException
	 * @throws InterruptedException
	 */
	Conduit getNewInstance(ServerInstance server, String rootDir) throws IOException, InterruptedException;

	/**
	 * Called when no more connections will be made to the current instance
	 * Clears all potential cached connections to server
	 * 
	 * @param server
	 *            the parent {@link ServerInstance}
	 */
	void evictServerConnections(ServerInstance server);
}
