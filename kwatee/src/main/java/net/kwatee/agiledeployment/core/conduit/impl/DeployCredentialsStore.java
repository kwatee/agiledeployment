/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.conduit.impl;

import java.util.ArrayList;
import java.util.Iterator;

import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.DeployCredentials;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

/**
 * This class manages interactively provided user names/passwords for server ssh connectivity
 */
public class DeployCredentialsStore {

	final static private long VALIDITY_TIME = 3600000;

	final private ArrayList<CredentialCacheItem> cache = new ArrayList<>();

	private class CredentialCacheItem {

		String deploymentName;
		String userName;
		String serverName;
		DeployCredentials credentials;
		long timestamp;
	}

	/**
	 * Implementation of <code>UserCredentialsVault</code>.
	 */
	public synchronized void removeInteractiveCredentials(String deploymentName, String userName, String serverName) {
		if (!removeFromCache(deploymentName, userName, serverName, true)) {
			removeFromCache(deploymentName, userName, null, true);
		}
	}

	/**
	 * Implementation of <code>UserCredentialsVault</code>.
	 */
	public synchronized void remove(String userName) {
		removeFromCache(userName);
	}

	/**
	 * 
	 * Update or create server credentials.
	 * 
	 * @param serverName
	 *            the server for which these credentials are valid, all servers in the environment when <code>null</code>
	 * @param credentials
	 *            new credentials
	 */
	public synchronized void updateServerCredentials(String serverName, DeployCredentials credentials) {
		removeExpiredEntries();
		// No need to store credentials for offline access
		if (credentials.getAccessLevel().ordinal() > AccessLevel.OFFLINE.ordinal()) {
			CredentialCacheItem c = getCached(serverName);
			if (c == null) {
				c = new CredentialCacheItem();
				c.serverName = serverName;
				cache.add(c);
			}
			c.credentials = credentials;
		}
	}

	/**
	 * Update or create interactively obtained credentials.
	 * 
	 * @param deploymentName
	 *            the name of the environment in which these credentials are valid
	 * @param userName
	 *            the userName for whom these credentials are valid
	 * @param serverName
	 *            the server for which these credentials are valid, all servers in the environment when <code>null</code>
	 * @param credentials
	 *            new credentials
	 */
	public synchronized void updateInteractiveCredentials(String deploymentName, String userName, String serverName, DeployCredentials credentials) {
		CredentialCacheItem c = getCached(deploymentName, userName, serverName);
		if (c == null) {
			c = new CredentialCacheItem();
			c.deploymentName = deploymentName;
			c.userName = userName;
			c.serverName = serverName;
			cache.add(c);
		}
		c.timestamp = System.currentTimeMillis() + VALIDITY_TIME;
		c.credentials = credentials;
	}

	/**
	 * Retrieves the first suitable credentials available in the store.
	 * 1. Interactively obtained, server-specific if exist
	 * 2. otherwise, interactively obtained, environment-wide if exist
	 * 3. otherwise, default server credentials if exist
	 * 4. otherwise, null
	 * 
	 * @param deploymentName
	 *            the id of the environment in which these credentials are valid
	 * @param userName
	 *            the userName for whom these credentials are valid
	 * @param serverName
	 *            the server for which these credentials are valid, all servers in the environment when <code>null</code>
	 * @param accessLevel
	 *            the minimum access the credentials must match
	 * @return credentials or null if not found
	 */
	public synchronized DeployCredentials getCredentials(String deploymentName, String userName, String serverName, AccessLevel accessLevel) {
		CredentialCacheItem c = getCached(deploymentName, userName, serverName);
		if (c == null || c.credentials.getAccessLevel().ordinal() < accessLevel.ordinal()) {
			c = getCached(deploymentName, userName, null);
		}
		if (c == null) {
			c = getCached(serverName);
		}
		if (c == null || c.credentials.getAccessLevel().ordinal() < accessLevel.ordinal()) {
			return null;
		}
		return c.credentials;
	}

	/**
	 * Retrieves the default credentials for server <code>serverName</code>.
	 * 
	 * @param serverName
	 * @return the cached credentials or null if not found
	 */
	private CredentialCacheItem getCached(final String serverName) {
		return (CredentialCacheItem) CollectionUtils.find(cache, new Predicate() {

			@Override
			public boolean evaluate(Object c) {
				CredentialCacheItem cacheItem = (CredentialCacheItem) c;
				return cacheItem.serverName != null && cacheItem.serverName.equals(serverName) && !cacheItem.credentials.interactivelyObtained();
			}
		});
	}

	/**
	 * Retrieves the credentials of user <code>userId</code> for server <code>serverName</code>. The validity timer is reset every time the credentials are accessed
	 * 
	 * @param deploymentName
	 * @param userId
	 * @param serverName
	 * @return the cached credentials or null if not found
	 */
	private CredentialCacheItem getCached(final String deploymentName, final String userName, final String serverName) {
		CredentialCacheItem cred = (CredentialCacheItem) CollectionUtils.find(cache, new Predicate() {

			@Override
			public boolean evaluate(Object c) {
				CredentialCacheItem cacheItem = (CredentialCacheItem) c;
				if (cacheItem.userName != null && cacheItem.userName.equals(cacheItem.userName) && deploymentName.equals(cacheItem.deploymentName)) {
					if ((cacheItem.serverName == null && serverName == null || cacheItem.serverName != null && cacheItem.serverName.equals(serverName)) &&
							cacheItem.credentials.interactivelyObtained()) {
						return true;
					}
				}
				return false;
			}
		});
		if (cred != null) {
			cred.timestamp = System.currentTimeMillis() + VALIDITY_TIME;
		}
		return cred;
	}

	private void removeExpiredEntries() {
		final long expiryTime = System.currentTimeMillis();
		CollectionUtils.filter(cache, new Predicate() {

			@Override
			public boolean evaluate(Object cacheItem) {
				return (((CredentialCacheItem) cacheItem).timestamp == 0 || expiryTime < ((CredentialCacheItem) cacheItem).timestamp);
			}
		});
	}

	/**
	 * Remove the credentials of user <code>userId</code> on server <code>serverName</code>. If <code>serverName</code> is null, all the user's cached credentials are removed
	 * 
	 * @param userId
	 * @return
	 */
	private boolean removeFromCache(String userName) {
		boolean found = false;
		for (Iterator<CredentialCacheItem> it = cache.iterator(); it.hasNext();) {
			CredentialCacheItem c = it.next();
			if (c.userName != null && c.userName.equals(userName)) {
				it.remove();
				found = true;
			}
		}
		return found;
	}

	/**
	 * Remove the credentials of user <code>userId</code> on server <code>serverName</code>. If <code>serverName</code> is null, all the user's cached credentials are removed
	 * 
	 * @param deploymentName
	 * @param userName
	 * @param serverName
	 * @param interactive
	 * @return
	 */
	private boolean removeFromCache(String deploymentName, String userName, String serverName, boolean interactive) {
		boolean found = false;
		for (Iterator<CredentialCacheItem> it = cache.iterator(); it.hasNext();) {
			CredentialCacheItem c = it.next();
			if (c.userName.equals(userName) && deploymentName.equals(deploymentName) && c.credentials.interactivelyObtained() == interactive && serverName.equals(c.serverName)) {
				it.remove();
				found = true;
			}
		}
		return found;
	}
}
