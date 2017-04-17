/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.core.service.FileSystemFileStoreService;
import net.kwatee.agiledeployment.core.service.LayerService;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.RepoVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactDto;
import net.kwatee.agiledeployment.repository.dto.VariableReference;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.Executable;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * 
 * @author kwatee
 * 
 */
@Repository
public class ArtifactRepository {

	@Autowired
	private PersistenceService persistenceService;
	@Autowired
	private FileSystemFileStoreService repositoryService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private LayerService layerService;

	/**
	 * List of kwatee artifacts
	 * 
	 * @return List of kwatee artifacts
	 */
	public Collection<Artifact> getArtifacts() {
		Collection<Artifact> artifacts = this.persistenceService.getArtifacts();
		return artifacts;
	}

	/**
	 * Retrieves a artifact by its id or null if not found
	 * 
	 * @param artifactId
	 *            The id of the artifact
	 * @return The kwatee artifact or null if not found
	 */
	public Artifact getUncheckedArtifact(long artifactId) {
		return persistenceService.getArtifactById(artifactId);
	}

	/**
	 * Retrieves a artifact by its id or throws an exception if not found
	 * 
	 * @param artifactId
	 *            The id of the artifact
	 * @return The kwatee artifact
	 * @throws ArtifactNotExistException
	 *             If artifact not found
	 */
	public Artifact getCheckedArtifact(long artifactId) throws ObjectNotExistException {
		Artifact artifact = this.persistenceService.getArtifactById(artifactId);
		if (artifact == null) {
			throw new ObjectNotExistException(ObjectNotExistException.ARTIFACT, artifactId);
		}
		return artifact;
	}

	/**
	 * Retrieves a artifact by its name or null if not found
	 * 
	 * @param artifactName
	 *            The name of the artifact
	 * @return The kwatee artifact or null if not found
	 */
	public Artifact getUncheckedArtifact(String artifactName) {
		return this.persistenceService.getArtifactByName(artifactName);
	}

	/**
	 * Retrieves a artifact by its name or throws an exception if not found
	 * 
	 * @param artifactName
	 *            The name of the artifact
	 * @return The kwatee artifact
	 */
	public Artifact getCheckedArtifact(String artifactName) throws ObjectNotExistException {
		Artifact artifact = this.persistenceService.getArtifactByName(artifactName);
		if (artifact == null) {
			throw new ObjectNotExistException(ObjectNotExistException.ARTIFACT, artifactName);
		}
		return artifact;
	}

	/**
	 * Saves the artifact
	 * 
	 * @param artifact
	 *            Kwatee artifact
	 */
	public void saveArtifact(Artifact artifact) {
		this.persistenceService.saveEntity(artifact);
	}

	/**
	 * Deletes artifact and its versions
	 * 
	 * @param artifact
	 *            Kwatee artifact
	 * @throws CannotDeleteObjectInUseException
	 */
	public void deleteArtifact(Artifact artifact) throws CannotDeleteObjectInUseException {
		Environment e = this.persistenceService.getEnvironmentUsingArtifact(artifact.getId());
		if (e != null) {
			throw new CannotDeleteObjectInUseException("artifact " + artifact, "environment " + e);
		}
		while (!artifact.getVersions().isEmpty()) {
			Version version = artifact.getVersions().iterator().next();
			deleteVersion(version, false);
		}
		this.persistenceService.deleteEntity(artifact);
		this.repositoryService.deleteArtifact(artifact.getName());
	}

	/**
	 * List of versions included in kwatee artifact identified by artifactId
	 * 
	 * @param artifactId
	 *            Kwatee artifact id
	 * @return List of versions included in kwatee artifact identified by artifactId
	 */
	public Collection<Version> getVersions(long artifactId) {
		return this.persistenceService.getVersionsByArtifactId(artifactId);
	}

	/**
	 * A version by its name within a artifact name or null if not found
	 * 
	 * @param artifactName
	 *            Name of a kwatee artifact
	 * @param versionName
	 *            Name of a kwatee version within the artifact
	 * @return A version by its name within a artifact name or null if not found
	 */
	public Version getUncheckedVersion(String artifactName, String versionName) {
		Version version = this.persistenceService.getVersionByName(artifactName, versionName);
		return version;
	}

	/**
	 * A version by its name within a artifact name or an exception if not found
	 * 
	 * @param artifactName
	 *            Name of a kwatee artifact
	 * @param versionName
	 *            Name of a kwatee version within the artifact
	 * @return A version by its name within a artifact name
	 * @throws ObjectNotExistException
	 */
	public Version getCheckedVersion(String artifactName, String versionName) throws ObjectNotExistException {
		Version version = this.persistenceService.getVersionByName(artifactName, versionName);
		if (version == null) {
			throw new ObjectNotExistException(ObjectNotExistException.VERSION, ArtifactDto.getRef(artifactName, versionName));
		}
		return version;
	}

	/**
	 * A version identified by its id or null if not found
	 * 
	 * @param versionId
	 *            Id of a kwatee version
	 * @return A version identified by its id or null if not found
	 */
	public Version getUncheckedVersion(long versionId) {
		Version version = this.persistenceService.getVersionById(versionId);
		return version;
	}

	/**
	 * A version identified by its id or an exception if not found
	 * 
	 * @param versionId
	 *            Id of a kwatee version
	 * @return A version identified by its id
	 * @throws ObjectNotExistException
	 */
	public Version getCheckedVersion(long versionId) throws ObjectNotExistException {
		Version version = this.persistenceService.getVersionById(versionId);
		if (version == null) {
			throw new ObjectNotExistException(ObjectNotExistException.VERSION, versionId);
		}
		return version;
	}

	/**
	 * Save kwatee version
	 * 
	 * @param version
	 */
	public void saveVersion(Version version) {
		this.persistenceService.saveEntity(version);
	}

	/**
	 * Delete kwatee version
	 * 
	 * @param version
	 *            Kwatee version to delete
	 * @throws CannotDeleteObjectInUseException
	 */
	public void deleteVersion(Version version, boolean removeFiles) throws CannotDeleteObjectInUseException {
		Environment e = this.persistenceService.getEnvironmentUsingVersion(version.getId());
		if (e != null) {
			throw new CannotDeleteObjectInUseException("version " + version, "environment " + e);
		}
		version.getArtifact().getVersions().remove(version);
		this.persistenceService.saveEntity(version.getArtifact());
		if (removeFiles) {
			this.repositoryService.deleteVersion(version.getArtifact().getName(), version.getName());
		}
	}

	/**
	 * 
	 * Retrieves all version variable references (in actions and files)
	 * 
	 * @param version
	 * @return map of variables
	 * @throws PackageRescanException
	 */
	public Map<String, Collection<VariableReference>> getVersionVariables(Version version) throws PackageRescanException {
		Map<String, Collection<VariableReference>> versionVariablesInfo = this.layerService.getVersionFilesVariables(version);
		Collection<VersionVariable> saveVariables = new ArrayList<>(version.getVariablesDefaultValues());
		version.getVariablesDefaultValues().clear();
		scanMissingVersionVariables(version, null, null, versionVariablesInfo);
		version.getVariablesDefaultValues().addAll(saveVariables);
		CollectionUtils.filter(versionVariablesInfo.entrySet(),
				new Predicate() {

					@SuppressWarnings("unchecked")
					public boolean evaluate(Object entry) {
						return !((Map.Entry<String, Collection<String>>) entry).getKey().startsWith("kwatee_");
					}
				});
		return versionVariablesInfo;
	}

	/**
	 * 
	 * @param version
	 * @param release
	 * @param server
	 * @param missingVariables
	 */
	public void scanMissingVersionVariables(Version version, Release release, Server server, Map<String, Collection<VariableReference>> missingVariables) {
		MissingVariableException exception = new MissingVariableException();
		VariableResolver resolver = new RepoVariableResolver(release, server, version);
		try {
			this.variableService.instantiateVariables(version.getPreDeployAction(), resolver);
		} catch (MissingVariableException ke) {
			exception.addMissingVariable(ke, "pre-deploy action");
		}
		try {
			this.variableService.instantiateVariables(version.getPostDeployAction(), resolver);
		} catch (MissingVariableException ke) {
			exception.addMissingVariable(ke, "post-deploy action");
		}
		try {
			this.variableService.instantiateVariables(version.getPreUndeployAction(), resolver);
		} catch (MissingVariableException ke) {
			exception.addMissingVariable(ke, "pre-undeploy action");
		}
		try {
			this.variableService.instantiateVariables(version.getPostUndeployAction(), resolver);
		} catch (MissingVariableException ke) {
			exception.addMissingVariable(ke, "post-undeploy action");
		}
		for (Executable executable : version.getExecutables()) {
			try {
				this.variableService.instantiateVariables(executable.getStartAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "start action");
			}
			try {
				this.variableService.instantiateVariables(executable.getStopAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "stop action");
			}
			try {
				this.variableService.instantiateVariables(executable.getStatusAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "status action");
			}
		}
		try {
			this.variableService.instantiateVariables(version.getFileOwner(), resolver);
		} catch (MissingVariableException ke) {
			exception.addMissingVariable(ke, "owner");
		}
		try {
			this.variableService.instantiateVariables(version.getFileGroup(), resolver);
		} catch (MissingVariableException ke) {
			exception.addMissingVariable(ke, "group");
		}
		for (RepositoryFile f : version.getFiles()) {
			try {
				this.variableService.instantiateVariables(f.getFileOwner(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, f.getRelativePath() + " owner");
			}
			try {
				this.variableService.instantiateVariables(f.getFileGroup(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, f.getRelativePath() + " group");
			}
		}
		this.variableService.addMissingVariables(exception, missingVariables);
	}

	/**
	 * 
	 * @param version
	 * @param release
	 * @param server
	 * @throws MissingVariableException
	 */
	public void checkMissingVersionVariables(Version version, Release release, Server server) throws MissingVariableException {
		Map<String, Collection<VariableReference>> missingVariables = new HashMap<>();
		scanMissingVersionVariables(version, release, server, missingVariables);
		if (!missingVariables.isEmpty()) {
			throw new MissingVariableException();
		}
	}

	/**
	 * 
	 * @param version
	 * @return true if version is used in frozen release
	 */
	public boolean isVersionUsedInFrozenRelease(Version version) {
		Collection<Release> releases = this.persistenceService.getFrozenReleasesUsingVersion(version.getId());
		return !releases.isEmpty();
	}

	/**
	 * 
	 * @param version
	 * @throws CannotDeleteObjectInUseException
	 */
	public void checkIfUsedInFrozenRelease(Version version) throws CannotDeleteObjectInUseException {
		Collection<Release> releases = this.persistenceService.getFrozenReleasesUsingVersion(version.getId());
		if (!releases.isEmpty()) {
			throw new CannotDeleteObjectInUseException("version " + version, "release " + releases.iterator().next());
		}
	}

	/**
	 * 
	 * @param serverId
	 * @return
	 */
	public Collection<Version> getVersionsInServer(Long serverId) {
		return this.persistenceService.getVersionsInServer(serverId);
	}

	public List<RepositoryFile> getMergedVersionFiles(Version version, boolean onlyTemplatized, String rootPath) {
		return this.persistenceService.getMergedVersionFiles(version, onlyTemplatized, rootPath);
	}
}