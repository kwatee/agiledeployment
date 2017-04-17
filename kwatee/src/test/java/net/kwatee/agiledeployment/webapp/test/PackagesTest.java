/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class PackagesTest extends AbstractTestBase {

	@Test
	public void plainFileArchive() throws Exception {
		/*
		 * Upload a simple file without variables and check that there is
		 * exactly one file and that it is the right one
		 */
		MockHttpServletRequestBuilder request = post("/artifacts/plainFileArchive");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/plainFileArchive/v1");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/plainFileArchive/v1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packageInfo").doesNotExist());

		String fileName = "file1.txt";
		File file = new File(TEST_ROOT_PATH, fileName);
		request = post("/artifacts/plainFileArchive/v1/package").param("url", file.getAbsolutePath());
		perform(request)
				.andExpect(status().isOk());
		request = get("/artifacts/plainFileArchive/v1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packageInfo.name", is(fileName)));

		request = get("/artifacts/plainFileArchive/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is(fileName)));
	}

	@Test
	public void versionFiles1() throws Exception {
		createVersionWithPackage("versionFiles1", "file2.txt");
		MockHttpServletRequestBuilder request = get("/artifacts/versionFiles1/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	public void versionFiles2() throws Exception {
		createVersionWithPackage("versionFiles2", "file2.txt");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionFiles2/v1/package/overlay", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionFiles2/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	public void versionFiles3() throws Exception {
		createVersionWithPackage("versionFiles3", "file2.txt");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionFiles3/v1/package/overlay?path=asd", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionFiles3/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	public void versionFiles4() throws Exception {
		createVersionWithPackage("versionFiles4", "file2.txt");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionFiles4/v1/package/overlay?path=asd/qwe/", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionFiles4/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)));
	}

	@Test
	public void versionFiles5() throws Exception {
		createVersionWithPackage("versionFiles5", "file2.txt");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionFiles5/v1/package/overlay", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = postFile("/artifacts/versionFiles5/v1/package/overlay", overlayFile, "file3.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionFiles5/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	public void versionFiles6() throws Exception {
		createVersionWithPackage("versionFiles6", "file2.txt");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionFiles6/v1/package/overlay?path=qwe", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = postFile("/artifacts/versionFiles6/v1/package/overlay?path=qwe", overlayFile, "file3.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionFiles6/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[0].name", is("file2.txt")))
				.andExpect(jsonPath("$[1].name", is("qwe")))
				.andExpect(jsonPath("$[2].name", is("file1.txt")))
				.andExpect(jsonPath("$[3].name", is("file3.txt")));
	}

	@Test
	public void versionFiles7() throws Exception {
		createVersionWithPackage("versionFiles7", "file2.txt");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionFiles7/v1/package/overlay?path=asd", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionFiles7/v1/package/files.json?path=/");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name", is("asd")))
				.andExpect(jsonPath("$[1].name", is("file2.txt")));
	}

	@Test
	public void specialFile() throws Exception {
		String fileName = "file2.txt";
		createVersionWithPackage("specialFile", fileName);
		MockHttpServletRequestBuilder request = get("/artifacts/specialFile/v1/package/specialFiles.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is(fileName)))
				.andExpect(jsonPath("$[0].hasVariables").exists());
	}

	@Test
	public void versionVariables() throws Exception {
		createVersionWithPackage("versionVariables", "file2.txt");

		MockHttpServletRequestBuilder request = get("/artifacts/versionVariables/v1/package/specialFiles.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is("file2.txt")))
				.andExpect(jsonPath("$[0].hasVariables").exists());

		request = get("/artifacts/versionVariables/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues", hasSize(1)))
				.andExpect(jsonPath("defaultVariableValues[0].name", is("var1")));
	}

	/**
	 * 
	 * samplewithroot.tar.gz contains 3 files out of which two contain variables
	 */
	@Test
	public void overlay1() throws Exception {
		createVersionWithPackage("versionoverlay1", "samplewithroot.tar.gz");

		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlay.txt");
		MockHttpServletRequestBuilder request = post("/artifacts/versionoverlay1/v1/package/overlay").param("url", overlayFile.getAbsolutePath());
		perform(request)
				.andExpect(status().isCreated());

		request = get("/artifacts/versionoverlay1/v1/package/specialFiles.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3))); // 2 files with variables plus the overlay from above
		request = get("/artifacts/versionoverlay1/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4))); // 3 files in archive plus overlay
		request = get("/artifacts/versionoverlay1/v1/package/files.json").param("path", "test/");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	public void overlay2() throws Exception {
		createVersionWithPackage("versionoverlay2", "samplewithroot.tar");

		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlay.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionoverlay2/v1/package/overlay", overlayFile, "file2.txt"); // the original contained variables but not the overlay
		perform(request)
				.andExpect(status().isCreated());

		request = get("/artifacts/versionoverlay2/v1/package/specialFiles.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2))) // file2.txt as overlay and file3.txt as template
				.andExpect(jsonPath("$[?(@.name == 'file2.txt')].hasVariables").doesNotExist()); // the original contained variables but not the overlay
		request = get("/artifacts/versionoverlay2/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
	}

	@Test
	public void versionoverlay3() throws Exception {
		createVersionWithPackage("versionoverlay3", "samplewithroot.zip");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/versionoverlay3/v1/package/overlay", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/versionoverlay3/v1/package/specialFiles.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == 'file1.txt')].hasVariables").exists())
				.andExpect(jsonPath("$[?(@.name == 'file2.txt')].hasVariables").exists())
				.andExpect(jsonPath("$[?(@.name == 'file3.txt')].hasVariables").exists())
				.andExpect(jsonPath("$", hasSize(3)));
		request = get("/artifacts/versionoverlay3/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues", hasSize(3)));
	}

	@Test
	public void ignoreVars() throws Exception {
		createVersionWithPackage("ignoreVars", "samplewithoutroot.zip");
		MockHttpServletRequestBuilder request = get("/artifacts/ignoreVars/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues", hasSize(2)));
		request = get("/artifacts/ignoreVars/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == 'file2.txt')].hasVariables").value(true))
				.andExpect(jsonPath("$[?(@.name == 'file3.txt')].properties.ignoreVariables").doesNotExist())
				.andExpect(jsonPath("$[?(@.name == 'file3.txt')].hasVariables").value(true))
				.andExpect(jsonPath("$", hasSize(3)));

		request = put("/artifacts/ignoreVars/v1/package/file")
				.param("path", "file3.txt")
				.content("{\"ignoreVariables\":true}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/ignoreVars/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == 'file2.txt')].hasVariables").value(true))
				.andExpect(jsonPath("$[?(@.name == 'file3.txt')].properties.ignoreVariables").value(true))
				.andExpect(jsonPath("$[?(@.name == 'file3.txt')].hasVariables").value(true))
				.andExpect(jsonPath("$", hasSize(3)));
		request = get("/artifacts/ignoreVars/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues", hasSize(1)));
	}

	@Test
	public void removeOverlay() throws Exception {
		createVersionWithPackage("removeOverlay", "samplewithoutroot.tar");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlay.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/removeOverlay/v1/package/overlay", overlayFile, "file3.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = get("/artifacts/removeOverlay/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[0].name", is("var1")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(1)));

		request = delete("/artifacts/removeOverlay/v1/package/overlay").param("path", "file3.txt");
		perform(request)
				.andExpect(status().isOk());
		request = get("/artifacts/removeOverlay/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[*].name", hasItems("var1", "varfile3")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(2)));
	}

	@Test
	public void updateArchive1() throws Exception {
		createVersionWithPackage("updateArchive1", "samplewithoutroot.tar");
		MockHttpServletRequestBuilder request = get("/artifacts/updateArchive1/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[*].name", hasItems("var1", "varfile3")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(2)));
		File newFile = new File(TEST_ROOT_PATH, "file2.txt");
		request = postFile("/artifacts/updateArchive1/v1/package", newFile, newFile.getName());
		perform(request)
				.andExpect(status().isOk());
		request = get("/artifacts/updateArchive1/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[0].name", is("var1")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(1)));
	}

	@Test
	public void updateArchive2() throws Exception {
		createVersionWithPackage("updateArchive2", "samplewithoutroot.tar");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/updateArchive2/v1/package/overlay", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());

		/*
		 * Expect to find new overlay.var but same number of files
		 */
		request = get("/artifacts/updateArchive2/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[*].name", hasItems("var1", "varfile3", "overlay.var")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(3)));
		request = get("/artifacts/updateArchive2/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));

		File newFile = new File(TEST_ROOT_PATH, "file1.txt");
		request = postFile("/artifacts/updateArchive2/v1/package/overlay", newFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());

		/*
		 * The variable overlay.var should have disappeared but the number of files remains constant
		 */
		request = get("/artifacts/updateArchive2/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)));
		request = get("/artifacts/updateArchive2/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[*].name", hasItems("var1", "varfile3")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(2)));
	}

	@Test
	public void updateArchive3() throws Exception {
		createVersionWithPackage("updateArchive3", "samplewithoutroot.tar");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/updateArchive3/v1/package/overlay", overlayFile, "file4.txt");
		perform(request)
				.andExpect(status().isCreated());

		/*
		 * The variable overlay.var should have been created and there should be an additional file
		 */
		request = get("/artifacts/updateArchive3/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[*].name", hasItems("var1", "varfile3", "overlay.var")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(3)));
		request = get("/artifacts/updateArchive3/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)));

		File newFile = new File(TEST_ROOT_PATH, "file1.txt");
		request = postFile("/artifacts/updateArchive3/v1/package/overlay", newFile, "file4.txt");
		perform(request)
				.andExpect(status().isCreated());

		/*
		 * The variable overlay.var should have disappeared but the number of files remains constant
		 */
		request = get("/artifacts/updateArchive3/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(4)));
		request = get("/artifacts/updateArchive3/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues[*].name", hasItems("var1", "varfile3")))
				.andExpect(jsonPath("defaultVariableValues", hasSize(2)));
	}

	@Test
	public void deleteArchive1() throws Exception {
		createVersionWithPackage("deleteArchive1", "samplewithroot");
		MockHttpServletRequestBuilder request = get("/artifacts/deleteArchive1/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues", hasSize(2)))
				.andExpect(jsonPath("defaultVariableValues[?(@.name == 'var1')]").exists())
				.andExpect(jsonPath("defaultVariableValues[?(@.name == 'varfile3')]").exists())
				.andExpect(jsonPath("defaultVariableValues[?(@.name == 'var1')].references").exists());
		request = delete("/artifacts/deleteArchive1/v1/package");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/deleteArchive1/v1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packageInfo").doesNotExist());
		request = get("/artifacts/deleteArchive1/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
		request = get("/artifacts/deleteArchive1/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues").doesNotExist());
	}

	@Test
	public void deleteArchive2() throws Exception {
		createVersionWithPackage("deleteArchive2", "samplewithroot");
		File overlayFile = new File(TEST_ROOT_PATH, "versionoverlaywithvar.txt");
		MockHttpServletRequestBuilder request = postFile("/artifacts/deleteArchive2/v1/package/overlay", overlayFile, "file1.txt");
		perform(request)
				.andExpect(status().isCreated());
		File overlayFile2 = new File(TEST_ROOT_PATH, "versionoverlay.txt");
		request = postFile("/artifacts/deleteArchive2/v1/package/overlay", overlayFile2, "file3.txt");
		perform(request)
				.andExpect(status().isCreated());
		request = delete("/artifacts/deleteArchive2/v1/package");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/deleteArchive2/v1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packageInfo").doesNotExist());
		request = get("/artifacts/deleteArchive2/v1/package/files.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
		request = get("/artifacts/deleteArchive2/v1/variables.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("defaultVariableValues").doesNotExist());
	}

	// test package contents
	// test layers
	// test overlays without underlying
	// test overlays with underlying
	// test overlay file on dir
	// test overlay dir on file
	// test special properties

}
