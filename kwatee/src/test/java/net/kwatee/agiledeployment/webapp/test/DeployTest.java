/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import net.kwatee.agiledeployment.conduit.impl.TestConduit;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class DeployTest extends AbstractTestBase {

	@Test
	public void simpleDeploy() throws Exception, InterruptedException {
		String name = "simpleDeploy";
		String batchRef = null;
		createVersionWithPackage(name, "file1.txt");
		prepareDeployEnvironment(name, TestConduit.SECRET_PASSWORD, encodeLf(TestConduit.SECRET_PEM));

		MockHttpServletRequestBuilder request = post("/deployments/" + name + "/snapshot/deploy");
		String response = perform(request)
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.ref").exists())
				.andExpect(jsonPath("$.authenticationRequested").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		batchRef = new JSONObject(response).getString("ref");
		request = get("/deployments/progress/status").param("ref", batchRef);
		perform(request)
				.andExpect(status().isNoContent());
		Thread.sleep(400L);
		request = get("/deployments/progress/status").param("ref", batchRef);
		perform(request)
				.andExpect(status().isOk());

		request = get("/deployments/progress/status.json").param("ref", batchRef);
		perform(request)
				.andExpect(jsonPath("$.status", is("done")))
				.andExpect(status().isOk());

		request = post("/deployments/progress/cancel").param("ref", batchRef).param("dontClear", "true");
		perform(request)
				.andExpect(status().isOk());

		request = get("/deployments/progress/status").param("ref", batchRef);
		perform(request)
				.andExpect(status().isOk());

		request = post("/deployments/progress/cancel").param("ref", batchRef);
		perform(request)
				.andExpect(status().isOk());

		request = get("/deployments/progress/status").param("ref", batchRef);
		perform(request)
				.andExpect(status().isGone());

		request = get("/deployments/ongoing.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ref").doesNotExist());

		request = post("/deployments/progress/cancel").param("ref", batchRef);
		perform(request);
	}

	private void prepareDeployEnvironment(String name, String password, String pem) throws Exception {
		createServer(name, password, true);
		createServer(name + "2", pem, false);

		MockHttpServletRequestBuilder request = post("/environments/" + name)
				.content("{\"servers\":[\"" + name + "\",\"" + name + "2\"],\"artifacts\":[\"" + name + "\"]}");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/" + name + "/snapshot")
				.content("{\"servers\":[{\"server\":\"" + name + "\", \"artifacts\":[{\"artifact\":\"" + name + "\", \"version\":\"v1\"}]}, {\"server\":\"" + name + "2\", \"artifacts\":[{\"artifact\":\"" + name + "\", \"version\":\"v1\"}]}], \"defaultArtifacts\":[{\"artifact\":\"" + name + "\", \"version\":\"v1\"}]}");
		perform(request)
				.andExpect(status().isOk());

	}

	@Test
	public void promptedPasswordError() throws Exception, InterruptedException {
		String name = "promptedPasswordError";
		createVersionWithPackage(name, "file1.txt");
		prepareTestEnvironment(name, true);

		MockHttpServletRequestBuilder request = put("/servers/" + name)
				.content("{\"credentials\":{\"promptPassword\":true}}");
		perform(request)
				.andExpect(status().isOk());

		request = post("/deployments/" + name + "/snapshot/deploy");
		perform(request)
				.andExpect(status().isExpectationFailed());

		request = post("/deployments/progress/credentials")
				.content("{\"environment\":\"" + name + "\", \"server\":\"" + name + "\", \"password\":\"password\"}");
		perform(request)
				.andExpect(status().isOk());

		request = post("/deployments/" + name + "/snapshot/deploy");
		String response = perform(request)
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.ref").exists())
				.andReturn().getResponse().getContentAsString();
		String batchRef = new JSONObject(response).getString("ref");
		try {
			Thread.sleep(500L);
			request = get("/deployments/progress/status").param("ref", batchRef);
			perform(request)
					.andExpect(status().isOk());
		} finally {
			request = post("/deployments/progress/cancel").param("ref", batchRef);
			perform(request);
		}
	}
}
