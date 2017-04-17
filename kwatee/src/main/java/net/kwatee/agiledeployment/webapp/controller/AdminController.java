/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.DBAdminErrorException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.utils.DateUtils;
import net.kwatee.agiledeployment.repository.dto.ApplicationParameterDto;
import net.kwatee.agiledeployment.repository.dto.DBInfoDto;
import net.kwatee.agiledeployment.repository.dto.UserDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.webapp.service.AdminService;

import org.apache.commons.io.FileUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(value = "/api")
/**
 * 
 * @author mac
 *
 */
public class AdminController {

	@Autowired
	private AdminService adminService;

	private final static Logger LOG = LoggerFactory.getLogger(AdminController.class);

	/**
	 * Retrieve schema version.
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/db/info.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DBInfoDto schemaVersion() {
		LOG.debug("schemaVersion");
		return this.adminService.getDBInfo();
	}

	/**
	 * Creates Schema.
	 * 
	 * @throws DBAdminErrorException
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/db/create", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void createSchema(@RequestBody Map<String, String> password) throws DBAdminErrorException {
		LOG.debug("createSchema");
		this.adminService.createDBSchema(password.get("password"));
	}

	/**
	 * Upgrade Schema
	 * 
	 * @throws DBAdminErrorException
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/db/upgrade", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void upgradeSchema(@RequestBody Map<String, String> password) throws DBAdminErrorException {
		LOG.debug("upgradeSchema");
		this.adminService.upgradeDBSchema(password.get("password"));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/admin/users.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<UserDto> getUsers() {
		LOG.debug("getUsers");
		return this.adminService.getUsers();

	}

	@RequestMapping(method = RequestMethod.GET, value = "/admin/users/{userName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserDto getUser(@PathVariable(value = "userName") String userName) throws ObjectNotExistException {
		LOG.debug("getUser {}", userName);
		return this.adminService.getUser(userName);
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/admin/users/{userName:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteUser(@PathVariable(value = "userName") String userName) throws CannotDeleteObjectInUseException {
		LOG.debug("deleteUser {}", userName);
		this.adminService.deleteUser(userName);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/admin/users/{userName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void createUser(
			@PathVariable(value = "userName") String userName,
			@RequestBody(required = false) UserDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException {
		LOG.debug("createUser {}", userName);
		if (dto == null)
			dto = new UserDto();
		dto.setName(userName);
		DtoValidator.validate(dto);
		this.adminService.createUser(dto);
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/admin/users/{userName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateUser(
			@PathVariable(value = "userName") String userName,
			@RequestBody(required = false) UserDto dto) throws ObjectNotExistException {
		LOG.debug("updateUser {}", userName);
		dto.setName(userName);
		DtoValidator.validate(dto);
		this.adminService.updateUser(dto);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/admin/variables.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<VariableDto> getVariables() {
		LOG.debug("getVariables");
		return this.adminService.getVariables();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/admin/variables", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void setVariables(@RequestBody Collection<VariableDto> dtos) {
		LOG.debug("setVariables");
		for (VariableDto dto : dtos)
			DtoValidator.validate(dto);
		this.adminService.setVariables(dtos);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/admin/parameters.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ApplicationParameterDto getParameters() {
		LOG.debug("getParameters");
		return this.adminService.getParameters();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/admin/parameters", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateParameters(@RequestBody ApplicationParameterDto dto) {
		LOG.debug("updateParameters");
		DtoValidator.validate(dto);
		this.adminService.updateParameters(dto);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/admin/export")
	@ResponseStatus(HttpStatus.OK)
	public void doExport(HttpServletResponse res) throws IOException {
		LOG.debug("doExport");
		File exportFile = null;
		InputStream in = null;
		try {
			exportFile = this.adminService.doExport();
			in = new FileInputStream(exportFile);
			String fileName = "kwatee_backup-" + DateUtils.currentGMT() + ".h2.db";
			ServletHelper.sendFile(res, in, (int) exportFile.length(), fileName, "application/octet-stream");
		} finally {
			if (in != null)
				IOUtils.closeQuietly(in);
			if (exportFile != null)
				FileUtils.deleteQuietly(exportFile);
		}
	}
}