/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit.impl;

import java.io.IOException;

import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ConduitFactory;
import net.kwatee.agiledeployment.conduit.ServerInstance;

public class TestConduitFactory implements ConduitFactory {

	static public String getId() {
		return "test";
	}

	static public String getDisplayName() {
		return "Test conduit";
	}

	@Override
	public Conduit getNewInstance(ServerInstance server, String rootDir) throws IOException, InterruptedException {
		return new TestConduit(server, rootDir);
	}

	@Override
	public void evictServerConnections(ServerInstance server) {}
}
