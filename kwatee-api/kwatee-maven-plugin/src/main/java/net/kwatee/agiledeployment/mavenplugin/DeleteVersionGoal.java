package net.kwatee.agiledeployment.mavenplugin;

import java.io.IOException;

import net.kwatee.agiledeployment.client.KwateeAPI;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 * Delete version
 * 
 * @goal delete_version
 * 
 * @phase package
 */
public class DeleteVersionGoal extends AbstractMojo {

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

	public void setServiceurl(String url) {
		this.serviceUrl = url;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public void execute() throws MojoExecutionException {
		Log log = getLog();
		KwateeAPI session = null;
		try {
			log.debug("Creating kwatee session");
			session = KwateeAPI.createInstance(this.serviceUrl);
			log.debug("Deleting version");
			session.deleteVersion(this.artifact, this.version);
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
