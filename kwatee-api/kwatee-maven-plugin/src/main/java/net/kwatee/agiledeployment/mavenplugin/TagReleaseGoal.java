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
 * Tag release in environment
 * 
 * @goal tag_release
 * 
 */
public class TagReleaseGoal extends AbstractMojo {

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
	 * @required
	 */
	private String release;
	/**
	 * @parameter property="description"
	 */
	private String description;

	public void setServiceurl(String url) {
		this.serviceUrl = url;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void execute() throws MojoExecutionException {
		Log log = getLog();
		KwateeAPI session = null;
		try {
			log.debug("Creating kwatee session");
			session = KwateeAPI.createInstance(this.serviceUrl);
			log.debug("Tagging release");
			session.tagRelease(this.environment, this.release, JsonUtils.jsonDescription(this.description));
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
