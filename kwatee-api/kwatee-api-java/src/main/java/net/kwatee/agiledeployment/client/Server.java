/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

class Server {

	static String getServers(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/servers.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getServer(Session session, String serverName) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/servers/" + serverName + ".json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static void updateServer(Session session, String serverName, String properties) throws IOException {
		if (properties != null && !properties.isEmpty()) {
			HttpURLConnection connection = session.openConnection("PUT", "/servers/" + serverName, null);
			try {
				Utils.sendJSONString(connection, properties);
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
					throw new IOException(session.getErrorMessage(connection));
			} finally {
				connection.disconnect();
			}
		}
	}

	static void createServer(Session session, String serverName, String properties) throws IOException {
		HttpURLConnection connection = session.openConnection("POST", "/servers/" + serverName, null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void duplicateServer(Session session, String serverName, String duplicateFrom, String properties) throws IOException {
		if (duplicateFrom == null)
			throw new IOException("Missing duplicate template");
		Map<String, String> params = new java.util.HashMap<String, String>();
		params.put("duplicateFrom", duplicateFrom);
		HttpURLConnection connection = session.openConnection("POST", "/servers/" + serverName, null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_CREATED)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static void deleteServer(Session session, String serverName) throws IOException {
		HttpURLConnection connection = session.openConnection("DELETE", "/servers/" + serverName, null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
		} finally {
			connection.disconnect();
		}
	}

	static String testConnection(Session session, String serverName, String properties) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/artifacts/" + serverName + "/testConnection", null);
		try {
			Utils.sendJSONString(connection, properties);
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}
}
