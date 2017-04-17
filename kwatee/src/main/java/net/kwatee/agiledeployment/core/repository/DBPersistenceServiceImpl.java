/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.core.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.repository.entity.ApplicationParameter;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.User;
import net.kwatee.agiledeployment.repository.entity.Version;

import org.apache.commons.lang3.StringUtils;

public class DBPersistenceServiceImpl implements PersistenceService {

	@PersistenceContext(unitName = "kwateePU")
	private EntityManager entityManager;

	private Object getSingleResult(Query query) {
		@SuppressWarnings("rawtypes")
		Collection result = query.getResultList();
		if (result.size() > 1) {
			throw new NonUniqueResultException();
		}
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getUsers()
	 */
	@SuppressWarnings("unchecked")
	public Collection<User> getUsers() {
		return this.entityManager
				.createQuery("from KWUser u order by u.login")
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#saveEntity(java.lang.Object)
	 */
	public void saveEntity(Object entity) {
		try {
			this.entityManager.persist(entity);
		} catch (Throwable e) {
			throw new InternalErrorException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#deleteEntity(java.lang.Object)
	 */
	public void deleteEntity(Object entity) {
		try {
			this.entityManager.remove(entity);
			this.entityManager.flush();
		} catch (Throwable e) {
			throw new InternalErrorException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getUserById(Long)
	 */
	public User getUserById(Long userId) {
		Query query = this.entityManager
				.createQuery("from KWUser u where u.id=:user")
				.setParameter("user", userId);
		return (User) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getUserByName(java.lang.String)
	 */
	public User getUserByName(String name) {
		Query query = this.entityManager
				.createQuery("from KWUser u where u.login=:login")
				.setParameter("login", name);
		return (User) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getArtifacts()
	 */
	@SuppressWarnings("unchecked")
	public Collection<Artifact> getArtifacts() {
		return this.entityManager
				.createQuery("from KWPackage")
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getArtifactById(long)
	 */
	public Artifact getArtifactById(Long artifactId) {
		Query query = this.entityManager
				.createQuery("from KWPackage p where p.id=:artifact")
				.setParameter("artifact", artifactId);
		return (Artifact) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getArtifactByName(java.lang.String)
	 */
	public Artifact getArtifactByName(String artifactName) {
		Query query = this.entityManager
				.createQuery("from KWPackage p where p.name=:name")
				.setParameter("name", artifactName);
		return (Artifact) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironmentUsingArtifact(long)
	 */
	@SuppressWarnings("unchecked")
	public Environment getEnvironmentUsingArtifact(Long artifactId) {
		Collection<Environment> environments;
		environments = this.entityManager
				.createNativeQuery("select e.* from KWEnvironment e join kw_environment_package ep on ep.environment_id=e.id where ep.package_id=:artifact", Environment.class)
				.setParameter("artifact", artifactId)
				.getResultList();
		return environments.isEmpty() ? null : environments.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getVersionsByArtifactId(Long)
	 */
	@SuppressWarnings("unchecked")
	public Collection<Version> getVersionsByArtifactId(Long artifactId) {
		return this.entityManager
				.createNativeQuery("from KWVersion v where v.package_id=:artifact", Version.class)
				.setParameter("artifact", artifactId)
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getVersionByName(java.lang.String, java.lang.String)
	 */
	public Version getVersionByName(String artifactName, String versionName) {
		Query query = this.entityManager
				.createNativeQuery("select v.* from KWVersion v join KWPackage p on p.id=v.package_id where p.name=:artifact and v.name=:version", Version.class)
				.setParameter("artifact", artifactName)
				.setParameter("version", versionName);
		return (Version) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getVersionById(Long)
	 */
	public Version getVersionById(Long versionId) {
		Query query = this.entityManager
				.createQuery("from KWVersion v where v.id=:version")
				.setParameter("version", versionId);
		return (Version) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironmentUsingVersion(long)
	 */
	public Environment getEnvironmentUsingVersion(Long versionId) {
		@SuppressWarnings("unchecked")
		Collection<Environment> environments = this.entityManager
				.createNativeQuery("select e.* from KWEnvironment e join KWDeployment d on d.environment_id=e.id join KWDeploymentPackage dp on dp.deployment_id=d.id where dp.version_id=:version", Environment.class)
				.setParameter("version", versionId)
				.getResultList();
		return environments.isEmpty() ? null : environments.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getFrozenReleasesUsingVersion(Long)
	 */
	@SuppressWarnings("unchecked")
	public Collection<Release> getFrozenReleasesUsingVersion(Long versionId) {
		return (Collection<Release>) this.entityManager
				.createNativeQuery("select d.* from KWDeployment d join KWDeploymentPackage dp on dp.deployment_id=d.id where dp.version_id=:version and d.creation_ts<>0", Release.class)
				.setParameter("version", versionId)
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getServers()
	 */
	@SuppressWarnings("unchecked")
	public Collection<Server> getServers() {
		return this.entityManager
				.createQuery("from KWServer")
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getServerById(Long)
	 */
	public Server getServerById(Long serverId) {
		Query query = this.entityManager
				.createQuery("from KWServer s where s.id=:server")
				.setParameter("server", serverId);
		return (Server) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getServerByName(java.lang.String)
	 */
	public Server getServerByName(String serverName) {
		Query query = this.entityManager
				.createQuery("from KWServer s where s.name=:name")
				.setParameter("name", serverName);
		return (Server) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironmentContainingServer(Long)
	 */
	public Environment getEnvironmentContainingServer(Long serverId) {
		@SuppressWarnings("unchecked")
		Collection<Environment> environments = this.entityManager
				.createNativeQuery("select e.* from KWEnvironment e join kw_environment_server es on es.environment_id=e.id where es.server_id=:server", Environment.class)
				.setParameter("server", serverId)
				.getResultList();
		return environments.isEmpty() ? null : environments.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getReleaseContainingServer(Long, Long)
	 */
	public Release getReleaseContainingServer(Long environmentId, Long serverId) {
		@SuppressWarnings("unchecked")
		Collection<Release> releases = this.entityManager
				.createNativeQuery("select d.* from KWDeployment d join KWDeploymentPackage dp on dp.deployment_id=d.id where d.environment_id=:environment and dp.server_id=:server", Release.class)
				.setParameter("environment", environmentId)
				.setParameter("server", serverId)
				.getResultList();
		return releases.isEmpty() ? null : releases.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getReleaseContainingArtifact(Long, Long)
	 */
	public Release getReleaseContainingArtifact(Long environmentId, Long artifactId) {
		@SuppressWarnings("unchecked")
		Collection<Release> releases = this.entityManager
				.createNativeQuery("select d.* from KWDeployment d join KWDeploymentPackage dp on dp.deployment_id=d.id where d.environment_id=:environment and dp.package_id=:artifact and dp.server_id is not NULL", Release.class)
				.setParameter("environment", environmentId)
				.setParameter("artifact", artifactId)
				.getResultList();
		return releases.isEmpty() ? null : releases.iterator().next();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#deleteDefaultVersions(net.kwatee.agiledeployment.core.model.Environment, Long)
	 */
	public void deleteDefaultVersions(Environment environment, Long artifactId) {
		for (Release release : environment.getReleases()) {
			this.entityManager
					.createNativeQuery("delete from KWDeploymentPackage where deployment_id=:deployment and package_id=:artifact and server_id is NULL")
					.setParameter("deployment", release.getId())
					.setParameter("artifact", artifactId)
					.executeUpdate();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironments()
	 */
	@SuppressWarnings("unchecked")
	public Collection<Environment> getEnvironments() {
		return this.entityManager
				.createQuery("from KWEnvironment")
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironmentsContainingVersion(Long)
	 */
	@SuppressWarnings("unchecked")
	public Collection<Environment> getEnvironmentsContainingVersion(Long versionId) {
		return this.entityManager
				.createNativeQuery("select distinct e.* from kw_deployment_server_package dsp join KWDeployment d on d.id=dsp.deployment_id join KWEnvironment e on e.id=d.environment_id where dsp.version_id=:version and e.disable_ts is null", Environment.class)
				.setParameter("version", versionId)
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironmentById(Long)
	 */
	public Environment getEnvironmentById(Long environmentId) {
		Query query = this.entityManager
				.createQuery("from KWEnvironment e where e.id=:environment")
				.setParameter("environment", environmentId);
		return (Environment) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getEnvironmentByName(java.lang.String)
	 */
	public Environment getEnvironmentByName(String environmentName) {
		Query query = this.entityManager
				.createQuery("from KWEnvironment e where e.name=:name")
				.setParameter("name", environmentName);
		return (Environment) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getReleaseByName(java.lang.String, java.lang.String)
	 */
	public Release getReleaseByName(String environmentName, String releaseName) {
		Query query;
		if (StringUtils.isEmpty(releaseName)) {
			// Snapshot release
			query = this.entityManager
					.createNativeQuery("select d.* from KWDeployment d join KWEnvironment e on e.id=d.environment_id where d.creation_ts=0 and e.name=:environment_name", Release.class)
					.setParameter("environment_name", environmentName);
		} else {
			query = this.entityManager
					.createNativeQuery("select d.* from KWDeployment d join KWEnvironment e on e.id=d.environment_id where d.name=:name and e.name=:environment_name", Release.class)
					.setParameter("name", releaseName)
					.setParameter("environment_name", environmentName);
		}
		return (Release) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getReleaseById(Long)
	 */
	public Release getReleaseById(Long releaseId) {
		Query query = this.entityManager
				.createQuery("from KWDeployment d where d.id=:deployment")
				.setParameter("deployment", releaseId);
		return (Release) getSingleResult(query);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getSystemProperties()
	 */
	public Collection<SystemProperty> getSystemProperties() {
		Collection<SystemProperty> properties = this.entityManager
				.createQuery("from KWSystemProperty p order by p.pos", SystemProperty.class)
				.getResultList();
		return properties;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#updateSystemProperties(java.util.Collection)
	 */
	public void updateSystemProperties(Collection<SystemProperty> properties) {
		for (SystemProperty property : properties) {
			this.entityManager.persist(property);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getVersionsInServer(Long)
	 */
	@SuppressWarnings("unchecked")
	public Collection<Version> getVersionsInServer(Long serverId) {
		// return only the direct references
		return this.entityManager
				// .createNativeQuery("select distinct v.* from KWVersion v join KWDeploymentPackage dp on dp.version_id=v.id where dp.server_id=:server", VersionShortDto.class)
				.createNativeQuery(
						"select distinct v.* from KWVersion v join KWDeploymentPackage dp1 on dp1.package_id=v.package_id join KWDeploymentPackage dp2 on dp2.package_id=v.package_id where dp1.server_id=:server and (dp1.version_id is not null or dp1.deployment_id=dp2.deployment_id and dp2.server_id is null and dp2.version_id is not null)",
						Version.class)
				.setParameter("server", serverId)
				.getResultList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.kwatee.agiledeployment.core.repository.PersistenceService#getServersContainingVersion(net.kwatee.agiledeployment.core.model.Version)
	 */
	@SuppressWarnings("unchecked")
	public Collection<Server> getServersContainingVersion(Version version) {
		/*
		 * First get the direct references
		 */
		Collection<Server> servers1 = this.entityManager
				.createNativeQuery("select distinct s.* from KWServer s join KWDeploymentPackage dp on dp.server_id=s.id where dp.version_id=:version", Server.class)
				.setParameter("version", version.getId())
				.getResultList();
		/*
		 * Add all the inherited references
		 */
		Collection<Server> servers2 = this.entityManager
				.createNativeQuery(
						"select distinct s.* from KWServer s join KWDeploymentPackage dp on dp.server_id=s.id join KWDeploymentPackage dp2 on dp2.deployment_id=dp.deployment_id where dp.version_id is null and dp2.server_id is null and dp.package_id=dp2.package_id and dp2.version_id=:version",
						Server.class)
				.setParameter("version", version.getId())
				.getResultList();
		HashSet<Server> servers = new HashSet<Server>();
		servers.addAll(servers1);
		servers.addAll(servers2);
		return servers;
	}

	@Override
	public ApplicationParameter getApplicationParameters() {
		Query query = this.entityManager.createQuery("from KWApplicationParameter p where p.id=0");
		return (ApplicationParameter) getSingleResult(query);
	}

	@Override
	public void updateApplicationParameters(ApplicationParameter parameters) {
		this.entityManager.persist(parameters);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<RepositoryFile> getMergedVersionFiles(Version version, boolean onlyTemplatized, String rootPath) {
		String query = "select f.* from (select f.path, max(f.layer_type) as maxLayer from kw_version_file vf inner join KWRepositoryFile f on f.id=vf.file_id where vf.version_id=:version @rootPath group by f.path) as x inner join KWRepositoryFile f on (f.path=x.path and f.layer_type=maxLayer) inner join kw_version_file vf1 on vf1.file_id=f.id where vf1.version_id=:version @templatized @rootPath order by LOWER(f.path)";
		if (onlyTemplatized)
			query = query.replaceAll("@templatized", "AND f.variables is not null");
		else
			query = query.replaceAll("@templatized", StringUtils.EMPTY);
		if (StringUtils.isNotEmpty(rootPath))
			query = query.replaceAll("@rootPath", "AND f.path LIKE '" + rootPath + "%'");
		else
			query = query.replaceAll("@rootPath", StringUtils.EMPTY);
		List<RepositoryFile> files = this.entityManager
				.createNativeQuery(query, RepositoryFile.class)
				.setParameter("version", version.getId())
				.getResultList();
		return files;
	}
}
