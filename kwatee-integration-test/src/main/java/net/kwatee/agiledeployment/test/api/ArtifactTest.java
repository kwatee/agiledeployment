/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.test.api;

import java.io.File;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.*;

import static org.junit.Assert.*;

public class ArtifactTest extends APITestBase {

	@Test
	public void getArtifacts() throws IOException, JSONException {
		String json = api.getArtifacts();
		JSONArray jsonArtifacts = new JSONArray(json);
		for (int i = 0; i < jsonArtifacts.length(); i++) {
			JSONObject a = jsonArtifacts.getJSONObject(i);
			if (a.getString("name").equals(ARTIFACT)) {
				assertTrue(a.has("description"));
				return;
			}
		}
		assertTrue(false);
	}

	@Test
	public void getArtifact() throws IOException, JSONException {
		String json = api.getArtifact(ARTIFACT);
		JSONObject jsonArtifact = new JSONObject(json);
		assertEquals(jsonArtifact.get("name"), ARTIFACT);
		assertTrue(jsonArtifact.has("description"));
		assertTrue(jsonArtifact.has("versions"));
		JSONArray versions = jsonArtifact.getJSONArray("versions");
		assertEquals(versions.length(), 1);
		JSONObject version = versions.getJSONObject(0);
		assertEquals(version.get("name"), "1.0");
		assertTrue(version.has("description"));
		assertTrue(version.has("platforms"));
		assertEquals(version.getJSONArray("platforms").length(), 3);
	}

	@Test
	public void getVersion() throws IOException, JSONException {
		String json = api.getVersion(ARTIFACT, "1.0");
		JSONObject jsonVersion = new JSONObject(json);
		assertEquals(jsonVersion.get("name"), "1.0");
		assertTrue(jsonVersion.has("description"));
		assertTrue(jsonVersion.has("varPrefixChar"));
		assertTrue(jsonVersion.has("executables"));
		JSONArray exe = jsonVersion.getJSONArray("executables");
		assertEquals(exe.length(), 0);
		assertTrue(jsonVersion.has("packageInfo"));
		JSONObject info = jsonVersion.getJSONObject("packageInfo");
		assertEquals(info.get("name"), "mywebsite.zip");
		assertEquals(info.get("size"), "10.0 KB");
		assertTrue(jsonVersion.has("platforms"));
		assertEquals(jsonVersion.getJSONArray("platforms").length(), 3);
	}

	@Test
	public void createUpdateAndDeleteArtifact() throws JSONException, IOException {
		String artifactName = "createUpdateAndDeleteArtifact";
		/*
		 * Create
		 */
		int artifactCount = new JSONArray(api.getArtifacts()).length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "test");
		api.createArtifact(artifactName, jsonProperties.toString());
		assertEquals(new JSONArray(api.getArtifacts()).length(), artifactCount + 1);
		JSONObject jsonArtifact = new JSONObject(api.getArtifact(artifactName));
		assertEquals(jsonArtifact.get("description"), "test");

		/*
		 * Update
		 */
		jsonProperties = new JSONObject();
		jsonProperties.put("description", "test2");
		api.updateArtifact(artifactName, jsonProperties.toString());
		jsonArtifact = new JSONObject(api.getArtifact(artifactName));
		assertEquals(jsonArtifact.get("description"), "test2");

		/*
		 * Delete
		 */
		api.deleteArtifact(artifactName);
		assertEquals(new JSONArray(api.getArtifacts()).length(), artifactCount);
	}

	@Test
	public void createUpdateAndDeleteVersion() throws JSONException, IOException {
		String versionName = "test";
		/*
		 * Create
		 */
		String json = api.getArtifact(ARTIFACT);
		JSONObject jsonArtifact = new JSONObject(json);
		int versionCount = jsonArtifact.getJSONArray("versions").length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "test");
		api.createVersion(ARTIFACT, versionName, jsonProperties.toString());
		json = api.getArtifact(ARTIFACT);
		assertEquals(new JSONObject(json).getJSONArray("versions").length(), versionCount + 1);
		JSONObject version = new JSONObject(api.getVersion(ARTIFACT, versionName));
		assertEquals(version.get("name"), versionName);
		assertEquals(version.get("description"), "test");
		assertTrue(version.has("varPrefixChar"));
		assertTrue(version.has("executables"));
		JSONArray exe = version.getJSONArray("executables");
		assertEquals(exe.length(), 0);
		assertTrue(version.has("packageInfo"));
		JSONObject info = version.getJSONObject("packageInfo");
		assertFalse(info.has("name"));
		assertEquals(info.get("size"), "0.0 Bytes");
		assertTrue(version.has("platforms"));
		assertEquals(version.getJSONArray("platforms").length(), 0);

		/*
		 * Update
		 */
		jsonProperties = new JSONObject();
		jsonProperties.put("description", "test2");
		api.updateVersion(ARTIFACT, versionName, jsonProperties.toString());
		version = new JSONObject(api.getVersion(ARTIFACT, versionName));
		assertEquals(version.get("description"), "test2");

		/*
		 * Delete
		 */
		api.deleteVersion(ARTIFACT, versionName);
		json = api.getArtifact(ARTIFACT);
		assertEquals(new JSONObject(json).getJSONArray("versions").length(), versionCount);
	}

	@Test
	public void duplicateAndDeleteVersion() throws JSONException, IOException {
		String versionName = "duplicate";
		String json = api.getArtifact(ARTIFACT);
		JSONObject jsonArtifact = new JSONObject(json);
		int versionCount = jsonArtifact.getJSONArray("versions").length();
		JSONObject jsonProperties = new JSONObject();
		jsonProperties.put("description", "duplicate");
		api.duplicateVersion(ARTIFACT, "1.0", versionName, jsonProperties.toString());
		json = api.getArtifact(ARTIFACT);
		assertEquals(new JSONObject(json).getJSONArray("versions").length(), versionCount + 1);

		json = api.getVersion(ARTIFACT, versionName);
		JSONObject version = new JSONObject(json);
		assertEquals(version.get("name"), versionName);
		assertEquals(version.get("description"), "duplicate");
		assertTrue(version.has("varPrefixChar"));
		assertTrue(version.has("executables"));
		JSONArray exe = version.getJSONArray("executables");
		assertEquals(exe.length(), 0);
		assertTrue(version.has("packageInfo"));
		JSONObject info = version.getJSONObject("packageInfo");
		assertEquals(info.get("name"), "mywebsite.zip");
		assertEquals(info.get("size"), "10.0 KB");
		assertTrue(version.has("platforms"));
		assertEquals(version.getJSONArray("platforms").length(), 3);

		api.deleteVersion(ARTIFACT, versionName);
		json = api.getArtifact(ARTIFACT);
		assertEquals(new JSONObject(json).getJSONArray("versions").length(), versionCount);
	}

	@Test
	public void createVersionAndUploadPackage() throws JSONException, IOException {
		String versionName = "createVersionAndUploadPackage";
		api.createVersion(ARTIFACT, versionName, null);

		/*
		 * Upload file
		 */
		api.uploadArtifactPackage(ARTIFACT, versionName, new File("/bin/ls"), false);
		String json = api.getVersion(ARTIFACT, versionName);
		JSONObject version = new JSONObject(json);
		JSONObject info = version.getJSONObject("packageInfo");
		assertEquals(info.get("name"), "ls");

		/*
		 * Update from local path
		 */
		api.uploadArtifactPackage(ARTIFACT, versionName, "/bin/cat", false);
		json = api.getVersion(ARTIFACT, versionName);
		version = new JSONObject(json);
		info = version.getJSONObject("packageInfo");
		assertEquals(info.get("name"), "cat");

		/*
		 * Upload from URL
		 */
		api.uploadArtifactPackage(ARTIFACT, versionName, "http://www.kwatee.net/index", false);
		json = api.getVersion(ARTIFACT, versionName);
		version = new JSONObject(json);
		info = version.getJSONObject("packageInfo");
		assertEquals(info.get("name"), "index");

		api.deleteVersion(ARTIFACT, versionName);
	}
}
