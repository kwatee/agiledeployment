/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

class Environment {

	static private final String SNAPSHOT = "snapshot";

	static String getReleaseName(String releaseName) {
		return releaseName == null ? SNAPSHOT : releaseName;
	}

	static String getEnvironments(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/environments.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getEnvironment(Session session, String environmentName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/environments/" + environmentName + ".json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updateEnvironment(Session session, String environmentName, String properties) throws IOException {
		if (properties != null && !properties.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/environments/" + environmentName, null);
			try {
				Utils.sendJSONString(connection, properties);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void createEnvironment(Session session, String environmentName, String properties) throws IOException {
		HttpURLConnection connection = session.openConnection("POST", "/environments/" + environmentName, null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void duplicateEnvironment(Session session, String environmentName, String duplicateFrom, String properties) throws IOException {
		if (duplicateFrom == null)
			throw new IOException("Missing duplicate template");
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("duplicateFrom", duplicateFrom);
		HttpURLConnection connection = session.openConnection("POST", "/environments/" + environmentName, params);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void deleteEnvironment(Session session, String environmentName) throws IOException {
		HttpURLConnection connection = session.openConnection("DELETE", "/environments/" + environmentName, null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String getRelease(Session session, String environmentName, String releaseName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + ".json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updateRelease(Session session, String environmentName, String releaseName, String properties) throws IOException {
		if (properties != null && !properties.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/environments/" + environmentName + "/" + getReleaseName(releaseName), null);
			try {
				Utils.sendJSONString(connection, properties);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void tagRelease(Session session, String environmentName, String releaseName, String properties) throws IOException {
		HttpURLConnection connection = session.openConnection("POST", "/environments/" + environmentName + "/" + releaseName, null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void reeditRelease(Session session, String environmentName, String releaseName) throws IOException {
		HttpURLConnection connection = session.openConnection("POST", "/environments/" + environmentName + "/" + releaseName + "/reedit", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void deleteRelease(Session session, String environmentName, String releaseName) throws IOException {
		HttpURLConnection connection = session.openConnection("DELETE", "/environments/" + environmentName + "/" + getReleaseName(releaseName), null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String getEffectiveReleaseArtifacts(Session session, String environmentName, String releaseName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/artifacts.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void setArtifactActiveVersion(Session session, String environmentName, String releaseName, String artifactName, String versionName, String serverName) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("artifactName", artifactName);
		params.put("versionName", versionName);
		if (serverName != null)
			params.put("serverName", serverName);
		HttpURLConnection connection = session.openConnection("PUT", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/activeVersion", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String getPackageFiles(Session session, String environmentName, String releaseName, String artifactName, String serverName, String path) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("artifactName", artifactName);
		if (serverName != null)
			params.put("serverName", serverName);
		if (path != null)
			params.put("path", serverName);
		HttpURLConnection connection = session.openConnection("GET", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/package/files.json", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getSpecialFiles(Session session, String environmentName, String releaseName, String artifactName, String serverName) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("artifactName", artifactName);
		if (serverName != null)
			params.put("serverName", serverName);
		HttpURLConnection connection = session.openConnection("GET", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/package/specialFiles.json", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updatePackageFileProperties(Session session, String environmentName, String releaseName, String artifactName, String serverName, String path, String properties) throws IOException {
		if (properties != null && !properties.isEmpty()) {
			Map<String, String> params = new java.util.HashMap<String, String>();
			params.put("artifactName", artifactName);
			params.put("path", path);
			if (serverName != null)
				params.put("serverName", serverName);
			HttpURLConnection connection = session.openConnection("PUT", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/package/file", params);
			try {
				Utils.sendJSONString(connection, properties);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void downloadPackageFile(Session session, String environmentName, String releaseName, String artifactName, String serverName, String path, File downloadFile) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void uploadPackageOverlay(Session session, String environmentName, String releaseName, String artifactName, String serverName, String path, File uploadFile) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void uploadPackageOverlay(Session session, String environmentName, String releaseName, String artifactName, String serverName, String path, String uploadUrl) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void deletePackageOverlay(Session session, String environmentName, String releaseName, String artifactName, String serverName, String path) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static String getVariables(Session session, String environmentName, String releaseName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/variables.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void setVariables(Session session, String environmentName, String releaseName, String variables) throws IOException {
		if (variables != null && !variables.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/environments/" + environmentName + "/" + getReleaseName(releaseName) + "/variables", null);
			try {
				Utils.sendJSONString(connection, variables);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

}
