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

public class BasicArtifactTest extends AbstractTestBase {

	@Test
	public void checkDataIntegrity() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/checkDataIntegrity")
				.content("{\"disabled\":true}");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/artifacts/checkDataIntegrity")
				.content("{\"description\":\"test artifact\"}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/checkDataIntegrity.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("checkDataIntegrity")))
				.andExpect(jsonPath("$.disabled", is(Boolean.TRUE)));
	}

	@Test
	public void duplicateError() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/duplicateError");
		perform(request)
				.andExpect(status().isCreated());
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void fetchError() throws Exception {
		MockHttpServletRequestBuilder request = get("/artifacts/fetchError.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void deleteNoError() throws Exception {
		MockHttpServletRequestBuilder request = delete("/artifacts/deleteDummy");
		perform(request)
				.andExpect(status().isOk());
	}

	@Test
	public void createDelete() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/createDelete1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/createDelete2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/createDelete3");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/artifacts.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("createDelete1", "createDelete2", "createDelete3")));

		request = delete("/artifacts/createDelete2");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("createDelete1", "createDelete3")))
				.andExpect(jsonPath("$[*].name", not(hasItem("createDelete2"))));
	}

	@Test
	public void deleteNotEmpty() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/deleteNotEmpty");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/deleteNotEmpty/v1");
		perform(request)
				.andExpect(status().isCreated());

		request = delete("/artifacts/deleteNotEmpty");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/deleteNotEmpty.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void deleteVersion() throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/deleteVersion");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/deleteVersion/v1");
		perform(request)
				.andExpect(status().isCreated());

		request = delete("/artifacts/deleteVersion/v1");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/deleteVersion/v1.json");
		perform(request)
				.andExpect(status().isNotFound());
	}
}
