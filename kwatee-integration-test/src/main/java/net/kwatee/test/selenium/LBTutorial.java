/*
 ${kwatee_copyright}
 */

package net.kwatee.test.selenium;

import org.openqa.selenium.By;

/**
 * 
 * @author kwatee
 *         Pre-requisites:
 *         1) Log in to http://aws.amazon.com
 *         2) Go to AWS Management Console
 *         3) Select Amazon EC2 tab and click "Running Instances" link
 *         4) Click "Launch instance", select "Launch Classic Wizzard" radio button and click "Continue"
 *         5) Select "Community AMIs" tab and type "ami-2e768d47" in "Viewing" text field (can take some time)
 *         6) Click Continue
 *         7) Type 2 into "Number of instances" text field and click "Continue"
 *         8) Keep default Advanced Instance Options and click "Continue"
 *         9) No changes to Instance Details, click "Continue"
 *         10) No changes, click "Continue"
 *         11) Select "web_access" Security Group and click "Continue"
 *         12) Click "Launch" and then click "Close"
 *         13) Click "Load Balancers" link on the left area and then click "Create Load Balancer"
 *         14) Type "webservers" into Load Balancer Name field and click "Continue"
 *         15) No changes, click "Continue"
 *         16) Select the two previously created instances and click "Continue"
 *         17) Click "Create" and then click "Close"
 * 
 */
public class LBTutorial {
	private final static String INSTANCE_ID = "i-20200c42"; // edit according to step 12
	private UIUtils utils;

	public LBTutorial(UIUtils utils) throws Exception {
		this.utils = utils;
	}

	protected void start() {
		createEC2LBPool();
		createLBWebsiteDeployment();
		deployLBWebsite();
	}

	private void createEC2LBPool() {
		utils.gotoMainTab("server", true);
		utils.clickElementById("new_server_pool");
		utils.selectListItem("poolTypes", "Amazon EC2");
		utils.typeKeys("poolName", "linuxELB", true);
		utils.clickElementById("pool_ok");
		utils.waitForElement(By.id("save"), true);

		utils.typeKeys("description", "Amazon Elastic Load Balanced servers", true);
		utils.selectListItem("platform", "linux_64");
		utils.selectListItem("sharedAccess", "SRM (full)");
		utils.typeKeys("login", "root", true);
		utils.clickElementWithOffsetById("Has private key");
		utils.waitForElement(By.id("area_ok"), true);
		utils.typeKeys("editor",
				"-----BEGIN RSA PRIVATE KEY-----\n" +
						"MIIEowIBAAKCAQEA1m4nzj8ip9m7EnbcfsDoAv4zrf6IXHnM1mH5hZvW8ltc9vocahZRpLXvDG/e\n" +
						"5JLaA7MhtdcA4/RYTfoSobJJLciwMxFFclwPj7Ne0qIaeICxJCzksamZn6iDJLoBnQADV6RciZDN\n" +
						"igXI1zD8uY0r+DONoKR0M8NnJqSO8HmkvGLe8e3ohRYkjJsEq3+fQ6uVSm7Ks+cNOGL0z5zwkmlF\n" +
						"2pkmTwzbj8m9gNRJSE/1uYC+tsipcL/z6J+P1utFbdSKWj9qD3Afx3/0duLiuZVBmAiUys8CSTkt\n" +
						"B529COtKm6gkxCe+/JDAfzMTFBcrBbR26cmvq15lYBu6S5uku2RPgQIDAQABAoIBABlUvgMRVki5\n" +
						"4e2WeQnIRCBGY1iEnxs9kEMrI8zy0fuja2IJvd2ScWahz3GKrawW5QZW3P6cPZCwRtY/WoAbjaWI\n" +
						"9beyrJ+L3JK/P85mM/ZUTZjWbNXIEUvZHlTyXEptu/dYhU4C+yT49fbl5JUO5kjV3j9B7jBHPyJD\n" +
						"gldKJ3nRhLnPzav+iMsTHyEsAya5/p4Z1XLR2l7M9GYOXflQdS7oe0t+OebG2jwMk4si1CrT0avs\n" +
						"QGbnY/TlnbdrEeIUd6PCmPEZdyEVYxzWV4pCJILMXZLjVM1r3oiWC62Xdv5AMP/05g7HD1Hs6aSr\n" +
						"45Yahi+/+X8yrGRu6x8TRFAPkvkCgYEA/XXsSzXfOGC5aA6nSqiJiYmDAqn2PetYvX20MxJDAzQr\n" +
						"xPsfLKWuOvW37B4pdXK6CAxYab0Rhv8fPel8EMLAQnx1pNl6snmrAg0rR+CG34cKKe+Wcwpigy88\n" +
						"LvnJTE0TLHWvXvYNg94fuswCxce3z0yREWjju+aHBM2QaBPN0V8CgYEA2JQglOrRcuWVwC6yOjHP\n" +
						"zGUuD7SFLCE3WFpZTBfo8FX0aqdFhxHDH4NHwrAfg5gvplbqUbfJ15MlXf7eET/M3dkVu9R3LANI\n" +
						"eXLcj4TyHnKTqrL7do2UmSorDYJMXXs/ngfHEpg7esLth/of5ODvOF6ne02Eh4MgdlX0YA7TKx8C\n" +
						"gYBL7fTAvz3E1O0WKDWjnwO86S4PMT1sZUGrWqoOFq1um0V1eElphaTBUvUrTgnbfmgOmywtWQ+U\n" +
						"In+Ie5bIKp+QC1ru7JveNaauMaXCnZeqBPldgMHQas8CP7dG11ufeQOCcSr9Rrbbx0I4In++IkuF\n" +
						"VYmr7oEyvCe8n5xgIXap/wKBgQDADfsmk2TQwo6dMcuSl4Fx+3dxLd+6VmpByzzMoZdLKK3pthON\n" +
						"x06Er2H/XzdS9q2qkACSHqy6oh3M0KoUGcOOJ2eCfNKBERg/un+kNXyPS4NvJ/CeHMbdW+t5u0YE\n" +
						"z7qGZNc/wgdcwWbp4gBOcrkv/5/9U6xmjv1GcsqWNA9GeQKBgFXamMt61x27fITeB3v/2esJLCGy\n" +
						"RapSjfi0vKwODF/GNiudbxrwGDqij66hyAOlEuUJOzBHiwvFtvIv6hy53XkW6GToZfQpaxS1q80x\n" +
						"r5WxUzeCCxtdvsco5dyKU8e7uGuMItG9t4790dX5KVnYaMNMWPVbEKpRlQTIIGl6z262\n" +
						"-----END RSA PRIVATE KEY-----",
						true
				);
		utils.clickElementById("area_ok");
		utils.selectListItem("concurrency", "one at a time");
		utils.typeKeys("accessKey", "AKIAJXXGFNLTRJTDC42Q", true);
		utils.typeKeys("secretKey", "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", true);
		utils.typeKeys("filter", "webservers", true);
		// utils.selectRadio("Load Balancer");
		utils.clickElementById("save");
		utils.clickElementById("instances");
		utils.waitForElement(By.id("instances_ok"), true);
		utils.delay(1000);
		utils.clickElementById("instances_ok");
	}

	private void createLBWebsiteDeployment() {
		utils.gotoMainTab("environment", true);
		utils.clickRowButton("mywebsite", "edit");
		utils.clickElementById("servers");
		utils.clickElementById("servers");
		utils.clickElementWithOffsetById("linuxELB");
		utils.clickElementById("select_ok");
		utils.clickElementById("save");
		utils.clickRowButton("SKETCH", "edit");
		utils.selectPackageTreeItem("linux");
		utils.clickElementById("change_server");
		// utils.selectRadio("linuxELB");
		utils.clickElementById("select_ok");
		utils.clickElementById("freeze");
		utils.typeKeys("prompt", "production", false);
		utils.clickElementById("prompt_ok");
		utils.typeKeys("description", "Kwatee site on the cloud", true);
		utils.clickElementById("save");
	}

	private void deployLBWebsite() {
		utils.clickElementById("manage");
		utils.clickElementById("deploy");
		utils.waitForElement(By.id("clear"), true);
		utils.clickRowButton("linuxELB:" + INSTANCE_ID, "edit");
		utils.delay(1000);
		utils.clickElementById("status_ok");
		utils.clickElementById("clear");
		utils.waitForElement(By.id("edit"), true);
		utils.delay(1000);
	}
}

/*
 * Setup overlay for myservice@linux
 */
// utils.toggleTreeFolder("linux");
// utils.selectPackageTreeItem("linux", "myservice");
// utils.clickButton("customize");
// utils.delay(3000);
// utils.toggleTreeFolder("data");
// utils.selectPackageTreeItem("data", "message.txt");
// utils.clickButton("tree_view");
// utils.delay(2000);
// utils.selectWindow("kwateeview");
// utils.delay(1000);
// utils.closeWindow();
// utils.delay(1000);
// utils.selectPackageTreeItem("data");
// utils.clickButton("tree_add_overlay");
// utils.waitForElement(By.id("upload_file_ok"), true, 5);
// utils.selectRadio("Import");
// utils.typeKeys("local_path", UIUtils.KWATEE_FOLDER+"examples/overlays/message.txt");
// utils.clickButton("upload_file_ok");
// utils.delay(1000);
// utils.toggleTreeFolder("data");
// utils.selectPackageTreeItem("data", "message.txt");
// utils.clickButton("tree_view");
// utils.delay(2000);
// utils.selectWindow("kwateeview");
// utils.delay(1000);
// utils.closeWindow();
// utils.delay(1000);
// utils.clickButton("back");
// utils.delay(3000);
