/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.Constants.LayerType;
import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.common.exception.ServerNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.SnapshotReleaseException;
import net.kwatee.agiledeployment.common.exception.TagReleaseException;
import net.kwatee.agiledeployment.conduit.DefaultServerInstance;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import net.kwatee.agiledeployment.core.Audit;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.core.repository.EnvironmentRepository;
import net.kwatee.agiledeployment.core.repository.ServerRepository;
import net.kwatee.agiledeployment.core.service.FileStoreService;
import net.kwatee.agiledeployment.core.service.LayerService;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.RepoVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.EnvironmentDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.FilePropertiesDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.ReleaseDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VariableReference;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.EnvironmentArtifact;
import net.kwatee.agiledeployment.repository.entity.EnvironmentServer;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.ReleaseVariable;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;
import net.kwatee.agiledeployment.webapp.controller.ServletHelper;
import net.kwatee.agiledeployment.webapp.mapper.AdminMapper;
import net.kwatee.agiledeployment.webapp.mapper.EnvironmentMapper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
public class EnvironmentService {

	@Autowired
	private EnvironmentMapper environmentMapper;
	@Autowired
	private AdminMapper adminMapper;
	@Autowired
	private EnvironmentRepository environmentRepository;
	@Autowired
	private ArtifactRepository artifactRepository;
	@Autowired
	private FileStoreService fileStoreService;
	@Autowired
	private ServerRepository serverRepository;
	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private LayerService layerService;
	@Autowired
	private ArtifactService artifactHandler;
	@Autowired
	private VariableService variableService;

	final static private org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EnvironmentService.class);

	/**
	 * 
	 * @return
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<EnvironmentDto> getEnvironments() {
		Collection<Environment> environments = new ArrayList<>(this.environmentRepository.getEnvironments());
		return this.environmentMapper.toShortEnvironmentDtos(environments);
	}

	/**
	 * 
	 * @param environmentName
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public EnvironmentDto getEnvironment(String environmentName) throws ObjectNotExistException {
		Environment environment = this.environmentRepository.getCheckedEnvironment(environmentName);
		return this.environmentMapper.toEnvironmentDto(environment);
	}

	/**
	 * 
	 * @param environmentName
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void deleteEnvironment(String environmentName) {
		Environment environment = this.environmentRepository.getUncheckedEnvironment(environmentName);
		if (environment != null) {
			this.environmentRepository.deleteEnvironment(environment);
			Audit.log("Deleted environment {}", environmentName);
		}
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void createEnvironment(EnvironmentDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException, CannotDeleteObjectInUseException {
		Environment environment = this.environmentRepository.getUncheckedEnvironment(dto.getName());
		if (environment != null)
			throw new ObjectAlreadyExistsException("Environment " + dto.getName());
		environment = new Environment();
		environment.setName(dto.getName());
		Release editRelease = new Release();
		environment.getReleases().add(editRelease);
		editRelease.setEnvironment(environment);
		updateEnvironment(environment, dto);
		Audit.log("Created environment {}", dto.getName());
	}

	/**
	 * 
	 * @param dto
	 * @param fromEnvironmentName
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void duplicateEnvironment(EnvironmentDto dto, String fromEnvironmentName) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		Environment environment = this.environmentRepository.getCheckedEnvironment(fromEnvironmentName);
		Environment duplicateEnvironment = environment.duplicate(dto.getName());
		duplicateEnvironment.setDescription("duplicate");
		this.environmentRepository.saveEnvironment(duplicateEnvironment);
		for (Release d : duplicateEnvironment.getReleases()) {
			this.fileStoreService.duplicateRelease(environment.getName(), d.getName(), duplicateEnvironment.getName(), d.getName());
		}
		environment = this.environmentRepository.getCheckedEnvironment(fromEnvironmentName);
		updateEnvironment(environment, dto);
		Audit.log("Created environment {}", dto.getName());
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void updateEnvironment(EnvironmentDto dto) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		Environment environment = this.environmentRepository.getCheckedEnvironment(dto.getName());
		updateEnvironment(environment, dto);
		Audit.log("Updated environment {}", dto.getName());
	}

	private void updateEnvironment(Environment environment, EnvironmentDto dto) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		if (dto.isDisabled() != null)
			environment.setDisabled(dto.isDisabled());
		if (dto.getDescription() != null)
			environment.setDescription(dto.getDescription());
		if (dto.isSequentialDeployment() != null)
			environment.setSequentialDeployment(dto.isSequentialDeployment());
		if (dto.getServers() != null)
			updateEnvironmentServers(environment, dto.getServers());
		if (dto.getArtifacts() != null)
			updateEnvironmentArtifacts(environment, dto.getArtifacts());
		this.environmentRepository.saveEnvironment(environment);
	}

	private void updateEnvironmentArtifacts(Environment environment, final Collection<String> environmentArtifacts) throws CannotDeleteObjectInUseException, ObjectNotExistException {
		/*
		 * Remove obsolete artifacts
		 */
		final ArrayList<Artifact> artifactsToRemove = new ArrayList<>();
		CollectionUtils.filter(environment.getArtifacts(),
				new Predicate() {

					public boolean evaluate(Object p) {
						EnvironmentArtifact a = (EnvironmentArtifact) p;
						if (!environmentArtifacts.contains(a.getArtifact().getName())) {
							artifactsToRemove.add(a.getArtifact());
							return false;
						}
						return true;
					}
				});
		// Check that the artifacts to remove are not in use in an environment
		for (Artifact artifact : artifactsToRemove) {
			Release d = this.environmentRepository.getReleaseContainingArtifact(environment.getId(), artifact.getId());
			if (d != null)
				throw new CannotDeleteObjectInUseException("artifact " + artifact, "release " + d);
			this.environmentRepository.deleteDefaultVersions(environment, artifact.getId());
		}

		/*
		 * Add & update artifacts
		 */
		int pos = 0;
		for (final String ea : environmentArtifacts) {
			EnvironmentArtifact p = (EnvironmentArtifact) CollectionUtils.find(environment.getArtifacts(),
					new Predicate() {

						public boolean evaluate(Object p) {
							EnvironmentArtifact a = (EnvironmentArtifact) p;
							return ea.equals(a.getArtifact().getName());
						}
					});
			if (p == null) {
				p = new EnvironmentArtifact();
				p.setArtifact(this.artifactRepository.getCheckedArtifact(ea));
				environment.getArtifacts().add(p);
			}
			p.setPos(pos++);
		}
	}

	private void updateEnvironmentServers(Environment environment, final Collection<String> environmentServers) throws CannotDeleteObjectInUseException, ObjectNotExistException {
		/*
		 * Remove obsolete artifacts
		 */
		final ArrayList<Server> serversToRemove = new ArrayList<>();
		CollectionUtils.filter(environment.getServers(),
				new Predicate() {

					public boolean evaluate(Object p) {
						EnvironmentServer e = (EnvironmentServer) p;
						if (!environmentServers.contains(e.getServer().getName())) {
							serversToRemove.add(e.getServer());
							return false;
						}
						return true;
					}
				});
		// Check that the servers to remove are not in use in an environment
		for (Server server : serversToRemove) {
			Release d = this.environmentRepository.getReleaseContainingServer(environment.getId(), server.getId());
			if (d != null)
				throw new CannotDeleteObjectInUseException("server " + server, "release " + d);
		}

		/*
		 * Add & update servers
		 */
		int pos = 0;
		for (final String es : environmentServers) {
			EnvironmentServer s = (EnvironmentServer) CollectionUtils.find(environment.getServers(),
					new Predicate() {

						public boolean evaluate(Object s) {
							EnvironmentServer e = (EnvironmentServer) s;
							return es.equals(e.getServer().getName());
						}
					});
			if (s == null) {
				s = new EnvironmentServer();
				s.setServer(this.serverRepository.getCheckedServer(es));
				environment.getServers().add(s);
			}
			s.setPos(pos++);
		}
		/*
		 * Physically remove obsolete directories
		 */
		for (Server removedServer : serversToRemove) {
			for (Release release : environment.getReleases()) {
				try {
					this.fileStoreService.deleteReleaseOverlay(release.getEnvironment().getName(), release.getName(), removedServer.getName());
				} catch (InternalErrorException e) {}
			}
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public ReleaseDto getRelease(String environmentName, String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		ReleaseDto dto = this.environmentMapper.toReleaseDto(release);
		return dto;
	}

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<String> getReleaseErrors(String environmentName, String releaseName) throws ObjectNotExistException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		if (release.hasErrors()) {
			return releaseErrors(release);
		}
		return Collections.emptyList();
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 * @throws SnapshotReleaseException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<ArtifactVersionDto> getEffectiveReleaseArtifacts(String environmentName, String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, SnapshotReleaseException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		if (release.hasErrors())
			throw new SnapshotReleaseException("Release " + release.toString() + "has errors");
		return this.environmentMapper.toEffectiveReleaseArtifactDtos(release);
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @throws SnapshotReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void deleteRelease(String environmentName, String releaseName) throws SnapshotReleaseException, ObjectAlreadyExistsException, ObjectNotExistException {
		Release release = this.environmentRepository.getUncheckedRelease(environmentName, releaseName);
		if (release != null) {
			this.environmentRepository.deleteRelease(release);
			this.fileStoreService.deleteRelease(environmentName, releaseName);
			Audit.log("Deleted release {}", release.toString());
		}
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectAlreadyExistsException
	 * @throws TagReleaseException
	 * @throws ObjectNotExistException
	 * @throws ServerNotInReleaseException
	 * @throws ArtifactNotInReleaseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void tagRelease(ReleaseDto dto) throws ObjectAlreadyExistsException, TagReleaseException, ObjectNotExistException, ServerNotInReleaseException, ArtifactNotInReleaseException {
		String environmentName = dto.getEnvironmentName();
		String releaseName = dto.getName();
		if (this.environmentRepository.getUncheckedRelease(environmentName, releaseName) != null)
			throw new ObjectAlreadyExistsException("ReleaseShortDto " + releaseName);
		Release snapshotRelease = this.environmentRepository.getCheckedSnapshotRelease(environmentName);
		if (checkReleaseStatus(snapshotRelease) != null)
			throw new TagReleaseException("Cannot tag snapshot with errors");
		Release release = duplicateRelease(snapshotRelease, releaseName, null);
		release.setName(releaseName);
		release.setCreationTs(new Date().getTime());

		/*
		 * Include snapshot of system properties in variables
		 */
		Collection<SystemProperty> properties = this.adminRepository.getSystemProperties();
		for (final SystemProperty property : properties) {
			if (!property.isHidden()) {
				if (CollectionUtils.find(release.getVariables(),
						new Predicate() {

							public boolean evaluate(Object p) {
								ReleaseVariable dv = (ReleaseVariable) p;
								return dv.getArtifactId() == null && dv.getServerId() == null && dv.getName().equals(property.getName());
							}
						}) == null) {
					ReleaseVariable parameter = new ReleaseVariable();
					parameter.setName(property.getName());
					parameter.setValue(property.getValue());
					parameter.setRelease(release);
					parameter.setFrozenSystemProperty(true);
					release.getVariables().add(parameter);
				}
			}
		}
		this.fileStoreService.duplicateRelease(release.getEnvironment().getName(), Constants.SNAPSHOT_RELEASE_NAME, release.getEnvironment().getName(), release.getName());
		updateRelease(release, dto);
		this.environmentRepository.saveRelease(release);
		Audit.log("Tagged release {}", release.toString());
	}

	/**
	 * 
	 * @param dto
	 * @return
	 * @throws ObjectNotExistException
	 * @throws ServerNotInReleaseException
	 * @throws ArtifactNotInReleaseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateRelease(ReleaseDto dto) throws ObjectNotExistException, ServerNotInReleaseException, ArtifactNotInReleaseException {
		Release release = this.environmentRepository.getCheckedRelease(dto.getEnvironmentName(), dto.getName());
		updateRelease(release, dto);
		updateAllReleaseArtifactsHasOverlays(release);
		Collection<String> errors = releaseErrors(release); // updates release.hasErrors
		this.environmentRepository.saveRelease(release);
		Audit.log("Updated release {}", release.toString());
		Map<String, Object> result = new HashMap<>();
		result.put("errors", errors);
		return result;
	}

	private Collection<String> releaseErrors(Release release) {
		Collection<String> errors = checkReleaseStatus(release);
		if (errors != null) {
			release.setHasErrors(true);
			return errors;
		}
		release.setHasErrors(false);
		return null;
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param artifactName
	 * @param versionName
	 * @param serverName
	 * @throws ObjectNotExistException
	 * @throws SnapshotReleaseException
	 * @throws ArtifactNotInReleaseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void setArtifactActiveVersion(String environmentName, String releaseName, String artifactName, String versionName, String serverName) throws ObjectNotExistException, SnapshotReleaseException, ArtifactNotInReleaseException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		if (!release.isSnapshot())
			throw new SnapshotReleaseException("Cannot modify this release");
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		Server server = serverName == null ? null : this.serverRepository.getCheckedServer(serverName);
		ReleaseArtifact dp = release.getReleaseArtifact(server, version.getArtifact());
		dp.setVersion(version);
		updateAllReleaseArtifactsHasOverlays(release);
		releaseErrors(release); // updates release.hasErrors
		this.environmentRepository.saveRelease(release);
		Audit.log("Updated artifact versions of release {}", release.toString());
	}

	private void updateRelease(Release release, ReleaseDto dto) throws ObjectNotExistException, ServerNotInReleaseException, ArtifactNotInReleaseException {
		if (dto.isDisabled() != null)
			release.setDisabled(dto.isDisabled());
		if (dto.getDescription() != null)
			release.setDescription(dto.getDescription());
		if (release.isSnapshot()) {
			release.setDisabled(false);
			if (dto.getPreSetupAction() != null)
				release.setPreSetupAction(dto.getPreSetupAction());
			if (dto.getPostSetupAction() != null)
				release.setPostSetupAction(dto.getPostSetupAction());
			if (dto.getPreCleanupAction() != null)
				release.setPreCleanupAction(dto.getPreCleanupAction());
			if (dto.getPostCleanupAction() != null)
				release.setPostCleanupAction(dto.getPostCleanupAction());
			PermissionDto permissions = dto.getPermissions();
			if (permissions != null) {
				if (permissions.getFileOwner() != null)
					release.setFileOwner(permissions.getFileOwner().isEmpty() ? null : permissions.getFileOwner());
				if (permissions.getFileGroup() != null)
					release.setFileGroup(permissions.getFileGroup().isEmpty() ? null : permissions.getFileGroup());
				String fileMode = permissions.getFileMode();
				if (fileMode != null)
					release.setFileMode(fileMode.isEmpty() ? null : Integer.parseInt(fileMode, 8));
				String dirMode = permissions.getDirMode();
				if (dirMode != null)
					release.setDirMode(dirMode.isEmpty() ? null : Integer.parseInt(dirMode, 8));
			}
			if (dto.getDefaultArtifacts() != null || dto.getServers() != null) {
				Collection<ServerArtifactsDto> servers = new ArrayList<>();
				if (dto.getDefaultArtifacts() != null) {
					ServerArtifactsDto server = new ServerArtifactsDto();
					server.setArtifacts(dto.getDefaultArtifacts());
					servers.add(server);
				}
				if (dto.getServers() != null)
					servers.addAll(dto.getServers());
				for (ServerArtifactsDto server : servers) {
					String serverName = server.getServer();
					/*
					 * Verify that servers and artifacts exist in environment
					 */
					if (serverName != null && !existsServerInEnvironment(release.getEnvironment(), serverName)) {
						throw new ServerNotInReleaseException(serverName);
					}
					Collection<ArtifactVersionDto> artifacts = server.getArtifacts();
					if (CollectionUtils.isNotEmpty(artifacts)) {
						for (ArtifactVersionDto artifact : artifacts) {
							if (artifact.getArtifact() != null && !existsArtifactInEnvironment(release.getEnvironment(), artifact.getArtifact())) {
								throw new ArtifactNotInReleaseException(artifact.getArtifact());
							}
						}
					}
					updateServerArtifacts(release, server);
				}
			}
		}
	}

	private void updateServerArtifacts(Release release, final ServerArtifactsDto dto) throws ObjectNotExistException {
		Collection<ReleaseArtifact> serverArtifacts = release.getServerArtifacts(dto.getServer());
		/*
		 * Remove obsolete release artifacts
		 */
		final ArrayList<ReleaseArtifact> toRemove = new ArrayList<>();
		CollectionUtils.filter(serverArtifacts,
				new Predicate() {

					public boolean evaluate(Object p) {
						ReleaseArtifact r = (ReleaseArtifact) p;
						if (CollectionUtils.isNotEmpty(dto.getArtifacts())) {
							for (ArtifactVersionDto artifact : dto.getArtifacts()) {
								if (r.getArtifact().getName().equals(artifact.getArtifact()))
									return true;
							}
						}
						toRemove.add(r);
						return false;
					}
				});
		for (ReleaseArtifact r : toRemove) {
			this.fileStoreService.deleteReleaseOverlay(release.getEnvironment().getName(), release.getName(), dto.getServer(), r.getArtifact().getName());
			removeVariables(release.getVariables(), r.getServer(), r.getArtifact());
			release.getReleaseArtifacts().remove(r);
		}

		/*
		 * Add & update
		 */
		Server server = dto.getServer() == null ? null : this.serverRepository.getCheckedServer(dto.getServer());
		if (CollectionUtils.isNotEmpty(dto.getArtifacts())) {
			for (final ArtifactVersionDto artifact : dto.getArtifacts()) {
				ReleaseArtifact p = (ReleaseArtifact) CollectionUtils.find(serverArtifacts,
						new Predicate() {

							public boolean evaluate(Object p) {
								ReleaseArtifact r = (ReleaseArtifact) p;
								return r.getArtifact().getName().equals(artifact.getArtifact());
							}
						});
				if (p == null) {
					p = new ReleaseArtifact();
					p.setRelease(release);
					p.setArtifact(this.artifactRepository.getCheckedArtifact(artifact.getArtifact()));
					p.setServer(server);
					release.getReleaseArtifacts().add(p);
				}
				if (artifact.getVersion() == null)
					p.setVersion(null);
				else
					p.setVersion(this.artifactRepository.getCheckedVersion(artifact.getArtifact(), artifact.getVersion()));
			}
		}
		release.resetReleaseArtifacts();
	}

	private void removeVariables(Collection<ReleaseVariable> variables, final Server server, final Artifact artifact) {
		CollectionUtils.filter(variables,
				new Predicate() {

					public boolean evaluate(Object v) {
						ReleaseVariable variable = (ReleaseVariable) v;
						if ((server == null || server.getId().equals(variable.getServerId())) &&
								(artifact == null || artifact.getId().equals(variable.getArtifactId()))) {
							return false;
						}
						return true;
					}
				});
	}

	private boolean existsServerInEnvironment(Environment environment, final String serverName) {
		return CollectionUtils.exists(environment.getServers(), new Predicate() {

			public boolean evaluate(Object es) {
				return ((EnvironmentServer) es).getServer().getName().equals(serverName);
			}
		});
	}

	private boolean existsArtifactInEnvironment(Environment environment, final String artifactName) {
		boolean b = CollectionUtils.exists(environment.getArtifacts(), new Predicate() {

			public boolean evaluate(Object ep) {
				return ((EnvironmentArtifact) ep).getArtifact().getName().equals(artifactName);
			}
		});
		return b;
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @throws SnapshotReleaseException
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void reeditRelease(String environmentName, String releaseName) throws SnapshotReleaseException, ObjectNotExistException {
		if (releaseName.equalsIgnoreCase(Constants.SNAPSHOT_RELEASE_NAME))
			throw new SnapshotReleaseException("ReleaseShortDto '" + releaseName + "' is a reserved name");
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		if (release.isSnapshot())
			throw new SnapshotReleaseException(environmentName);
		Release oldSnapshotRelease = this.environmentRepository.getUncheckedSnapshotRelease(environmentName);
		if (oldSnapshotRelease != null) {
			// deleteRelease(oldSnapshotRelease);
			this.fileStoreService.deleteRelease(oldSnapshotRelease.getEnvironment().getName(), oldSnapshotRelease.getName());
		}
		Release snapshotRelease = duplicateRelease(release, Constants.SNAPSHOT_RELEASE_NAME, oldSnapshotRelease);
		// if (oldSnapshotRelease != null)
		// repository_service.deleteRelease(oldSnapshotRelease);
		this.fileStoreService.duplicateRelease(snapshotRelease.getEnvironment().getName(), releaseName, snapshotRelease.getEnvironment().getName(), Constants.SNAPSHOT_RELEASE_NAME);
		updateAllReleaseArtifactsHasOverlays(snapshotRelease);
		this.environmentRepository.saveEnvironment(release.getEnvironment());
		Audit.log("Reedited release {}", release.toString());
	}

	private Release duplicateRelease(Release release, String duplicateName, Release duplicateRelease) throws ObjectNotExistException {
		Environment environment = release.getEnvironment();
		duplicateRelease = release.duplicate(duplicateRelease, null); // will be snapshot by default, otherwise we risk an illegal name exception
		duplicateRelease.setName(duplicateName);
		environment.getReleases().add(duplicateRelease);
		updateAllReleaseArtifactsHasOverlays(duplicateRelease);
		this.environmentRepository.saveEnvironment(environment);
		duplicateRelease = this.environmentRepository.getCheckedRelease(environment.getName(), duplicateName);
		return duplicateRelease;
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @param path
	 * @return
	 * @throws ObjectNotExistException
	 * @throws ArtifactNotInReleaseException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<FileDto> getPackageFiles(String environmentName, String releaseName, String serverName, String artifactName, String path) throws ObjectNotExistException, ArtifactNotInReleaseException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		this.artifactRepository.getCheckedArtifact(artifactName); // to make sure it exists
		for (ReleaseArtifact rp : release.getServerArtifacts(serverName)) {
			if (rp.getArtifact().getName().equals(artifactName)) {
				//				path = path == null ? StringUtils.EMPTY : path;
				if (path != null && !path.endsWith("/"))
					path += "/";
				Collection<RepositoryFile> files = this.layerService.getMergedReleaseFilesWithoutActiveVersion(rp, false, path);
				return this.environmentMapper.toFileDtos(files, path);
			}
		}
		throw new ArtifactNotInReleaseException(artifactName);
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws ArtifactNotInReleaseException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<FileDto> getSpecialFiles(String environmentName, String releaseName, String serverName, String artifactName) throws ObjectNotExistException, ArtifactNotInReleaseException, CompatibilityException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		this.artifactRepository.getCheckedArtifact(artifactName); // to make sure it exists
		for (ReleaseArtifact rp : release.getServerArtifacts(serverName)) {
			if (rp.getArtifact().getName().equals(artifactName)) {
				Collection<RepositoryFile> files = null;
				try {
					files = this.layerService.getMergedReleaseFiles(rp, false, null);
				} catch (NoActiveVersionException e) {}
				Collection<FileDto> dtos = this.environmentMapper.toFileDtos(files, null);
				return dtos;
			}
		}
		throw new ArtifactNotInReleaseException(artifactName);
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @param filePath
	 * @param dto
	 * @throws ObjectNotExistException
	 * @throws SnapshotReleaseException
	 * @throws ArtifactNotInReleaseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void updateReleasePackageFileProperties(String environmentName, String releaseName, String serverName, String artifactName, String filePath, FilePropertiesDto dto) throws ObjectNotExistException, SnapshotReleaseException, ArtifactNotInReleaseException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		if (!release.isSnapshot())
			throw new SnapshotReleaseException("Not editable");
		Artifact artifact = this.artifactRepository.getCheckedArtifact(artifactName);
		Server server = serverName == null ? null : this.serverRepository.getCheckedServer(serverName);
		ReleaseArtifact releaseArtifact = release.getReleaseArtifact(server, artifact);
		RepositoryFile file = releaseArtifact.getFile(filePath);
		if (dto.getIgnoreIntegrity() != null)
			file.setIgnoreIntegrity(dto.getIgnoreIntegrity());
		if (dto.getIgnoreVariables() != null)
			file.setIgnoreVariables(dto.getIgnoreVariables());
		if (dto.getDontDelete() != null)
			file.setDontDelete(dto.getDontDelete());
		PermissionDto permissions = dto.getPermissions();
		if (permissions != null) {
			String fileOwner = permissions.getFileOwner();
			if (fileOwner != null)
				file.setFileOwner(fileOwner.isEmpty() ? null : fileOwner);
			String fileGroup = permissions.getFileGroup();
			if (fileGroup != null)
				file.setFileGroup(fileGroup.isEmpty() ? null : fileGroup);
			String fileMode = permissions.getFileMode();
			if (fileMode != null)
				file.setFileMode(fileMode.isEmpty() ? null : Integer.parseInt(fileMode, 8));
			String dirMode = permissions.getDirMode();
			if (dirMode != null)
				file.setDirMode(dirMode.isEmpty() ? null : Integer.parseInt(dirMode, 8));
		}
		updateReleaseArtifactHasOverlays(releaseArtifact);
		boolean hadErrors = release.hasErrors();
		releaseErrors(release); // updates release.hasErrors
		this.environmentRepository.saveRelease(release);
		if (hadErrors != release.hasErrors()) {
			Audit.log("Updated file {} of ", filePath, releaseArtifact.toString());
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @param path
	 * @param res
	 * @throws ObjectNotExistException
	 * @throws ArtifactNotInReleaseException
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public void viewPackageFile(String environmentName, String releaseName, String serverName, String artifactName, String path, HttpServletResponse res) throws ObjectNotExistException, ArtifactNotInReleaseException, CompatibilityException, NoActiveVersionException {
		try {
			Artifact artifact = this.artifactRepository.getCheckedArtifact(artifactName);
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			String overlayPath = null;
			Server server = serverName == null ? null : this.serverRepository.getCheckedServer(serverName);
			if (server != null) {
				overlayPath = this.fileStoreService.getReleaseOverlayPath(environmentName, release.getName(), serverName, artifactName);
				if (!this.fileStoreService.existsFile(overlayPath, path) && serverName != null)
					overlayPath = null;
			}
			if (overlayPath == null) {
				overlayPath = this.fileStoreService.getReleaseOverlayPath(environmentName, release.getName(), null, artifactName);
				if (!this.fileStoreService.existsFile(overlayPath, path))
					overlayPath = null;
			}
			if (overlayPath == null) {
				String versionName = this.environmentRepository.getReleaseArtifactActiveVersion(release, server, artifact);
				this.artifactHandler.getPackageFile(artifactName, versionName, path, res);
			} else {
				ReleaseArtifact releaseArtifact = release.getReleaseArtifact(server, artifact);
				RepositoryFile file = releaseArtifact.getFile(path);
				boolean hasVariables = file != null && file.isTemplatized() && !file.ignoreVariables();
				InputStream in = this.fileStoreService.getFileInputStream(overlayPath, path);
				int len = (int) this.fileStoreService.getFileLength(overlayPath, path);
				if (!hasVariables)
					ServletHelper.sendFile(res, in, len, null, "text/plain");
				else {
					Pattern pattern = VariableService.getVariablePattern(VariableService.VAR_PREFIX_CHAR);
					ServletHelper.sendHighlightedText(res, in, len, pattern);
				}
			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param artifactName
	 * @param serverName
	 * @param path
	 * @param uploadMultipart
	 * @param uploadUrl
	 * @throws ObjectNotExistException
	 * @throws ArtifactNotInReleaseException
	 * @throws IOException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void uploadPackageOverlay(String environmentName, String artifactName, String serverName, String path, MultipartFile uploadMultipart, String uploadUrl) throws ObjectNotExistException, ArtifactNotInReleaseException, IOException {
		FileUpload upload = null;
		try {
			upload = new FileUpload(uploadUrl, uploadMultipart, true);
			String overlayFilePath = StringUtils.strip(path.replace("\\", "/"), "/");
			if (!overlayFilePath.isEmpty()) {
				overlayFilePath += "/";
			}
			overlayFilePath += upload.getName();
			Release release = this.environmentRepository.getCheckedSnapshotRelease(environmentName);
			Artifact artifact = this.artifactRepository.getCheckedArtifact(artifactName);
			Server server = serverName == null ? null : this.serverRepository.getCheckedServer(serverName);
			ReleaseArtifact releaseArtifact = release.getReleaseArtifact(server, artifact);
			String overlayPath = this.fileStoreService.getReleaseOverlayPath(environmentName, release.getName(), serverName, artifactName);
			OutputStream out = this.fileStoreService.getFileOutputStream(overlayPath, overlayFilePath);
			IOUtils.copy(upload.getInputStream(), out);
			IOUtils.closeQuietly(out);
			this.layerService.updateReleaseOverlay(releaseArtifact, overlayFilePath);
			updateReleaseArtifactHasOverlays(releaseArtifact);
			releaseErrors(release); // updates release.hasErrors
			this.environmentRepository.saveRelease(release);
			Audit.log("Added overlay {} to {}", path, releaseArtifact.toString());
		} finally {
			FileUpload.releaseQuietly(upload);
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param artifactName
	 * @param serverName
	 * @param filePath
	 * @throws ObjectNotExistException
	 * @throws ArtifactNotInReleaseException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void deletePackageOverlay(String environmentName, String artifactName, String serverName, String filePath) throws ObjectNotExistException, ArtifactNotInReleaseException {
		Release release = this.environmentRepository.getCheckedSnapshotRelease(environmentName);
		Artifact artifact = this.artifactRepository.getCheckedArtifact(artifactName);
		Server server = serverName == null ? null : this.serverRepository.getCheckedServer(serverName);
		ReleaseArtifact releaseArtifact = release.getReleaseArtifact(server, artifact);
		this.layerService.removeReleaseOverlay(releaseArtifact, filePath);
		updateReleaseArtifactHasOverlays(releaseArtifact);
		releaseErrors(release); // updates release.hasErrors
		this.environmentRepository.saveRelease(release);
		Audit.log("Deleted overlay {} of {}", filePath, releaseArtifact.toString());
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public ReleaseDto getVariables(String environmentName, String releaseName) throws ObjectNotExistException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		ReleaseDto dto = this.environmentMapper.toShortReleaseDto(release);
		List<ReleaseVariable> variables = release.getVariables();
		Collection<VariableDto> variableDtos = this.environmentMapper.toReleaseVariableDtos(variables, false);
		dto.setVariables(variableDtos);

		Set<VersionVariable> defaultValues = new HashSet<VersionVariable>();
		Collection<Version> versions = this.environmentRepository.getReleaseVersions(release, false);
		for (Version version : versions) {
			defaultValues.addAll(version.getVariablesDefaultValues());
		}
		Collection<VariableDto> defaultVariablesDtos = this.environmentMapper.toVersionVariableDtos(defaultValues);
		if (CollectionUtils.isNotEmpty(defaultVariablesDtos))
			dto.setArtifactVariables(defaultVariablesDtos);

		Map<String, Collection<VariableReference>> missingVariables = new HashMap<>();
		scanMissingVariables(release, missingVariables);
		Collection<VariableDto> missingVariablesDtos = this.environmentMapper.toMissingVariableDtos(missingVariables);
		dto.setMissingVariables(missingVariablesDtos);
		return dto;
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public ReleaseDto getExportVariables(String environmentName, String releaseName) throws ObjectNotExistException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		List<ReleaseVariable> variables = release.getVariables();
		Collection<VariableDto> variableDtos = this.environmentMapper.toReleaseVariableDtos(variables, true);
		ReleaseDto dto = new ReleaseDto();
		dto.setEnvironmentName(environmentName);
		dto.setVariables(variableDtos);
		return dto;
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param serverName
	 * @param artifactName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws ArtifactNotInReleaseException
	 * @throws MissingVariableException
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<VariableDto> getResolvedVariables(String environmentName, String releaseName, String serverName, String artifactName) throws ObjectNotExistException, ArtifactNotInReleaseException, MissingVariableException, CompatibilityException, NoActiveVersionException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		Server server = this.serverRepository.getCheckedServer(serverName);
		Artifact artifact = this.artifactRepository.getCheckedArtifact(artifactName);
		ReleaseArtifact releaseArtifact = release.getReleaseArtifact(server, artifact);
		ServerInstance instance = new DefaultServerInstance("testInstance", "test.ip.address", 42);
		instance.setParent(server);
		VariableResolver resolver = new RepoVariableResolver(release, server, instance, releaseArtifact.getActiveVersion());

		HashMap<String, String> variables = new HashMap<>();
		for (SystemProperty var : this.adminRepository.getSystemProperties()) {
			String name = var.getName();
			variables.put(name, this.variableService.fetchVariableValue(name, resolver));
		}
		for (ReleaseVariable var : release.getVariables()) {
			String name = var.getName();
			variables.put(name, this.variableService.fetchVariableValue(name, resolver));
		}
		Collection<VariableDto> dtos = new ArrayList<>();
		for (Map.Entry<String, String> var : variables.entrySet()) {
			VariableDto dto = new VariableDto();
			dto.setName(var.getKey());
			dto.setValue(var.getValue());
			dtos.add(dto);
		}
		return dtos;
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param dtos
	 * @param updateOnly
	 *            if true, don't remove or create variables
	 * @return
	 * @throws ObjectNotExistException
	 * @throws SnapshotReleaseException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_SRM")
	@Transactional(rollbackFor = Exception.class)
	public void updateVariables(String environmentName, String releaseName, Collection<VariableDto> dtos, boolean updateOnly) throws ObjectNotExistException, SnapshotReleaseException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		if (!release.isSnapshot())
			throw new SnapshotReleaseException();
		updateReleaseVariables(release, dtos, updateOnly);
		releaseErrors(release); // updates release.hasErrors
		updateAllReleaseArtifactsHasOverlays(release);
		this.environmentRepository.saveRelease(release);
		Audit.log("Updated variables of release {}", release.toString());
	}

	private void scanMissingVariables(Release release, Map<String, Collection<VariableReference>> missingVariables) throws PackageRescanException {
		for (EnvironmentServer environmentServer : release.getEnvironment().getServers()) {
			Server server = environmentServer.getServer();
			if (server.isDisabled())
				continue;
			VariableResolver resolver = new RepoVariableResolver(release, server);
			MissingVariableException exception = new MissingVariableException();
			try {
				this.variableService.fetchVariableValue(VariableResolver.DEPLOYMENT_DIR, resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "deployment directory");
			}
			try {
				this.variableService.instantiateVariables(release.getPreSetupAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "deployment pre-setup action");
			}
			try {
				this.variableService.instantiateVariables(release.getPostSetupAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "deployment post-setup action");
			}
			try {
				this.variableService.instantiateVariables(release.getPreCleanupAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "deployment pre-cleanup action");
			}
			try {
				this.variableService.instantiateVariables(release.getPostCleanupAction(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "deployment post-cleanup action");
			}
			for (ReleaseArtifact dp : release.getServerArtifacts(server.getName())) {
				if (dp.getArtifact().isDisabled())
					continue;
				Version version = null;
				try {
					version = dp.getActiveVersion();
					if (version.isDisabled())
						continue;
					this.layerService.checkReleaseFilesVariables(dp);
				} catch (NoActiveVersionException e1) {} catch (CompatibilityException e2) {} catch (ArtifactNotInReleaseException e) {} catch (MissingVariableException ke) {
					exception.addMissingVariable(ke, null);
				}
				// Must also check version/executable script parameters
				if (version != null)
					this.artifactRepository.scanMissingVersionVariables(version, release, server, missingVariables);
			}
			try {
				this.variableService.instantiateVariables(release.getFileOwner(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "owner");
			}
			try {
				this.variableService.instantiateVariables(release.getFileGroup(), resolver);
			} catch (MissingVariableException ke) {
				exception.addMissingVariable(ke, "group");
			}
			this.variableService.addMissingVariables(exception, missingVariables);
		}
	}

	/**
	 * Check if there are errors such as undefined variables, missing default versions, etc.
	 * 
	 * @param release
	 */
	private Collection<String> checkReleaseStatus(Release release) {
		Collection<String> descriptorErrors = new java.util.ArrayList<String>();
		// Check active version errors
		for (EnvironmentServer environmentServer : release.getEnvironment().getServers()) {
			Server server = environmentServer.getServer();
			if (!server.isDisabled()) {
				for (ReleaseArtifact dp : release.getServerArtifacts(server.getName())) {
					if (!dp.getArtifact().isDisabled()) {
						try {
							if (dp.getActiveVersion().isDisabled())
								continue;
							// TODO this.serverRepository.scanMissingServerVariables(v, release, server, missingVariables);
							// this.artifactRepository.scanMissingVersionVariables(version, release, server, missingVariables);

						} catch (CompatibilityException | NoActiveVersionException e) {
							addDescriptorErrors(descriptorErrors, e.getMessage());
						}
						try {
							this.layerService.checkReleaseFilesVariables(dp);
						} catch (MissingVariableException e) {
							addDescriptorErrors(descriptorErrors, "Missing variable(s)");
						} catch (CompatibilityException | NoActiveVersionException | ArtifactNotInReleaseException | PackageRescanException e) {
							addDescriptorErrors(descriptorErrors, e.getMessage());
						}
					}
				}
			}
		}
		// If no errors, proceed to check variables
		if (descriptorErrors.isEmpty()) {
			try {
				Map<String, Collection<VariableReference>> missingVariables = new HashMap<>();
				scanMissingVariables(release, missingVariables);
				if (!missingVariables.isEmpty())
					addDescriptorErrors(descriptorErrors, "Missing variable(s)");
			} catch (PackageRescanException e) {
				addDescriptorErrors(descriptorErrors, e.getMessage());
			}
		}
		release.setHasErrors(!descriptorErrors.isEmpty());
		return descriptorErrors.isEmpty() ? null : descriptorErrors;
	}

	/**
	 * 
	 * @param descriptorErrors
	 * @param e
	 */
	private void addDescriptorErrors(Collection<String> descriptorErrors, String msg) {
		if (!descriptorErrors.contains(msg))
			descriptorErrors.add(msg);
	}

	private void updateReleaseVariables(Release release, final Collection<VariableDto> dtos, boolean updateOnly) throws SnapshotReleaseException, ObjectNotExistException {
		if (!updateOnly) {
			/*
			 * Remove obsolete variables
			 */
			CollectionUtils.filter(release.getVariables(),
					new Predicate() {

						public boolean evaluate(Object var) {
							for (VariableDto dto : dtos) {
								if (variableEquals((ReleaseVariable) var, dto))
									return true;
							}
							return false;
						}
					});
		}
		/*
		 * Add & update
		 */
		for (final VariableDto dto : dtos) {
			ReleaseVariable var = (ReleaseVariable) CollectionUtils.find(release.getVariables(),
					new Predicate() {

						public boolean evaluate(Object o) {
							return variableEquals((ReleaseVariable) o, dto);
						}
					});
			if (var == null && !updateOnly) {
				var = new ReleaseVariable();
				var.setRelease(release);
				var.setName(dto.getName());
				var.setValue(StringUtils.EMPTY);
				if (dto.getServer() != null) {
					Server server = this.serverRepository.getCheckedServer(dto.getServer());
					var.setServerId(server.getId());
				}
				if (dto.getArtifact() != null) {
					Artifact artifact = this.artifactRepository.getCheckedArtifact(dto.getArtifact());
					var.setArtifactId(artifact.getId());
				}
				release.getVariables().add(var);
			}
			if (var != null) {
				if (dto.getDescription() != null)
					var.setDescription(dto.getDescription());
				if (dto.getValue() != null)
					var.setValue(dto.getValue());
			}
		}
		release.resetVariables();
		Collections.sort(release.getVariables());
	}

	private boolean variableEquals(ReleaseVariable var, VariableDto dto) {
		if (!var.getName().equals(dto.getName()))
			return false;
		try {
			if (var.getArtifactId() == null) {
				if (StringUtils.isNotEmpty(dto.getArtifact()))
					return false;
			} else {
				if (StringUtils.isEmpty(dto.getArtifact()))
					return false;
				Artifact artifact = this.artifactRepository.getCheckedArtifact(var.getArtifactId());
				if (!dto.getArtifact().equals(artifact.getName()))
					return false;
			}
			if (var.getServerId() == null) {
				if (StringUtils.isNotEmpty(dto.getServer()))
					return false;
			} else {
				if (StringUtils.isEmpty(dto.getServer()))
					return false;
				Server server = this.serverRepository.getCheckedServer(var.getServerId());
				if (!dto.getServer().equals(server.getName()))
					return false;
			}
		} catch (ObjectNotExistException e) {
			return false;
		}
		return true;
	}

	private void updateAllReleaseArtifactsHasOverlays(Release release) {
		for (ReleaseArtifact r : release.getRawReleaseArtifacts()) {
			if (!r.overlaysInitialized()) {
				updateReleaseArtifactHasOverlays(r);
			}
		}
	}

	private void updateReleaseArtifactHasOverlays(ReleaseArtifact releaseArtifact) {
		boolean hasOverlays = false;
		for (RepositoryFile file : releaseArtifact.getFiles()) {
			if (file.getLayerType().ordinal() >= LayerType.ARTIFACT_OVERLAY.ordinal()) {
				hasOverlays = true;
				break;
			}
		}
		releaseArtifact.setHasOverlays(hasOverlays);
	}

}