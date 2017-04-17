/*
 ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import org.openqa.selenium.By;

class TomcatWebappTutorial {
	private UIUtils utils;

	protected TomcatWebappTutorial(UIUtils utils) throws Exception {
		this.utils = utils;
	}

	protected void start() {
		createPackageAndVersion();
		createEnvironment();
		setupDeployment();
		deployAndViewWebsite();
	}

	private void createPackageAndVersion() {
		utils.gotoMainTab("repository", true);
		utils.clickElementById("new");
		utils.typeKeys("prompt", "mytomcatwebapp", false);
		utils.clickElementById("prompt_ok");
		utils.waitForElement(By.id("save"), true);
		utils.delay(1000);
		utils.typeKeys("description", "sample tomcat web application", true);
		utils.clickElementById("save");

		/*
		 * Create version
		 */
		utils.clickElementById("new");
		utils.typeKeys("prompt", "v1.0", false);
		utils.clickElementById("prompt_ok");
		utils.waitForElement(By.id("save"), true);
		utils.clickElementById("upload");
		utils.waitForElement(By.id("upload_pkg_ok"), true);
		utils.selectRadio("Import");
		utils.typeKeys("local_path", UIUtils.KWATEE_FOLDER + "examples/packages/mytomcatwebapp.war", false);
		utils.clickElementById("upload_pkg_ok");

		utils.clickElementById("new");
		utils.typeKeys("prompt", "main", false);
		utils.clickElementById("prompt_ok");
		utils.waitForElement(By.id("exe_ok"), true);
		utils.typeKeys("exeStartAction", "curl --user %{TOMCAT_USER}:%{TOMCAT_PASSWORD} http://ubuntu.kwatee.local:8080/manager/text/start?path=/mytomcatwebapp", true);
		utils.typeKeys("exeStopAction", "curl --user %{TOMCAT_USER}:%{TOMCAT_PASSWORD} http://ubuntu.kwatee.local:8080/manager/text/stop?path=/mytomcatwebapp", true);
		utils.clickElementById("exe_ok");
		utils.clickElementById("save");
	}

	private void createEnvironment() {
		utils.gotoMainTab("environment", true);
		utils.clickElementById("new");
		utils.typeKeys("prompt", "tomcatwebapp", false);
		utils.clickElementById("prompt_ok");
		utils.waitForElement(By.id("save"), true);
		utils.clickElementById("packages");
		utils.clickElementById("packages");
		utils.waitForElement(By.id("select_ok"), true);
		utils.clickElementWithOffsetById("mytomcatwebapp");
		utils.clickElementById("select_ok");
		utils.clickElementById("save");
		utils.clickElementById("servers");
		utils.clickElementById("servers");
		utils.waitForElement(By.id("select_ok"), true);
		utils.clickElementWithOffsetById("linux");
		utils.clickElementById("select_ok");
		utils.clickElementById("save");
		utils.typeKeys("description", "tomcat web application", true);
		utils.clickElementById("save");
	}

	private void setupDeployment() {
		utils.clickRowButton("SKETCH", "edit");
		utils.selectPackageTreeItem("linux");
		utils.clickElementById("add_package");
		utils.waitForElement(By.id("select_ok"), true);
		utils.clickElementWithOffsetById("mytomcatwebapp");
		utils.clickElementById("select_ok");
		utils.clickElementById("version");
		utils.waitForElement(By.id("select_ok"), true);
		utils.selectRadio("v1.0");
		utils.clickElementById("select_ok");
		utils.clickElementById("save");
		/*
		 * Setup variables
		 */
		utils.clickElementById("variables");
		utils.waitForElement(By.id("new_var"), true);
		utils.clickElementById("new_var");
		utils.typeKeys("varname", "TOMCAT_USER", true);
		utils.typeKeys("varvalue", "admin", true);
		utils.clickElementById("variable_ok");
		utils.clickElementById("new_var");
		utils.typeKeys("varname", "TOMCAT_PASSWORD", true);
		utils.typeKeys("varvalue", "password", true);
		utils.clickElementById("variable_ok");
		utils.clickElementById("new_var");
		utils.typeKeys("varname", "HELLO_MESSAGE", true);
		utils.typeKeys("varvalue", "Hello World", true);
		utils.clickElementById("variable_ok");
		utils.clickElementById("new_var");
		utils.typeKeys("varname", "kwatee_deployment_dir", true);
		utils.typeKeys("varvalue", "/var/lib/tomcat7/webapps", true);
		utils.clickElementById("variable_ok");
		utils.clickElementById("save");
		utils.clickElementById("back");
	}

	private void deployAndViewWebsite() {
		utils.clickElementById("manage");
		utils.waitForElement(By.id("deploy"), true);
		utils.clickElementById("deploy");
		utils.waitForElement(By.id("clear"), true);
		utils.clickElementById("clear");
		utils.waitForElement(By.id("deploy"), true);
		utils.showWindow("http://ubuntu.kwatee.local:8080/mytomcatwebapp");
	}
}