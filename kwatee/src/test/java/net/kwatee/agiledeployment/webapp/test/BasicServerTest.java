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

public class BasicServerTest extends AbstractTestBase {

	@Test
	public void checkDataIntegrity1() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/checkDataIntegrity1")
				.content("{\"disabled\":true}");
		perform(request)
				.andExpect(status().isCreated());
		request = put("/servers/checkDataIntegrity1")
				.content("{\"description\":\"test server\", \"ipAddress\":\"test.kwatee.net\", \"port\":4242, \"platform\":5, \"credentials\":{\"login\":\"user\"}}");
		perform(request)
				.andExpect(status().isOk());

		request = get("/servers/checkDataIntegrity1.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("checkDataIntegrity1")))
				.andExpect(jsonPath("$.description", is("test server")))
				.andExpect(jsonPath("$.ipAddress", is("test.kwatee.net")))
				.andExpect(jsonPath("$.port", is(4242)))
				.andExpect(jsonPath("$.platform", is(5)))
				.andExpect(jsonPath("$.disabled", is(true)))
				.andExpect(jsonPath("$.credentials.login", is("user")));
	}

	@Test
	public void checkDataIntegrity2() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/checkDataIntegrity2")
				.content("{\"poolType\":\"manual\", \"description\":\"test server pool\", \"ipAddress\":\"test.kwatee.net\", \"port\":4242, \"platform\":5, \"credentials\":{\"login\":\"user\"}, \"poolConcurrency\":5, \"poolProperties\":{\"p1\":\"value1\", \"p2\":\"value2\"}}");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/servers/checkDataIntegrity2.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", is("checkDataIntegrity2")))
				.andExpect(jsonPath("$.description", is("test server pool")))
				.andExpect(jsonPath("$.ipAddress", is("test.kwatee.net")))
				.andExpect(jsonPath("$.port", is(4242)))
				.andExpect(jsonPath("$.platform", is(5)))
				.andExpect(jsonPath("$.credentials.login", is("user")))
				.andExpect(jsonPath("$.poolType", is("manual")))
				.andExpect(jsonPath("$.poolConcurrency", is(5)))
				.andExpect(jsonPath("$.poolProperties.p1", is("value1")))
				.andExpect(jsonPath("$.poolProperties.p2", is("value2")));
	}

	@Test
	public void duplicateError() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/duplicateError");
		perform(request)
				.andExpect(status().isCreated());
		perform(request)
				.andExpect(status().is(420));
	}

	@Test
	public void fetchError() throws Exception {
		MockHttpServletRequestBuilder request = get("/servers/fetchError.json");
		perform(request)
				.andExpect(status().isNotFound());
	}

	@Test
	public void deleteNoError() throws Exception {
		MockHttpServletRequestBuilder request = delete("/servers/deleteDummy");
		perform(request)
				.andExpect(status().isOk());
	}

	@Test
	public void badPlatformError() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/badPlatformError")
				.content("{\"ipAddress\":\"test.kwatee.net\", \"platform\":12}");
		perform(request)
				.andExpect(status().is(404));
	}

	@Test
	public void createDelete() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/createDelete1");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/servers/createDelete2");
		perform(request)
				.andExpect(status().isCreated());
		request = post("/servers/createDelete3");
		perform(request)
				.andExpect(status().isCreated());

		request = get("/servers.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("createDelete1", "createDelete2", "createDelete3")));

		request = delete("/servers/createDelete2");
		perform(request)
				.andExpect(status().isOk());

		request = get("/servers.json");
		perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", hasItems("createDelete1", "createDelete3")))
				.andExpect(jsonPath("$[*].name", not(hasItem("createDelete2"))));
	}

	@Test
	public void pemFile() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/pemFile")
				.content("{\"credentials\":{\"pem\":\"/dummyPemPath\"}}");
		perform(request)
				.andExpect(status().is(201));
	}

	@Test
	public void badPemTypeError() throws Exception {
		String wrongPem = "-----BEGIN CERTIFICATE-----" +
				"MIICUTCCAfugAwIBAgIBADANBgkqhkiG9w0BAQQFADBXMQswCQYDVQQGEwJDTjEL" +
				"MAkGA1UECBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMC" +
				"VU4xFDASBgNVBAMTC0hlcm9uZyBZYW5nMB4XDTA1MDcxNTIxMTk0N1oXDTA1MDgx" +
				"NDIxMTk0N1owVzELMAkGA1UEBhMCQ04xCzAJBgNVBAgTAlBOMQswCQYDVQQHEwJD" +
				"TjELMAkGA1UEChMCT04xCzAJBgNVBAsTAlVOMRQwEgYDVQQDEwtIZXJvbmcgWWFu" +
				"ZzBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQCp5hnG7ogBhtlynpOS21cBewKE/B7j" +
				"V14qeyslnr26xZUsSVko36ZnhiaO/zbMOoRcKK9vEcgMtcLFuQTWDl3RAgMBAAGj" +
				"gbEwga4wHQYDVR0OBBYEFFXI70krXeQDxZgbaCQoR4jUDncEMH8GA1UdIwR4MHaA" +
				"FFXI70krXeQDxZgbaCQoR4jUDncEoVukWTBXMQswCQYDVQQGEwJDTjELMAkGA1UE" +
				"CBMCUE4xCzAJBgNVBAcTAkNOMQswCQYDVQQKEwJPTjELMAkGA1UECxMCVU4xFDAS" +
				"BgNVBAMTC0hlcm9uZyBZYW5nggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEE" +
				"BQADQQA/ugzBrjjK9jcWnDVfGHlk3icNRq0oV7Ri32z/+HQX67aRfgZu7KWdI+Ju" +
				"Wm7DCfrPNGVwFWUQOmsPue9rZBgO" +
				"-----END CERTIFICATE-----";
		MockHttpServletRequestBuilder request = post("/servers/badPemTypeError")
				.content("{\"credentials\":{\"pem\":\"" + wrongPem + "\"}}");
		perform(request)
				.andExpect(status().is(406));
	}

	@Test
	public void pem() throws Exception {
		String pem = "-----BEGIN RSA PRIVATE KEY-----\\n" +
				"MIIEpAIBAAKCAQEA5Vr5vER3XkvTFeulgLr23h8PRug+fAtrZeLitn8D4Hxg5/AF\\n" +
				"P5np7hyPiMMqxf/rxY01Sn6u1hlBNkeJdz2YpPzGm5318wK5iCSTfL+HxWBO06Ep\\n" +
				"ISN0eeTa1+PB9oFnkrH3OIMrIcZji3kdyBD8/cnn8rejtw9uXhVn0O6uZqE9vqkR\\n" +
				"pj19iqfppCBYp/LPqAi6+YeRJYUWQ0xi1cZRtWWcMTVegX2xsOzEBXHl1ByQt+Lh\\n" +
				"NtpLbEEVmrxBcPVxAK5mKAGoPkZ1lzrTWHEHM6K32E8AM/evhw3pgGIozkvuQaqb\\n" +
				"IyG20PvGbgBF/ipFe8BlK/j/xxrnVaaOy3R1WwIDAQABAoIBAQDFr+yhwcHBnUmM\\n" +
				"E1jL5Fr0iYD5dSSmdpFTeIRBGKmWzJ4aTF+5ukhP3H47Oz2a/EOUO2o7k4XPNGVp\\n" +
				"C7AI/yaazuXBLB3aH/cayav2UoAMOD65WqfA0HaMuh2HB+EfP9quLocQRr7elckn\\n" +
				"ZnWtBDeL6IPsSSzrlHlkzr5078WS3rYbA2cLDpT+nwpgDyvovEQ6ay3RoUMpOTfj\\n" +
				"GXk00da6d6uq8Rg5MYQ/4e6Q71ysOT0U4Jn4mI1qn3yb2v3PKNZXsdveezIjL0WP\\n" +
				"EaPWBminDvkpBZOpf7ip//9zUK5Ecp6a6UpZsLzHcxotpB1BY45voGhOiUT9Cdrc\\n" +
				"f1xGOggBAoGBAPmjbZiONW31r63CRurjeNqbOrOHgsFA7xxROEujDiUzyGo8e+Bz\\n" +
				"A+A4WWcKMSiP0SO9qt00ZZ+5s66TSs2GO/mvE9uhLG/1/wJOhryd8P9KxEPipqbg\\n" +
				"/70qzvVKZ+vwDSzmX1xiz8cjhLvHwTKWPn1rLqGgr6XqsZOAEZkQ1TiBAoGBAOsz\\n" +
				"OgNzqO0dmSrh6jJ0NLkW4vKKWbNihH8FGz/rt6ZzQ53El4r9SGnh+oRg0Fuv38b4\\n" +
				"2gKpp5UYjZZTzI4n91RLJjelJY8oZ3Xm48mv50GahcVjsuDHXpzKIh/PqfYAbhdU\\n" +
				"Xp4yzQfSqeaSYtjKyTsWxjnQdHFcuY+zF8QkdZ/bAoGAQPINnzTQHa1faRs0DAPl\\n" +
				"+ymLWg2VLOXRz1IqDTN8iJ0yNMFLkRcbGQhP6giyxVS1GlFL1IS/M5DCeFaFjXaw\\n" +
				"v8KWrfr+bppXH6iCUSvd+OzgprCenqfGoNY0RYh4BN/M7bN5d7WVTL+m1pufXaPM\\n" +
				"2iA6X35IdCfczvzwlxvT/QECgYBFSvZQhgbCtPGnPJ+u9aWC0kdShYgf3WqDsZFx\\n" +
				"p5SXqWXGWWD8RdrLtxQDZDJ+kLYw/KfeGPWSa4VrQI0HZKbtYqaDxlfmcVTp23hI\\n" +
				"t1Y+cbPvj4vnxVUOhE7BaID+ROoqXWTy3qyhHWOI1p/glv3qNq934P4tV9AjkfPy\\n" +
				"iXtN+QKBgQDZ5CvALoK3xeK2bPFALWTTvhTVzcfduU2Sg5KZB7VQ5asnBGM/R/+f\\n" +
				"IgFEV9PJz97ERjoRtYS7vZ/2tztYBzm2+enH2DVF/z7BhU0ToQl9s5CP8FIdJOv1\\n" +
				"8hSb51a1ctW7gWKCe2QEWrUfgoaKfLe1mvdO+5PnQiSH8/36eKX3eg==\\n" +
				"-----END RSA PRIVATE KEY-----";
		MockHttpServletRequestBuilder request = post("/servers/pem")
				.content("{\"credentials\":{\"pem\":\"" + pem + "\"}}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createLinux86Server() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/linux86")
				.content("{\"platform\":1}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createLinux64Server() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/linux64")
				.content("{\"platform\":7}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createSolaris86Server() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/solarisx86")
				.content("{\"platform\":3}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createSolarisSparcServer() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/solarissparc").content("{\"platform\":4}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createMacosxServer() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/macos")
				.content("{\"platform\":2}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createAixServer() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/aix")
				.content("{\"platform\":8}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createWin32Server() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/win32")
				.content("{\"platform\":5}");
		perform(request)
				.andExpect(status().isCreated());
	}

	@Test
	public void createWin64Server() throws Exception {
		MockHttpServletRequestBuilder request = post("/servers/win64")
				.content("{\"platform\":9}");
		perform(request)
				.andExpect(status().isCreated());
	}
}
