/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.kwatee.agiledeployment.common.Constants;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.DBAdminErrorException;
import net.kwatee.agiledeployment.common.exception.NoKwateeDBException;
import net.kwatee.agiledeployment.common.exception.NoKwateeSchemaException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.SchemaOutOfDateException;
import net.kwatee.agiledeployment.common.exception.UnauthorizedException;
import net.kwatee.agiledeployment.common.exception.UnavailableException;
import net.kwatee.agiledeployment.core.Audit;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.repository.UserRepository;
import net.kwatee.agiledeployment.core.service.DBAdminService;
import net.kwatee.agiledeployment.core.service.FileStoreService;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.repository.dto.ApplicationParameterDto;
import net.kwatee.agiledeployment.repository.dto.DBInfoDto;
import net.kwatee.agiledeployment.repository.dto.UserDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.entity.ApplicationParameter;
import net.kwatee.agiledeployment.repository.entity.Authority;
import net.kwatee.agiledeployment.repository.entity.SystemProperty;
import net.kwatee.agiledeployment.repository.entity.User;
import net.kwatee.agiledeployment.webapp.importexport.ExportService;
import net.kwatee.agiledeployment.webapp.mapper.AdminMapper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminService {

	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private DBAdminService dbadminService;
	@Autowired
	private FileStoreService repositoryService;
	@Autowired
	private ExportService exportService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private VariableService variableService;
	@Autowired
	private AdminMapper adminMapper;

	/**
	 * 
	 * @return
	 */
	@Secured({"ROLE_USER", "ROLE_EXTERNAL"})
	@Transactional(readOnly = true)
	public Collection<UserDto> getUsers() {
		Collection<User> users = this.userRepository.getUsers(true);
		return this.adminMapper.toShortUserDtos(users);
	}

	/**
	 * 
	 * @param userName
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured({"ROLE_USER", "ROLE_EXTERNAL"})
	@Transactional(readOnly = true)
	public UserDto getUser(String userName) throws ObjectNotExistException {
		User user = this.userRepository.getCheckedUser(userName);
		return this.adminMapper.toUserDto(user);
	}

	/**
	 * 
	 * @param userName
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_ADMIN")
	@Transactional
	public void deleteUser(String userName) throws CannotDeleteObjectInUseException {
		User user = this.userRepository.getUncheckedUser(userName);
		if (user != null) {
			this.userRepository.deleteUser(user);
			Audit.log("Deleted user {}", userName);
		}
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_ADMIN")
	@Transactional
	public void createUser(UserDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException {
		User user = this.userRepository.getUncheckedUser(dto.getName());
		if (user != null) {
			throw new ObjectAlreadyExistsException("User " + dto.getName());
		}
		/*
		 * Create new user
		 */
		user = new User();
		user.setLogin(dto.getName());
		user.setAndHashPassword(StringUtils.EMPTY);
		Authority auth = new Authority();
		auth.setAuthority(Authority.ROLE_USER); // default authority
		user.getAuthorities().add(auth);
		updateUser(user, dto);
		Audit.log("Created user {}", dto.getName());
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_ADMIN")
	@Transactional
	public void updateUser(UserDto dto) throws ObjectNotExistException {
		User user = this.userRepository.getCheckedUser(dto.getName());
		updateUser(user, dto);
		Audit.log("Updated user {}", dto.getName());
	}

	private void updateUser(User user, UserDto dto) {
		Boolean disabled = dto.isDisabled();
		if (disabled != null)
			user.setDisabled(disabled);
		if (dto.getDescription() != null)
			user.setDescription(dto.getDescription());
		if (dto.getEmail() != null)
			user.setEmail(dto.getEmail());
		if (dto.getPassword() != null)
			user.setAndHashPassword(dto.getPassword());
		Collection<Authority> authorities = new ArrayList<>();
		if (dto.isOperator() != null)
			authorities.add(new Authority(Authority.ROLE_OPERATOR));
		if (dto.isSrm() != null)
			authorities.add(new Authority(Authority.ROLE_SRM));
		if (dto.isAdmin() != null)
			authorities.add(new Authority(Authority.ROLE_ADMIN));
		if (authorities.size() > 0)
			updateAuthorities(user, authorities);
		this.userRepository.saveUser(user);
	}

	private void updateAuthorities(User user, Collection<Authority> authorities) {
		Authority authority = getAuthority(user.getAuthorities(), Authority.ROLE_OPERATOR);
		Authority newAuthority = getAuthority(authorities, Authority.ROLE_OPERATOR);
		if (newAuthority != null) {
			if (authority == null) {
				user.getAuthorities().add(newAuthority);
			}
		} else if (authority != null) {
			user.getAuthorities().remove(authority);
		}
		authority = getAuthority(user.getAuthorities(), Authority.ROLE_SRM);
		newAuthority = getAuthority(authorities, Authority.ROLE_SRM);
		if (newAuthority != null) {
			if (authority == null) {
				user.getAuthorities().add(newAuthority);
			}
		} else if (authority != null) {
			user.getAuthorities().remove(authority);
		}
		authority = getAuthority(user.getAuthorities(), Authority.ROLE_ADMIN);
		newAuthority = getAuthority(authorities, Authority.ROLE_ADMIN);
		if (newAuthority != null) {
			if (authority == null) {
				user.getAuthorities().add(newAuthority);
			}
		} else if (authority != null) {
			user.getAuthorities().remove(authority);
		}
	}

	private Authority getAuthority(Collection<Authority> authorities, final String authorityName) {
		return (Authority) CollectionUtils.find(authorities,
				new Predicate() {

					public boolean evaluate(Object a) {
						return authorityName.equals(((Authority) a).getAuthority());
					}
				});
	}

	/**
	 * 
	 * @return
	 */
	@Secured({"ROLE_USER", "ROLE_EXTERNAL"})
	@Transactional(readOnly = true)
	public Collection<VariableDto> getVariables() {
		Collection<SystemProperty> variables = this.adminRepository.getSystemProperties();
		return this.adminMapper.toGlobalVariableDtos(variables);
	}

	/**
	 * 
	 * @param dtos
	 */
	@Secured("ROLE_ADMIN")
	@Transactional
	public void setVariables(Collection<VariableDto> dtos) {
		Collection<SystemProperty> properties = this.adminRepository.getSystemProperties();
		for (VariableDto property : dtos) {
			String propertyName = property.getName();
			for (SystemProperty p : properties) {
				if (p.getName().equals(propertyName)) {
					p.setValue(property.getValue());
				}
			}
		}
		this.adminRepository.updateSystemProperties(properties);
		updateGlobalVariables();
		Audit.log("Updated global variables");
	}

	private void updateGlobalVariables() {
		Collection<SystemProperty> globalProperties = this.adminRepository.getSystemProperties();
		Map<String, String> variables = new HashMap<>(globalProperties.size());
		for (SystemProperty prop : globalProperties) {
			variables.put(prop.getName(), prop.getValue());
		}
		this.variableService.setGlobalProperties(variables); // pre-fetch
	}

	/**
	 * 
	 * @return
	 */
	@Secured({"ROLE_USER", "ROLE_EXTERNAL"})
	@Transactional(readOnly = true)
	public ApplicationParameterDto getParameters() {
		ApplicationParameter appParams = this.adminRepository.getApplicationParameters();
		return this.adminMapper.toParametersDto(appParams);
	}

	/**
	 * 
	 * @param dto
	 */
	@Secured("ROLE_ADMIN")
	@Transactional
	public void updateParameters(ApplicationParameterDto dto) {
		ApplicationParameter parameters = this.adminRepository.getApplicationParameters();
		if (dto.getTitle() != null) {
			parameters.setTitle(dto.getTitle());
		}
		if (dto.getExcludedExtensions() != null) {
			String ext = StringUtils.join(dto.getExcludedExtensions(), ",").toLowerCase();
			parameters.setExcludedExtensions(ext);
		}
		this.adminRepository.updateApplicationParameters(parameters);
		Audit.log("Updated application parameters");
	}

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	@Secured({"ROLE_USER", "ROLE_EXTERNAL"})
	@Transactional(readOnly = true)
	public File doExport() throws IOException {
		File exportFile = this.exportService.exportBackup();
		Audit.log("Exported DB");
		return exportFile;
	}

	/**
	 * 
	 * @return
	 */
	@Transactional(readOnly = true)
	public DBInfoDto getDBInfo() {
		DBInfoDto dto = new DBInfoDto();
		String requiredVersion = Constants.CURRENT_SCHEMA_VERSION;
		String version = dbadminService.checkSchema();
		if (version != null)
			dto.setSchemaVersion(version.isEmpty() ? requiredVersion : version);
		dto.setRequiredSchemaVersion(requiredVersion);
		dto.setJdbcUserName(this.dbadminService.getJdbcUserName());
		dto.setJdbcUrl(this.dbadminService.getJdbcUrl());
		return dto;
	}

	/**
	 * 
	 * @throws UnavailableException
	 */
	@Transactional(readOnly = true)
	public void checkSchema() throws UnavailableException {
		if (!dbadminService.checkIfDBExists())
			throw new NoKwateeDBException();
		String upgradeFrom = this.dbadminService.checkSchema();
		if (upgradeFrom == null) {
			throw new NoKwateeSchemaException();
		} else if (!upgradeFrom.isEmpty()) {
			throw new SchemaOutOfDateException(upgradeFrom);
		}
		updateGlobalVariables();
	}

	/**
	 * 
	 * @param password
	 * @throws DBAdminErrorException
	 */
	@Transactional
	public void createDBSchema(String password) throws DBAdminErrorException {
		if (!dbadminService.createSchema(password))
			throw new UnauthorizedException();
		Audit.log("Created Schema");
	}

	/**
	 * 
	 * @param password
	 * @throws DBAdminErrorException
	 */
	@Transactional
	public void upgradeDBSchema(String password) throws DBAdminErrorException {
		if (!dbadminService.upgradeSchema(password))
			throw new UnauthorizedException();
		this.repositoryService.upgradeRepository();
		Audit.log("Upgraded schema");
	}
}
