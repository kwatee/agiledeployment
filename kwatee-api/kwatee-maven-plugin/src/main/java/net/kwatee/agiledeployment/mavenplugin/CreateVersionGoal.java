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

import java.io.IOException;

import net.kwatee.agiledeployment.client.KwateeAPI;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Create version
 * 
 * @goal create_version
 * 
 * @phase package
 * 
 */
public class CreateVersionGoal extends AbstractMojo {

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
	 * @parameter property="description"
	 */
	private String	description;
	/**
	 * @parameter property="duplicateFrom"
	 */
	private String	duplicateFrom;

	public void setServiceurl(String url) {
		this.serviceUrl = url;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDuplicateFrom(String duplicateFrom) {
		this.duplicateFrom = duplicateFrom;
	}

	public void execute() throws MojoExecutionException {
		Log log = getLog();
		KwateeAPI session = null;
		try {
			log.debug("Creating kwatee session");
			session = KwateeAPI.createInstance(this.serviceUrl);
			if (this.duplicateFrom == null) {
				log.debug("Creating version");
				session.createVersion(this.artifact, this.version, JsonUtils.jsonDescription(this.description));
			} else {
				log.debug("Duplicating version");
				session.duplicateVersion(this.artifact, this.version, this.duplicateFrom, JsonUtils.jsonDescription(this.description));
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
