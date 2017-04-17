/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.io.InputStream;

public interface DeployStreamProvider {

	InputStream getPackageFileInputStream(String artifactName, String versionName);

	InputStream getOverlayFileInputStream(String path, String environmentName, String releaseName, String serverName, String artifactName, String versionName);

}
