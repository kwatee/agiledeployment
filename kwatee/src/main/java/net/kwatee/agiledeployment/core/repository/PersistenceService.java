/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.util.Collection;
import java.util.List;

import net.kwatee.agiledeployment.repository.entity.ApplicationParameter;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.User;
import net.kwatee.agiledeployment.repository.entity.Version;

/**
 * 
 * @author mac
 * 
 */
public interface PersistenceService {

	/**
	 * Kwatee users
	 * 
	 * @return Kwatee users
	 */
	Collection<User> getUsers();

	/**
	 * Saves changes to an entity in the database
	 * 
	 * @param entity
	 */
	void saveEntity(Object entity);

	/**
	 * Deletes an entity from the database
	 * 
	 * @param entity
	 */
	void deleteEntity(Object entity);

	/*
	 * 
	 */
	User getUserById(Long userId);

	/*
	 * 
	 */
	User getUserByName(String name);

	/*
	 * 
	 */
	Collection<Artifact> getArtifacts();

	/*
	 * 
	 */
	Artifact getArtifactById(Long artifactId);

	/*
	 * 
	 */
	Artifact getArtifactByName(String artifactName);

	/**
	 * Retrieves an environment that reference the artifact identified by artifactId
	 * 
	 * @param artifactId
	 *            Kwatee artifact id
	 * @return the first environment in which the artifact is used
	 */
	Environment getEnvironmentUsingArtifact(Long artifactId);

	/*
	 * 
	 */
	Collection<Version> getVersionsByArtifactId(Long artifactId);

	/*
	 * 
	 */
	Version getVersionByName(String artifactName, String versionName);

	Version getVersionById(Long versionId);

	/**
	 * An environment referencing the kwatee version identified by versionId or null
	 * 
	 * @param versionId
	 *            Id of a kwatee version
	 * @return An environment referencing the kwatee version identified by versionId or null
	 */
	Environment getEnvironmentUsingVersion(Long versionId);

	/**
	 * A release referencing the kwatee version identified by versionId or null
	 * 
	 * @param versionId
	 *            Id of a kwatee version
	 * @return A release referencing the kwatee version identified by versionId or null
	 */
	Collection<Release> getFrozenReleasesUsingVersion(Long versionId);

	/*
	 * 
	 */
	Collection<Server> getServers();

	/*
	 * 
	 */
	Server getServerById(Long serverId);

	/*
	 * 
	 */
	Server getServerByName(String serverName);

	/**
	 * An environment referencing the server identified by serverId
	 * 
	 * @param serverId
	 *            Id of kwatee server
	 * @return An environment referencing server identified by serverId
	 */
	Environment getEnvironmentContainingServer(Long serverId);

	/**
	 * A release referencing the server identified by serverId
	 * 
	 * @param serverId
	 *            Id of kwatee server
	 * @return An release referencing server identified by serverId
	 */
	Release getReleaseContainingServer(Long environmentId, Long serverId);

	/**
	 * A release referencing the artifact identified by artifactId
	 * 
	 * @param artifactId
	 *            Id of kwatee artifact
	 * @return A release referencing the artifact identified by artifactId
	 */
	Release getReleaseContainingArtifact(Long environmentId, Long artifactId);

	/**
	 * Called when an environment artifact is removed from and environment to clear all release default versions
	 * 
	 * @param environment
	 * @param artifactId
	 */
	void deleteDefaultVersions(Environment environment, Long artifactId);

	/**
	 * The list of kwatee environments
	 * 
	 * @return The list of kwatee environments
	 */
	Collection<Environment> getEnvironments();

	/**
	 * The list of kwatee environments referencing the kwatee version identified by versionId
	 * 
	 * @param versionId
	 *            Id of kwatee version
	 * @return The list of kwatee environments referencing the kwatee version identified by versionId
	 */
	Collection<Environment> getEnvironmentsContainingVersion(Long versionId);

	/*
	 * 
	 */
	Environment getEnvironmentById(Long environmentId);

	/*
	 * 
	 */
	Environment getEnvironmentByName(String environmentName);

	/*
	 * 
	 */
	Release getReleaseByName(String environmentName, String releaseName);

	/*
	 * 
	 */
	Release getReleaseById(Long releaseId);

	/**
	 * The list of system properties
	 * 
	 * @return The list of system properties
	 */
	Collection<SystemProperty> getSystemProperties();

	/**
	 * Updates systemproperties
	 * 
	 * @param properties
	 */
	void updateSystemProperties(Collection<SystemProperty> properties);

	/**
	 * The list of kwatee versions contained in this server across all release
	 * 
	 * @param serverId
	 *            The id of a kwatee server
	 * @return The list of kwatee versions contained in this server across all release
	 */
	Collection<Version> getVersionsInServer(Long serverId);

	/**
	 * The list of servers referencing a kwatee version in all environments
	 * 
	 * @param version
	 *            A kwatee version
	 * @return The list of servers referencing a kwatee version in all environments
	 */
	Collection<Server> getServersContainingVersion(Version version);

	/**
	 * @return the object containing the customer configurable application parameters
	 */
	ApplicationParameter getApplicationParameters();

	/**
	 * Updates customer configurable application parameters
	 * 
	 * @param parameters
	 */
	void updateApplicationParameters(ApplicationParameter parameters);

	/**
	 * Returns list of version files where the highest layer ordinal version is returned in case of equality
	 * 
	 * @param version
	 * @param onlyTemplatized
	 * @param rootPath
	 * @return list of files
	 */
	List<RepositoryFile> getMergedVersionFiles(Version version, boolean onlyTemplatized, String rootPath);
}
