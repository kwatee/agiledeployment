/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.SnapshotReleaseException;
import net.kwatee.agiledeployment.core.service.FileStoreService;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author mac
 * 
 */
@Service
public class EnvironmentRepository {

	private final static Logger LOG = LoggerFactory.getLogger(EnvironmentRepository.class);

	@Autowired
	private PersistenceService persistenceService;
	@Autowired
	private FileStoreService repositoryService;

	/**
	 * The list of kwatee environments with disabled ones filtered or not
	 * 
	 * @return The list of kwatee environments with disabled ones filtered or not
	 */
	public Collection<Environment> getEnvironments() {
		Collection<Environment> environments = this.persistenceService.getEnvironments();
		return environments;
	}

	/**
	 * Retrieves an environment by its id or null if not found
	 * 
	 * @param environmentId
	 *            Id of the environment
	 * @return The kwatee environment or null if not found
	 */
	public Environment getUncheckedEnvironment(long environmentId) {
		return this.persistenceService.getEnvironmentById(environmentId);
	}

	/**
	 * Retrieves an environment by its id and throws an exception if not found
	 * 
	 * @param environmentId
	 *            Id of the user
	 * @return The kwatee environment
	 * @throws ObjectNotExistException
	 */
	public Environment getCheckedEnvironment(long environmentId) throws ObjectNotExistException {
		Environment environment = this.persistenceService.getEnvironmentById(environmentId);
		if (environment == null) {
			throw new ObjectNotExistException(ObjectNotExistException.ENVIRONMENT, environmentId);
		}
		return environment;
	}

	/**
	 * Retrieves an environment by its name or null if not found
	 * 
	 * @param environmentName
	 *            Name of the environment
	 * @return The kwatee environment or null if not found
	 */
	public Environment getUncheckedEnvironment(String environmentName) {
		return this.persistenceService.getEnvironmentByName(environmentName);
	}

	/**
	 * Retrieves an environment by its name and throws an exception if not found
	 * 
	 * @param environmentName
	 *            Name of the environment
	 * @return The kwatee environment
	 * @throws ObjectNotExistException
	 */
	public Environment getCheckedEnvironment(String environmentName) throws ObjectNotExistException {
		Environment environment = this.persistenceService.getEnvironmentByName(environmentName);
		if (environment == null) {
			throw new ObjectNotExistException(ObjectNotExistException.ENVIRONMENT, environmentName);
		}
		return environment;
	}

	/**
	 * Save kwatee environment
	 * 
	 * @param environment
	 *            Kwatee environment
	 */
	public void saveEnvironment(Environment environment) {
		this.persistenceService.saveEntity(environment);
	}

	/**
	 * Delete kwatee environment
	 * 
	 * @param environment
	 *            Kwatee environment
	 */
	public void deleteEnvironment(Environment environment) {
		environment.getReleases().clear();
		this.persistenceService.saveEntity(environment);
		this.persistenceService.deleteEntity(environment);
		this.repositoryService.deleteEnvironment(environment.getName());
	}

	/**
	 * A kwatee release identified by name within an environment identified by name or null if not found
	 * 
	 * @param environmentName
	 *            Name of a kwatee environment
	 * @param releaseName
	 *            Name of a kwatee release
	 * @return A kwatee release identified by name within an environment identified by name or null if not found
	 */
	public Release getUncheckedRelease(String environmentName, String releaseName) {
		return this.persistenceService.getReleaseByName(environmentName, releaseName);
	}

	/**
	 * A kwatee release identified by name within an environment identified by name or exception if not found
	 * 
	 * @param environmentName
	 *            Name of a kwatee environment
	 * @param releaseName
	 *            Name of a kwatee release
	 * @return A kwatee release identified by name within an environment identified by name
	 * @throws ReleaseNotExistException
	 *             If release not found
	 */
	public Release getCheckedRelease(String environmentName, String releaseName) throws ObjectNotExistException {
		Release release = this.persistenceService.getReleaseByName(environmentName, releaseName);
		if (release == null) {
			throw new ObjectNotExistException(ObjectNotExistException.RELEASE, environmentName + "-" + releaseName);
		}
		return release;
	}

	/**
	 * The snapshot release of an environment identified by name or null if not found
	 * 
	 * @param environmentName
	 *            Name of a kwatee environment
	 * @return The snapshot release of an environment identified by name or null if not found
	 */
	public Release getUncheckedSnapshotRelease(String environmentName) {
		return getUncheckedRelease(environmentName, null);
	}

	/**
	 * The snapshot release of an environment identified by name or exception if not found
	 * 
	 * @param environmentName
	 *            Name of a kwatee environment
	 * @return The snapshot release of an environment identified by name
	 * @throws ObjectNotExistException
	 *             If no snapshot release in environment
	 */
	public Release getCheckedSnapshotRelease(String environmentName) throws ObjectNotExistException {
		return getCheckedRelease(environmentName, null);
	}

	/**
	 * A kwatee release identified by id or null if not found
	 * 
	 * @param releaseId
	 *            Id of a kwatee release
	 * @return A kwatee release identified by id or null if not found
	 */
	public Release getUncheckedRelease(long releaseId) {
		return this.persistenceService.getReleaseById(releaseId);
	}

	/**
	 * A kwatee release identified by id or exception if not found
	 * 
	 * @param releaseId
	 *            Id of a kwatee release
	 * @return A kwatee release identified by id
	 * @throws ObjectNotExistException
	 *             If release not found
	 */
	public Release getCheckedRelease(long releaseId) throws ObjectNotExistException {
		Release release = this.persistenceService.getReleaseById(releaseId);
		if (release == null) {
			throw new ObjectNotExistException(ObjectNotExistException.RELEASE, releaseId);
		}
		return release;
	}

	/**
	 * Save a kwatee release
	 * 
	 * @param release
	 *            A kwatee release
	 */
	public void saveRelease(Release release) {
		LOG.debug("saveRelease hasErrors:{}", Boolean.toString(release.hasErrors()));
		this.persistenceService.saveEntity(release);
		release.clearArtifactActiveVersions();
	}

	/**
	 * Delete a kwatee release
	 * 
	 * @param release
	 *            A kwatee release
	 * @throws SnapshotReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotExistException
	 */
	public void deleteRelease(Release release) throws SnapshotReleaseException, ObjectAlreadyExistsException, ObjectNotExistException {
		Environment environment = release.getEnvironment();
		if (environment.getReleases().size() == 1 && release.isSnapshot() && release.getReleaseArtifacts().isEmpty()) {
			throw new SnapshotReleaseException("EnvironmentDto must contain at least one release; delete environment instead");
		}
		release.getReleaseArtifacts().clear();
		release.getVariables().clear();
		this.persistenceService.saveEntity(release);
		environment.getReleases().remove(release);
		// We can't have an empty environment. If this was the last release then re-create a snapshot release
		if (environment.getReleases().size() == 0) {
			createRelease(environment.getName(), Constants.SNAPSHOT_RELEASE_NAME);
		}
		this.persistenceService.saveEntity(environment);
	}

	private void createRelease(String environmentName, String name) throws ObjectAlreadyExistsException, ObjectNotExistException {
		Environment environment = getCheckedEnvironment(environmentName);
		Release release = getUncheckedRelease(environmentName, name);
		if (release != null) {
			throw new ObjectAlreadyExistsException("ReleaseShortDto " + release.toString());
		}
		release = new Release();
		release.setName(name);
		release.setEnvironment(environment);
		environment.getReleases().add(release);
	}

	/**
	 * 
	 * @param release
	 * @param server
	 * @param artifact
	 * @return version of artifact
	 * @throws ArtifactNotInReleaseException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 */
	public String getReleaseArtifactActiveVersion(Release release, Server server, final Artifact artifact) throws ArtifactNotInReleaseException, CompatibilityException, NoActiveVersionException {
		Version version = null;
		if (server == null) {
			ReleaseArtifact dp = release.getDefaultReleaseArtifact(artifact.getName());
			if (dp != null) {
				version = dp.getVersion();
			}
		} else {
			ReleaseArtifact dp = (ReleaseArtifact) CollectionUtils.find(release.getServerArtifacts(server.getName()),
					new Predicate() {

						public boolean evaluate(Object dp) {
							return artifact.equals(((ReleaseArtifact) dp).getArtifact());
						}
					});
			if (dp == null) {
				throw new ArtifactNotInReleaseException("ArtifactShortDto " + artifact.getName() + " is not in release");
			}
			version = dp.getActiveVersion();
		}
		return version == null ? null : version.getName();
	}

	/**
	 * 
	 * @param release
	 * @param includeDisabled
	 * @return set of versions used in release
	 */
	public Set<Version> getReleaseVersions(Release release, boolean includeDisabled) {
		HashSet<Version> versions = new HashSet<Version>();
		for (ReleaseArtifact dp : release.getReleaseArtifacts()) {
			if (dp.getVersion() != null && (includeDisabled || !dp.getArtifact().isDisabled()) &&
					(includeDisabled || dp.getServer() == null || !dp.getServer().isDisabled())) {
				versions.add(dp.getVersion());
			}
		}
		return versions;
	}

	/**
	 * An environment referencing the server identified by serverId
	 * 
	 * @param serverId
	 *            Id of kwatee server
	 * @return An environment referencing server identified by serverId
	 */
	public Environment getEnvironmentContainingServer(Long serverId) {
		return this.persistenceService.getEnvironmentContainingServer(serverId);
	}

	/**
	 * 
	 * @param environmentId
	 * @param artifactId
	 * @return
	 */
	public Release getReleaseContainingArtifact(Long environmentId, Long artifactId) {
		return this.persistenceService.getReleaseContainingArtifact(environmentId, artifactId);
	}

	/**
	 * 
	 * @param environmentId
	 * @param serverId
	 * @return
	 */
	public Release getReleaseContainingServer(Long environmentId, Long serverId) {
		return this.persistenceService.getReleaseContainingServer(environmentId, serverId);
	}

	/**
	 * 
	 * @param environment
	 * @param artifactId
	 */
	public void deleteDefaultVersions(Environment environment, Long artifactId) {
		this.persistenceService.deleteDefaultVersions(environment, artifactId);
	}

}