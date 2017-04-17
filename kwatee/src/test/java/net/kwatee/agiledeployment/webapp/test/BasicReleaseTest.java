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

public class BasicReleaseTest extends AbstractTestBase {

	@Test
	public void releaseCheckDataIntegrity() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/releaseCheckDataIntegrity");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/environments/releaseCheckDataIntegrity/snapshot")
				.content("{\"description\" : \"test release\", \"preSetupAction\" : \"presetup\", \"postSetupAction\" : \"postsetup\", \"preCleanupAction\" : \"precleanup\", \"postCleanupAction\" : \"postcleanup\"}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/releaseCheckDataIntegrity/snapshot.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("snapshot")))
				.andExpect(jsonPath("$.editable", is(true)))
				.andExpect(jsonPath("$.description", is("test release")))
				.andExpect(jsonPath("$.preSetupAction", is("presetup")))
				.andExpect(jsonPath("$.postSetupAction", is("postsetup")))
				.andExpect(jsonPath("$.preCleanupAction", is("precleanup")))
				.andExpect(jsonPath("$.postCleanupAction", is("postcleanup")));
	}

	@Test
	public void tag() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/tag");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/environments/tag/snapshot")
				.content("{\"description\" : \"test release\", \"preSetupAction\" : \"presetup\", \"postSetupAction\" : \"postsetup\", \"preCleanupAction\" : \"precleanup\", \"postCleanupAction\" : \"postcleanup\"}");
		perform(request)
				.andExpect(status().isOk());
		request = post("/environments/tag/tagged")
				.content("{\"description\" : \"tagged release\", \"preSetupAction\" : \"presetup1\", \"postSetupAction\" : \"postsetup1\", \"preCleanupAction\" : \"precleanup1\", \"postCleanupAction\" : \"postcleanup1\"}");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/tag.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releases", hasSize(2)));
		request = get("/environments/tag/tagged.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("tagged")))
				.andExpect(jsonPath("$.editable", not(is(true))))
				.andExpect(jsonPath("$.description", is("tagged release")))
				.andExpect(jsonPath("$.preSetupAction", is("presetup")))
				.andExpect(jsonPath("$.postSetupAction", is("postsetup")))
				.andExpect(jsonPath("$.preCleanupAction", is("precleanup")))
				.andExpect(jsonPath("$.postCleanupAction", is("postcleanup")));
	}

	@Test
	public void duplicateError() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/duplicateReleaseError");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/duplicateReleaseError/r1");
		perform(request)
				.andExpect(status().isCreated());
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void fetchError1() throws Exception {
		MockHttpServletRequestBuilder request = get("/environments/fetchReleaseError1/r1.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void fetchError2() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/fetchReleaseError2");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/environments/fetchReleaseError2/v1.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void createDelete() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/createDeleteRelease");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/createDeleteRelease/r1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/createDeleteRelease/r2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/environments/createDeleteRelease/r3");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/environments/createDeleteRelease.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releases[*].name", hasItems("r1", "r2", "r3", "snapshot")));

		request = delete("/environments/createDeleteRelease/r2");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/createDeleteRelease.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.releases[*].name", hasItems("r1", "r3", "snapshot")))
				.andExpect(jsonPath("$.releases[*].name", not(hasItem("r2"))));
	}

	@Test
	public void reedit() throws Exception {
		MockHttpServletRequestBuilder request = post("/environments/reedit");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/environments/reedit/snapshot")
				.content("{\"preSetupAction\" : \"presetup1\", \"postSetupAction\" : \"postsetup1\", \"preCleanupAction\" : \"precleanup1\", \"postCleanupAction\" : \"postcleanup1\"}");
		perform(request)
				.andExpect(status().isOk());

		request = post("/environments/reedit/tagged");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/reedit/snapshot")
				.content("{\"description\" : \"test release\", \"preSetupAction\" : \"presetup\", \"postSetupAction\" : \"postsetup\", \"preCleanupAction\" : \"precleanup\", \"postCleanupAction\" : \"postcleanup\"}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/reedit/snapshot.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("snapshot")))
				.andExpect(jsonPath("$.editable", is(true)))
				.andExpect(jsonPath("$.preSetupAction", is("presetup")))
				.andExpect(jsonPath("$.postSetupAction", is("postsetup")))
				.andExpect(jsonPath("$.preCleanupAction", is("precleanup")))
				.andExpect(jsonPath("$.postCleanupAction", is("postcleanup")));

		request = post("/environments/reedit/tagged/reedit");
		perform(request)
				.andExpect(status().isOk());

		request = get("/environments/reedit/snapshot.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("snapshot")))
				.andExpect(jsonPath("$.editable", is(true)))
				.andExpect(jsonPath("$.preSetupAction", is("presetup1")))
				.andExpect(jsonPath("$.postSetupAction", is("postsetup1")))
				.andExpect(jsonPath("$.preCleanupAction", is("precleanup1")))
				.andExpect(jsonPath("$.postCleanupAction", is("postcleanup1")));
	}
}
