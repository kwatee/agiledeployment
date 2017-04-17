/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class SystemPropertiesTest extends AbstractTestBase {

	@Test
	public void defaultProperties() throws Exception {
		MockHttpServletRequestBuilder request = get("/admin/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("kwatee_root_dir", "kwatee_deployment_dir", "kwatee_package_dir")))
				.andExpect(jsonPath("$[?(@.name == 'kwatee_package_dir')].value", hasItem("%{kwatee_deployment_dir}/%{kwatee_package_name}")));
	}

	@Test
	public void updateProperty() throws Exception {
		MockHttpServletRequestBuilder request = put("/admin/variables")
				.content("[{\"name\":\"kwatee_deployment_dir\", \"value\":\"dummy\"}, {\"name\":\"newvar\", \"value\":\"newvalue\"}]");
		perform(request)
				.andExpect(status().isOk());

		request = get("/admin/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("kwatee_root_dir", "kwatee_deployment_dir", "kwatee_package_dir")))
				.andExpect(jsonPath("$[?(@.name == 'kwatee_package_dir')].value", hasItem("%{kwatee_deployment_dir}/%{kwatee_package_name}")))
				.andExpect(jsonPath("$[?(@.name == 'kwatee_deployment_dir')].value", hasItem("dummy")));

		request = put("/admin/variables").content("[{\"name\":\"kwatee_deployment_dir\", \"value\":\"\"}]");
		perform(request)
				.andExpect(status().isOk());
	}
}
