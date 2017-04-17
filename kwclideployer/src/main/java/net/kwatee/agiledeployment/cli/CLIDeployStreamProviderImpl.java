/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.core.deploy.entity.DeploymentArtifact;
import net.kwatee.agiledeployment.core.repository.DeployStreamProvider;

public class CLIDeployStreamProviderImpl implements DeployStreamProvider {

	final private String bundleDir;
	final private String configurationDir;

	public CLIDeployStreamProviderImpl(String bundleDir, String configurationDir) {
		this.bundleDir = bundleDir;
		this.configurationDir = configurationDir;
	}

	@Override
	public InputStream getPackageFileInputStream(String artifactName, String versionName) throws KwateeException {
		String artifactVersion = DeploymentArtifact.getRef(artifactName, versionName);
		File pkgFile = new File(new File(this.bundleDir, artifactVersion), "archive.kwatee");
		try {
			return new FileInputStream(pkgFile);
		} catch (FileNotFoundException e) {
			throw new InternalErrorException(e);
		}
	}

	@Override
	public InputStream getOverlayFileInputStream(String path, String environmentName, String releaseName, String serverName, String artifactName, String versionName) throws KwateeException {
		try {
			File layerDir = new File(new File(this.configurationDir, serverName + ".server"), artifactName);
			return new FileInputStream(new File(layerDir, path));
		} catch (IOException e) {
		}
		String artifactVersion = DeploymentArtifact.getRef(artifactName, versionName);
		try {
			File layerDir = new File(new File(this.bundleDir, artifactVersion), Constants.OVERLAYS_DIR);
			return new FileInputStream(new File(layerDir, path));
		} catch (IOException e) {
		}
		try {
			File layerDir = new File(new File(this.bundleDir, artifactVersion), Constants.TEMPLATES_DIR);
			return new FileInputStream(new File(layerDir, path));
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

}
