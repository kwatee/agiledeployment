/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.IOException;
import java.net.HttpURLConnection;

class Misc {

	static String getInfoContext(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/info/context.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getInfoPlatforms(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/info/platforms.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getInfoConduitTypes(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/info/conduitTypes.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

	static String getInfoServerPoolTypes(Session session) throws IOException {
		HttpURLConnection connection = session.openConnection("GET", "/info/serverPoolTypes.json", null);
		try {
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				throw new IOException(session.getErrorMessage(connection));
			return Utils.loadString(connection);
		} finally {
			connection.disconnect();
		}
	}

}
