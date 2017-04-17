/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.test.api;

import static org.junit.Assert.*;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

public class EnvironmentTest extends APITestBase {

	@Test
	public void getEnvironments() throws IOException, JSONException {
		String json = api.getEnvironments();
		JSONArray jsonEnvironments = new JSONArray(json);
		for (int i = 0; i < jsonEnvironments.length(); i++) {
			JSONObject e = jsonEnvironments.getJSONObject(i);
			if (e.getString("name").equals(ENVIRONMENT)) {
				assertTrue(e.has("description"));
				return;
			}
		}
		assertTrue(false);
	}

	@Test
	public void getEnvironment() throws IOException, JSONException {
		String json = api.getEnvironment(ENVIRONMENT);
		JSONObject jsonEnvironment = new JSONObject(json);
		assertEquals(jsonEnvironment.get("name"), ENVIRONMENT);
		assertTrue(jsonEnvironment.has("description"));
		assertTrue(jsonEnvironment.getBoolean("sequentialDeployment"));
		assertTrue(jsonEnvironment.has("servers"));
		JSONArray servers = jsonEnvironment.getJSONArray("servers");
		assertEquals(servers.length(), 1);
		assertEquals(servers.get(0), SERVER);
		assertTrue(jsonEnvironment.has("artifacts"));
		JSONArray artifacts = jsonEnvironment.getJSONArray("artifacts");
		assertEquals(artifacts.length(), 1);
		assertEquals(artifacts.get(0), ARTIFACT);
		assertTrue(jsonEnvironment.has("releases"));
		JSONArray releases = jsonEnvironment.getJSONArray("releases");
		assertEquals(releases.length(), 2);
		assertEquals(releases.getJSONObject(0).get("name"), "snapshot");
		assertTrue(releases.getJSONObject(0).has("editable"));
		assertFalse(releases.getJSONObject(1).has("editable"));
	}

	@Test
	public void getRelease() throws IOException, JSONException {
		String json = api.getRelease(ENVIRONMENT, "acme-1.0");
		JSONObject jsonRelease = new JSONObject(json);
		assertEquals(jsonRelease.get("name"), "acme-1.0");
		assertTrue(jsonRelease.has("description"));
		assertEquals(jsonRelease.getJSONArray("defaultArtifacts").length(), 1);
		assertEquals(jsonRelease.getJSONArray("serverArtifacts").length(), 1);
	}

	@Test
	public void createUpdateAndDeleteEnvironment() throws JSONException, IOException {
		String environmentName = "createUpdateAndDeleteEnvironment";
		/*
		 * Create
		 */
		int environmentCount = new JSONArray(api.getEnvironments()).length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "test");
		api.createEnvironment(environmentName, jsonProperties.toString());
		assertEquals(new JSONArray(api.getEnvironments()).length(), environmentCount + 1);
		JSONObject jsonEnvironment = new JSONObject(api.getEnvironment(environmentName));
		assertEquals(jsonEnvironment.get("description"), "test");
		assertEquals(jsonEnvironment.getJSONArray("artifacts").length(), 0);
		assertEquals(jsonEnvironment.getJSONArray("servers").length(), 0);
		assertEquals(jsonEnvironment.getJSONArray("releases").length(), 1);
		JSONObject release = new JSONObject(api.getRelease(environmentName, null));
		assertEquals(release.getJSONArray("defaultArtifacts").length(), 0);
		assertEquals(release.getJSONArray("serverArtifacts").length(), 0);
		/*
		 * Update
		 */
		jsonProperties = new JSONObject();
		jsonProperties.put("description", "test2");
		api.updateEnvironment(environmentName, jsonProperties.toString());
		jsonEnvironment = new JSONObject(api.getEnvironment(environmentName));
		assertEquals(jsonEnvironment.get("description"), "test2");

		/*
		 * Delete
		 */
		api.deleteEnvironment(environmentName);
		assertEquals(new JSONArray(api.getEnvironments()).length(), environmentCount);
	}

	@Test
	public void duplicateAndDeleteEnvironment() throws JSONException, IOException {
		String environmentName = "duplicateAndDeleteEnvironment";
		/*
		 * Duplicate
		 */
		int environmentCount = new JSONArray(api.getEnvironments()).length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "test");
		api.duplicateEnvironment(ENVIRONMENT, environmentName, jsonProperties.toString());
		assertEquals(new JSONArray(api.getEnvironments()).length(), environmentCount + 1);
		JSONObject jsonEnvironment = new JSONObject(api.getEnvironment(environmentName));
		assertEquals(jsonEnvironment.get("description"), "test");
		assertEquals(jsonEnvironment.getJSONArray("artifacts").length(), 1);
		assertEquals(jsonEnvironment.getJSONArray("servers").length(), 1);
		assertEquals(jsonEnvironment.getJSONArray("releases").length(), 1);
		JSONObject release = new JSONObject(api.getRelease(ENVIRONMENT, null));
		assertEquals(release.getJSONArray("defaultArtifacts").length(), 1);
		assertEquals(release.getJSONArray("serverArtifacts").length(), 1);

		/*
		 * Delete
		 */
		api.deleteEnvironment(environmentName);
		assertEquals(new JSONArray(api.getEnvironments()).length(), environmentCount);
	}
}
