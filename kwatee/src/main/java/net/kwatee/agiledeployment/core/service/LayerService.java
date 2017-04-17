/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.service;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.Constants.LayerType;
import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.core.repository.PersistenceService;
import net.kwatee.agiledeployment.core.variable.VarInfo;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.RepoVariableResolver;
import net.kwatee.agiledeployment.repository.dto.VariableReference;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Version;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author mac
 * 
 */
@Service
public class LayerService {

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LayerService.class);

	@Autowired
	private FileStoreService repositoryService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private PersistenceService persistenceService;

	/**
	 * Returns merged file list with priority to overlays over templates over package files
	 * 
	 * @param version
	 * @return modifiable list of repository files
	 * @throws PackageRescanException
	 */
	public Collection<RepositoryFile> getMergedVersionFiles(Version version, boolean onlyTemplatized, String rootPath) throws PackageRescanException {
		if (rootPath != null && rootPath.startsWith("/")) {
			rootPath = rootPath.substring(1);
		}
		if (version != null) {
			Collection<RepositoryFile> files = this.persistenceService.getMergedVersionFiles(version, onlyTemplatized, rootPath);
			if (version.isPackageRescanNeeded()) {
				patchDirectoryEntries(files);
			}
			return files;
		}
		return new ArrayList<>(0);
	}

	/**
	 * Check if all the variables used in this release artifact have a defined value otherwise throw an exception
	 * 
	 * @param releaseArtifact
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws MissingVariableException
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 */
	public void checkReleaseFilesVariables(ReleaseArtifact releaseArtifact) throws CompatibilityException, NoActiveVersionException, MissingVariableException, ArtifactNotInReleaseException, PackageRescanException {
		Collection<RepositoryFile> files = getMergedReleaseFiles(releaseArtifact, true, null);
		VariableResolver resolver = null;
		MissingVariableException exception = null;
		for (RepositoryFile file : files) {
			try {
				this.variableService.instantiateVariables(file.getFileOwner(), resolver);
				this.variableService.instantiateVariables(file.getFileGroup(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, file.getRelativePath() + " owner/group");
			}
			if (file.isTemplatized() && !file.ignoreVariables()) {
				VarInfo varInfo = VarInfo.valueOf(file.getVariables());
				for (int i = 0; i < varInfo.size(); i++) {
					if (varInfo.getDefaultValue(i) == null) {
						try {
							if (resolver == null)
								resolver = new RepoVariableResolver(releaseArtifact.getRelease(), releaseArtifact.getServer(), releaseArtifact.getActiveVersion());
							this.variableService.fetchVariableValue(varInfo.getName(i), resolver);
						} catch (MissingVariableException e) {
							if (exception == null) {
								exception = new MissingVariableException();
							}
							exception.addMissingVariable(e, file.getRelativePath());
						}
					}
				}
			}
		}
		if (exception != null) {
			throw exception;
		}
	}

	/**
	 * Updates the repository file record for the release overlay file if it exists otherwise create a new one.
	 * 
	 * @param releaseArtifact
	 * @param path
	 */
	public void updateReleaseOverlay(ReleaseArtifact releaseArtifact, String path) {
		RepositoryFile file;
		try {
			file = releaseArtifact.getFile(path);
		} catch (ObjectNotExistException e) {
			LayerType layer = releaseArtifact.getServer() == null ? LayerType.COMMON_OVERLAY : LayerType.SERVER_OVERLAY;
			addOverlayParentDirs(path, layer, releaseArtifact.getFiles());
			file = new RepositoryFile(path);
			file.setLayerType(layer);
			releaseArtifact.getFiles().add(file);
		}
		file.setDirectory(false);
		file.setSymbolicLink(false);
		String serverName = releaseArtifact.getServer() == null ? null : releaseArtifact.getServer().getName();
		String overlayPath = this.repositoryService.getReleaseOverlayPath(releaseArtifact.getRelease().getEnvironment().getName(), releaseArtifact.getRelease().getName(), serverName, releaseArtifact.getArtifact().getName());
		MessageDigest md = CryptoUtils.getNewDigest(path);
		String variables = this.variableService.extractStreamVariables(this.repositoryService.getFileInputStream(overlayPath, path), md, VariableService.VAR_PREFIX_CHAR);
		file.setVariables(variables);
		file.setSignature(CryptoUtils.getSignature(md));
		file.setSize(this.repositoryService.getFileLength(overlayPath, path));
	}

	private void addOverlayParentDirs(String path, LayerType layer, Collection<RepositoryFile> files) {
		int idx = path.length();
		while ((idx = path.lastIndexOf('/', idx - 1)) > 0) {
			RepositoryFile dir = new RepositoryFile(path.substring(0, idx + 1));
			dir.setDirectory(true);
			if (files.contains(dir)) {
				return;
			}
			dir.setLayerType(layer);
			files.add(dir);
		}
	}

	/**
	 * Remove a release overlay file
	 * 
	 * @param releaseArtifact
	 * @param path
	 */
	public void removeReleaseOverlay(ReleaseArtifact releaseArtifact, String path) {
		String environmentName = releaseArtifact.getRelease().getEnvironment().getName();
		String releaseName = releaseArtifact.getRelease().getName();
		String artifactName = releaseArtifact.getArtifact().getName();
		String serverName = releaseArtifact.getServer() == null ? null : releaseArtifact.getServer().getName();
		String overlayPath = this.repositoryService.getReleaseOverlayPath(
				environmentName,
				releaseName,
				serverName,
				artifactName);
		for (Iterator<RepositoryFile> it = releaseArtifact.getFiles().iterator(); it.hasNext();) {
			RepositoryFile f = it.next();
			if (f.getRelativePath().equals(path)) {
				it.remove();
				break;
			}
		}
		this.repositoryService.deleteFile(
				overlayPath,
				path,
				this.repositoryService.getReleasePath(environmentName, releaseName));
	}

	public List<RepositoryFile> getMergedReleaseFiles(ReleaseArtifact releaseArtifact, boolean onlyTemplatized, String rootPath) throws ArtifactNotInReleaseException, CompatibilityException, NoActiveVersionException, PackageRescanException {
		return getMergedReleaseFiles(releaseArtifact, true, onlyTemplatized, rootPath);
	}

	public List<RepositoryFile> getMergedReleaseFilesWithoutActiveVersion(ReleaseArtifact releaseArtifact, boolean onlyTemplatized, String rootPath) throws ArtifactNotInReleaseException, PackageRescanException {
		try {
			return getMergedReleaseFiles(releaseArtifact, false, onlyTemplatized, rootPath);
		} catch (CompatibilityException e) {} catch (NoActiveVersionException e) {}
		return null;
	}

	private List<RepositoryFile> getMergedReleaseFiles(ReleaseArtifact releaseArtifact, boolean mustHaveVersion, boolean onlyTemplatized, String rootPath) throws ArtifactNotInReleaseException, CompatibilityException, NoActiveVersionException, PackageRescanException {
		LOG.debug("getMergedReleaseFiles for {}", releaseArtifact);
		List<RepositoryFile> files = null;
		Version activeVersion = null;
		try {
			activeVersion = releaseArtifact.getActiveVersion();
			files = (List<RepositoryFile>) getMergedVersionFiles(activeVersion, onlyTemplatized, rootPath);
		} catch (CompatibilityException e) {
			if (mustHaveVersion) {
				throw e;
			}
		} catch (NoActiveVersionException e) {
			if (mustHaveVersion) {
				throw e;
			}
		}
		if (files == null) {
			files = new ArrayList<>();
		}
		if (releaseArtifact.getServer() == null) {
			mergeFileLists(
					files,
					releaseArtifact.getFiles());
		} else {
			ReleaseArtifact commonArtifact = releaseArtifact.getRelease().getReleaseArtifact(null, releaseArtifact.getArtifact());
			mergeFileLists(
					files,
					commonArtifact.getFiles());
			mergeFileLists(
					files,
					releaseArtifact.getFiles());
			if (activeVersion != null && activeVersion.isPackageRescanNeeded()) {
				patchDirectoryEntries(files);
			}
		}
		LOG.debug("getMergedReleaseFiles ok");
		return files;
	}

	/**
	 * 
	 * @param files
	 * @param filesToMerge
	 */
	private void mergeFileLists(Collection<RepositoryFile> files, Collection<RepositoryFile> filesToMerge) {
		for (RepositoryFile f : filesToMerge) {
			files.remove(f);
			files.add(f);
		}
	}

	/**
	 * Returns the repository path for the version repository file layer type
	 * 
	 * @param layerType
	 * @param environmentName
	 * @param releaseName
	 * @param artifactName
	 * @param versionName
	 * @param serverName
	 * @return path
	 */
	public String getLayerPath(Constants.LayerType layerType, String environmentName, String releaseName, String artifactName, String versionName, String serverName) {
		switch (layerType) {
			case COMMON_OVERLAY:
				return this.repositoryService.getReleaseOverlayPath(environmentName, releaseName, null, artifactName);
			case SERVER_OVERLAY:
				return this.repositoryService.getReleaseOverlayPath(environmentName, releaseName, serverName, artifactName);
			default:
				return getLayerPath(layerType, artifactName, versionName);
		}
	}

	/**
	 * Returns the repository path for the version repository file layer type
	 * 
	 * @param layerType
	 * @param releaseArtifact
	 * @return path
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 */
	public String getLayerPath(Constants.LayerType layerType, ReleaseArtifact releaseArtifact) throws CompatibilityException, NoActiveVersionException {
		Release release = releaseArtifact.getRelease();
		Artifact artifact = releaseArtifact.getArtifact();
		switch (layerType) {
			case COMMON_OVERLAY:
				return this.repositoryService.getReleaseOverlayPath(release.getEnvironment().getName(), release.getName(), null, artifact.getName());
			case SERVER_OVERLAY:
				return this.repositoryService.getReleaseOverlayPath(release.getEnvironment().getName(), release.getName(), releaseArtifact.getServer().getName(), artifact.getName());
			default:
				Version version = releaseArtifact.getActiveVersion();
				return getLayerPath(layerType, version.getArtifact().getName(), version.getName());
		}
	}

	/**
	 * Returns the repository path for the release overlay layer type
	 * 
	 * @param layerType
	 * @param artifactName
	 * @param versionName
	 * @return path
	 */
	public String getLayerPath(Constants.LayerType layerType, String artifactName, String versionName) {
		if (layerType == Constants.LayerType.ARTIFACT_TEMPLATE)
			return this.repositoryService.getVersionTemplatePath(artifactName, versionName);
		return this.repositoryService.getVersionOverlayPath(artifactName, versionName);
	}

	private void patchDirectoryEntries(Collection<RepositoryFile> files) {
		HashMap<String, RepositoryFile> directories = new HashMap<>();
		for (RepositoryFile file : files) {
			if (file.isDirectory()) {
				directories.put(file.getRelativePath(), file);
			} else {
				String path = file.getRelativePath();
				int idx = path.length();
				while ((idx = path.lastIndexOf('/', idx - 1)) > 0) {
					String dirPath = path.substring(0, idx + 1);
					if (directories.containsKey(dirPath)) {
						break;
					}
					RepositoryFile dir = new RepositoryFile(dirPath, true);
					dir.setLayerType(file.getLayerType());
					directories.put(dirPath, dir);
				}
			}
		}
	}

	/**
	 * Check if all the variables used in this version have a defined value otherwise throw an exception
	 * 
	 * @param version
	 * @return a map that associates to each variable the list of files in which it is referenced
	 * @throws PackageRescanException
	 */
	public Map<String, Collection<VariableReference>> getVersionFilesVariables(Version version) throws PackageRescanException {
		HashMap<String, Collection<VariableReference>> versionVariables = new HashMap<>();
		List<RepositoryFile> files = (List<RepositoryFile>) getMergedVersionFiles(version, true, null);
		for (RepositoryFile file : files) {
			if (file.isTemplatized() && !file.ignoreVariables()) {
				VarInfo varInfo = VarInfo.valueOf(file.getVariables());
				for (int i = 0; i < varInfo.size(); i++) {
					String varName = varInfo.getName(i);
					Collection<VariableReference> references = versionVariables.get(varInfo.getName(i));
					if (references == null) {
						references = new ArrayList<>();
						versionVariables.put(varName, references);
					}
					references.add(new VariableReference(version.getArtifact().getName(), file.getRelativePath()));
				}
			}
		}
		return versionVariables;
	}
}
