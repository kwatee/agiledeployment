/*
 * ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import org.junit.Test;
import org.openqa.selenium.By;

public class TakeScreenShots extends AbstractUITestsBase {

	final static String SCREENSHOTS_FOLDER = "/Users/kwatee/Downloads/selenium_screenshots/";

	@Test
	public void start() {

		utils.enableScreenShot(SCREENSHOTS_FOLDER);
		utils.setDebugMode(true);
		login();

		utils.screenShot("artifacts");
		utils.clickElementWithOffset(By.xpath("//a[.='acme_webapp']"));
		utils.screenShot("artifact");
		utils.clickElementWithOffset(By.xpath("//a[.='1.2']"));
		utils.setWindowHeight(670);
		utils.screenShot("version");
		utils.clickElementWithOffset(By.xpath("//a[.='manager']"));
		utils.screenShot("version_exe");
		utils.clickElementById("cancel");
		utils.clickElementWithOffsetById("examine");
		utils.clickElement(By.xpath("//span[.='WEB-INF']"), -40, -8, false);
		utils.screenShot("version_package");
		utils.clickSpan("web.xml");
		utils.screenShot("version_package_options");
		utils.clickElementById("view");
		utils.delay(1000);
		utils.selectTab();
		utils.screenShot("version_package_browse", -1);
		utils.closeWindow();
		utils.clickElementById("specialFiles");
		utils.screenShot("version_package_special");
		utils.clickElementById("cancel");
		utils.back("1.2");
		utils.clickElementById("variables");
		utils.mouseMoveTo(utils.waitForElement(By.xpath("//a[.='HELLO_MESSAGE']"), true), 0, 0);
		utils.delay(250);
		utils.screenShot("version_variables");
		utils.clickElementWithOffset(By.xpath("//a[.='HELLO_MESSAGE']"));
		utils.screenShot("version_variables_edit");
		utils.clickElementById("cancel");

		utils.gotoMainTab("servers", false);
		utils.screenShot("servers");
		utils.clickElementWithOffset(By.xpath("//a[.='qa_appserver']"));
		utils.selectListItem("platform", "linux_64");
		utils.screenShot("server");
		utils.clickElementById("revert");
		utils.back("Servers");
		utils.clickElementWithOffset(By.xpath("//a[.='prod_appserver_pool']"));
		utils.selectListItem("platform", "linux_64");
		utils.screenShot("serverpool");
		utils.clickElementById("revert");
		utils.back("Servers");
		utils.clickElementWithOffset(By.xpath("//a[.='iis_test_server']"));
		utils.clickElementById("telnetOptions");
		utils.screenShot("server_telnet");
		utils.clickElementById("cancel");

		utils.gotoMainTab("environments", false);
		utils.screenShot("environments");
		utils.clickElementWithOffset(By.xpath("//a[.='acme_prod']"));

		utils.setWindowHeight(700);
		utils.screenShot("environment");
		utils.clickElementById("addArtifact");
		utils.screenShot("environment_artifacts");
		utils.clickElementById("cancel");
		utils.clickElementById("addServer");
		utils.screenShot("environment_servers");
		utils.clickElementById("cancel");

		utils.clickElement(By.xpath("//div[@id='releasesPanel']/descendant::a[.='snapshot']"), 0, -8, false);
		utils.clickElementWithOffset(By.xpath("//div[@id='serverArtifactsPanel']/descendant::a[.='prod_batchserver']"));
		utils.screenShot("release");
		utils.clickElementById("addArtifact");
		utils.screenShot("release_addpackage");
		utils.clickElementById("cancel");

		utils.clickElement(By.xpath("//table[@id='defaultArtifactsPanel']/descendant::button[@id='version']"), 0, -8, false);
		utils.screenShot("release_active_version");
		utils.clickElementById("cancel");
		utils.clickElementById("variables");
		utils.screenShot("release_variables");
		utils.clickElementWithOffset(By.xpath("//a[.='kwatee_deployment_dir']"));
		utils.screenShot("release_var_edit");
		utils.clickElementById("cancel");

		utils.gotoMainTab("deployments", false);
		utils.clickElementWithOffset(By.xpath("//span[.='acme_prod']"));
		utils.screenShot("deployments");
		utils.clickElementWithOffset(By.xpath("//a[.='snapshot']"));
		utils.screenShot("deployment");
		utils.clickElementById("deploy");
		utils.delay(1500);
		utils.screenShot("deploy_status");

		utils.gotoMainTab("admin", false);
		utils.screenShot("admin_users");
		utils.clickElementWithOffset(By.xpath("//a[.='admin']"));
		utils.screenShot("admin_account");
		utils.clickElementById("var", 20, -10, false);
		utils.screenShot("admin_properties");
		utils.clickElementById("prm", 20, -10, false);
		utils.screenShot("admin_parameters");
		utils.clickElementById("maint", 20, -10, false);
		utils.screenShot("admin_impexp");

		logout();
	}
}