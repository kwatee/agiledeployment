/*
 ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openqa.selenium.By;

public class TestsMain {
	private UIUtils utils;
	private StringBuilder verificationErrors = new StringBuilder();

	@BeforeClass
	public static void setup() throws IOException {
		UIUtils.setup();
	}

	@AfterClass
	public static void cleanup() {
		UIUtils.cleanup();
	}

	@Before
	public void testSetUp() throws Exception {
		utils = new UIUtils("http://127.0.0.1:8080/kwatee/");
	}

	@After
	public void testTearDown() throws Exception {
		utils.stop();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString)) {
			fail(verificationErrorString);
		}
	}

	// @Test
	// public void introTutorial() throws Exception {
	// utils.delay(10000);
	// login();
	// new IntroTutorial(utils).start();
	// utils.clickButton("logout");
	// }

	/*
	 * @Test
	 * public void brandingTutorial() throws Exception {
	 * // utils.delay(10000);
	 * login();
	 * new BrandingTutorial(utils).start();
	 * }
	 */

	/*
	 * @Test
	 * public void loadBalancerTutorial() throws Exception {
	 * // utils.delay(10000);
	 * login();
	 * new LBTutorial(utils).start();
	 * }
	 */

	/*
	 * @Test
	 * public void tomcatWebappTutorial() throws Exception {
	 * // utils.delay(10000);
	 * login();
	 * new TomcatWebappTutorial(utils).start();
	 * }
	 */

	void login() {
		utils.typeKeys("j_username", "admin", true);
		utils.typeKeys("j_password", "password", true);
		utils.clickElementById("login");
		utils.waitForElement(By.id("new"), true);
	}
}