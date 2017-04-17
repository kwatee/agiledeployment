/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class AdvancedReleaseTest extends AbstractTestBase {

	@Test
	public void createRelease() throws Exception {
		createVersionWithPackage("createRelease", "file1.txt");
		prepareTestEnvironment("createRelease", true);
		MockHttpServletRequestBuilder request = get("/environments/createRelease.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releases", hasSize(2)))
				.andExpect(jsonPath("$.releases[?(@.name == 'snapshot')].name").exists())
				.andExpect(jsonPath("$.releases[?(@.name == 'rel1')].name").exists());
	}

	@Test
	public void removeUsed() throws Exception {
		createVersionWithPackage("removeUsed", "file1.txt");
		prepareTestEnvironment("removeUsed", true);
		MockHttpServletRequestBuilder request = delete("/artifacts/removeUsed/v1");
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void badVersionPlatform() throws Exception {
		createVersionWithPackage("badVersionPlatform", "file1.txt");
		prepareTestEnvironment("badVersionPlatform", false);
		MockHttpServletRequestBuilder request = put("/artifacts/badVersionPlatform/v1")
				.content("{\"description\":\"test\"}");
		perform(request)
				.andExpect(status().isOk());
		request = put("/artifacts/badVersionPlatform/v1")
				.content("{\"platforms\":[3]}");
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void changeFrozenVersion1() throws Exception {
		createVersionWithPackage("changeFrozenVersion1", "file1.txt");
		prepareTestEnvironment("changeFrozenVersion1", true);
		MockHttpServletRequestBuilder request = put("/artifacts/changeFrozenVersion1/v1")
				.content("{\"preDeployAction\":\"anything\"}");
		perform(request)
				.andExpect(status().isOk());
		request = get("/artifacts/changeFrozenVersion1/v1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.preDeployAction").doesNotExist());
	}

	@Test
	public void defaultActiveVersion() throws Exception {
		createVersionWithPackage("defaultActiveVersion", "file1.txt");
		prepareTestEnvironment("defaultActiveVersion", true);
		MockHttpServletRequestBuilder request = post("/artifacts/defaultActiveVersion/v2");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/environments/defaultActiveVersion/snapshot/activeVersion")
				.param("artifactName", "defaultActiveVersion")
				.param("versionName", "v2");
		perform(request)
				.andExpect(status().isOk());
		request = delete("/artifacts/defaultActiveVersion/v2");
		perform(request)
				.andExpect(status().is(420));
		request = put("/environments/defaultActiveVersion/snapshot/activeVersion")
				.param("artifactName", "defaultActiveVersion")
				.param("versionName", "v1");
		perform(request)
				.andExpect(status().isOk());
		request = delete("/artifacts/defaultActiveVersion/v2");
		perform(request)
				.andExpect(status().isOk());
	}

	@Test
	public void unmappedEnvArtifact() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/unmappedEnvArtifact");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/unmappedEnvArtifact")
				.content("{\"servers\":[\"unmappedEnvArtifact\"]}");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/unmappedEnvArtifact");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/unmappedEnvArtifact/v1");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/unmappedEnvArtifact/snapshot")
				.content("{\"servers\":[{\"server\":\"unmappedEnvArtifact\", \"artifacts\":[{\"artifact\":\"unmappedEnvArtifact\", \"version\":\"v1\"}]}]}");
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void releaseOverlayA() throws Exception {
		createVersionWithPackage("releaseOverlayA", "samplewithroot.zip");
		prepareTestEnvironment("releaseOverlayA", false);
		MockHttpServletRequestBuilder request = get("/environments/releaseOverlayA/snapshot/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.artifactVariables", hasSize(1)))
				.andExpect(jsonPath("$.missingVariables", hasSize(1)));
		File overlay = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		request = postFile("/environments/releaseOverlayA/snapshot/package/overlay", overlay, "test")
				.param("artifactName", "releaseOverlayA");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/releaseOverlayA/snapshot/package/files.json")
				.param("artifactName", "releaseOverlayA");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)));
		request = get("/environments/releaseOverlayA/snapshot/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.missingVariables", hasSize(2)));
	}

	@Test
	public void releaseOverlayB() throws Exception {
		createVersionWithPackage("releaseOverlayB", "file1.txt");
		prepareTestEnvironment("releaseOverlayB", true);
		MockHttpServletRequestBuilder request = delete("/environments/releaseOverlayB/rel1");
		perform(request)
				.andExpect(status().isOk());

		File overlay = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		request = postFile("/environments/releaseOverlayB/snapshot/package/overlay", overlay, "test")
				.param("artifactName", "releaseOverlayB");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/releaseOverlayB/snapshot/package/files.json")
				.param("artifactName", "releaseOverlayB");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		File packageFile = new File(TEST_ROOT_PATH, "file1.txt");
		request = postFile("/artifacts/releaseOverlayB/v1/package", packageFile, packageFile.getName()).param("deleteOverlays", "true");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/releaseOverlayB/snapshot/package/files.json")
				.param("artifactName", "releaseOverlayB");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		request = get("/environments/releaseOverlayB/snapshot/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.missingVariables", hasSize(1)));
	}

	@Test
	public void releaseServerOverlay() throws Exception {
		createVersionWithPackage("releaseServerOverlay", "samplewithroot.zip");
		prepareTestEnvironment("releaseServerOverlay", false);

		MockHttpServletRequestBuilder request = get("/environments/releaseServerOverlay/snapshot/errors.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		request = get("/environments/releaseServerOverlay/snapshot/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.artifactVariables", hasSize(1)))
				.andExpect(jsonPath("$.missingVariables", hasSize(1)))
				.andExpect(jsonPath("$.artifactVariables[?(@.name == 'var1')].name").exists())
				.andExpect(jsonPath("$.missingVariables[?(@.name == 'varfile3')].name").exists());
		request = get("/environments/releaseServerOverlay/snapshot/package/files.json")
				.param("artifactName", "releaseServerOverlay");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
		request = get("/environments/releaseServerOverlay/snapshot/package/files.json")
				.param("artifactName", "releaseServerOverlay")
				.param("serverName", "releaseServerOverlay");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));

		File overlay = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		request = postFile("/environments/releaseServerOverlay/snapshot/package/overlay", overlay, "test")
				.param("artifactName", "releaseServerOverlay")
				.param("serverName", "releaseServerOverlay");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/environments/releaseServerOverlay/snapshot/package/files.json")
				.param("artifactName", "releaseServerOverlay");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
		request = get("/environments/releaseServerOverlay/snapshot/package/files.json")
				.param("artifactName", "releaseServerOverlay")
				.param("serverName", "releaseServerOverlay");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)));
		request = get("/environments/releaseServerOverlay/snapshot/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.artifactVariables", hasSize(1)))
				.andExpect(jsonPath("$.missingVariables", hasSize(2)))
				.andExpect(jsonPath("$.artifactVariables[?(@.name == 'var1')].name").exists())
				.andExpect(jsonPath("$.missingVariables[?(@.name == 'varfile3')].name").exists())
				.andExpect(jsonPath("$.missingVariables[?(@.name == 'overlay.var')].name").exists());
	}

	@Test
	public void removeServerArtifact() throws Exception {
		createVersionWithPackage("removeServerArtifact", "samplewithroot.zip");
		prepareTestEnvironment("removeServerArtifact", false);
		MockHttpServletRequestBuilder request = get("/environments/removeServerArtifact/snapshot.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servers", hasSize(2)))
				.andExpect(jsonPath("$.servers[?(@.server == 'removeServerArtifact')].artifacts", hasSize(1)));
		request = put("/environments/removeServerArtifact/snapshot")
				.content("{\"servers\":[{\"server\":\"removeServerArtifact\", \"artifacts\":[]}, {\"server\":\"removeServerArtifact2\", \"artifacts\":[{\"artifact\":\"removeServerArtifact\", \"version\":\"v1\"}]}], \"defaultArtifacts\":[{\"artifact\":\"removeServerArtifact\", \"version\":\"v1\"}]}");
		perform(request)
				.andExpect(status().isOk());
		request = get("/environments/removeServerArtifact/snapshot.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servers", hasSize(2)))
				.andExpect(jsonPath("$.servers[?(@.server == 'removeServerArtifact')].unused").exists())
				.andExpect(jsonPath("$.servers[?(@.server == 'removeServerArtifact')].artifacts").doesNotExist());
	}
}
