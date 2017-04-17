/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class BasicVersionTest extends AbstractTestBase {

	@Test
	public void versionCheckDataIntegrity() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/versionCheckDataIntegrity");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/versionCheckDataIntegrity/v1")
				.content("{\"disabled\":true}");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/artifacts/versionCheckDataIntegrity/v1")
				.content("{\"description\":\"description\", \"preDeployAction\":\"predeploy\", \"postDeployAction\":\"postdeploy\", \"preUndeployAction\":\"preundeploy\", \"postUndeployAction\":\"postundeploy\", \"executables\":[{\"name\":\"exe\", \"description\":\"description\", \"startAction\":\"startaction\", \"stopAction\":\"stopaction\", \"statusAction\":\"statusaction\"}], \"platforms\":[1,3,4]}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/versionCheckDataIntegrity/v1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("v1")))
				.andExpect(jsonPath("$.description", is("description")))
				.andExpect(jsonPath("$.preDeployAction", is("predeploy")))
				.andExpect(jsonPath("$.postDeployAction", is("postdeploy")))
				.andExpect(jsonPath("$.preUndeployAction", is("preundeploy")))
				.andExpect(jsonPath("$.postUndeployAction", is("postundeploy")))
				.andExpect(jsonPath("$.executables", hasSize(1)))
				.andExpect(jsonPath("$.executables[0].name", is("exe")))
				.andExpect(jsonPath("$.executables[0].description", is("description")))
				.andExpect(jsonPath("$.executables[0].startAction", is("startaction")))
				.andExpect(jsonPath("$.executables[0].stopAction", is("stopaction")))
				.andExpect(jsonPath("$.executables[0].statusAction", is("statusaction")))
				.andExpect(jsonPath("$.platforms", hasSize(3)))
				.andExpect(jsonPath("$.platforms", hasItems(1, 3, 4)));
	}

	@Test
	public void duplicateVersion() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/duplicateVersion");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/duplicateVersion/v1")
				.content("{\"description\":\"description\", \"preDeployAction\":\"predeploy\", \"postDeployAction\":\"postdeploy\", \"preUndeployAction\": \"preundeploy\", \"postUndeployAction\": \"postundeploy\", \"executables\":[{\"name\":\"exe\", \"description\":\"description\", \"startAction\":\"startaction\", \"stopAction\":\"stopaction\", \"statusAction\":\"statusaction\"}], \"platforms\":[1,3,4]}");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/artifacts/duplicateVersion/v2").param("duplicateFrom", "v1")
				.content("{\"description\":\"duplicate\"}");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/artifacts/duplicateVersion/v2.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("v2")))
				.andExpect(jsonPath("$.description", is("duplicate")))
				.andExpect(jsonPath("$.preDeployAction", is("predeploy")))
				.andExpect(jsonPath("$.postDeployAction", is("postdeploy")))
				.andExpect(jsonPath("$.preUndeployAction", is("preundeploy")))
				.andExpect(jsonPath("$.postUndeployAction", is("postundeploy")))
				.andExpect(jsonPath("$.executables", hasSize(1)))
				.andExpect(jsonPath("$.executables[0].name", is("exe")))
				.andExpect(jsonPath("$.executables[0].description", is("description")))
				.andExpect(jsonPath("$.executables[0].startAction", is("startaction")))
				.andExpect(jsonPath("$.executables[0].stopAction", is("stopaction")))
				.andExpect(jsonPath("$.executables[0].statusAction", is("statusaction")))
				.andExpect(jsonPath("$.platforms", hasSize(3)))
				.andExpect(jsonPath("$.platforms", hasItems(1, 3, 4)));
	}

	@Test
	public void duplicateError() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/duplicateVersionError");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/duplicateVersionError/v1");
		perform(request)
				.andExpect(status().isCreated());
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void fetchError1() throws Exception {
		MockHttpServletRequestBuilder request = get("/artifacts/fetchVersionError1/v1.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void fetchError2() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/fetchVersionError2");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/fetchVersionError2/v1.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void createDelete() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/createDeleteVersion");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/createDeleteVersion/v1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/createDeleteVersion/v2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/createDeleteVersion/v3");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/artifacts/createDeleteVersion.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.versions[*].name", hasItems("v1", "v2", "v3")));

		request = delete("/artifacts/createDeleteVersion/v2");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/createDeleteVersion.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.versions[*].name", hasItems("v1", "v3")))
				.andExpect(jsonPath("$.versions[*].name", not(hasItem("v2"))));
	}
}
