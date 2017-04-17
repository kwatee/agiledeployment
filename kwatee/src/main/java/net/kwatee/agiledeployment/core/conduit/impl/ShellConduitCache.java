/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.util.ArrayList;
import java.util.Iterator;

import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ServerInstance;

class ShellConduitCache {

	final private ArrayList<ShellConduitCacheItem> cache = new ArrayList<>(0);

	synchronized protected AbstractShellConduit get(ServerInstance server) {
		String ref = server.toString();
		for (ShellConduitCacheItem conn : this.cache)
			if (conn.getRef().equals(ref)) {
				conn.setNew(false);
				conn.incRefCount();
				return conn.getConduit();
			}
		return null;
	}

	synchronized protected void release1(Conduit conduit, long conduitTimeToLive) {
		for (ShellConduitCacheItem s : this.cache)
			if (s.getConduit() == conduit) {
				s.decRefCount();
				if (s.getRefCount() == 0) {
					s.setTimeStamp(System.currentTimeMillis() + conduitTimeToLive);
				}
				s.setNew(false);
				return;
			}
		conduit.close();
	}

	synchronized void evictServerConnections(ServerInstance server) {
		// session_timer.cancel();
		String ref = server.toString();
		for (Iterator<ShellConduitCacheItem> it = this.cache.iterator(); it.hasNext();) {
			ShellConduitCacheItem c = it.next();
			if (c.getRef().equals(ref)) {
				c.getConduit().close();
				it.remove();
			}
		}
	}

	synchronized protected void add(ServerInstance server, AbstractShellConduit conduit) {
		ShellConduitCacheItem s = new ShellConduitCacheItem(conduit, server.toString());
		s.incRefCount();
		this.cache.add(s);
	}

	synchronized boolean justCreated(AbstractShellConduit conduit) {
		for (ShellConduitCacheItem c : this.cache) {
			if (c.getConduit() == conduit) {
				return c.isNew();
			}
		}
		return false;
	}
}
