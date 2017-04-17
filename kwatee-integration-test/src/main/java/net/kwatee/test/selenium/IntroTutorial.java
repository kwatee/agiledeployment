/*
 * ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class IntroTutorial extends AbstractUITestsBase {

	private final static Logger LOG = Logger.getLogger(IntroTutorial.class.getName());

	@Test
	public void start() {

		try {
			FileUtils.deleteDirectory(new File("/Library/WebServer/Documents/demowebsite"));
		} catch (IOException e) {}
		if (!new File(UIUtils.KWATEE_FOLDER).exists())
			throw new RuntimeException(UIUtils.KWATEE_FOLDER + " does not exist");
		if (!utils.debugMode())
			utils.delay(4000);
		utils.clickElementById("userName", -15, 0, false);
		utils.delay(1000);

		login();
		if (!utils.debugMode()) {
			utils.delay(1000);
		}
		createArtifact("demowebsite", "Demo PHP web site");
		createVersion("1.0");
		utils.back("demowebsite");
		createServer("demoserver", "Demo server", "macosx_x86", "kwtest", "password", "demo.kwatee.net");
		createEnvironment("intro", new String[] {"demowebsite"}, new String[] {"demoserver"}, "introductory tutorial");
		setupPart1Deployment();
		managePart1Deployment();
		setupPart2Deployment();
		managePart2Deployment();

		//		tagDeployment("acme-1.0", "initial deployment");
		logout();
	}

	private void createVersion(String name) {
		LOG.log(Level.INFO, "Create version {0}", name);
		enterPromptText("new", name);
		utils.waitForElement(By.id("save"), true);
		utils.typeKeys("description", "initial release", true);
		utils.clickElementWithOffsetById("linux_x86");
		utils.clickElementWithOffsetById("linux_64");
		utils.clickElementWithOffsetById("macosx_x86");

		LOG.info("Upload package");
		utils.clickElementById("upload", true);
		utils.fillWindowPrompt(UIUtils.KWATEE_FOLDER + "examples/artifacts/mywebsite.zip");
		utils.clickElementById("save");
		utils.delay(250);
		LOG.info("Examine package");
		utils.clickElementWithOffsetById("examine");
		if (!utils.debugMode())
			utils.delay(1000);
		LOG.info("View file");
		utils.clickSpan("index.php");
		if (!utils.debugMode())
			utils.delay(500);
		utils.clickElementById("view");
		utils.delay(500);
		utils.selectTab();
		utils.delay(1000);
		utils.closeWindow();
		utils.delay(500);

		utils.back(name);
		LOG.info("Version variables");
		utils.clickElementById("variables");
		utils.delay(1000);
	}

	private void setupPart1Deployment() {
		LOG.info("Configure release");
		utils.clickElement(By.xpath("//div[@id='releasesPanel']/descendant::a[.='snapshot']"), 0, -8, false);
		utils.clickElementById("addArtifact");
		utils.clickElementWithOffsetByValue("demowebsite");
		utils.clickElementById("ok");
		utils.clickElement(By.xpath("//table[@id='defaultArtifactsPanel']/descendant::button[@id='version']"), 0, -8, false);
		utils.clickElementWithOffsetByValue("1.0");
		utils.clickElementById("ok");
		utils.clickElementById("save");
		if (!utils.debugMode())
			utils.delay(1000);
		utils.clickElementWithOffsetById("variables");
		LOG.info("Click link CUSTOMER_NAME");
		WebElement el = utils.waitForElement(By.xpath("//a[.='CUSTOMER_NAME']"));
		utils.mouseMoveTo(el, 0, -15);
		utils.delay(1000);
		utils.clickElementWithOffset(By.xpath("//a[.='CUSTOMER_NAME']"));
		utils.typeKeys("varValue", "ACME Corp.", false);
		if (!utils.debugMode())
			utils.delay(1000);
		utils.clickElementById("ok");
		utils.clickRowButton("kwatee_deployment_dir", "duplicate");
		utils.typeKeys("varValue", "/Library/WebServer/Documents", true);
		if (!utils.debugMode())
			utils.delay(1000);
		else
			utils.delay(1000);
		utils.selectListItem("forArtifact", "demowebsite");
		if (!utils.debugMode())
			utils.delay(1000);
		utils.clickElementById("ok");
		utils.clickElementById("save");
		utils.back("snapshot");
	}

	private void managePart1Deployment() {
		utils.gotoMainTab("deployments", true);
		utils.clickElementWithOffset(By.xpath("//span[.='intro']"));
		utils.clickElementWithOffset(By.xpath("//a[.='snapshot']"));
		utils.clickElementById("deploy");
		utils.waitForElement(By.id("opSuccess"), true);
		// utils.back("snapshot");
		utils.clickElementById("done");
		utils.newBrowserWindow("http://demo.kwatee.net/demowebsite/index.php", false);
	}

	private void setupPart2Deployment() {
		utils.gotoMainTab("environments", false);
		utils.clickElementWithOffset(By.xpath("//div[@id='serverArtifactsPanel']/descendant::a[.='demoserver']"));
		utils.clickElementWithOffset(By.xpath("//div[@id='serverArtifactsPanel']/descendant::button[@id='overlay']"));
		utils.clickElementById("overlay", true);
		utils.fillWindowPrompt(UIUtils.KWATEE_FOLDER + "examples/overlays/logo.gif");
		utils.clickElementById("ok");
		if (!utils.debugMode())
			utils.delay(1000);
		utils.back("snapshot");
	}

	private void managePart2Deployment() {
		utils.gotoMainTab("deployments", false);
		utils.clickElementById("deploy");
		utils.waitForElement(By.id("opSuccess"), true);
		utils.clickElementById("done");
		utils.newBrowserWindow("http://demo.kwatee.net/demowebsite/index.php", true);
	}

	private void tagDeployment(String name, String desc) {
		utils.gotoMainTab("environments", false);
		enterPromptText("tag", name);
		utils.typeKeys("description", desc, true);
		utils.clickElementById("save");
	}
}