/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

class Deployment {

	static String getDeployments(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/deployments.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getDeployment(Session session, String environmentName, String releaseName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/deployments/" + environmentName + "/" + Environment.getReleaseName(releaseName) + ".json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void downloadLightweightInstaller(Session session, String environmentName, String releaseName, File downloadFile) throws IOException {
		throw new IOException("No longer supported");
	}

	static void downloadInstaller(Session session, String environmentName, String releaseName, File downloadFile) throws IOException {
		throw new IOException("No longer supported");
	}

	static String manage(Session session, String environmentName, String releaseName, String serverName, String artifactName, String operation, boolean force) throws IOException {
		return manage(session, environmentName, releaseName, serverName, artifactName, operation, false, force);
	}

	static String manage(Session session, String environmentName, String releaseName, String serverName, String artifactName, String operation, boolean skipIntegrityCheck, boolean force) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		if (serverName != null)
			params.put("serverName", serverName);
		if (artifactName != null)
			params.put("artifactName", artifactName);
		if (force)
			params.put("forcedUndeploy", "true");
		HttpURLConnection connection = session.openConnection("POST", "/deployments/" + environmentName + "/" + Environment.getReleaseName(releaseName) + "/" + operation, params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getOngoingOperation(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/deployments/ongoing.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getOperationProgress(Session session, String ref) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("ref", ref);
		HttpURLConnection connection = session.openConnection("GET", "/deployments/progress/status.json", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static int getOperationStatus(Session session, String ref) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("ref", ref);
		HttpURLConnection connection = session.openConnection("GET", "/deployments/progress/status", params);
		try {
			int code = connection.getResponseCode();
			if (code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_GONE || code == HttpURLConnection.HTTP_BAD_REQUEST)
				return code;
			throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String getProgressMessages(Session session, String ref, String serverName, String artifactName) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("ref", ref);
		params.put("serverName", serverName);
		if (artifactName != null)
			params.put("artifactName", artifactName);
		HttpURLConnection connection = session.openConnection("GET", "/deployments/progress/messages.json", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void sendCredentials(Session session, String environmentName, String serverName, boolean sameForAllServers, String creds) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("environmentName", environmentName);
		params.put("serverName", serverName);
		params.put("sameForAll", sameForAllServers ? "true" : "false");
		HttpURLConnection connection = session.openConnection("POST", "/deployments/progress/credentials", params);
		try {
			Utils.sendJSONString(connection, creds);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void manageCancel(Session session, String ref, boolean dontClear) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("ref", ref);
		if (dontClear)
			params.put("dontClear", "true");
		HttpURLConnection connection = session.openConnection("POST", "/deployments/progress/cancel", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

}
