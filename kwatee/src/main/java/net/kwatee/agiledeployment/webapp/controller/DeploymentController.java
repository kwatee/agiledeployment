/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.OperationFailedException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.core.conduit.ConduitException;
import net.kwatee.agiledeployment.repository.dto.DeploymentLoginDto;
import net.kwatee.agiledeployment.repository.dto.EnvironmentDto;
import net.kwatee.agiledeployment.repository.dto.OperationProgressDto;
import net.kwatee.agiledeployment.repository.dto.RefDto;
import net.kwatee.agiledeployment.repository.dto.ReleaseDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;
import net.kwatee.agiledeployment.webapp.service.DeploymentService;

import org.apache.commons.lang3.BooleanUtils;
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
public class DeploymentController {

	@Autowired
	private DeploymentService deploymentService;

	private final static Logger LOG = LoggerFactory.getLogger(DeploymentController.class);

	/**
	 * Retrieves the list of deployments.
	 * 
	 * @return <code>application/json</code> array of deployments objectes
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetDeployments
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{name: 'intro', description: 'introductory tutorial', releases: [{name: 'snapshot',
	 *                      editable: true}, {name: 'acme-1.0', description: 'initial deployment'}]}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<EnvironmentDto> getDeployments() throws ObjectNotExistException {
		LOG.debug("getDeployments");
		return this.deploymentService.getDeployments();
	}

	/**
	 * Retrieves the deployment properties.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @return <code>application/json</code> deployment object
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 * 
	 * @rest GetDeployment
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/snapshot.json
	 * @exampleResponse 200
	 * @exampleResponseBody {name: 'snapshot', serverArtifacts: [{server: 'demoserver', artifacts: [{name:
	 *                      'demowebsite', version: '1.0'}]}], editable: true}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments/{environmentName}/{releaseName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ReleaseDto getDeployment(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		LOG.debug("getDeployment {}-{}", environmentName, releaseName);
		return this.deploymentService.getDeployment(environmentName, releaseName);
	}

	/**
	 * Downloads an installer.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @return <code>application/x-gtar</code> installer
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * 
	 * @rest DownloadInstaller
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/snapshot/installer_cli.tar.gz
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments/{environmentName}/{releaseName}/installer_cli.tar.gz")
	@ResponseStatus(HttpStatus.OK)
	public void deploymentDownloadInstaller(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException {
		LOG.debug("downloadInstaller {}-{}", environmentName, releaseName);
		this.deploymentService.downloadInstaller(environmentName, releaseName, res);
	}

	/**
	 * Initiates a deploy operation.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 * 
	 * @rest ManageDeploy
	 * @restStatus <code>202</code> accepted
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/deploy
	 * @exampleResponse 202
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/{environmentName}/{releaseName}/deploy", consumes = "application/json", produces = "application/json")
	@ResponseBody
	public Object manageDeploy(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestBody(required = false) Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		LOG.debug("manageDeploy {}-{}", environmentName, releaseName);
		try {
			res.setStatus(HttpServletResponse.SC_ACCEPTED);
			return this.deploymentService.manageDeploy(environmentName, releaseName, targets);
		} catch (ConduitAuthenticationPromptPasswordException e) {
			res.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
			return passwordRequired(e);
		}
	}

	/**
	 * Initiates an undeploy operation.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param skipIntegrityCheck
	 *            if true, operation is performed without prior integrity check by the agent
	 * @param forcedUndeploy
	 *            when <code>true</code> forces undeploy even if there are errors (<code>false</code> if omitted)
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 * 
	 * @rest ManageUndeploy
	 * @restStatus <code>202</code> accepted
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/undeploy
	 * @exampleResponse 202
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/{environmentName}/{releaseName}/undeploy", produces = "application/json")
	@ResponseBody
	public Object manageUndeploy(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "skipIntegrityCheck", required = false) Boolean skipIntegrityCheck,
			@RequestParam(value = "forceUndeploy", required = false) Boolean forceUndeploy,
			@RequestBody(required = false) Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		LOG.debug("manageUndeploy {}-{}", environmentName, releaseName);
		try {
			res.setStatus(HttpServletResponse.SC_ACCEPTED);
			return this.deploymentService.manageUndeploy(environmentName, releaseName, targets, BooleanUtils.isTrue(skipIntegrityCheck), forceUndeploy == null ? false : forceUndeploy);
		} catch (ConduitAuthenticationPromptPasswordException e) {
			res.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
			return passwordRequired(e);
		}
	}

	/**
	 * Initiates a check integrity operation.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param skipIntegrityCheck
	 *            if true, operation is performed without prior integrity check by the agent on deployment descriptor
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 * 
	 * @rest ManageCheckIntegrity
	 * @restStatus <code>202</code> accepted
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/check
	 * @exampleResponse 202
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/{environmentName}/{releaseName}/check", produces = "application/json")
	@ResponseBody
	public Object manageCheckIntegrity(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "skipIntegrityCheck", required = false) Boolean skipIntegrityCheck,
			@RequestBody(required = false) Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		LOG.debug("manageCheckIntegrity {}-{}", environmentName, releaseName);
		try {
			res.setStatus(HttpServletResponse.SC_ACCEPTED);
			return this.deploymentService.manageCheckIntegrity(environmentName, releaseName, targets, BooleanUtils.isTrue(skipIntegrityCheck));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			res.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
			return passwordRequired(e);
		}
	}

	/**
	 * Initiates a start executables operation.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param skipIntegrityCheck
	 *            if true, operation is performed without prior integrity check by the agent
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 * 
	 * @rest ManageStart
	 * @restStatus <code>202</code> accepted
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/start
	 * @exampleResponse 202
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/{environmentName}/{releaseName}/start", produces = "application/json")
	@ResponseBody
	public Object manageStart(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "skipIntegrityCheck", required = false) Boolean skipIntegrityCheck,
			@RequestParam(value = "actionParams", required = false) String actionParams,
			@RequestBody(required = false) Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		LOG.debug("manageStart {}-{}", environmentName, releaseName);
		try {
			res.setStatus(HttpServletResponse.SC_ACCEPTED);
			return this.deploymentService.manageStart(environmentName, releaseName, targets, actionParams, BooleanUtils.isTrue(skipIntegrityCheck));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			res.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
			return passwordRequired(e);
		}
	}

	/**
	 * Initiates a stop executables operation.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param skipIntegrityCheck
	 *            if true, operation is performed without prior integrity check by the agent
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 * 
	 * @rest ManageStop
	 * @restStatus <code>202</code> accepted
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/stop
	 * @exampleResponse 202
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/{environmentName}/{releaseName}/stop", produces = "application/json")
	@ResponseBody
	public Object manageStop(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "skipIntegrityCheck", required = false) Boolean skipIntegrityCheck,
			@RequestParam(value = "actionParams", required = false) String actionParams,
			@RequestBody(required = false) Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		LOG.debug("manageStop {}-{}", environmentName, releaseName);
		try {
			res.setStatus(HttpServletResponse.SC_ACCEPTED);
			return this.deploymentService.manageStop(environmentName, releaseName, targets, actionParams, BooleanUtils.isTrue(skipIntegrityCheck));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			res.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
			return passwordRequired(e);
		}
	}

	/**
	 * Initiates an executables status operation.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param skipIntegrityCheck
	 *            if true, operation is performed without prior integrity check by the agent
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectAlreadyExistsException
	 * @throws ArtifactNotInReleaseException
	 * @throws OperationFailedException
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws PackageRescanException
	 * @throws ConduitException
	 * 
	 * @rest ManageStatus
	 * @restStatus <code>202</code> accepted
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/intro/status
	 * @exampleResponse 202
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/{environmentName}/{releaseName}/status", produces = "application/json")
	@ResponseBody
	public Object manageStatus(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "skipIntegrityCheck", required = false) Boolean skipIntegrityCheck,
			@RequestParam(value = "actionParams", required = false) String actionParams,
			@RequestBody(required = false) Collection<ServerArtifactsDto> targets) throws ObjectNotExistException, OperationFailedException, ArtifactNotInReleaseException, ObjectAlreadyExistsException, MissingVariableException, CompatibilityException, NoActiveVersionException, PackageRescanException, ConduitException {
		LOG.debug("manageStatus {}-{}", environmentName, releaseName);
		try {
			res.setStatus(HttpServletResponse.SC_ACCEPTED);
			return this.deploymentService.manageStatus(environmentName, releaseName, targets, actionParams, BooleanUtils.isTrue(skipIntegrityCheck));
		} catch (ConduitAuthenticationPromptPasswordException e) {
			res.setStatus(HttpServletResponse.SC_EXPECTATION_FAILED);
			return passwordRequired(e);
		}
	}

	/**
	 * Retrieves an ongoing deployment operation.
	 * 
	 * @return
	 * 
	 * @return <code>application/json</code> deployment operation reference
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetOngoingOperation
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/ongoing.json
	 * @exampleResponse 200
	 * @exampleResponseBody {ref: '1:1417387427690'}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments/ongoing.json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public RefDto getOngoing() throws ObjectNotExistException {
		LOG.debug("deploymentGetOngoing");
		return this.deploymentService.getOngoing();
	}

	/**
	 * Is an ongoing operation in progress?
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * 
	 * @rest hasOperationCompleted
	 * @restStatus <code>200</code> the operation completed successfully
	 * @restStatus <code>204</code> the operation has not completed yet
	 * @restStatus <code>400</code> the operation has completed but failed
	 * @restStatus <code>410</code> the operation is gone
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/progress/status
	 * @exampleResponse 204
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments/progress/status")
	public void checkProgressStatus(HttpServletResponse res, @RequestParam(value = "ref") String ref) {
		int status = this.deploymentService.getProgressStatus(ref);
		ServletHelper.sendData(res, StringUtils.EMPTY, status);
	}

	/**
	 * Retrieves the progress of a deployment operation.
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @return <code>application/json</code> deployment operation progress
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetOperationProgress
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/progress/status.json?ref=1:1417387427690
	 * @exampleResponse 200
	 * @exampleResponseBody {operation: 'deploy', status: 'done', servers: [{name: 'demoserver', status: 'ok',
	 *                      artifacts: [{name: 'demowebsite', status: 'ok'}]}]}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments/progress/status.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public OperationProgressDto getProgress(@RequestParam(value = "ref") String ref) throws ObjectNotExistException {
		return this.deploymentService.getProgress(ref);
	}

	/**
	 * Retrieves the details of a deployment operation for a given server and or artifact.
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @param serverName
	 *            the name of the server for which info is requested
	 * @param artifactName
	 *            the optional name of an artifact for which info is requested (if omitted, only server-wide info is
	 *            returned)
	 * @return <code>application/json</code> deployment operation details object
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetProgressMessages
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/deployments/progress/messages.json?ref=1:1417387427690&
	 *                 serverName
	 *                 =demoserver
	 * @exampleResponse 200
	 * @exampleResponseBody {messages: 'Establishing server connection...\nConnection established\nDeploy of artifact
	 *                      demowebsite[1.0] in intro-snapshot@demoserver\nDeploy complete'}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/deployments/progress/messages.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Object getProgressMessages(
			@RequestParam(value = "ref") String ref,
			@RequestParam(value = "serverName") String serverName,
			@RequestParam(value = "artifactName", required = false) String artifactName) throws ObjectNotExistException {
		LOG.debug("getProgressMessages {} {} {}", ref, serverName, StringUtils.defaultString(artifactName, "-"));
		return this.deploymentService.getProgressMessages(ref, serverName, artifactName);
	}

	/**
	 * Cancels an ongoing operation.
	 * 
	 * @param ref
	 *            the reference of an ongoing deployment operation
	 * @param dontClear
	 *            if <code>true</code> the status is kept active
	 * 
	 * @rest ManageCancel
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/deployments/progress/cancel?ref=1:1417387427690
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/progress/cancel", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public RefDto cancel(
			@RequestParam(value = "ref") String ref,
			@RequestParam(value = "dontClear", required = false) boolean dontClear) {
		LOG.debug("cancel {}", ref);
		return this.deploymentService.cancel(ref, dontClear);
	}

	/**
	 * Supply server credentials without storing them in kwatee.
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * @param serverName
	 *            the name of the server
	 * @param sameForAllServers
	 *            optional, if <code>true</code>, these same credentials will be applied to all servers that need it.
	 * @param credentials
	 *            the optional JSON credentials
	 * @throws ObjectNotExistException
	 * @throws MissingVariableException
	 * @throws ConduitAuthenticationFailedException
	 * @throws ConduitException
	 * 
	 * @rest SendCredentials
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/deployments/progress/credentials?environmentName=intro&
	 *                 deploymentName=snapshot
	 * @exampleRequestBody {login: 'kwtest', password: 'password'}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/deployments/progress/credentials", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void sendCredentials(
			@RequestParam(value = "sameForAll", required = false) boolean sameForAllServers,
			@RequestBody DeploymentLoginDto dto) throws MissingVariableException, ObjectNotExistException, ConduitAuthenticationFailedException, ConduitException {
		LOG.debug("sendCredentials");
		DtoValidator.validate(dto);
		this.deploymentService.sendCredentials(dto, sameForAllServers);
	}

	private DeploymentLoginDto passwordRequired(ConduitAuthenticationPromptPasswordException e) {
		DeploymentLoginDto dto = new DeploymentLoginDto();
		dto.setEnvironment(e.getEnvironmentName());
		dto.setServer(e.getServerName());
		dto.setLogin(e.getLogin());
		dto.setAccess(e.getAccessLevel());
		dto.setMessage(e.getMessage());
		return dto;
	}
}
