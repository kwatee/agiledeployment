/*
 * ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;

abstract public class AbstractUITestsBase {

	private final static Logger LOG = Logger.getLogger(AbstractUITestsBase.class.getName());
	UIUtils utils;
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
		utils = new UIUtils("http://deploy.kwatee.net:8080/kwatee/");
	}

	@After
	public void testTearDown() throws Exception {
		utils.stop();
		String verificationErrorString = verificationErrors.toString();
		if (!"".equals(verificationErrorString)) {
			fail(verificationErrorString);
		}
	}

	@Test
	@Ignore
	public void brandingTutorial() throws Exception {
		login();
		new BrandingTutorial(utils).start();
	}

	@Test
	@Ignore
	public void loadBalancerTutorial() throws Exception {
		login();
		new LBTutorial(utils).start();
	}

	@Test
	@Ignore
	public void tomcatWebappTutorial() throws Exception {
		login();
		new TomcatWebappTutorial(utils).start();
	}

	void login() {
		utils.typeKeys("userName", "admin", true);
		utils.typeKeys("pwd", "password", true);
		utils.clickElementById("login");
		utils.waitForElement(By.id("import"), true);
	}

	void logout() {
		utils.clickElementById("logout", 0, -10, false);
	}

	void createArtifact(String name, String description) {
		LOG.log(Level.INFO, "Create artifact {0}", name);
		utils.gotoMainTab("artifacts", true);
		enterPromptText("new", name);
		if (description != null) {
			utils.waitForElement(By.id("save"), true);
			utils.typeKeys("description", description, true);
			utils.clickElementById("save");
		}
	}

	void createServer(String name, String desc, String platform, String user, String password, String addr) {
		LOG.log(Level.INFO, "Create server {0}", name);
		utils.gotoMainTab("servers", true);
		enterPromptText("newServer", name);
		utils.waitForElement(By.id("save"), true);

		if (!utils.debugMode())
			utils.delay(1000);
		utils.typeKeys("description", desc, true);
		if (platform != null)
			utils.selectListItem("platform", platform);
		utils.typeKeys("ip", addr, true);
		if ("windows".equals(platform))
			utils.selectListItem("conduitType", "Telnet / FTP");
		if (!utils.debugMode())
			utils.delay(1000);
		utils.typeKeys("login", user, true);
		utils.typeKeys("password", password, true);
		utils.clickElementById("save");
		utils.clickElementById("testConnection");
		utils.delay(1000);
		utils.clickElementById("ok");
	}

	void createEnvironment(String name, String[] artifacts, String[] servers, String description) {
		LOG.log(Level.INFO, "Create environment {0}", name);
		utils.gotoMainTab("environments", true);
		enterPromptText("new", name);
		utils.waitForElement(By.id("save"), true);
		if (description != null) {
			utils.typeKeys("description", "introductory tutorial", true);
		}
		if (artifacts != null) {
			utils.delay(350);
			utils.clickElementById("addArtifact");
			for (String artifact : artifacts) {
				utils.delay(500);
				utils.clickElementWithOffsetByValue(artifact);
			}
			utils.clickElementById("ok");
			utils.delay(350);
		}
		if (servers != null) {
			utils.clickElementById("addServer");
			for (String server : servers) {
				utils.delay(500);
				utils.clickElementWithOffsetByValue(server);
			}
			utils.clickElementById("ok");
			utils.delay(350);
		}
		utils.clickElementById("save");
	}

	void enterPromptText(String buttonId, String text) {
		if (buttonId != null) {
			utils.clickElementById(buttonId);
		}
		utils.waitForElement(By.id("ok"), true);
		utils.typeKeys("askValue", text, false);
		utils.clickElementById("ok");
	}

}