/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.test.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

public class ServerTest extends APITestBase {

	@Test
	public void getServers() throws IOException, JSONException {
		String json = api.getServers();
		JSONArray jsonServers = new JSONArray(json);
		for (int i = 0; i < jsonServers.length(); i++) {
			JSONObject s = jsonServers.getJSONObject(i);
			if (s.getString("name").equals(SERVER)) {
				assertTrue(s.has("description"));
				return;
			}
		}
		assertTrue(false);
	}

	@Test
	public void getServer() throws IOException, JSONException {
		String json = api.getServer(SERVER);
		JSONObject jsonServer = new JSONObject(json);
		assertEquals(jsonServer.get("name"), SERVER);
		assertTrue(jsonServer.has("description"));
		assertEquals(jsonServer.get("port"), 22);
		assertEquals(jsonServer.get("platform"), 2);
		assertEquals(jsonServer.get("poolConcurrency"), 0);
		assertEquals(jsonServer.get("conduitType"), "ssh");
		assertEquals(jsonServer.get("ipAddress"), "demo.kwatee.net");
		assertTrue(jsonServer.has("properties"));
		assertTrue(jsonServer.has("credentials"));
		JSONObject creds = jsonServer.getJSONObject("credentials");
		assertEquals(creds.get("login"), "kwtest");
	}

	@Test
	public void createUpdateAndDeleteServer() throws JSONException, IOException {
		String serverName = "createUpdateAndDeleteServer";
		/*
		 * Create
		 */
		int serverCount = new JSONArray(api.getServers()).length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "test");
		api.createServer(serverName, jsonProperties.toString());
		assertEquals(new JSONArray(api.getServers()).length(), serverCount + 1);
		JSONObject jsonServer = new JSONObject(api.getServer(serverName));
		assertEquals(jsonServer.get("description"), "test");

		/*
		 * Update
		 */
		jsonProperties = new JSONObject();
		jsonProperties.put("description", "test2");
		api.updateServer(serverName, jsonProperties.toString());
		jsonServer = new JSONObject(api.getServer(serverName));
		assertEquals(jsonServer.get("description"), "test2");

		/*
		 * Delete
		 */
		api.deleteServer(serverName);
		assertEquals(new JSONArray(api.getServers()).length(), serverCount);
	}

	@Test
	public void duplicateAndDeleteServer() throws JSONException, IOException {
		String serverName = "duplicateAndDeleteServer";
		/*
		 * Duplicate
		 */
		int serverCount = new JSONArray(api.getServers()).length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "test");
		api.duplicateServer(SERVER, serverName, jsonProperties.toString());
		assertEquals(new JSONArray(api.getServers()).length(), serverCount + 1);
		JSONObject jsonServer = new JSONObject(api.getServer(serverName));
		assertEquals(jsonServer.get("description"), "test");
		assertEquals(jsonServer.get("port"), 22);
		assertEquals(jsonServer.get("platform"), 1);
		assertEquals(jsonServer.get("poolConcurrency"), 0);
		assertEquals(jsonServer.get("conduitType"), "ssh");
		assertEquals(jsonServer.get("ipAddress"), "localhost");
		assertTrue(jsonServer.has("properties"));
		assertTrue(jsonServer.has("credentials"));

		/*
		 * Delete
		 */
		api.deleteServer(serverName);
		assertEquals(new JSONArray(api.getServers()).length(), serverCount);
	}
}
