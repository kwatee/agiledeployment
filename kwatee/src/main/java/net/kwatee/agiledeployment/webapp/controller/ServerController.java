/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.repository.dto.ServerDto;
import net.kwatee.agiledeployment.webapp.service.ServerService;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
@RequestMapping(value = "/api")
/**
 * 
 * @author mac
 *
 */
public class ServerController {

	@Autowired
	private ServerService serverService;

	private final static Logger LOG = LoggerFactory.getLogger(ServerController.class);

	/**
	 * Retrieves the list of servers in the repository
	 * 
	 * @return <code>application/json</code> array of server objects
	 * 
	 * @rest GetServers
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/servers.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{name: 'demoserver', description: 'Demo server'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/servers.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<ServerDto> serverGetServers() {
		LOG.debug("getServers");
		return serverService.getServers();
	}

	/**
	 * Retrieves server properties.
	 * 
	 * @param serverName
	 *            the name of the server
	 * @return <code>application/json</code> server object
	 * @throws ObjectNotExistException
	 * @throws ConduitException
	 * 
	 * @rest GetServer
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/servers/demoserver.json
	 * @exampleResponse 200
	 * @exampleResponseBody {name: 'demoserver', description: 'Demo server', platform: 2, conduitType: 'ssh', ipAddress:
	 *                      'demo.kwatee.net', port: 22, credentials: {login: 'kwtest'}, properties: {},
	 *                      poolConcurrency: 0}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/servers/{serverName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ServerDto getServer(
			@PathVariable(value = "serverName") String serverName) throws ObjectNotExistException, ConduitException {
		LOG.debug("getServer {}", serverName);
		return serverService.getServer(serverName, false);
	}

	/**
	 * Updates the properties of a server.
	 * 
	 * @param serverName
	 *            the name of the server
	 * @param serverProperties
	 *            the JSON server properties to update
	 * @throws CompatibilityException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateServer
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/servers/demoserver
	 * @exampleRquestBody {description: 'new description'}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/servers/{serverName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateServer(
			@PathVariable(value = "serverName") String serverName,
			@RequestBody ServerDto dto) throws ObjectNotExistException, CompatibilityException {
		LOG.debug("updateServer {}", serverName);
		dto.setName(serverName);
		DtoValidator.validate(dto);
		serverService.updateServer(dto);
	}

	/**
	 * Creates or duplicates a server and optionally set additional properties.
	 * 
	 * @param serverName
	 *            the name of the server to create
	 * @param duplicateFrom
	 *            the optional name of a server to duplicate
	 * @param serverProperties
	 *            the optional JSON server properties
	 * @throws ObjectNotExistException
	 * @throws ObjectAlreadyExistsException
	 * @throws CompatibilityException
	 * 
	 * @rest CreateOrDuplicateServer
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/servers/newserver
	 * @exampleRquestBody {description: 'initial description'}
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/servers/{serverName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void createServer(HttpServletResponse res,
			@PathVariable(value = "serverName") String serverName,
			@RequestParam(value = "duplicateFrom", required = false) String duplicateFrom,
			@RequestBody(required = false) ServerDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException, CompatibilityException {
		LOG.debug("createServer {} from ", serverName, StringUtils.defaultString(duplicateFrom, "<none>"));
		if (dto == null)
			dto = new ServerDto();
		dto.setName(serverName);
		DtoValidator.validate(dto);
		if (duplicateFrom == null)
			serverService.createServer(dto);
		else
			serverService.duplicateServer(dto, duplicateFrom);
	}

	/**
	 * Deletes a server. The operation is successful even if the server does not exist.
	 * 
	 * @param serverName
	 *            the name of the server
	 * @throws CannotDeleteObjectInUseException
	 * 
	 * @rest DeleteServer
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/servers/newserver
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/servers/{serverName:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteServer(@PathVariable(value = "serverName") String serverName) throws CannotDeleteObjectInUseException {
		LOG.debug("deleteServer {}", serverName);
		serverService.deleteServer(serverName);
	}

	/**
	 * Tests a connection to the server and returns server capabilities.
	 * 
	 * @param serverName
	 *            the name of the server
	 * @param properties
	 *            JSON server properties (typically for credentials)
	 * @return <code>application/json</code> array of server capabilities
	 * @throws ConduitAuthenticationFailedException
	 * @throws CompatibilityException
	 * @throws ObjectNotExistException
	 * @throws ConduitException
	 * 
	 * @rest ServerDiagnostics
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/servers/demoserver
	 * @exampleResponse 200
	 * @exampleResponseBody ['Test platform command availability (support=0 is good)', 'cd support=0', 'chmod
	 *                      support=0', 'mkdir support=0', 'rm support=0', 'cat support=0']
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/servers/{serverName}/testConnection", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Object diagnostics(HttpServletResponse res,
			@PathVariable(value = "serverName") String serverName,
			@RequestBody(required = false) ServerDto dto) throws ConduitAuthenticationFailedException, ObjectNotExistException, CompatibilityException, ConduitException {
		LOG.debug("diagnostics {}", serverName);
		if (dto == null)
			dto = new ServerDto();
		dto.setName(serverName);
		return serverService.testConnection(dto);
	}
}
