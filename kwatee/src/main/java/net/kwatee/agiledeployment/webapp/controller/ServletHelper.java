/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.utils.Charsets;
import org.apache.commons.compress.utils.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ServletHelper {

	static public void sendData(HttpServletResponse res, String data, int status) {
		try {
			byte[] bytes = data.toString().getBytes(Charsets.UTF_8);
			res.setStatus(status);
			res.setContentType("text/plain");
			res.setContentLength(bytes.length);
			OutputStream out = res.getOutputStream();
			out.write(bytes);
		} catch (IOException e) {
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public static void sendJsonObject(HttpServletResponse res, JSONObject json) {
		sendJsonObject(res, json, HttpServletResponse.SC_OK);
	}

	public static void sendJsonObject(HttpServletResponse res, JSONObject json, int status) {
		try {
			byte[] data = json.toString().getBytes(Charsets.UTF_8);
			res.setContentType("application/json");
			res.setContentLength(data.length);
			res.getOutputStream().write(data);
			res.setStatus(status);
		} catch (IOException e) {
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public static void sendJsonObject(HttpServletResponse res, JSONArray json) {
		sendJsonObject(res, json, HttpServletResponse.SC_OK);
	}

	static void sendJsonObject(HttpServletResponse res, JSONArray json, int status) {
		try {
			byte[] data = json.toString().getBytes(Charsets.UTF_8);
			res.setContentType("application/json");
			res.setContentLength(data.length);
			res.getOutputStream().write(data);
			res.setStatus(status);
		} catch (IOException e) {
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Send the file <code>file</code> with name <code>sendAsFileName</code>
	 * 
	 * @param res
	 * @param stream
	 * @param length
	 * @param sendAsFileName
	 * @param mimeType
	 * @throws IOException
	 */
	public static void sendFile(
			HttpServletResponse res,
			InputStream stream,
			int length,
			String sendAsFileName,
			String mimeType) throws IOException {
		if (sendAsFileName != null) {
			res.setHeader("Content-Disposition", "attachment; filename=\"" + sendAsFileName + "\"");
		}
		res.setContentLength(length);
		if (mimeType != null) {
			res.setContentType(mimeType);
		}
		ServletOutputStream out = res.getOutputStream();
		IOUtils.copy(stream, out);
	}

	/**
	 * 
	 * @param res
	 * @param stream
	 * @param length
	 * @param variablePattern
	 */
	public static void sendHighlightedText(
			HttpServletResponse res,
			InputStream stream,
			int length,
			Pattern variablePattern
			) {
		try {
			byte[] buffer = new byte[length];
			stream.read(buffer);
			StringBuilder data = new StringBuilder(new String(buffer, Charsets.UTF_8).replace("<", "&lt;"));
			data.insert(0, "<html><head><style type='text/css'>span.highlight {background-color:yellow;}</style></head><body><pre>");
			Matcher m = variablePattern.matcher(data);
			int offset = 0;
			while (m.find(offset)) {
				data.insert(m.end(), "</span>");
				data.insert(m.start(), "<span class='highlight'>");
				offset = m.end() + 30;
			}
			data.append("</pre></body></html>");
			res.setContentType("text/html");
			ServletOutputStream out = res.getOutputStream();
			buffer = data.toString().getBytes(Charsets.UTF_8);
			res.setContentLength(buffer.length);
			out.write(buffer);
			res.setStatus(HttpServletResponse.SC_OK);
		} catch (IOException e) {
			res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

}
