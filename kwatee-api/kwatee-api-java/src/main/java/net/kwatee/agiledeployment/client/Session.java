/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

class Session {

	private String serviceUrl;
	private String authToken;

	Session(String serviceUrl) throws IOException {
		this.serviceUrl = serviceUrl;
		if (!this.serviceUrl.endsWith("/"))
			this.serviceUrl += "/";
		this.serviceUrl += "api";
	}

	String doLogin(String login, String password) throws IOException {
		try {
			Properties properties = new Properties();
			InputStream in = this.getClass().getResourceAsStream("/net/kwatee/agiledeployment/client/api.properties");
			properties.load(in);
			in.close();
			String apiVersion = properties.getProperty("kwatee_api_version");

			URL url = new URL(this.serviceUrl + "/authenticate/" + login + "?version=" + apiVersion);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setRequestProperty("X-API-AUTH", password);
			connection.setRequestMethod("POST");
			if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				this.authToken = Utils.loadString(connection);
				return this.authToken;
			}
			throw new IOException(getErrorMessage(connection));
		} catch (IOException e) {
			throw new IOException(e.getMessage());
		}
	}

	void doLogout() {
		try {
			openConnection("POST", "/signout", null);
		} catch (IOException e) {}
		this.authToken = null;
	}

	void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	String getAuthToken() {
		return this.authToken;
	}

	HttpURLConnection openConnection(String method, String path, Map<String, String> params) throws IOException {
		String paramString = "";
		if (params != null && !params.isEmpty()) {
			for (Iterator<Entry<String, String>> it = params.entrySet().iterator(); it.hasNext();) {
				paramString += paramString.isEmpty() ? "?" : "&";
				Map.Entry<String, String> p = it.next();
				paramString += p.getKey() + '=' + URLEncoder.encode(p.getValue(), "UTF-8");
			}
		}
		URL url = new URL(this.serviceUrl + path + paramString);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod(method);
		if (this.authToken != null)
			connection.setRequestProperty("X-API-AUTH", this.authToken);
		return connection;
	}

	boolean isLocalHost() {
		return this.serviceUrl.contains("localhost");
	}

	String getErrorMessage(HttpURLConnection connection) {
		int contentLength = connection.getContentLength();
		if (contentLength > 0) {
			try {
				InputStream in = connection.getErrorStream();
				byte buffer[] = new byte[contentLength];
				in.read(buffer);
				return new String(buffer);
			} catch (IOException e) {}
		}
		try {
			return connection.getResponseMessage();
		} catch (IOException e) {}
		return "unknown error";
	}
}
