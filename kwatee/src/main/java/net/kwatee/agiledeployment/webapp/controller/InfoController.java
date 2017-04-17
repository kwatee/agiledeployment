/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.repository.dto.DBInfoDto;
import net.kwatee.agiledeployment.repository.dto.IdNameDto;
import net.kwatee.agiledeployment.webapp.service.AdminService;
import net.kwatee.agiledeployment.webapp.service.InfoService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
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
public class InfoController {

	@Autowired
	private InfoService infoService;

	@Autowired
	private AdminService adminService;

	private final static Logger LOG = LoggerFactory.getLogger(InfoController.class);

	/**
	 * Retrieves kwatee information.
	 * 
	 * @return <code>application/json</code> kwatee context object
	 * @throws IOException
	 * 
	 * @rest GetInfoContext
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/info/context.json
	 * @exampleResponse 200
	 * @exampleResponseBody {organization: '&lt;your organisation name here&gt;', version: 'VersionShortDto 2.3.0',
	 *                      user:
	 *                      'admin', copyright: 'Copyright 2010-2014'}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/info/context.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Object infoContext(HttpServletResponse res, Principal principal) throws IOException {
		if (principal == null) {
			// not authenticated, check if db exists before sending 401
			DBInfoDto dbInfo = this.adminService.getDBInfo();
			if (dbInfo.getSchemaVersion() == null)
				res.sendError(HttpServletResponse.SC_GONE); // no db
			else if (!dbInfo.getSchemaVersion().equals(dbInfo.getRequiredSchemaVersion()))
				res.sendError(426); // wrong db version
			else
				res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return dbInfo;
		}
		LOG.debug("infoContext");
		return this.infoService.getContextInfo(principal);
	}

	/**
	 * Retrieves the available platforms.
	 * 
	 * @return <code>application/json</code> array of platform objects
	 * 
	 * @rest GetInfoPlatforms
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/info/platforms.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 1, name: 'linux_x86'}, {id: 7, name: 'linux_64'}, {id: 3, name: 'solaris_x86'}, {id:
	 *                      4, name: 'solaris_sparc'}, {id: 2, name: 'macosx_x86'}, {id: 8, name: 'aix'}, {id: 5, name:
	 *                      'win32'}, {id: 9, name: 'win64'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/info/platforms.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<IdNameDto> infoPlatforms(HttpServletResponse res) {
		LOG.debug("infoPlatforms");
		return this.infoService.getPlatforms();
	}

	/**
	 * Retrieves the available conduit types.
	 * 
	 * @return <code>application/json</code> array of conduit type objects
	 * 
	 * @rest GetInfoConduitTypes
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/info/conduitTypes.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 'telnetftp', name: 'Telnet / FTP'}, {id: 'ssh', name: 'Secure Shell / scp'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/info/conduitTypes.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<IdNameDto> infoConduitTypes(HttpServletResponse res) {
		LOG.debug("infoConduitTypes");
		return this.infoService.getConduitTypes();
	}

	/**
	 * Retrieves the available server pool types.
	 * 
	 * @return <code>application/json</code> array of server pool type objects in case of success
	 * 
	 * @rest GetInfoServerPoolTypes
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/info/serverPoolTypes.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 'manual', name: 'Enumerated pool'},{id: 'ec2', name: 'Amazon EC2 pool'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/info/serverPoolTypes.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<IdNameDto> infoServerPoolTypes(HttpServletResponse res) {
		LOG.debug("infoServerPoolTypes");
		return this.infoService.getServerPoolTypes();
	}
}
