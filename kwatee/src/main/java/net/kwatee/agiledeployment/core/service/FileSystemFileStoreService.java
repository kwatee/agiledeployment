/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
 * @author mac
 * 
 */
public class FileSystemFileStoreService implements FileStoreService {

	@Value("${kwatee.repository.path:./}")
	private String repositoryPath;

	private File repositoryDir;

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FileSystemFileStoreService.class);

	@PostConstruct
	void initialize() {
		try {
			this.repositoryDir = new File(this.repositoryPath).getCanonicalFile();
			this.repositoryDir.mkdirs();
			if (!this.repositoryDir.isDirectory()) {
				throw new InternalErrorException("Cannot find/create repository at " + this.repositoryPath);
			}
		} catch (IOException ie) {
			throw new InternalErrorException(ie);
		}
		LOG.info("Repository location: {}", this.repositoryPath);
	}

	/**
	 * Deletes entire repository structure and files
	 * 
	 */
	@Override
	public void deleteRepository() {
		FileUtils.deleteQuietly(new File(this.repositoryDir, Constants.ARTIFACTS_DIR));
		FileUtils.deleteQuietly(new File(this.repositoryDir, Constants.ENVIRONMENTS_DIR));
	}

	/**
	 * @param artifactName
	 */
	@Override
	public void deleteArtifact(String artifactName) {
		FileUtils.deleteQuietly(getArtifactDir(artifactName));
		LOG.debug("Delete artifact dir {}", artifactName);
	}

	/**
	 * @param artifactName
	 * @param versionName
	 */
	@Override
	public void deleteVersion(String artifactName, String versionName) {
		FileUtils.deleteQuietly(getVersionDir(artifactName, versionName));
		LOG.debug("Delete version dir {}[{}]", artifactName, versionName);
	}

	/**
	 * @param artifactName
	 * @param versionName
	 * @param sourceVersionName
	 */
	@Override
	public void duplicateVersion(String artifactName, String versionName, String sourceVersionName) {
		File duplicateDir = getVersionDir(artifactName, versionName);
		File sourceDir = new File(duplicateDir.getParentFile(), sourceVersionName);
		if (sourceDir.exists()) {
			FileUtils.deleteQuietly(duplicateDir);
			try {
				FileUtils.copyDirectory(sourceDir, duplicateDir);
			} catch (IOException e) {
				throw new InternalErrorException(e);
			}
			LOG.debug("Duplicate version dir {} to {}[{}]", sourceVersionName, artifactName, versionName);
		}
	}

	/**
	 * @param environmentName
	 */
	@Override
	public void deleteEnvironment(String environmentName) {
		FileUtils.deleteQuietly(new File(this.repositoryDir, Constants.ENVIRONMENTS_DIR + environmentName));
		LOG.debug("Delete environment dir {}", environmentName);
	}

	/**
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	@Override
	public String getVersionPath(String artifactName, String versionName) {
		return Constants.ARTIFACTS_DIR + artifactName + '/' + versionName + '/';
	}

	/**
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	@Override
	public String getVersionTemplatePath(String artifactName, String versionName) {
		return getVersionPath(artifactName, versionName) + Constants.TEMPLATES_DIR;
	}

	/**
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	@Override
	public String getVersionOverlayPath(String artifactName, String versionName) {
		return getVersionPath(artifactName, versionName) + Constants.OVERLAYS_DIR;
	}

	/**
	 * Repository path to release
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return path
	 */
	@Override
	public String getReleasePath(String environmentName, String releaseName) {
		return Constants.ENVIRONMENTS_DIR + environmentName + '/' + releaseName + '/';
	}

	/**
	 * Repository path to release overlays
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return path
	 */
	@Override
	public String getReleaseOverlayPath(String environmentName, String releaseName) {
		return getReleasePath(environmentName, releaseName) + Constants.OVERLAYS_DIR;
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @return path
	 */
	private String getReleaseOverlayPath(String environmentName, String releaseName, String serverName) {
		String relPath = getReleaseOverlayPath(environmentName, releaseName) + serverName + '/';
		return relPath;
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @return path
	 */
	@Override
	public String getReleaseOverlayPath(String environmentName, String releaseName, String serverName, String artifactName) {
		String relPath;
		if (serverName == null) {
			relPath = getReleaseOverlayPath(environmentName, releaseName) + "_all_/" + artifactName + '/';
		}
		else {
			relPath = getReleaseOverlayPath(environmentName, releaseName, serverName) + artifactName + '/';
		}
		return relPath;
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 */
	@Override
	public void deleteRelease(String environmentName, String releaseName) {
		FileUtils.deleteQuietly(new File(this.repositoryDir, getReleasePath(environmentName, releaseName)));
		LOG.debug("Delete release dir {}-{}", environmentName, releaseName);
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 */
	@Override
	public void deleteReleaseOverlay(String environmentName, String releaseName, String serverName, String artifactName) {
		String relPath = getReleaseOverlayPath(environmentName, releaseName, serverName, artifactName);
		FileUtils.deleteQuietly(new File(this.repositoryDir, relPath));
		if (serverName == null) {
			LOG.debug("Delete release overlay {}-{}/_all_/{}", environmentName, releaseName, artifactName);
		}
		else {
			LOG.debug("Delete release overlay {}-{}/{}", environmentName, releaseName, serverName + '/' + artifactName);
		}
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 */
	@Override
	public void deleteReleaseOverlay(String environmentName, String releaseName, String serverName) {
		String relPath = getReleaseOverlayPath(environmentName, releaseName, serverName);
		FileUtils.deleteQuietly(new File(this.repositoryDir, relPath));
		LOG.debug("Delete release server overlay dir {}-{}/{}", environmentName, releaseName, serverName);
	}

	/**
	 * @param sourceEnvironmentName
	 * @param sourceReleaseName
	 * @param destEnvironmentName
	 * @param destReleaseName
	 */
	@Override
	public void duplicateRelease(String sourceEnvironmentName, String sourceReleaseName, String destEnvironmentName, String destReleaseName) {
		File sourceDir = new File(this.repositoryDir, getReleasePath(sourceEnvironmentName, sourceReleaseName));
		File duplicateDir = new File(this.repositoryDir, getReleasePath(destEnvironmentName, destReleaseName));
		if (sourceDir.exists()) {
			FileUtils.deleteQuietly(duplicateDir);
			try {
				FileUtils.copyDirectory(sourceDir, duplicateDir);
			} catch (IOException e) {
				throw new InternalErrorException(e);
			}
			LOG.debug("Duplicate release dir {} to {}", sourceReleaseName, destReleaseName);
		}
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 */
	@Override
	public void renameSnapshotRelease(String environmentName, String releaseName) {
		File dir = new File(this.repositoryDir, Constants.ENVIRONMENTS_DIR + environmentName + '/' + Constants.SNAPSHOT_RELEASE_NAME);
		if (!dir.exists()) {
			File newDir = new File(this.repositoryDir, getReleasePath(environmentName, releaseName));
			FileUtils.deleteQuietly(newDir);
			if (!dir.renameTo(newDir)) {
				throw new InternalErrorException("Failed to rename release dir");
			}
			LOG.debug("Rename snapshot release dir {}-{}", environmentName, releaseName);
		}
	}

	/**
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param newServerName
	 */
	@Override
	public void mergeReleaseOverlays(String environmentName, String releaseName, String serverName, String newServerName) {
		String oldPath = getReleaseOverlayPath(environmentName, releaseName, serverName);
		String newPath = getReleaseOverlayPath(environmentName, releaseName, newServerName);
		File oldDir = new File(this.repositoryDir, oldPath);
		File newDir = new File(this.repositoryDir, newPath + '/');
		if (newDir.exists()) {
			try {
				FileUtils.copyDirectory(oldDir, newDir);
			} catch (IOException e) {
				throw new InternalErrorException(e);
			}
			deleteReleaseOverlay(environmentName, releaseName, serverName);
		} else {
			oldDir.renameTo(newDir);
		}
		LOG.debug("Renamed release {}-{} overlay from {} to {}", environmentName, releaseName, oldPath, newPath);
	}

	/**
	 * @param artifactName
	 * @return dir
	 */
	private File getArtifactDir(String artifactName) {
		return new File(this.repositoryDir, Constants.ARTIFACTS_DIR + artifactName);
	}

	/**
	 * @param artifactName
	 * @param versionName
	 * @return dir
	 */
	private File getVersionDir(String artifactName, String versionName) {
		return new File(getArtifactDir(artifactName), versionName);
	}

	/**
	 * @param basePath
	 * @param path
	 * @param deleteEmptySubdirs
	 */
	@Override
	public void deleteFile(String basePath, String path, String deleteEmptySubdirs) {
		File f = new File(this.repositoryDir, basePath + path);
		f.delete();
		if (deleteEmptySubdirs != null) {
			File rootDir = deleteEmptySubdirs == null ? null : new File(this.repositoryDir, deleteEmptySubdirs);
			for (f = f.getParentFile(); !f.equals(rootDir); f = f.getParentFile()) {
				if (!f.delete()) {
					return;
				}
			}
		}
	}

	/**
	 * @param basePath
	 * @param path
	 * @return size of stream
	 */
	@Override
	public InputStream getFileInputStream(String basePath, String path) {
		File f = new File(this.repositoryDir, basePath + path);
		try {
			return new FileInputStream(f);
		} catch (FileNotFoundException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * @param basePath
	 * @param path
	 * @return size of stream
	 */
	@Override
	public OutputStream getFileOutputStream(String basePath, String path) {
		File f = new File(this.repositoryDir, basePath + path);
		f.getParentFile().mkdirs();
		try {
			f.delete();
			f.createNewFile();
			return new FileOutputStream(f);
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * @param basePath
	 * @param path
	 * @return size of file
	 */
	@Override
	public long getFileLength(String basePath, String path) {
		File f = new File(this.repositoryDir, basePath + path);
		if (!f.exists()) {
			throw new InternalErrorException("File " + f.getAbsolutePath() + " not found");
		}
		return f.length();
	}

	/**
	 * @param basePath
	 * @param path
	 * @return true if file exists
	 */
	@Override
	public boolean existsFile(String basePath, String path) {
		try {
			return new File(this.repositoryDir, basePath + path).exists();
		} catch (InternalErrorException e) {
			return false;
		}
	}

	/**
	 * 
	 */
	@Override
	public void upgradeRepository() {
		boolean error = false;
		/*
		 * Rename packages to artifacts
		 */
		File packagesDir = new File(this.repositoryDir, "packages");
		if (packagesDir.exists()) {
			File artifactsDir = new File(this.repositoryDir, Constants.ARTIFACTS_DIR);
			packagesDir.renameTo(artifactsDir);
			if (!artifactsDir.exists()) {
				error = true;
			}
		}
		/*
		 * Rename all SKETCH directories to snapshot
		 */
		File environmentsDir = new File(this.repositoryDir, Constants.ENVIRONMENTS_DIR);
		if (environmentsDir.exists()) {
			File[] environments = environmentsDir.listFiles();
			for (File environment : environments) {
				File sketchDir = new File(environment, "SKETCH");
				if (sketchDir.exists()) {
					File snapshotDir = new File(environment, Constants.SNAPSHOT_RELEASE_NAME);
					sketchDir.renameTo(snapshotDir);
					if (!snapshotDir.exists()) {
						error = true;
					}
				}
			}
		}
		if (error) {
			throw new InternalErrorException("Repository was not properly upgraded");
		}
	}

	@Override
	public Collection<String> listFilesRecursively(String path) {
		Collection<String> files = new ArrayList<>();
		listFiles(new File(this.repositoryDir, path), null, files);
		return files;
	}

	private void listFiles(File baseDir, String path, Collection<String> files) {
		File file = path == null ? baseDir : new File(baseDir, path);
		if (file.isFile()) {
			files.add(path);
			return;
		}
		String[] dirFiles = file.list();
		if (dirFiles != null) {
			for (String f : dirFiles) {
				listFiles(baseDir, path == null ? f : path + "/" + f, files);
			}
		}
	}
}
