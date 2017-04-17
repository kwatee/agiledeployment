/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.conduit.impl.TestConduit;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.User;
import net.kwatee.agiledeployment.webapp.security.ApiTokenAuthentication;
import net.kwatee.agiledeployment.webapp.security.UserDetailsImpl;
import net.kwatee.agiledeployment.webapp.test.spring.ApiTestConfiguration;
import net.kwatee.agiledeployment.webapp.test.spring.TestApplication;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {TestApplication.class})
@Import(ApiTestConfiguration.class)
@WebIntegrationTest("server.port=0")
public abstract class AbstractTestBase {

	static final String TEST_ROOT_PATH = "src/main/test_data_files";

	static private UserDetails defaultUser;

	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private VariableService variableService;
	@Value("${kwatee.repository.path}")
	private String repoDir;
	@Value("${kwatee.tmp.path}")
	private String tmpDir;

	@Autowired
	private WebApplicationContext wac;

	private MockMvc rest;

	protected AbstractTestBase() {}

	@Before
	final public void setup() {
		if (defaultUser == null) {
			FileUtils.deleteQuietly(new File(this.repoDir));
			new File(this.tmpDir).mkdirs();

			defaultUser = new UserDetailsImpl().loadUserByUsername("admin");
			Authentication authentication = new ApiTokenAuthentication((User) defaultUser);
			SecurityContextHolder.getContext().setAuthentication(authentication);
			Map<String, String> globalProperties = new HashMap<>();
			for (SystemProperty p : this.adminRepository.getSystemProperties())
				globalProperties.put(p.getName(), p.getValue());
			this.variableService.setGlobalProperties(globalProperties); // pre-fetch
		}
		this.rest = MockMvcBuilders.webAppContextSetup(this.wac).build();

	}

	protected MockHttpServletRequestBuilder get(String url) {
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api" + url);
	}

	protected MockHttpServletRequestBuilder post(String url) {
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api" + url).contentType(MediaType.parseMediaType("application/json"));
	}

	protected MockHttpServletRequestBuilder put(String url) {
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api" + url).contentType(MediaType.parseMediaType("application/json"));
	}

	protected MockHttpServletRequestBuilder delete(String url) {
		return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api" + url);
	}

	protected MockHttpServletRequestBuilder postFile(String url, File file, String originalName) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		MockMultipartFile multipartFile = new MockMultipartFile("file", originalName, null, fis);
		MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload("/api" + url)
				.file(multipartFile);
		return request;
	}

	protected ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
		ResultActions result = this.rest.perform(request);
		MockHttpServletResponse response = result.andReturn().getResponse();
		System.out.println(response.getContentAsString());
		return result;
	}

	/**
	 * 
	 * Creates a artifact 'name' with a version 'v1' to which the package
	 * 'fileName' is uploaded
	 * 
	 * @param artifactName
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	ResultActions createVersionWithPackage(String artifactName, String fileName) throws Exception {
		MockHttpServletRequestBuilder request = post("/artifacts/" + artifactName);
		perform(request)
				.andExpect(status().isCreated());
		request = post("/artifacts/" + artifactName + "/v1")
				.content("{\"platforms\":[1]}");
		perform(request)
				.andExpect(status().isCreated());

		if (fileName != null) {
			File file = new File(TEST_ROOT_PATH, fileName);
			request = post("/artifacts/" + artifactName + "/v1/package").param("url", file.getAbsolutePath());
			perform(request)
					.andExpect(status().isOk());
		}

		request = put("/artifacts/" + artifactName + "/v1/variables")
				.content("[{\"name\":\"var1\", \"value\":\"value1\"}]");
		perform(request)
				.andExpect(status().isOk());

		request = get("/artifacts/" + artifactName + "/v1.json");
		ResultActions result = perform(request)
				.andExpect(status().isOk());
		if (fileName == null)
			result.andExpect(jsonPath("$.packageInfo").doesNotExist());
		else
			result.andExpect(jsonPath("$.packageInfo.name").exists());
		return result;
	}

	/**
	 * 
	 * Setup an environment 'name' containing two servers ('name' and 'name2')
	 * and one artifact 'name' containing itself a version 'v1' This environment
	 * contains a release 'rel1' which associate name[v1] to server 'name'. This
	 * is also the state of the snapshot release.
	 * 
	 * @param name
	 * @return
	 * @throws KwateeException
	 */
	void prepareTestEnvironment(String name, boolean tag) throws Exception {
		createServer(name);
		createServer(name + "2");

		MockHttpServletRequestBuilder request = post("/environments/" + name)
				.content("{\"servers\":[\"" + name + "\",\"" + name + "2\"],\"artifacts\":[\"" + name + "\"]}");
		perform(request)
				.andExpect(status().isCreated());

		request = put("/environments/" + name + "/snapshot")
				.content("{\"servers\":[{\"server\":\"" + name + "\", \"artifacts\":[{\"artifact\":\"" + name + "\", \"version\":\"v1\"}]}, {\"server\":\"" + name + "2\", \"artifacts\":[{\"artifact\":\"" + name + "\", \"version\":\"v1\"}]}], \"defaultArtifacts\":[{\"artifact\":\"" + name + "\", \"version\":\"v1\"}]}");
		perform(request)
				.andExpect(status().isOk());

		if (tag) {
			request = post("/environments/" + name + "/rel1");
			perform(request)
					.andExpect(status().isCreated());
			request = put("/artifacts/" + name + "/v1")
					.content("{\"preDeployAction\":\"dummyAction\"}");
			perform(request)
					.andExpect(status().isOk());
		}
	}

	ResultActions createServer(String serverName) throws Exception {
		return createServer(serverName, TestConduit.SECRET_PASSWORD, true);
	}

	ResultActions createServer(String serverName, String secret, boolean isPassword) throws Exception {
		if (isPassword)
			secret = "\"password\":\"" + secret + "\"";
		else
			secret = "\"pem\":\"" + secret + "\"";
		MockHttpServletRequestBuilder request = post("/servers/" + serverName)
				.content("{\"ipAddress\":\"test.kwatee.net\", \"conduitType\":\"test\", \"platform\":1,\"credentials\":{\"login\":\"test\", " + secret + "}}");
		return perform(request)
				.andExpect(status().isCreated());
	}

	String encodeLf(String data) {
		return data.replaceAll("\\n", "\\\\n");
	}
}
