/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.test.api;

import java.io.IOException;

import net.kwatee.agiledeployment.client.KwateeAPI;

import org.junit.After;
import org.junit.Before;

public class APITestBase {

	final static String ARTIFACT = "demowebsite";
	final static String SERVER = "demoserver";
	final static String ENVIRONMENT = "intro";

	KwateeAPI api;

	@Before
	public void setup() throws IOException {
		api = KwateeAPI.createInstance("http://admin:password@localhost:8082/kwatee");
	}

	@After
	public void cleanup() {
		api.close();
	}

}
