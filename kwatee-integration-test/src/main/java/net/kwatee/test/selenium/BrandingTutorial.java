/*
 ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import org.openqa.selenium.By;

class BrandingTutorial {
	private UIUtils utils;

	protected BrandingTutorial(UIUtils utils) throws Exception {
		this.utils = utils;
	}

	protected void start() {
		createArtifactAndVersion();
		createEnvironment();
		setupDeployment();
		deployAndViewWebsite();
		utils.clickElementById("edit");
		configureDeploymentBranding();
		deployAndViewWebsite();
	}

	private void createArtifactAndVersion() {
		utils.gotoMainTab("repository", true);
		utils.clickElementById("new");
		utils.typeKeys("prompt", "mywebsite", false);
		utils.clickElementById("prompt_ok");
		utils.waitForElement(By.id("save"), true);
		utils.delay(1000);
		utils.typeKeys("description", "generic PHP web site", true);
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
		utils.typeKeys("local_path", UIUtils.KWATEE_FOLDER + "examples/packages/mywebsite", true);
		utils.clickElementById("upload_pkg_ok");
	}

	private void createEnvironment() {
		utils.gotoMainTab("environment", true);
		utils.clickElementById("new");
		utils.typeKeys("prompt", "mywebsite", false);
		utils.clickElementById("prompt_ok");
		utils.waitForElement(By.id("save"), true);
		utils.clickElementById("packages");
		utils.clickElementById("packages");
		utils.waitForElement(By.id("select_ok"), true);
		utils.clickElementWithOffsetById("mywebsite");
		utils.clickElementById("select_ok");
		utils.clickElementById("save");
		utils.clickElementById("servers");
		utils.clickElementById("servers");
		utils.waitForElement(By.id("select_ok"), true);
		utils.clickElementWithOffsetById("linux");
		utils.clickElementById("select_ok");
		utils.clickElementById("save");
		utils.typeKeys("description", "web site", true);
		utils.clickElementById("save");
	}

	private void setupDeployment() {
		utils.clickRowButton("SKETCH", "edit");
		utils.selectPackageTreeItem("linux");
		utils.clickElementById("add_package");
		utils.waitForElement(By.id("select_ok"), true);
		utils.clickElementWithOffsetById("mywebsite");
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
		utils.typeKeys("varname", "kwatee_deployment_dir", true);
		utils.typeKeys("varvalue", "/var/www", true);
		utils.clickElementById("variable_ok");
		utils.clickElementById("save");
		utils.clickElementById("back");
	}

	private void deployAndViewWebsite() {
		utils.clickElementById("manage");
		utils.clickElementById("deploy");
		utils.waitForElement(By.id("clear"), true);
		utils.clickElementById("clear");
		utils.waitForElement(By.id("edit"), true);
		utils.showWindow("http://ubuntu.kwatee.local/mywebsite/");
	}

	private void configureDeploymentBranding() {
		utils.delay(1000);
		utils.toggleTreeFolder("linux");
		utils.clickTreeItemButton("linux", "mywebsite");
		utils.clickElementById("customize");
		utils.clickElementById("tree_add_overlay");
		utils.waitForElement(By.id("upload_file_ok"), true);
		utils.selectRadio("Import");
		utils.typeKeys("local_path", UIUtils.KWATEE_FOLDER + "examples/overlays/logo.gif", false);
		utils.clickElementById("upload_file_ok");
		utils.clickElementById("back");
		utils.clickElementById("freeze");
		utils.typeKeys("prompt", "branded", false);
		utils.clickElementById("prompt_ok");
		utils.typeKeys("description", "Kwatee site", true);
		utils.clickElementById("save");
	}
}