/*
 * KWATEE CONFIDENTIAL
 * ___________________
 * 
 * 2010-2012 Kwatee Ltd
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains
 * the property of Kwatee Ltd and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Kwatee Ltd and its suppliers
 * and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Kwatee Ltd.
 */

package net.kwatee.agiledeployment.mavenplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Update version packages
 * 
 * @goal help
 * 
 */
public class HelpGoal extends AbstractMojo {

	public void execute() throws MojoExecutionException {
		getLog().info("The Kwatee plugin has 6 goals:");
		getLog().info("");
		getLog().info("1. update_package - updates one or more packages for versions configured in kwatee");
		getLog().info("   Parameters:");
		getLog().info("   serviceurl: url of kwatee service: e.g. http://admin:password@localhost:8080/kwatee");
		getLog().info("   artifact: name of kwatee artifact");
		getLog().info("   version: name of kwatee artifact version");
		getLog().info("   file: local file or directory");
		getLog().info("   deleteoverlays: if true deletes existing version overlays (optional)");
		getLog().info("");
		getLog().info("2. deploy - deploys packages onto target servers");
		getLog().info("   Parameters:");
		getLog().info("   serviceurl: url of kwatee service: e.g. http://admin:password@localhost:8080/kwatee");
		getLog().info("   operations: list of operation objects");
		getLog().info("      action: start | stop | deploy");
		getLog().info("      environment: name of kwatee environment");
		getLog().info("      release: name of kwatee environment release");
		getLog().info("      server: name of kwatee server to deploy on (optional)");
		getLog().info("      artifact: name of kwatee artifact to deploy (optional)");
		getLog().info("3. generate_installer - creates an installer for off-line deployments");
		getLog().info("   Parameters:");
		getLog().info("   serviceurl: url of kwatee service: e.g. http://admin:password@localhost:8080/kwatee");
		getLog().info("    environment: name of kwatee environment");
		getLog().info("    release: name of kwatee environment release");
		getLog().info("    file: generated installer file (should end in .zip, .tar.gz or .tgz)");
		getLog().info("    servers: list of kwatee servers to include in installer (optional)");
		getLog().info("4. create_version - creates or duplicates of kwatee package version");
		getLog().info("   Parameters:");
		getLog().info("   serviceurl: url of kwatee service: e.g. http://admin:password@localhost:8080/kwatee");
		getLog().info("   operations: list of operation objects");
		getLog().info("      artifact: name of kwatee artifact in which to create version");
		getLog().info("      version: name of version to create");
		getLog().info("      description: version description (optional)");
		getLog().info("      template: name of existing kwatee package version to duplicate (optional)");
		getLog().info("5. active_version - sets the active version of an artifact within an environment");
		getLog().info("   Parameters:");
		getLog().info("   serviceurl: url of kwatee service: e.g. http://admin:password@localhost:8080/kwatee");
		getLog().info("   operations: list of operation objects");
		getLog().info("      environment: name of kwatee environment");
		getLog().info("      artifact: name of kwatee artifact for which to set the active version");
		getLog().info("      version: name of the active version to set");
		getLog().info("      server: name of kwatee server for which active version is to be set (optional, default all)");
		getLog().info("6. tag_release - tags a snapshot release");
		getLog().info("   Parameters:");
		getLog().info("   serviceurl: url of kwatee service: e.g. http://admin:password@localhost:8080/kwatee");
		getLog().info("   operations: list of operation objects");
		getLog().info("      environment: name of kwatee environment");
		getLog().info("      release: name of tag to create");
		getLog().info("      description: release description (optional)");
	}
}
