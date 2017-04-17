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
 * Download installer
 * 
 * @goal download_installer
 * 
 */
public class DownloadInstallerGoal extends AbstractMojo {

	/**
	 * The kwatee service url
	 * 
	 * @parameter property="serviceUrl"
	 * @required
	 */
	private String serviceUrl;

	/**
	 * @parameter property="environment"
	 * @required
	 */
	private String environment;
	/**
	 * @parameter property="release"
	 */
	private String release;
	/**
	 * @parameter property="file"
	 * @required
	 */
	private File file;
	/**
	 * @parameter property="lightweight"
	 */
	private Boolean lightweight;

	public void setServiceurl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public void setFile(File installerFile) {
		this.file = installerFile;
	}

	public void setLightweight(Boolean lightweight) {
		this.lightweight = lightweight;
	}

	public void execute() throws MojoExecutionException {
		Log log = getLog();
		KwateeAPI session = null;
		try {
			log.debug("Creating kwatee session");
			session = KwateeAPI.createInstance(this.serviceUrl);
			if (this.lightweight != null && this.lightweight) {
				log.debug("Downloading lightweight installer");
				session.downloadLightweightInstaller(this.environment, this.release, this.file);
			} else {
				log.debug("Downloading installer");
				session.downloadInstaller(this.environment, this.release, this.file);
			}
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
