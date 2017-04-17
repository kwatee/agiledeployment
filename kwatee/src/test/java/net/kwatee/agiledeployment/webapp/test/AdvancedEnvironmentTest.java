/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class AdvancedEnvironmentTest extends AbstractTestBase {

	@Test
	public void environmentServers() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/environmentServers");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/environmentServers.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("environmentServers"))
				.andExpect(jsonPath("$.servers").doesNotExist());

		request = post("/servers/environmentServer1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/servers/environmentServer2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/servers/environmentServer3");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/environmentServers")
				.content("{\"servers\" : [\"environmentServer1\",\"environmentServer2\",\"environmentServer3\"]}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/environmentServers.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("environmentServers"))
				.andExpect(jsonPath("$.servers", hasSize(3)));
	}

	@Test
	public void unkownEnvServer() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/unkownEnvServer");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/unkownEnvServer")
				.content("{\"servers\" : [\"unknownEnvServer\"]}");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void environmentArtifacts() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/environmentArtifacts");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/environmentArtifacts.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("environmentArtifacts"))
				.andExpect(jsonPath("$.artifacts").doesNotExist());

		request = post("/artifacts/environmentArtifact1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/environmentArtifact2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/environmentArtifact3");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/environmentArtifacts")
				.content("{\"artifacts\" : [\"environmentArtifact1\",\"environmentArtifact2\",\"environmentArtifact3\"]}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/environmentArtifacts.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("environmentArtifacts"))
				.andExpect(jsonPath("$.artifacts", hasSize(3)));
	}

	@Test
	public void unkownEnvArtifact() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/unkownEnvArtifact");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/environments/unkownEnvArtifact")
				.content("{\"artifacts\" : [\"unknownEnvArtifact\"]}");
		perform(request)
				.andExpect(status().isNotFound());
	}
}
