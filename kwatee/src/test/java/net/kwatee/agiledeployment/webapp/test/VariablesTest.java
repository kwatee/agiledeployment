/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class VariablesTest extends AbstractTestBase {

	@Test
	public void variables1() throws Exception {
		createVersionWithPackage("variables1", null);
		prepareTestEnvironment("variables1", false);

		MockHttpServletRequestBuilder request = get("/environments/variables1/snapshot/resolvedVariables.json")
				.param("serverName", "variables1")
				.param("artifactName", "variables1");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("/variables1"));
	}

	@Test
	public void variables2() throws Exception {
		createVersionWithPackage("variables2", null);
		prepareTestEnvironment("variables2", false);

		MockHttpServletRequestBuilder request = put("/environments/variables2/snapshot/variables")
				.content("[{\"name\":\"kwatee_root_dir\", \"value\":\"root\"}]");
		perform(request)
				.andExpect(status().isOk());
		request = get("/environments/variables2/snapshot/resolvedVariables.json")
				.param("serverName", "variables2")
				.param("artifactName", "variables2");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("root"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("/variables2"));
		request = put("/environments/variables2/snapshot/variables")
				.content("[{\"name\":\"kwatee_deployment_dir\", \"value\":\"deployment\"}]");
		perform(request)
				.andExpect(status().isOk());
		request = get("/environments/variables2/snapshot/resolvedVariables.json")
				.param("serverName", "variables2")
				.param("artifactName", "variables2");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_deployment_dir')].value").value("deployment"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("deployment/variables2"));
		request = get("/environments/variables2/snapshot/resolvedVariables.json")
				.param("serverName", "variables22")
				.param("artifactName", "variables2");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_deployment_dir')].value").value("deployment"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("deployment/variables2"));
	}

	@Test
	public void serverVariables() throws Exception {
		createVersionWithPackage("serverVariables", null);
		prepareTestEnvironment("serverVariables", false);

		MockHttpServletRequestBuilder request = put("/environments/serverVariables/snapshot/variables")
				.content("[{\"name\":\"kwatee_deployment_dir\", \"value\":\"deployment\", \"server\":\"serverVariables\"}]");
		perform(request)
				.andExpect(status().isOk());
		request = get("/environments/serverVariables/snapshot/resolvedVariables.json")
				.param("serverName", "serverVariables")
				.param("artifactName", "serverVariables");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_deployment_dir')].value").value("deployment"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("deployment/serverVariables"));
		request = get("/environments/serverVariables/snapshot/resolvedVariables.json")
				.param("serverName", "serverVariables2")
				.param("artifactName", "serverVariables");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("/serverVariables"));
	}

	@Test
	public void artifactVariables() throws Exception {
		createVersionWithPackage("artifactVariables", null);
		prepareTestEnvironment("artifactVariables", false);

		MockHttpServletRequestBuilder request = put("/environments/artifactVariables/snapshot/variables")
				.content("[{\"name\":\"kwatee_deployment_dir\", \"value\":\"deployment\", \"artifact\":\"artifactVariables\"}]");
		perform(request)
				.andExpect(status().isOk());
		request = get("/environments/artifactVariables/snapshot/resolvedVariables.json")
				.param("serverName", "artifactVariables")
				.param("artifactName", "artifactVariables");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_deployment_dir')].value").value("deployment"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("deployment/artifactVariables"));
		request = get("/environments/artifactVariables/snapshot/resolvedVariables.json")
				.param("serverName", "artifactVariables2")
				.param("artifactName", "artifactVariables");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'kwatee_root_dir')].value").value("/var/tmp/kwatee"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_deployment_dir')].value").value("deployment"))
				.andExpect(jsonPath("[?(@.name == 'kwatee_package_dir')].value").value("deployment/artifactVariables"));
	}

	@Test
	public void specialVariables() throws Exception {
		createVersionWithPackage("specialVariables", null);
		prepareTestEnvironment("specialVariables", false);

		MockHttpServletRequestBuilder request = put("/environments/specialVariables/snapshot/variables")
				.content("[{\"name\":\"serverName\", \"value\":\"%{kwatee_server_name}\"}, {\"name\":\"artifactName\", \"value\":\"%{kwatee_artifact_name}\"}, {\"name\":\"packageName\", \"value\":\"%{kwatee_package_name}\"}, {\"name\":\"environmentName\", \"value\":\"%{kwatee_environment_name}\"}, {\"name\":\"releaseName\", \"value\":\"%{kwatee_release_name}\"}, {\"name\":\"deploymentName\", \"value\":\"%{kwatee_deployment_name}\"}, {\"name\":\"versionName\", \"value\":\"%{kwatee_version_name}\"}, {\"name\":\"serverPlatform\", \"value\":\"%{kwatee_server_platform}\"}, {\"name\":\"ipAddress\", \"value\":\"%{kwatee_server_ip}\"}, {\"name\":\"serverInstance\", \"value\":\"%{kwatee_server_instance}\"}]");
		perform(request)
				.andExpect(status().isOk());
		request = get("/environments/specialVariables/snapshot/resolvedVariables.json")
				.param("serverName", "specialVariables")
				.param("artifactName", "specialVariables");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("[?(@.name == 'serverName')].value").value("specialVariables"))
				.andExpect(jsonPath("[?(@.name == 'artifactName')].value").value("specialVariables"))
				.andExpect(jsonPath("[?(@.name == 'packageName')].value").value("specialVariables"))
				.andExpect(jsonPath("[?(@.name == 'environmentName')].value").value("specialVariables"))
				.andExpect(jsonPath("[?(@.name == 'releaseName')].value").value("snapshot"))
				.andExpect(jsonPath("[?(@.name == 'deploymentName')].value").value("snapshot"))
				.andExpect(jsonPath("[?(@.name == 'versionName')].value").value("v1"))
				.andExpect(jsonPath("[?(@.name == 'serverPlatform')].value").value("linux_x86"))
				.andExpect(jsonPath("[?(@.name == 'ipAddress')].value").value("test.ip.address"))
				.andExpect(jsonPath("[?(@.name == 'serverInstance')].value").value("testInstance"));
	}
}
