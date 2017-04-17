/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * 
 * @author mac
 * 
 */
public interface FileStoreService {

	/**
	 * Deletes entire repository structure and files
	 * 
	 */
	public void deleteRepository();

	/**
	 * @param artifactName
	 */
	public void deleteArtifact(String artifactName);

	/**
	 * @param artifactName
	 * @param versionName
	 */
	public void deleteVersion(String artifactName, String versionName);

	/**
	 * @param artifactName
	 * @param versionName
	 * @param sourceVersionName
	 */
	public void duplicateVersion(String artifactName, String versionName, String sourceVersionName);

	/**
	 * @param environmentName
	 */
	public void deleteEnvironment(String environmentName);

	/**
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	public String getVersionPath(String artifactName, String versionName);

	/**
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	public String getVersionTemplatePath(String artifactName, String versionName);

	/**
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	public String getVersionOverlayPath(String artifactName, String versionName);

	/**
	 * Repository path to release
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return path
	 */
	public String getReleasePath(String environmentName, String releaseName);

	/**
	 * Repository path to release overlays
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return path
	 */
	public String getReleaseOverlayPath(String environmentName, String releaseName);

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @return path
	 */
	public String getReleaseOverlayPath(String environmentName, String releaseName, String serverName, String artifactName);

	/**
	 * @param environmentName
	 * @param releaseName
	 */
	public void deleteRelease(String environmentName, String releaseName);

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 */
	public void deleteReleaseOverlay(String environmentName, String releaseName, String serverName, String artifactName);

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 */
	public void deleteReleaseOverlay(String environmentName, String releaseName, String serverName);

	/**
	 * @param sourceEnvironmentName
	 * @param sourceReleaseName
	 * @param destEnvironmentName
	 * @param destReleaseName
	 */
	public void duplicateRelease(String sourceEnvironmentName, String sourceReleaseName, String destEnvironmentName, String destReleaseName);

	/**
	 * @param environmentName
	 * @param releaseName
	 */
	public void renameSnapshotRelease(String environmentName, String releaseName);

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param newServerName
	 */
	public void mergeReleaseOverlays(String environmentName, String releaseName, String serverName, String newServerName);

	/**
	 * @param basePath
	 * @param path
	 * @param deleteEmptySubdirs
	 */
	public void deleteFile(String basePath, String path, String deleteEmptySubdirs);

	/**
	 * @param basePath
	 * @param path
	 * @return size of stream
	 */
	public InputStream getFileInputStream(String basePath, String path);

	/**
	 * @param basePath
	 * @param path
	 * @return size of stream
	 */
	public OutputStream getFileOutputStream(String basePath, String path);

	/**
	 * @param basePath
	 * @param path
	 * @return size of file
	 */
	public long getFileLength(String basePath, String path);

	/**
	 * @param basePath
	 * @param path
	 * @return true if file exists
	 */
	public boolean existsFile(String basePath, String path);

	/**
	 * 
	 */
	public void upgradeRepository();

	public Collection<String> listFilesRecursively(String path);
}
