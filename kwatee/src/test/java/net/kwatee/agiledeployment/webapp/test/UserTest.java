/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class UserTest extends AbstractTestBase {

	@Test
	public void checkDataIntegrity() throws Exception {
		MockHttpServletRequestBuilder request = post("/admin/users/checkDataIntegrity")
				.content("{\"disabled\":true}");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/admin/users/checkDataIntegrity")
				.content("{\"description\":\"test user\"}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/admin/users/checkDataIntegrity.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("checkDataIntegrity")))
				.andExpect(jsonPath("$.description", is("test user")))
				.andExpect(jsonPath("$.disabled", is(true)));
	}

	@Test
	public void duplicateError() throws Exception {
		MockHttpServletRequestBuilder request = post("/admin/users/duplicateError");
		perform(request)
				.andExpect(status().isCreated());
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void fetchError() throws Exception {
		MockHttpServletRequestBuilder request = get("/admin/users/fetchError.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void deleteNoError() throws Exception {
		MockHttpServletRequestBuilder request = delete("/admin/users/deleteDummy");
		perform(request)
				.andExpect(status().isOk());
	}

	@Test
	public void createDelete() throws Exception {
		MockHttpServletRequestBuilder request = post("/admin/users/createDelete1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/admin/users/createDelete2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/admin/users/createDelete3");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/admin/users.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("admin", "createDelete1", "createDelete2", "createDelete3")));

		request = delete("/admin/users/createDelete2");
		perform(request)
				.andExpect(status().isOk());

		request = get("/admin/users.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("admin", "createDelete1", "createDelete3")));
	}
}
