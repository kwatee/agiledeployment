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
 * 
 * @phase deploy
 * 
 * @goal deploy
 * 
 */
public class DeployGoal extends AbstractMojo {

	/**
	 * The kwatee service url
	 * 
	 * @parameter property="serviceUrl"
	 * @required
	 */
	private String			serviceUrl;
	/**
	 * @parameter property="environment"
	 * @required
	 */
	private String	environment;
	/**
	 * @parameter property="release"
	 */
	private String	release;
	/**
	 * @parameter property="server"
	 */
	private String	server;
	/**
	 * @parameter property="artifact"
	 */
	private String	artifact;
	/**
	 * Actions
	 * 
	 * @parameter property="actions"
	 * @required
	 */
	private String[]	actions;

	public void setServiceurl(String url) {
		this.serviceUrl = url;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public void setActions(String[] actions) {
		this.actions = actions;
	}

	public void execute() throws MojoExecutionException {
		Log log = getLog();
		KwateeAPI session = null;
		try {
			log.debug("Creating kwatee session");
			session = KwateeAPI.createInstance(this.serviceUrl);
			for (String action : this.actions) {
				String ref;
				if ("deploy".equalsIgnoreCase(action)) {
					log.debug("Deploying");
					ref = session.manageDeploy(this.environment, this.release, this.server, this.artifact);
				} else if ("undeploy".equalsIgnoreCase(action)) {
					log.debug("Undeploying");
					ref = session.manageUndeploy(this.environment, this.release, this.server, this.artifact, false);
				} else if ("start".equalsIgnoreCase(action)) {
					log.debug("Starting");
					ref = session.manageStart(this.environment, this.release, this.server, this.artifact);
				} else if ("stop".equalsIgnoreCase(action)) {
					log.debug("Stopping");
					ref = session.manageStop(this.environment, this.release, this.server, this.artifact);
				} else {
					throw new MojoExecutionException("Unknown operation '" + action + "'");
				}
				log.debug("Waiting for completion");
				waitForCompletion(session, ref);
				log.debug("Done");
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

	private void waitForCompletion(KwateeAPI session, String ref) throws IOException {
		long endTime = System.currentTimeMillis() + 30000L;
		int code = 0;
		do {
			try {
				Thread.sleep(1000L);
			} catch (InterruptedException e) {
				break;
			}
			code = session.getOperationStatus(ref);
		} while (code == 204 && System.currentTimeMillis() < endTime);
		if (code == 204) {
			session.manageCancel(ref, true);
			throw new IOException("timeout");
		}
		session.manageCancel(ref, true);
	}
}
