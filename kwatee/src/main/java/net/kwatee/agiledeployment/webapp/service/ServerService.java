/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.validation.ConstraintViolation;
import javax.validation.Path;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.DtoValidationException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.utils.CryptoUtils;
import net.kwatee.agiledeployment.conduit.AccessLevel;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.ConduitFactory;
import net.kwatee.agiledeployment.conduit.DefaultServerInstance;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstanceException;
import net.kwatee.agiledeployment.conduit.ServerInstanceFactory;
import net.kwatee.agiledeployment.core.Audit;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.core.conduit.ConduitService;
import net.kwatee.agiledeployment.core.conduit.InstanceService;
import net.kwatee.agiledeployment.core.deploy.DeployService;
import net.kwatee.agiledeployment.core.deploy.PlatformService;
import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.core.repository.ServerRepository;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.core.variable.impl.RepoVariableResolver;
import net.kwatee.agiledeployment.repository.dto.CredentialsDto;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.ServerCredentials;
import net.kwatee.agiledeployment.repository.entity.ServerProperty;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.webapp.mapper.ServerMapper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import ch.ethz.ssh2.crypto.PEMDecoder;
import ch.ethz.ssh2.crypto.PEMStructure;

@Component
public class ServerService {

	@Autowired
	private ServerMapper serverMapper;
	@Autowired
	private ArtifactRepository artifactRepository;
	@Autowired
	private ServerRepository serverRepository;
	@Autowired
	private PlatformService platformService;
	@Autowired
	private DeployService deployService;
	@Autowired
	private VariableService variableService;
	@Autowired
	private ConduitService conduitService;
	@Autowired
	private InstanceService instanceService;

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<ServerDto> getServers() {
		Collection<Server> servers = serverRepository.getServers();
		return serverMapper.toShortServerDtos(servers);
	}

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public ServerDto getServer(String serverName, boolean withSecret) throws ObjectNotExistException, ConduitException {
		Server server = serverRepository.getCheckedServer(serverName);
		return serverMapper.toServerDto(server, withSecret);
	}

	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void deleteServer(String serverName) throws CannotDeleteObjectInUseException {
		Server server = serverRepository.getUncheckedServer(serverName);
		if (server != null) {
			serverRepository.deleteServer(server);
			Audit.log("Deleted server {}", serverName);
		}
	}

	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void createServer(ServerDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException, CompatibilityException {
		Server server = serverRepository.getUncheckedServer(dto.getName());
		if (server != null)
			throw new ObjectAlreadyExistsException("Server " + dto.getName());
		server = new Server();
		server.setName(dto.getName());
		server.setPlatform(1);
		server.setIPAddress("localhost");
		server.setPort(22);
		server.setConduitType(conduitService.getDefaultConduitType());
		ServerCredentials credentials = new ServerCredentials();
		credentials.setAccessLevel(AccessLevel.SRM);
		credentials.setServer(server);
		server.setCredentials(credentials);
		String poolType = dto.getPoolType();
		if (poolType != null) {
			Map<String, String> types = instanceService.getFactories();
			if (!types.containsKey(poolType))
				throw new InternalErrorException("Unknown server type " + poolType);
			server.setPoolType(poolType);
		}
		int platform = dto.getPlatform() == null ? 1 : dto.getPlatform();
		if (platformService.getName(platform) == null)
			throw new ObjectNotExistException(ObjectNotExistException.PLATFORM, platform);
		server.setPlatform(platform);
		updateServer(server, dto, true);
		Audit.log("Created server {}", dto.getName());
	}

	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void duplicateServer(ServerDto dto, String fromServerName) throws ObjectNotExistException, CompatibilityException {
		Server server = serverRepository.getCheckedServer(fromServerName);
		Server duplicateServer = server.duplicate(dto.getName());
		duplicateServer = serverRepository.getCheckedServer(server.getName());
		updateServer(duplicateServer, dto, true);
		Audit.log("Created server {}", dto.getName());
	}

	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void updateServer(ServerDto dto) throws ObjectNotExistException, CompatibilityException {
		Server server = serverRepository.getCheckedServer(dto.getName());
		updateServer(server, dto, true);
		Audit.log("Updated server {}", dto.getName());
	}

	private void updateServer(Server server, ServerDto dto, boolean saveChanges) throws CompatibilityException {
		if (dto.isDisabled() != null)
			server.setDisabled(dto.isDisabled());
		if (dto.getDescription() != null)
			server.setDescription(dto.getDescription());
		if (dto.getPlatform() != null && dto.getPlatform() != 0)
			updatePlatform(server, dto.getPlatform());
		if (dto.getConduitType() != null)
			server.setConduitType(dto.getConduitType());
		if (dto.getIpAddress() != null)
			server.setIPAddress(dto.getIpAddress());
		if (dto.getPort() != null)
			server.setPort(dto.getPort());
		if (dto.getPoolConcurrency() != null)
			server.setPoolConcurrency(dto.getPoolConcurrency());
		if (dto.getCredentials() != null)
			updateServerCredentials(server, dto.getCredentials());
		if (dto.getProperties() != null || dto.getPoolProperties() != null) {
			server.getProperties().clear();
			if (dto.getProperties() != null) {
				for (Map.Entry<String, String> prop : dto.getProperties().entrySet()) {
					ServerProperty p = new ServerProperty();
					p.setName(prop.getKey());
					p.setValue(prop.getValue());
					server.getProperties().add(p);
				}
			}
			if (dto.getPoolProperties() != null) {
				for (Map.Entry<String, String> prop : dto.getPoolProperties().entrySet()) {
					ServerProperty p = new ServerProperty();
					p.setName(server.getPoolType() + "." + prop.getKey());
					p.setValue(prop.getValue());
					server.getProperties().add(p);
				}
			}
		}
		if (dto.getUseSudo() != null)
			server.setUseSudo(dto.getUseSudo());
		if (saveChanges)
			serverRepository.saveServer(server);

	}

	private void updatePlatform(Server server, int platformId) throws CompatibilityException {
		if (platformId != server.getPlatform()) {
			Collection<Version> serverVersions = artifactRepository.getVersionsInServer(server.getId());
			for (Version version : serverVersions) {
				if (!version.getPlatforms().isEmpty() && !isInPlatforms(version.getPlatforms(), platformId))
					throw new CompatibilityException("Cannot change server platform due to compatibility conflicts with artifact  " + version + " in some release(s)");
			}
		}
		server.setPlatform(platformId);
	}

	private boolean isInPlatforms(Collection<Integer> platforms, int platformId) {
		return platforms == null || platforms.contains(platformId);
	}

	private void updateServerCredentials(Server server, CredentialsDto dto) {
		ServerCredentials creds = server.getCredentials();
		if (creds == null) {
			creds = new ServerCredentials();
			server.setCredentials(creds);
			creds.setServer(server);
		}
		if (dto.getAccessLevel() != null)
			creds.setAccessLevel(AccessLevel.valueOf(dto.getAccessLevel()));
		else if (creds.getAccessLevel() == null)
			creds.setAccessLevel(AccessLevel.SRM);
		if (creds.getAccessLevel().ordinal() > AccessLevel.OFFLINE.ordinal()) {
			if (dto.getLogin() != null)
				creds.setLogin(dto.getLogin());
			if (dto.getPromptPassword() != null) {
				creds.setPasswordPrompted(dto.getPromptPassword());
				if (creds.isPasswordPrompted())
					creds.setPassword(null);
				else if (StringUtils.isNotEmpty(dto.getPassword()))
					creds.setPassword(CryptoUtils.encrypt(dto.getPassword()));
			} else {
				if (StringUtils.isNotEmpty(dto.getPassword()))
					creds.setPassword(CryptoUtils.encrypt(dto.getPassword()));
			}
			String pem = dto.getPem();
			if (pem != null) {
				if (pem.isEmpty()) {
					creds.setPem(null);
				} else {
					if (DeployCredentials.isPrivateKeyPath(pem)) {
						// pem is actually the path to a pem file bracketed by []
						creds.setPem(pem);
					} else {
						try {
							PEMStructure ps = PEMDecoder.parsePEM(pem.toCharArray());
							if (!PEMDecoder.isPEMEncrypted(ps))
								PEMDecoder.decode(dto.getPem().toCharArray(), dto.getPassword());
						} catch (IOException e) {
							Path path = PathImpl.createPathFromString("pem");
							ConstraintViolation<Object> violation = ConstraintViolationImpl.forBeanValidation(
									"invalid key",
									"invalid key",
									Object.class,
									dto,
									null,
									pem,
									path,
									null,
									(ElementType) null);
							HashSet<ConstraintViolation<Object>> violations = new HashSet<ConstraintViolation<Object>>(1);
							violations.add(violation);
							throw new DtoValidationException(violations);
						}
						creds.setPem(CryptoUtils.encrypt(pem));
					}
				}
			}
		}
	}

	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Object testConnection(ServerDto dto) throws ConduitAuthenticationFailedException, ObjectNotExistException, CompatibilityException, ConduitException {
		Server server = serverRepository.getCheckedServer(dto.getName());
		updateServer(server, dto, false);
		String status;
		try {
			if (server.getCredentials() == null)
				throw new ConduitAuthenticationFailedException("No credentials");
			String pem = CryptoUtils.decrypt(server.getCredentials().getPem());
			if (DeployCredentials.isPrivateKeyPath(pem)) {
				try {
					pem = FileUtils.readFileToString(new File(pem));
				} catch (IOException e) {
					throw new InternalErrorException(e);
				}
			}
			DeployCredentials credentials = new DeployCredentials(
					server.getCredentials().getAccessLevel(),
					server.getCredentials().getLogin(),
					CryptoUtils.decrypt(server.getCredentials().getPassword()),
					pem,
					false);
			String rootDir = variableService.fetchVariableValue(Constants.REMOTE_ROOT_DIR, new RepoVariableResolver(null, server));
			ConduitFactory conduitFactory = conduitService.getFactory(server.getConduitType());
			DefaultServerInstance instance = new DefaultServerInstance(server.getName(), server.getIPAddress(), server.getPort());
			instance.setParent(server);
			try {
				Conduit conduit = conduitFactory.getNewInstance(instance, rootDir);
				status = conduit.remoteDiagnostics(credentials);
			} catch (ConduitAuthenticationFailedException e) {
				throw e;
			} catch (Exception e) {
				throw new ConduitException(e.getMessage());
			} finally {
				conduitFactory.evictServerConnections(instance);
			}
		} catch (ConduitAuthenticationFailedException e) {
			throw e;
		} catch (MissingVariableException e1) {
			throw new ConduitException(server.getCredentials().getAccessLevel().toString() + " access failed: " + e1.getMessage());
		}

		return CollectionUtils.arrayToList(status.split("\n"));
	}

	/**
	 * @param poolType
	 * @param poolProperties
	 * @return server name-ip Map
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Map<String, String> fetchInstances(String poolType, Map<String, String> poolProperties) throws ConduitException {
		ServerInstanceFactory factory = instanceService.getFactory(poolType);
		try {
			Map<String, String> instances = factory.getUninstantiatedServers(poolProperties);
			return instances;
		} catch (ServerInstanceException e) {
			throw new InternalErrorException(e);
		}
	}

	/**
	 * @param properties
	 * @return EC2 regions
	 * @throws ConduitException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<String> fetchEC2Regions(Map<String, String> properties) throws ConduitException {
		ServerInstanceFactory factory = instanceService.getFactory("ec2");
		try {
			Collection<String> regions = new ArrayList<>(factory.getElementList("regions", properties).values());
			return regions;
		} catch (ServerInstanceException e) {
			throw new InternalErrorException(e);
		}
	}

}