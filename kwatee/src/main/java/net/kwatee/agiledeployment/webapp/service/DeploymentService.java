/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.Constants.LayerType;
import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.OperationFailedException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ConduitFactory;
import net.kwatee.agiledeployment.conduit.DefaultServerInstance;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.core.deploy.DeployService;
import net.kwatee.agiledeployment.core.deploy.Deployment;
import net.kwatee.agiledeployment.core.deploy.DeploymentServer;
import net.kwatee.agiledeployment.core.deploy.task.DeployStatus;
import net.kwatee.agiledeployment.core.deploy.task.DeploymentStatus;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.core.repository.EnvironmentRepository;
import net.kwatee.agiledeployment.core.repository.ServerRepository;
import net.kwatee.agiledeployment.core.repository.UserRepository;
import net.kwatee.agiledeployment.core.service.LayerService;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.RepoVariableResolver;
import net.kwatee.agiledeployment.repository.dto.ArtifactOperationStatusDto;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.DeploymentLoginDto;
import net.kwatee.agiledeployment.repository.dto.EnvironmentDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.OperationProgressDto;
import net.kwatee.agiledeployment.repository.dto.RefDto;
import net.kwatee.agiledeployment.repository.dto.ReleaseDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.repository.dto.ServerOperationStatusDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;
import net.kwatee.agiledeployment.repository.entity.Authority;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;
import net.kwatee.agiledeployment.webapp.controller.ServletHelper;
import net.kwatee.agiledeployment.webapp.importexport.ExportService;
import net.kwatee.agiledeployment.webapp.mapper.AdminMapper;
import net.kwatee.agiledeployment.webapp.mapper.ArtifactMapper;
import net.kwatee.agiledeployment.webapp.mapper.EnvironmentMapper;
import net.kwatee.agiledeployment.webapp.mapper.PackageMapper;
import net.kwatee.agiledeployment.webapp.mapper.ServerMapper;
import net.kwatee.agiledeployment.webapp.security.UserDetailsImpl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeploymentService {

	static private final HashMap<Integer, String> STATUS_NAMES = new HashMap<>(9);
	{
		STATUS_NAMES.put(DeployStatus.STATUS_OK, "ok");
		STATUS_NAMES.put(DeployStatus.STATUS_FAILED, "failed");
		STATUS_NAMES.put(DeployStatus.STATUS_DISABLED, "no status");
		STATUS_NAMES.put(DeployStatus.STATUS_UNDETERMINED, "undetermined");
		STATUS_NAMES.put(DeployStatus.STATUS_CANCELED, "canceled");
		STATUS_NAMES.put(DeployStatus.STATUS_RUNNING, "running");
		STATUS_NAMES.put(DeployStatus.STATUS_STOPPED, "stopped");
		STATUS_NAMES.put(DeployStatus.STATUS_INPROGRESS, "in progress");
		STATUS_NAMES.put(DeployStatus.STATUS_PENDING, "pending");
	}
	static private final HashMap<Integer, String> OPERATION_NAMES = new HashMap<>(6);
	{
		OPERATION_NAMES.put(DeploymentStatus.OP_DEPLOY, "deploy");
		OPERATION_NAMES.put(DeploymentStatus.OP_UNDEPLOY, "undeploy");
		OPERATION_NAMES.put(DeploymentStatus.OP_CHECK, "check integrity");
		OPERATION_NAMES.put(DeploymentStatus.OP_START, "start");
		OPERATION_NAMES.put(DeploymentStatus.OP_STOP, "stop");
		OPERATION_NAMES.put(DeploymentStatus.OP_STATUS, "runtime status");
	}
	static private final HashMap<Integer, String> COMPLETION_NAMES = new HashMap<>(4);
	{
		COMPLETION_NAMES.put(DeploymentStatus.COMPLETION_INPROGRESS, "in progress");
		COMPLETION_NAMES.put(DeploymentStatus.COMPLETION_CANCELING, "canceling");
		COMPLETION_NAMES.put(DeploymentStatus.COMPLETION_OK, "done");
		COMPLETION_NAMES.put(DeploymentStatus.COMPLETION_INTERRUPTED, "interrupted");
	}

	@Autowired
	private EnvironmentMapper environmentMapper;
	@Autowired
	private ServerMapper serverMapper;
	@Autowired
	private ArtifactMapper artifactMapper;
	@Autowired
	private AdminMapper adminMapper;
	@Autowired
	private DeployService deployService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private ServerRepository serverRepository;
	@Autowired
	private EnvironmentRepository environmentRepository;
	@Autowired
	private ArtifactRepository artifactRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private ConduitService conduitService;
	@Autowired
	private LayerService layerService;
	@Autowired
	private ExportService exportService;

	/**
	 * 
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<EnvironmentDto> getDeployments() throws ObjectNotExistException {
		Collection<Environment> environments = getEnvironments(false, true);
		return this.environmentMapper.toDeploymentDtos(environments);
	}

	private Collection<Environment> getEnvironments(final boolean includeDisabled, final boolean manageableOnly) {
		ArrayList<Environment> environments = new ArrayList<>(this.environmentRepository.getEnvironments());
		if (!includeDisabled || manageableOnly) {
			environments = new ArrayList<>(environments);
			CollectionUtils.filter(environments,
					new Predicate() {

						public boolean evaluate(Object e) {
							Environment environment = (Environment) e;
							if (environment.isDisabled() && (!includeDisabled || manageableOnly)) {
								return false;
							}
							if (manageableOnly) {
								if (environment.getServers().isEmpty() || environment.getArtifacts().isEmpty()) {
									return false;
								}
								return CollectionUtils.find(environment.getReleases(),
										new Predicate() {

											public boolean evaluate(Object r) {
												return DeploymentService.this.environmentMapper.releaseIsManageable((Release) r);
											}
										}) != null;
							}
							return true;
						}
					});
		}
		return environments;
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
	public ReleaseDto getDeployment(String environmentName, String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
		return this.environmentMapper.toDeploymentDto(release);
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param res
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public void downloadInstaller(String environmentName, String releaseName, HttpServletResponse res) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException {
		File bundleFile = null;
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			bundleFile = this.exportService.exportDeployment(release);
			try (InputStream in = new FileInputStream(bundleFile)) {
				ServletHelper.sendFile(res, in, (int) bundleFile.length(), release.toString() + org.h2.engine.Constants.SUFFIX_MV_FILE + ".gz", "application/octet-stream");
			}
		} catch (IOException e) {
			throw new InternalErrorException(e);
		} finally {
			FileUtils.deleteQuietly(bundleFile);
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param targets
	 *            the server/artifacts on which to perform the operation
	 * @return
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional
	public RefDto manageDeploy(String environmentName, String releaseName, Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, ConduitAuthenticationPromptPasswordException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			Deployment deployment = toDeployment(release, targets, false);
			return new RefDto(this.deployService.deploy(deployment, UserDetailsImpl.getAuthenticatedUser().getUsername()));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (e.getEnvironmentName() == null)
				throw new ConduitAuthenticationPromptPasswordException(environmentName, e.getServerName(), e.getLogin(), e.getAccessLevel(), e.getMessage());
			throw e;
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param targets
	 *            the server/artifacts on which to perform the operation
	 * @param skipIntegrityCheck
	 *            true if operation should be performed without agent's prior integrity check
	 * @param forcedUndeploy
	 * @return
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional
	public RefDto manageUndeploy(String environmentName, String releaseName, Collection<ServerArtifactsDto> targets, boolean skipIntegrityCheck, boolean forcedUndeploy) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, ConduitAuthenticationPromptPasswordException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			Deployment deployment = toDeployment(release, targets, skipIntegrityCheck);
			return new RefDto(this.deployService.undeploy(deployment, forcedUndeploy, UserDetailsImpl.getAuthenticatedUser().getUsername()));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (e.getEnvironmentName() == null)
				throw new ConduitAuthenticationPromptPasswordException(environmentName, e.getServerName(), e.getLogin(), e.getAccessLevel(), e.getMessage());
			throw e;
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param targets
	 *            the server/artifacts on which to perform the operation
	 * @param skipIntegrityCheck
	 *            true if operation should be performed without agent's prior integrity check of deployment descriptor
	 * @return
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public RefDto manageCheckIntegrity(String environmentName, String releaseName, Collection<ServerArtifactsDto> targets, boolean skipIntegrityCheck) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, ConduitAuthenticationPromptPasswordException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			Deployment deployment = toDeployment(release, targets, skipIntegrityCheck);
			return new RefDto(this.deployService.checkIntegrity(deployment, UserDetailsImpl.getAuthenticatedUser().getUsername()));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (e.getEnvironmentName() == null)
				throw new ConduitAuthenticationPromptPasswordException(environmentName, e.getServerName(), e.getLogin(), e.getAccessLevel(), e.getMessage());
			throw e;
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param targets
	 *            the server/artifacts on which to perform the operation
	 * @param skipIntegrityCheck
	 *            true if operation should be performed without agent's prior integrity check
	 * @param actionParams
	 *            optional parameters sent to the start command
	 * @return
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional
	public RefDto manageStart(String environmentName, String releaseName, Collection<ServerArtifactsDto> targets, String actionParams, boolean skipIntegrityCheck) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, ConduitAuthenticationPromptPasswordException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			Deployment deployment = toDeployment(release, targets, skipIntegrityCheck);
			deployment.setActionParams(actionParams);
			return new RefDto(this.deployService.start(deployment, UserDetailsImpl.getAuthenticatedUser().getUsername()));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (e.getEnvironmentName() == null)
				throw new ConduitAuthenticationPromptPasswordException(
						environmentName,
						e.getServerName(),
						e.getLogin(),
						e.getAccessLevel(),
						e.getMessage());
			throw e;
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param targets
	 *            the server/artifacts on which to perform the operation
	 * @param skipIntegrityCheck
	 *            true if operation should be performed without agent's prior integrity check
	 * @param actionParams
	 *            optional parameters sent to the stop command
	 * @return
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional
	public RefDto manageStop(String environmentName, String releaseName, Collection<ServerArtifactsDto> targets, String actionParams, boolean skipIntegrityCheck) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, ConduitAuthenticationPromptPasswordException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			Deployment deployment = toDeployment(release, targets, skipIntegrityCheck);
			deployment.setActionParams(actionParams);
			return new RefDto(this.deployService.stop(deployment, UserDetailsImpl.getAuthenticatedUser().getUsername()));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (e.getEnvironmentName() == null)
				throw new ConduitAuthenticationPromptPasswordException(
						environmentName,
						e.getServerName(),
						e.getLogin(),
						e.getAccessLevel(),
						e.getMessage());
			throw e;
		}
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @param targets
	 *            the server/artifacts on which to perform the operation
	 * @param skipIntegrityCheck
	 *            true if operation should be performed without agent's prior integrity check
	 * @param actionParams
	 *            optional parameters sent to the status command
	 * @return
	 * @throws ObjectNotExistException
	 * @throws OperationFailedException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectAlreadyExistsException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationPromptPasswordException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public RefDto manageStatus(String environmentName, String releaseName, Collection<ServerArtifactsDto> targets, String actionParams, boolean skipIntegrityCheck) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, ConduitAuthenticationPromptPasswordException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		try {
			Release release = this.environmentRepository.getCheckedRelease(environmentName, releaseName);
			if (release.hasErrors())
				throw new OperationFailedException("Errors in release configuration");
			Deployment deployment = toDeployment(release, targets, skipIntegrityCheck);
			deployment.setActionParams(actionParams);
			return new RefDto(this.deployService.status(deployment, UserDetailsImpl.getAuthenticatedUser().getUsername()));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			if (e.getEnvironmentName() == null)
				throw new ConduitAuthenticationPromptPasswordException(environmentName, e.getServerName(), e.getLogin(), e.getAccessLevel(), e.getMessage());
			throw e;
		}
	}

	/**
	 * 
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public RefDto getOngoing() throws ObjectNotExistException {
		UserDetails user = UserDetailsImpl.getAuthenticatedUser();
		if (user.getUsername().startsWith("$"))
			user = this.userRepository.getCheckedUser(user.getUsername());
		String deploymentRef = this.deployService.getOngoingRemoteOperation(user.getUsername());
		if (deploymentRef != null)
			return new RefDto(deploymentRef);
		return new RefDto();

	}

	/**
	 * 
	 * @param deploymentRef
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public OperationProgressDto getProgress(String deploymentRef) throws ObjectNotExistException {
		DeploymentStatus status = this.deployService.remoteOperationProgress(deploymentRef);
		OperationProgressDto dto = new OperationProgressDto();
		dto.setOperation(OPERATION_NAMES.get(status.getOperation()));
		dto.setStatus(COMPLETION_NAMES.get(status.getCompletionType()));

		Collection<ServerOperationStatusDto> servers = new ArrayList<>();
		Collection<ArtifactOperationStatusDto> artifacts = null;
		String previousServer = null;
		for (DeployStatus s : status.getStatusList()) {
			if (!s.getReference1().equals(previousServer)) {
				ServerOperationStatusDto server = new ServerOperationStatusDto();
				server.setServer(s.getReference1());
				server.setStatus(STATUS_NAMES.get(s.getStatus()));
				artifacts = new ArrayList<>();
				server.setArtifacts(artifacts);
				servers.add(server);
				previousServer = s.getReference1();
			}
			if (s.getReference2() != null) {
				ArtifactOperationStatusDto artifact = new ArtifactOperationStatusDto();
				artifact.setArtifact(s.getReference2());
				artifact.setStatus(STATUS_NAMES.get(s.getStatus()));
				artifacts.add(artifact);
			}
		}
		dto.setServers(servers);
		return dto;
	}

	/**
	 * @param deploymentRef
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public int getProgressStatus(String deploymentRef) {
		DeploymentStatus status = null;
		try {
			status = this.deployService.remoteOperationProgress(deploymentRef);
		} catch (ObjectNotExistException e) {}
		if (status == null)
			return HttpServletResponse.SC_GONE;
		if (status.getCompletionType() == DeploymentStatus.COMPLETION_OK) {
			for (DeployStatus s : status.getStatusList()) {
				if (s.getStatus() == DeployStatus.STATUS_FAILED || s.getStatus() == DeployStatus.STATUS_CANCELED)
					return HttpServletResponse.SC_BAD_REQUEST;
			}
			return HttpServletResponse.SC_OK;
		}
		if (status.getCompletionType() == DeploymentStatus.COMPLETION_INTERRUPTED)
			return HttpServletResponse.SC_BAD_REQUEST;
		return HttpServletResponse.SC_NO_CONTENT;
	}

	/**
	 * @param deploymentRef
	 * @param serverName
	 * @param artifactName
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Object getProgressMessages(String deploymentRef, String serverName, String artifactName) throws ObjectNotExistException {
		Map<String, String> result = new HashMap<>();
		DeploymentStatus status = this.deployService.remoteOperationProgress(deploymentRef);
		for (DeployStatus s : status.getStatusList()) {
			if (serverName.equals(s.getReference1())) {
				if (artifactName == null) {
					if (s.getReference2() == null) {
						result.put("messages", s.getMessages());
						break;
					}
				} else if (artifactName.equals(s.getReference2())) {
					result.put("messages", s.getMessages());
					break;
				}
			}
		}
		return result;
	}

	/**
	 * 
	 * @param dto
	 * @param sameForAllServers
	 * @throws MissingVariableException
	 * @throws ObjectNotExistException
	 * @throws ConduitAuthenticationFailedException
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public void sendCredentials(DeploymentLoginDto dto, boolean sameForAllServers) throws MissingVariableException, ObjectNotExistException, ConduitAuthenticationFailedException, ConduitException {
		String login = dto.getLogin();
		String password = dto.getPassword();
		Server server = this.serverRepository.getCheckedServer(dto.getServer());
		UserDetails user = UserDetailsImpl.getAuthenticatedUser();
		if (user == null)
			throw new InternalErrorException("no current user");
		AccessLevel accessLevel = AccessLevel.VIEWER;
		for (GrantedAuthority a : user.getAuthorities()) {
			Authority auth = (Authority) a;
			if (auth.getAuthority().equals(Authority.ROLE_OPERATOR) && AccessLevel.OPERATOR.ordinal() > accessLevel.ordinal())
				accessLevel = AccessLevel.OPERATOR;
			if (auth.getAuthority().equals(Authority.ROLE_SRM) && AccessLevel.SRM.ordinal() > accessLevel.ordinal())
				accessLevel = AccessLevel.SRM;
		}
		if (server.getCredentials().getLogin() != null && !server.getCredentials().getLogin().isEmpty())
			login = server.getCredentials().getLogin();
		if (password == null)
			password = CryptoUtils.decrypt(server.getCredentials().getPassword());
		String pem = CryptoUtils.decrypt(server.getCredentials().getPem());
		if (DeployCredentials.isPrivateKeyPath(pem)) {
			// It's actually the path to a certificate file, open it and read it
			try {
				pem = FileUtils.readFileToString(new File(pem));
			} catch (IOException e) {
				throw new InternalErrorException(e);
			}
		}
		DeployCredentials creds = new DeployCredentials(accessLevel, login, password, pem, true);
		String rootDir = this.variableService.fetchVariableValue(Constants.REMOTE_ROOT_DIR, new RepoVariableResolver(null, server));
		ConduitFactory conduitFactory = this.conduitService.getFactory(server.getConduitType());
		DefaultServerInstance instance = new DefaultServerInstance(server.getName(), server.getIPAddress(), server.getPort());
		instance.setParent(server);
		try {
			Conduit conduit = conduitFactory.getNewInstance(instance, rootDir);
			conduit.remoteDiagnostics(creds);
		} catch (ConduitAuthenticationFailedException e) {
			throw e;
		} catch (Exception e) {
			throw new ConduitException(e.getMessage());
		} finally {
			conduitFactory.evictServerConnections(instance);
		}
		this.deployService.updateInteractiveCredentials(dto.getEnvironment(), user.getUsername(), sameForAllServers ? null : dto.getServer(), creds);
	}

	/**
	 * 
	 * @param deploymentRef
	 * @param dontClear
	 * @return
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public RefDto cancel(String deploymentRef, boolean dontClear) {
		boolean canceled = this.deployService.cancelRemoteOperation(deploymentRef, dontClear);
		RefDto dto = new RefDto(deploymentRef);
		dto.setCanceled(canceled);
		return dto;
	}

	private Deployment toDeployment(Release release, Collection<ServerArtifactsDto> targets, boolean skipIntegrityCheck) throws CompatibilityException, NoActiveVersionException, ObjectNotExistException, PackageRescanException, ArtifactNotInReleaseException, ConduitException {
		ReleaseDto releaseDto = this.environmentMapper.toReleaseDto(release);
		pruneRelease(releaseDto, targets);
		setupReleaseArtifacts(releaseDto);

		Deployment deployment = new Deployment(releaseDto, release.getEnvironment().isDeploymentSequential(), skipIntegrityCheck);
		setupDeploymentServers(deployment, releaseDto);
		setupCustomFiles(releaseDto.getServers(), release);

		Set<VariableDto> variables = new HashSet<>();
		Collection<VariableDto> releaseVariables = this.environmentMapper.toReleaseVariableDtos(release.getVariables(), false);
		if (releaseVariables != null) {
			pruneReleaseVariables(releaseVariables, targets);
			variables.addAll(releaseVariables);
		}
		Set<VersionVariable> defaultValues = setupDeploymentVersions(deployment, release);
		variables.addAll(this.environmentMapper.toVersionVariableDtos(defaultValues));
		variables.addAll(this.adminMapper.toGlobalVariableDtos(this.adminRepository.getSystemProperties()));
		deployment.setVariables(variables);
		return deployment;
	}

	private void pruneRelease(ReleaseDto releaseDto, Collection<ServerArtifactsDto> targets) {
		// Remove disabled servers
		CollectionUtils.filter(releaseDto.getServers(), new Predicate() {

			@Override
			public boolean evaluate(Object object) {
				ServerArtifactsDto serverArtifact = (ServerArtifactsDto) object;
				if (CollectionUtils.isNotEmpty(serverArtifact.getArtifacts()) && BooleanUtils.isNotTrue(serverArtifact.getDisabled())) {
					// Remove disabled artifacts
					CollectionUtils.filter(serverArtifact.getArtifacts(), new Predicate() {

						@Override
						public boolean evaluate(Object artifactVersion) {
							return BooleanUtils.isNotTrue(((ArtifactVersionDto) artifactVersion).getDisabled());
						}

					});
					return true;
				}
				return false;
			}

		});
		// Remove servers that are not in 'targets'
		if (CollectionUtils.isNotEmpty(releaseDto.getServers()) && CollectionUtils.isNotEmpty(targets)) {
			Iterator<ServerArtifactsDto> it = releaseDto.getServers().iterator();
			while (it.hasNext()) {
				if (!targetsContainServerArtifact(targets, it.next()))
					it.remove();
			}
		}
	}

	private boolean targetsContainServerArtifact(Collection<ServerArtifactsDto> targets, final ServerArtifactsDto serverArtifact) {
		return CollectionUtils.exists(targets, new Predicate() {

			@Override
			public boolean evaluate(Object object) {
				ServerArtifactsDto target = (ServerArtifactsDto) object;
				if (target.getServer().equals(serverArtifact.getServer())) {
					pruneServerArtifacts(serverArtifact.getArtifacts(), target.getArtifacts());
					if (CollectionUtils.isEmpty(serverArtifact.getArtifacts()))
						return false;
					return true;
				}
				return false;
			}

		});
	}

	private void pruneServerArtifacts(Collection<ArtifactVersionDto> serverArtifacts, Collection<ArtifactVersionDto> targetArtifacts) {
		if (CollectionUtils.isNotEmpty(targetArtifacts)) {
			Iterator<ArtifactVersionDto> it = serverArtifacts.iterator();
			while (it.hasNext()) {
				ArtifactVersionDto artifactVersion = it.next();
				if (ArtifactVersionDto.findByName(targetArtifacts, artifactVersion.getArtifact()) == null)
					it.remove();
			}
		}
	}

	/*
	 * Resolve inherited versions
	 */
	private void setupReleaseArtifacts(ReleaseDto releaseDto) {
		Collection<ArtifactVersionDto> defaultArtifacts = releaseDto.getDefaultArtifacts();
		for (ServerArtifactsDto serverArtifact : releaseDto.getServers()) {
			for (ArtifactVersionDto artifactVersion : serverArtifact.getArtifacts()) {
				if (artifactVersion.getVersion() == null) {
					ArtifactVersionDto defaultArtifactVersion = ArtifactVersionDto.findByName(defaultArtifacts, artifactVersion.getArtifact());
					artifactVersion.setVersion(defaultArtifactVersion.getVersion());
				}
			}
		}
		releaseDto.setDefaultArtifacts(null);
	}

	private void pruneReleaseVariables(Collection<VariableDto> variables, Collection<ServerArtifactsDto> targets) {
		if (CollectionUtils.isNotEmpty(variables) && CollectionUtils.isNotEmpty(targets)) {
			Iterator<VariableDto> it = variables.iterator();
			while (it.hasNext()) {
				VariableDto variable = it.next();
				if (!targetsContainVariable(targets, variable))
					it.remove();
			}
		}
	}

	private boolean targetsContainVariable(Collection<ServerArtifactsDto> targets, final VariableDto variable) {
		if (variable.getServer() == null && variable.getArtifact() == null)
			return true;
		return CollectionUtils.exists(targets, new Predicate() {

			@Override
			public boolean evaluate(Object object) {
				ServerArtifactsDto server = (ServerArtifactsDto) object;
				if (variable.getServer() != null && !variable.getServer().equals(server.getServer()))
					return false;
				if (variable.getArtifact() == null || CollectionUtils.isEmpty(server.getArtifacts()))
					return true;
				return CollectionUtils.exists(server.getArtifacts(), new Predicate() {

					@Override
					public boolean evaluate(Object object) {
						ArtifactVersionDto artifact = (ArtifactVersionDto) object;
						return artifact.getArtifact().equals(variable.getArtifact());
					}
				});
			}
		});
	}

	private void setupCustomFiles(Collection<ServerArtifactsDto> servers, Release release) throws ArtifactNotInReleaseException, CompatibilityException, PackageRescanException {
		for (final ServerArtifactsDto serverArtifacts : servers) {
			for (final ArtifactVersionDto artifactVersion : serverArtifacts.getArtifacts()) {
				ReleaseArtifact rp = (ReleaseArtifact) CollectionUtils.find(release.getRawReleaseArtifacts(), new Predicate() {

					@Override
					public boolean evaluate(Object object) {
						ReleaseArtifact r = (ReleaseArtifact) object;
						if (r.getServer() != null && r.getServer().getName().equals(serverArtifacts.getServer()))
							return artifactVersion.getArtifact().equals(r.getArtifact().getName());
						return false;
					}

				});
				if (rp != null) {
					Collection<RepositoryFile> files = null;
					try {
						files = this.layerService.getMergedReleaseFiles(rp, false, null);
						Iterator<RepositoryFile> it = files.iterator();
						// keep only release overlays
						while (it.hasNext()) {
							RepositoryFile f = it.next();
							if (f.getLayerType().ordinal() < LayerType.COMMON_OVERLAY.ordinal())
								it.remove();
						}
					} catch (NoActiveVersionException e) {}
					Collection<FileDto> fileDtos = PackageMapper.toFileDtos(files, null, true);
					artifactVersion.setCustomFiles(fileDtos);
				}
			}
		}

	}

	private void setupDeploymentServers(Deployment deployment, ReleaseDto releaseDto) throws ConduitException, ObjectNotExistException {
		Collection<DeploymentServer> servers = new ArrayList<>();
		for (ServerArtifactsDto serverArtifacts : releaseDto.getServers()) {
			ServerDto server = this.serverMapper.toServerDto(this.serverRepository.getCheckedServer(serverArtifacts.getServer()), true);
			servers.add(new DeploymentServer(server, deployment));
		}
		deployment.setServers(servers);
	}

	private Set<VersionVariable> setupDeploymentVersions(Deployment deployment, Release release) throws PackageRescanException {
		Collection<VersionDto> versions = new ArrayList<>();
		Set<VersionVariable> defaultValues = new HashSet<VersionVariable>();
		for (Version v : this.environmentRepository.getReleaseVersions(release, false)) {
			VersionDto version = this.artifactMapper.toVersionDto(v);
			if (version.getPackageInfo() != null) {
				Collection<RepositoryFile> files = this.layerService.getMergedVersionFiles(v, false, null);
				version.getPackageInfo().setFiles(this.artifactMapper.toFileDtos(files, null, true));
			}
			versions.add(version);
			defaultValues.addAll(v.getVariablesDefaultValues());
		}
		deployment.setVersions(versions);
		return defaultValues;
	}

}