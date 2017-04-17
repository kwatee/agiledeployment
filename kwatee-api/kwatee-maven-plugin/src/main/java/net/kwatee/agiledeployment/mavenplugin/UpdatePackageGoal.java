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

import java.io.File;
import java.io.IOException;

import net.kwatee.agiledeployment.client.KwateeAPI;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Update version packages
 * 
 * @goal update_package
 * 
 * @phase package
 */
public class UpdatePackageGoal extends AbstractMojo {

	/**
	 * The kwatee service url
	 * 
	 * @parameter property="serviceUrl"
	 * @required
	 */
	private String	serviceUrl;

	/**
	 * @parameter property="artifact"
	 * @required
	 */
	private String	artifact;
	/**
	 * @parameter property="version"
	 * @required
	 */
	private String	version;
	/**
	 * @parameter property="file"
	 * @required
	 */
	private File	file;
	/**
	 * @parameter property="deleteOverlays"
	 * @default-value=false
	 */
	private boolean	deleteOverlays;

	public void setServiceurl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setFile(File installerFile) {
		this.file = installerFile;
	}

	public void setDeleteoverlays(boolean deleteOverlays) {
		this.deleteOverlays = deleteOverlays;
	}

	public void execute() throws MojoExecutionException {
		Log log = getLog();
		KwateeAPI session = null;
		try {
			log.debug("Creating kwatee session");
			session = KwateeAPI.createInstance(this.serviceUrl);
			log.debug("Uploading package");
			session.uploadArtifactPackage(this.artifact, this.version, this.file, this.deleteOverlays);
			log.debug("Success");
		} catch (IOException e) {
			log.error(e);
			throw new MojoExecutionException(e.getMessage());
		} finally {
			if (session != null)
				session.close();
		}
	}
}
