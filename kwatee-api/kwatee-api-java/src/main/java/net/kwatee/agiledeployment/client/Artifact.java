/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

class Artifact {

	static String getArtifacts(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getArtifact(Session session, String artifactName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts/" + artifactName + ".json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updateArtifact(Session session, String artifactName, String properties) throws IOException {
		if (properties != null && !properties.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/artifacts/" + artifactName, null);
			try {
				Utils.sendJSONString(connection, properties);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void createArtifact(Session session, String artifactName, String properties) throws IOException {
		HttpURLConnection connection = session.openConnection("POST", "/artifacts/" + artifactName, null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void deleteArtifact(Session session, String artifactName) throws IOException {
		HttpURLConnection connection = session.openConnection("DELETE", "/artifacts/" + artifactName, null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String getVersion(Session session, String artifactName, String versionName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts/" + artifactName + "/" + versionName + ".json", null);
		try {
			connection.setDoOutput(true);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updateVersion(Session session, String artifactName, String versionName, String json) throws IOException {
		if (json != null && !json.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/artifacts/" + artifactName + "/" + versionName, null);
			try {
				Utils.sendJSONString(connection, json);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void createVersion(Session session, String artifactName, String versionName, String properties) throws IOException {
		HttpURLConnection connection = session.openConnection("POST", "/artifacts/" + artifactName + "/" + versionName, null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void duplicateVersion(Session session, String artifactName, String versionName, String duplicateFrom, String properties) throws IOException {
		if (duplicateFrom == null)
			throw new IOException("Missing duplicate template");
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("duplicateFrom", duplicateFrom);
		HttpURLConnection connection = session.openConnection("POST", "/artifacts/" + artifactName + "/" + versionName, params);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void deleteVersion(Session session, String artifactName, String versionName) throws IOException {
		HttpURLConnection connection = session.openConnection("DELETE", "/artifacts/" + artifactName + "/" + versionName, null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void uploadPackage(Session session, String artifactName, String versionName, File packageFile, boolean deleteOverlays) throws IOException {
		Map<String, String> params = null;
		if (deleteOverlays) {
			params = new java.util.HashMap<String, String>();
			params.put("deleteOverlays", "true");
		}

		HttpURLConnection connection = session.openConnection("POST", "/artifacts/" + artifactName + "/" + versionName + "/package", params);
		try {
			Utils.sendFile(connection, packageFile);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void uploadPackage(Session session, String artifactName, String versionName, String packageFileOrUrl, boolean deleteOverlays) throws IOException {
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("url", packageFileOrUrl);
		if (deleteOverlays) {
			params.put("deleteOverlays", "true");
		}

		HttpURLConnection connection = session.openConnection("POST", "/artifacts/" + artifactName + "/" + versionName + "/package", params);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void deletePackage(Session session, String artifactName, String versionName) throws IOException {
		HttpURLConnection connection = session.openConnection("DELETE", "/artifacts/" + artifactName + "/" + versionName + "/package", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String getPackageFiles(Session session, String artifactName, String versionName, String path) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts/" + artifactName + "/" + versionName + "/files.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getSpecialFiles(Session session, String artifactName, String versionName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts/" + artifactName + "/" + versionName + "/specialFiles.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updatePackageFileProperties(Session session, String artifactName, String versionName, String path, String properties) throws IOException {
		if (properties != null && !properties.isEmpty()) {
			Map<String, String> params = new java.util.HashMap<String, String>();
			params.put("path", path);
			HttpURLConnection connection = session.openConnection("PUT", "/artifacts/" + artifactName + "/" + versionName + "/package/file", params);
			try {
				Utils.sendJSONString(connection, properties);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void downloadPackageFile(Session session, String artifactName, String versionName, String path, File downloadFile) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void uploadPackageOverlay(Session session, String artifactName, String versionName, String path, File uploadFile) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void uploadPackageOverlay(Session session, String artifactName, String versionName, String path, String uploadUrl) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void deletePackageOverlay(Session session, String artifactName, String versionName, String path) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static String getVariables(Session session, String artifactName, String versionName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts/" + artifactName + "/" + versionName + "/variables.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updateVariables(Session session, String artifactName, String versionName, String variables) throws IOException {
		if (variables != null && !variables.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/artifacts/" + artifactName + "/" + versionName + "/variables", null);
			try {
				Utils.sendJSONString(connection, variables);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void importBundle(Session session, File bundleFile) throws IOException {
		throw new IOException("Not implemented yet");
	}

	static void importBundle(Session session, String bundleUrl) throws IOException {
		throw new IOException("Not implemented yet");
	}
}
