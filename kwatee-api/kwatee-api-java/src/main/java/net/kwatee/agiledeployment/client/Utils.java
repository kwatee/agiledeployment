/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;

class Utils {

	static String loadString(HttpURLConnection connection) throws IOException {
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte[2048];
		int len;
		InputStream is = connection.getInputStream();
		while ((len = is.read(buffer)) > 0) {
			sb.append(new String(buffer, 0, len, "UTF8"));
		}
		is.close();
		return sb.toString();
	}

	static void sendString(HttpURLConnection connection, String string) throws IOException {
		if (string == null)
			string = "";
		connection.setDoOutput(true);
		OutputStream os = connection.getOutputStream();
		byte[] bytes = string.getBytes("UTF8");
		os.write(bytes);
		os.close();
	}

	static void sendJSONString(HttpURLConnection connection, String string) throws IOException {
		connection.setRequestProperty("Content-Type", "application/json");
		sendString(connection, string);
	}

	static void copyStream(HttpURLConnection connection, OutputStream os) throws IOException {
		InputStream is = connection.getInputStream();
		try {
			byte[] buffer = new byte[4096];
			int len;
			while ((len = is.read(buffer)) > 0) {
				os.write(buffer, 0, len);
			}
		} finally {
			os.close();
			is.close();
		}
	}

	static void sendFile(HttpURLConnection connection, File file) throws IOException {
		String boundary = "---------------------------KWATEE-"+System.currentTimeMillis();
		connection.addRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);
		connection.setDoOutput(true);
		
		OutputStream outputStream = connection.getOutputStream();
		DataOutputStream request = new DataOutputStream(outputStream);

		request.writeBytes("--" + boundary + "\r\n");
		request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
		request.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName()) + "\r\n");
		request.writeBytes("Content-Transfer-Encoding: binary\r\n");
		request.writeBytes("\r\n");
		request.flush();
		FileInputStream inputStream = new FileInputStream(file);
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = inputStream.read(buffer)) > 0) {
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
		inputStream.close();
		request.writeBytes("\r\n\r\n");
		request.flush();
		request.writeBytes("--" + boundary + "--\r\n");
		request.close();
	}
}
