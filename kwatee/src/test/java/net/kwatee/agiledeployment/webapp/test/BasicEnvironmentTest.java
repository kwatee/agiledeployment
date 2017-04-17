/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class BasicEnvironmentTest extends AbstractTestBase {

	@Test
	public void checkDataIntegrity() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/checkDataIntegrity")
				.content("{\"disabled\":true}");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/environments/checkDataIntegrity")
				.content("{\"description\":\"test environment\", \"sequentialDeployment\":true}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/checkDataIntegrity.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("checkDataIntegrity")))
				.andExpect(jsonPath("$.description", is("test environment")))
				.andExpect(jsonPath("$.sequentialDeployment", is(true)))
				.andExpect(jsonPath("$.disabled", is(true)));
	}

	@Test
	public void duplicateError() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/duplicateError");
		perform(request)
				.andExpect(status().isCreated());
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void fetchError() throws Exception {
		MockHttpServletRequestBuilder request = get("/environments/fetchError.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void deleteNoError() throws Exception {
		MockHttpServletRequestBuilder request = delete("/environments/deleteDummy");
		perform(request)
				.andExpect(status().isOk());
	}

	@Test
	public void duplicate() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/duplicate1")
				.content("{\"disabled\":true, \"sequentialDeployment\":true}");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/environments/duplicate2").param("duplicateFrom", "duplicate1")
				.content("{\"description\":\"duplicate\"}");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/duplicate2.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("duplicate2")))
				.andExpect(jsonPath("$.description", is("duplicate")))
				.andExpect(jsonPath("$.sequentialDeployment", is(true)))
				.andExpect(jsonPath("$.disabled").doesNotExist()); // duplicate
																	// clears
																	// the
																	// disabled
																	// flag
	}

	@Test
	public void createDelete() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/createDelete1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/createDelete2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/createDelete3");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("createDelete1", "createDelete2", "createDelete3")));

		request = delete("/environments/createDelete2");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("createDelete1", "createDelete3")))
				.andExpect(jsonPath("$[*].name", not(hasItem("createDelete2"))));
	}

	@Test
	public void deleteNotEmpty() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/deleteNotEmptyError");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/environments/deleteNotEmptyError/d1");
		perform(request)
				.andExpect(status().isCreated());

		request = delete("/environments/deleteNotEmptyError");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/deleteNotEmptyError.json");
		perform(request)
				.andExpect(status().isNotFound());
	}
}
