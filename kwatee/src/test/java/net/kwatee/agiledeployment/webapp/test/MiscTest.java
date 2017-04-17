/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MiscTest extends AbstractTestBase {

	@Test
	public void platforms() throws Exception {
		MockHttpServletRequestBuilder request = get("/info/platforms.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(8)));
	}

	@Test
	public void conduitTypes() throws Exception {
		MockHttpServletRequestBuilder request = get("/info/conduitTypes.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	public void serverPoolTypes() throws Exception {
		MockHttpServletRequestBuilder request = get("/info/serverPoolTypes.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

}
