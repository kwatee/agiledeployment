/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.conduit.impl.TestConduit;

import org.junit.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class AdvancedServerTest extends AbstractTestBase {

	@Test
	public void passwordLogin() throws Exception {
		createServer("passwordLogin");
		MockHttpServletRequestBuilder request = post("/servers/passwordLogin/testConnection");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(6)));
	}

	@Test
	public void passwordPromptLogin() throws Exception {
		createServer("passwordPromptLogin");
		MockHttpServletRequestBuilder request = put("/servers/passwordPromptLogin")
				.content("{\"credentials\":{\"promptPassword\":true}}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/servers/passwordPromptLogin.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.credentials.promptPassword").exists())
				.andExpect(jsonPath("$.credentials.password").doesNotExist());
		request = post("/servers/passwordPromptLogin/testConnection");
		perform(request)
				.andExpect(status().is(420));
		request = post("/servers/passwordPromptLogin/testConnection")
				.content("{\"credentials\":{\"password\":\"" + TestConduit.SECRET_PASSWORD + "\"}}");
		request = get("/servers/passwordPromptLogin.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.credentials.promptPassword").exists())
				.andExpect(jsonPath("$.credentials.password").doesNotExist());

	}

	@Test
	public void badPasswordLogin() throws Exception {
		createServer("badPasswordLogin");
		MockHttpServletRequestBuilder request = put("/servers/badPasswordLogin")
				.content("{\"credentials\":{\"password\":\"badPassword\"}}");
		perform(request)
				.andExpect(status().isOk());

		request = post("/servers/badPasswordLogin/testConnection");
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void pemLogin() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/pemLogin")
				.content("{\"conduitType\":\"test\", \"ipAddress\":\"test.kwatee.net\", \"platform\":2, \"credentials\":{\"login\":\"test\", \"pem\":\"" + encodeLf(TestConduit.SECRET_PEM) + "\"}}");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/servers/pemLogin/testConnection");
		perform(request)
				.andExpect(status().isOk());
	}

	@Test
	public void manualPool() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/manualPool")
				.content("{\"conduitType\":\"test\", \"poolType\":\"manual\", \"ipAddress\":\"test.kwatee.net\", \"platform\":2, \"credentials\":{\"login\":\"test\", \"password\":\"" + TestConduit.SECRET_PASSWORD + "\"}, \"poolProperties\":{\"pool.1.name\":\"instance1\", \"pool.1.ip_address\":\"test.kwatee.net\", \"pool.1.port\":22}}");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/servers/manualPool/testConnection");
		perform(request)
				.andExpect(status().isOk());
	}

	/**
	 * Using a password protected PEM, and given a password, make sure
	 * connection works
	 * 
	 * @throws KwateeException
	 */
	@Test
	public void passwordSuppliedProtectedPem() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/passwordSuppliedProtectedPem")
				.content("{\"conduitType\":\"test\", \"ipAddress\":\"test.kwatee.net\", \"platform\":2, \"credentials\":{\"login\":\"test\", \"" + TestConduit.SECRET_PASSWORD + "\":\"password\", \"pem\":\"" + encodeLf(TestConduit.SECRET_PEM_ENCRYPTED) + "\"}}");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/servers/passwordSuppliedProtectedPem/testConnection");
		perform(request)
				.andExpect(status().isOk());
	}

	/**
	 * Using a password protected PEM, and given a password, make sure
	 * connection works
	 * 
	 * @throws KwateeException
	 */
	@Test
	public void badPasswordSuppliedProtectedPem() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/badPasswordSuppliedProtectedPem")
				.content("{\"conduitType\":\"test\", \"ipAddress\":\"test.kwatee.net\", \"platform\":2, \"credentials\":{\"login\":\"test\", \"password\":\"badpassword\", \"pem\":\"" + encodeLf(TestConduit.SECRET_PEM_ENCRYPTED) + "\"}}");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/servers/badPasswordSuppliedProtectedPem/testConnection");
		perform(request)
				.andExpect(status().is(420));
	}

	/**
	 * Using a password protected PEM where password is prompted
	 * 
	 * @throws KwateeException
	 */
	@Test
	public void passwordPromptProtectedPem() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/passwordPromptedProtectedPem")
				.content("{\"conduitType\":\"test\", \"ipAddress\":\"test.kwatee.net\", \"platform\":2, \"credentials\":{\"login\":\"test\", \"promptPassword\":true, \"pem\":\"" + encodeLf(TestConduit.SECRET_PEM_ENCRYPTED) + "\"}}");
		perform(request)
				.andExpect(status().isCreated());

		request = post("/servers/passwordPromptedProtectedPem/testConnection");
		perform(request)
				.andExpect(status().is(420));

		request = post("/servers/passwordPromptedProtectedPem/testConnection").content("{\"credentials\":{\"password\":\"" + TestConduit.SECRET_PASSWORD + "\"}}");
		perform(request)
				.andExpect(status().isOk());
	}
}
