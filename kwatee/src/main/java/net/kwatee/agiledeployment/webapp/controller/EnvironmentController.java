/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.MissingVariableException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.common.exception.ServerNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.SnapshotReleaseException;
import net.kwatee.agiledeployment.common.exception.TagReleaseException;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.EnvironmentDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.FilePropertiesDto;
import net.kwatee.agiledeployment.repository.dto.ReleaseDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.webapp.service.EnvironmentService;

import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping(value = "/api")
/**
 * 
 * @author mac
 *
 */
public class EnvironmentController {

	@Autowired
	private EnvironmentService environmentService;

	@Autowired
	private ObjectMapper mapper;

	private final static Logger LOG = LoggerFactory.getLogger(EnvironmentController.class);

	/**
	 * Retrieves the list of environments in the repository.
	 * 
	 * @return <code>application/json</code> array of environment objects
	 * 
	 * @rest GetEnvironments
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{name: 'intro', description: 'introductory tutorial'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<EnvironmentDto> getEnvironments() {
		LOG.debug("getEnvironments");
		return this.environmentService.getEnvironments();
	}

	/**
	 * Retrieves an environment's properties.
	 * 
	 * @param environmentName
	 *            the name of the environment
	 * @return <code>application/json</code> environment object
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetEnvironment
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro.json
	 * @exampleResponse 200
	 * @exampleResponseBody {name: 'intro', description: 'introductory tutorial', artifacts: ['demowebsite'], servers:
	 *                      ['demoserver'], sequentialDeployment: true, releases: [{name: 'snapshot', editable: true},
	 *                      {name: 'acme-1.0', description: 'initial deployment'}]}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public EnvironmentDto getEnvironment(@PathVariable(value = "environmentName") String environmentName) throws ObjectNotExistException {
		LOG.debug("getEnvironment {}", environmentName);
		EnvironmentDto o = this.environmentService.getEnvironment(environmentName);
		return o;
	}

	/**
	 * Updates the environment with new properties.
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * @param environmentProperties
	 *            the JSON environment properties to update
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateEnvironment
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro
	 * @exampleRequestBody {description: 'new description'}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/environments/{environmentName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateEnvironment(
			@PathVariable(value = "environmentName") String environmentName,
			@RequestBody EnvironmentDto dto) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("updateEnvironment {}", environmentName);
		dto.setName(environmentName);
		DtoValidator.validate(dto);
		this.environmentService.updateEnvironment(dto);
	}

	/**
	 * Creates or duplicates an environment and optionally set additional properties. <br/>
	 * Note that only the snapshot environment is included in a duplicate operation.
	 * 
	 * @param environmentName
	 *            the name of the environment to create
	 * @param duplicateFrom
	 *            the optional name of an environment to duplicate
	 * @param environmentProperties
	 *            the optional JSON environment properties
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @rest CreateOrDuplicateEnvironment
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/testEnvironment
	 * @exampleRequestBody {description: 'initial description'}
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/environments/{environmentName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void createEnvironment(
			@PathVariable(value = "environmentName") String environmentName,
			@RequestParam(value = "duplicateFrom", required = false) String duplicateFrom,
			@RequestBody(required = false) EnvironmentDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("createEnvironment {}", environmentName, StringUtils.defaultString(duplicateFrom, "<none>"));
		if (dto == null)
			dto = new EnvironmentDto();
		dto.setName(environmentName);
		DtoValidator.validate(dto);
		if (duplicateFrom == null)
			this.environmentService.createEnvironment(dto);
		else
			this.environmentService.duplicateEnvironment(dto, duplicateFrom);
	}

	/**
	 * Deletes an environment. The operation is successful even if the
	 * environment does not exist.
	 * 
	 * @param environmentName
	 *            the name of an environment
	 * 
	 * @rest DeleteEnvironment
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/testEnvironment
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/environments/{environmentName:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteEnvironment(@PathVariable(value = "environmentName") String environmentName) {
		LOG.debug("deleteEnvironment {}", environmentName);
		this.environmentService.deleteEnvironment(environmentName);
	}

	/**
	 * Retrieve an environment's release properties.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @return <code>application/json</code> release object
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 * 
	 * @rest GetRelease
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/snapshot.json
	 * @exampleResponse 200
	 * @exampleResponseBody {name: 'snapshot', editable: true, defaultArtifacts: [{name: 'demowebsite', version:
	 *                      '1.0'}], serverArtifacts: [{server: 'demoserver', artifacts: [{name: 'demowebsite'}]}]}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ReleaseDto getRelease(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		LOG.debug("getRelease {}-{}", environmentName, releaseName);
		return this.environmentService.getRelease(environmentName, releaseName);
	}

	/**
	 * 
	 * @param environmentName
	 * @param releaseName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 * @throws ArtifactNotInReleaseException
	 * @throws PackageRescanException
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/errors.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<String> getReleaseErrors(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		LOG.debug("getReleaseErrors {}-{}", environmentName, releaseName);
		return this.environmentService.getReleaseErrors(environmentName, releaseName);
	}

	/**
	 * Updates a release with new properties.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param releaseProperties
	 *            the release JSON properties to update
	 * @throws ArtifactNotInReleaseException
	 * @throws ServerNotInReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateRelease
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/snapshot
	 * @exampleRequestBody {description: 'new description'}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/environments/{environmentName}/{releaseName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateRelease(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestBody ReleaseDto dto) throws ObjectNotExistException, ServerNotInReleaseException, ArtifactNotInReleaseException {
		LOG.debug("updateRelease {}-{}", environmentName, releaseName);
		dto.setEnvironmentName(environmentName);
		dto.setName(releaseName);
		DtoValidator.validate(dto);
		this.environmentService.updateRelease(dto);
	}

	/**
	 * Tags a release and optionally sets additional properties.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of tagged release to create
	 * @param releaseProperties
	 *            the optional JSON release properties to set
	 * @throws ArtifactNotInReleaseException
	 * @throws ServerNotInReleaseException
	 * @throws ObjectNotExistException
	 * @throws TagReleaseException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @rest TagRelease
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/newtag
	 * @exampleRequestBody {description: 'tagged on Oct 5, 2014'}
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/environments/{environmentName}/{releaseName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void tagRelease(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestBody(required = false) ReleaseDto dto) throws ObjectAlreadyExistsException, TagReleaseException, ObjectNotExistException, ServerNotInReleaseException, ArtifactNotInReleaseException {
		LOG.debug("tagRelease {}-{}", environmentName, releaseName);
		if (dto == null)
			dto = new ReleaseDto();
		dto.setEnvironmentName(environmentName);
		dto.setName(releaseName);
		DtoValidator.validate(dto);
		this.environmentService.tagRelease(dto);
	}

	/**
	 * Reedits a previously tagged release.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @throws ObjectNotExistException
	 * @throws SnapshotReleaseException
	 * 
	 * @rest ReeditRelease
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/newtag/reedit
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/environments/{environmentName}/{releaseName}/reedit")
	@ResponseStatus(HttpStatus.OK)
	public void reeditRelease(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws SnapshotReleaseException, ObjectNotExistException {
		LOG.debug("reeditRelease {}-{}", environmentName, releaseName);
		this.environmentService.reeditRelease(environmentName, releaseName);
	}

	/**
	 * Deletes a release. The operation is successful even if the release does not exist.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @throws ObjectNotExistException
	 * @throws ObjectAlreadyExistsException
	 * @throws SnapshotReleaseException
	 * 
	 * @rest DeleteRelease
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/newtag
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/environments/{environmentName}/{releaseName:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteRelease(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws SnapshotReleaseException, ObjectAlreadyExistsException, ObjectNotExistException {
		LOG.debug("deleteRelease {}-{}", environmentName, releaseName);
		this.environmentService.deleteRelease(environmentName, releaseName);
	}

	/**
	 * Retrieves the effective release artifacts.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @return <code>application/json</code> array of release artifact objects
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws ObjectNotExistException
	 * @throws SnapshotReleaseException
	 * 
	 * @rest GetEffectiveReleaseArtifacts
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/snapshot/artifacts.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{artifact: 'demowebsite', version: '1.0'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/artifacts.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<ArtifactVersionDto> getEffectiveReleaseArtifacts(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, CompatibilityException, NoActiveVersionException, SnapshotReleaseException {
		LOG.debug("getEffectiveReleaseArtifacts {}-{}", environmentName, releaseName);
		return this.environmentService.getEffectiveReleaseArtifacts(environmentName, releaseName);
	}

	/**
	 * Sets the active version (default of server-specific) of a release artifact.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param versionName
	 *            the active version to be set
	 * @param serverName
	 *            an optional server name (omit to set default active version)
	 * @throws ArtifactNotInReleaseException
	 * @throws SnapshotReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest SetReleaseArtifactActiveVersion
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/activeVersion?artifactName=
	 *                 demowebsite
	 *                 &versionName=1.0
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/environments/{environmentName}/{releaseName}/activeVersion")
	@ResponseStatus(HttpStatus.OK)
	public void setArtifactActiveVersion(
			@PathVariable("environmentName") String environmentName,
			@PathVariable("releaseName") String releaseName,
			@RequestParam(value = "artifactName", required = true) String artifactName,
			@RequestParam(value = "versionName", required = true) String versionName,
			@RequestParam(value = "serverName", required = false) String serverName) throws ObjectNotExistException, SnapshotReleaseException, ArtifactNotInReleaseException {
		LOG.debug("setArtifactActiveVersion {}-{} {}[{}] {}", environmentName, releaseName, artifactName, versionName, StringUtils.defaultString(serverName, "-"));
		this.environmentService.setArtifactActiveVersion(environmentName, releaseName, artifactName, versionName, serverName);
	}

	/**
	 * Retrieves the files present in an release artifact package at a given
	 * relative path within the package.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for listing files common to all servers overlays)
	 * @param path
	 *            the optional relative path of the directory to list within package (omit for full recursive listing)
	 * @return <code>application/json</code> array of file objects
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetReleasePackageFiles
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/files.json?artifactName=
	 *                 demowebsite
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 1, name: 'index.php', hasVariables: true}, {id: 2, name: 'logo.gif'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/package/files.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<FileDto> getPackageFiles(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName,
			@RequestParam(value = "path", required = false) String path) throws ObjectNotExistException, ArtifactNotInReleaseException, PackageRescanException {
		LOG.debug("getPackageFiles {}-{} {} {} : ", environmentName, releaseName, artifactName, StringUtils.defaultString(serverName, "-"), StringUtils.defaultString(path, "<root>"));
		return this.environmentService.getPackageFiles(environmentName, releaseName, StringUtils.isEmpty(serverName) ? null : serverName, artifactName, path);
	}

	/**
	 * Retrieves all the <i>special files</i> within the release artifact package.
	 * Files with custom properties, containing variables or overlays are considered "special".
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for listing files common to all servers overlays)
	 * @return <code>application/json</code> array of file objects
	 * @throws PackageRescanException
	 * @throws CompatibilityException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetReleaseSpecialFiles
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/specialFiles.json?
	 *                 artifactName
	 *                 =demowebsite
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 1, name: 'index.php', hasVariables: true}, {id: 2, name: 'logo.gif'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/package/specialFiles.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<FileDto> getSpecialFiles(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName) throws ObjectNotExistException, ArtifactNotInReleaseException, CompatibilityException, PackageRescanException {
		LOG.debug("getSpecialFiles {}-{} {} {}", environmentName, releaseName, artifactName, StringUtils.defaultString(serverName));
		return this.environmentService.getSpecialFiles(environmentName, releaseName, StringUtils.isEmpty(serverName) ? null : serverName, artifactName);
	}

	/**
	 * Update custom flags of a file within a release artifact package.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for making changes common to all servers)
	 * @param path
	 *            the relative path of the file within the package
	 * @param fileProperties
	 *            the JSON file properties to update
	 * @throws ArtifactNotInReleaseException
	 * @throws SnapshotReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateReleasePackageFileProperties
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/file?artifactName=
	 *                 demowebsite
	 *                 &path=index.php
	 * @exampleRequestBody {ignoreVariables: true}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/environments/{environmentName}/{releaseName}/package/file", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updatePackageFileProperties(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName,
			@RequestParam(value = "path") String path,
			@RequestBody FilePropertiesDto dto) throws ObjectNotExistException, SnapshotReleaseException, ArtifactNotInReleaseException {
		LOG.debug("updatePackageFileProperties {}-{} {} {} : {}", environmentName, releaseName, artifactName, StringUtils.defaultString(serverName), path);
		DtoValidator.validate(dto);
		this.environmentService.updateReleasePackageFileProperties(environmentName, releaseName, StringUtils.isEmpty(serverName) ? null : serverName, artifactName, path, dto);
	}

	/**
	 * Downloads a file within the release artifact package.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for referring to common environment file)
	 * @param path
	 *            the relative path of the file within the package
	 * @return <code>text/plain</code> or <code>application/octet-stream</code> contents of the file
	 * @throws NoActiveVersionException
	 * @throws CompatibilityException
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest DownloadReleasePackageFile
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/file?artifactName=
	 *                 demowebsite
	 *                 &path=index.php
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/package/file")
	@ResponseStatus(HttpStatus.OK)
	public void downloadPackageFile(HttpServletResponse res,
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName,
			@RequestParam(value = "path") String path) throws ObjectNotExistException, ArtifactNotInReleaseException, CompatibilityException, NoActiveVersionException {
		LOG.debug("downloadPackageFile {}-{} {} {} : {}", environmentName, releaseName, artifactName, StringUtils.defaultString(serverName), path);
		this.environmentService.viewPackageFile(environmentName, releaseName, StringUtils.isEmpty(serverName) ? null : serverName, artifactName, path, res);
	}

	/**
	 * Uploads an overlay at a relative path within the release artifact package.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for referring to common environment file)
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param file
	 *            overlay file to upload as POST data
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectNotExistException
	 * @throws IOException
	 * 
	 * @rest UploadReleasePackageOverlay
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/overlay?artifactName=
	 *                 demowebsite
	 * @exampleRequestBody multipart/form-data: ...Content-Disposition: form-data; name="file";filename="logo.gif"
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/environments/{environmentName}/snapshot/package/overlay", headers = "content-type=multipart/*")
	@ResponseStatus(HttpStatus.CREATED)
	public void uploadPackageOverlayMultipart(
			@PathVariable(value = "environmentName") String environmentName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName,
			@RequestParam(value = "path", required = false) String path,
			@RequestParam(value = "file") MultipartFile file) throws ObjectNotExistException, ArtifactNotInReleaseException, IOException {
		LOG.debug("uploadPackageOverlayMultipart {} {} {} : {} {}", environmentName, artifactName, StringUtils.defaultString(serverName), StringUtils.defaultString(path, "<root>"), file.getOriginalFilename());
		this.environmentService.uploadPackageOverlay(environmentName, artifactName, StringUtils.isEmpty(serverName) ? null : serverName, path == null ? StringUtils.EMPTY : path, file, null);
	}

	/**
	 * Uploads an overlay at a relative path within the release artifact package.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for referring to common environment file)
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param url
	 *            url or server path of overlay file to upload
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectNotExistException
	 * @throws IOException
	 * 
	 * @rest UploadReleasePackageOverlay
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/overlay?artifactName=
	 *                 demowebsite
	 * @exampleRequestBody multipart/form-data: ...Content-Disposition: form-data; name="file";filename="logo.gif"
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/environments/{environmentName}/snapshot/package/overlay")
	@ResponseStatus(HttpStatus.CREATED)
	public void uploadPackageOverlay(
			@PathVariable(value = "environmentName") String environmentName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName,
			@RequestParam(value = "path", required = false) String path,
			@RequestParam(value = "url") String url) throws ObjectNotExistException, ArtifactNotInReleaseException, IOException {
		LOG.debug("uploadPackageOverlay {} {} {} : {} {}", environmentName, artifactName, StringUtils.defaultString(serverName), StringUtils.defaultString(path, "<root>"), url);
		this.environmentService.uploadPackageOverlay(environmentName, artifactName, StringUtils.isEmpty(serverName) ? null : serverName, path == null ? StringUtils.EMPTY : path, null, url);
	}

	/**
	 * Deletes an existing release artifact package overlay.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param artifactName
	 *            the name of an artifact
	 * @param serverName
	 *            an optional server name (omit for referring to common environment file)
	 * @param path
	 *            the relative path of the file within the package
	 * @throws ArtifactNotInReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest DeleteReleasePackageOverlay
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest
	 *                 http://kwatee.local:8080/kwate/api/environments/intro/snapshot/package/overlay?artifactName=
	 *                 demowebsite
	 *                 &path=logo.gif
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/environments/{environmentName}/snapshot/package/overlay")
	@ResponseStatus(HttpStatus.OK)
	public void deletePackageOverlay(
			@PathVariable(value = "environmentName") String environmentName,
			@RequestParam(value = "artifactName") String artifactName,
			@RequestParam(value = "serverName", required = false) String serverName,
			@RequestParam(value = "path") String path) throws ObjectNotExistException, ArtifactNotInReleaseException {
		LOG.debug("deletePackageOverlay {} {} {} : {}", environmentName, artifactName, StringUtils.defaultString(serverName), path);
		this.environmentService.deletePackageOverlay(environmentName, artifactName, StringUtils.isEmpty(serverName) ? null : serverName, path);
	}

	/**
	 * Retrieves release variables.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @return <code>application/json</code> array of variable objects
	 * @throws PackageRescanException
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetReleaseVariables
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/snapshot/variables.json
	 * @exampleResponse 200
	 * @exampleResponseBody {variables: [{name: 'CUSTOMER_NAME', value: 'ACME Corp.'}, {name: 'kwatee_deployment_dir',
	 *                      description: 'Default deployment directory', artifact: 'demowebsite', value:
	 *                      '/Library/WebServer/Documents'}], globalVariables: [{name: 'kwatee_root_dir', description:
	 *                      'Kwatee agent
	 *                      and metadata directory', value: '/var/tmp/kwatee'}, {name: 'kwatee_deployment_dir',
	 *                      description: 'Default deployment directory', value:
	 *                      '/var/tmp/kwateetest/%{kwatee_environment_name}'}, {name: 'kwatee_package_dir', description:
	 *                      'Default deployment artifact directory',
	 *                      value: '%{kwatee_deployment_dir}/%{kwatee_package_name}'}]}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/variables.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ReleaseDto getVariables(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, PackageRescanException {
		LOG.debug("getVariables {}-{}", environmentName, releaseName);
		ReleaseDto dto = this.environmentService.getVariables(environmentName, releaseName);
		return dto;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/variables.export", produces = "application/octet-stream")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String exportVariables(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName) throws ObjectNotExistException, PackageRescanException, JsonProcessingException {
		LOG.debug("getVariables {}-{}", environmentName, releaseName);
		ReleaseDto dto = this.environmentService.getExportVariables(environmentName, releaseName);
		return this.mapper.writeValueAsString(dto);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/environments/{environmentName}/{releaseName}/variables.import")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ReleaseDto importVariables(
			@PathVariable(value = "artifactName") String environmentName,
			@PathVariable(value = "versionName") String releaseName,
			@RequestParam(value = "file") MultipartFile file,
			HttpServletResponse response
			) throws ObjectNotExistException, PackageRescanException, IOException, CannotDeleteObjectInUseException, SnapshotReleaseException {
		LOG.debug("importVariables {}[{}]", environmentName, releaseName);
		Collection<VariableDto> dtos = this.mapper.readValue(file.getBytes(), ReleaseDto.class).getVariables();
		if (CollectionUtils.isNotEmpty(dtos)) {
			for (VariableDto dto : dtos)
				DtoValidator.validate(dto);
			this.environmentService.updateVariables(environmentName, releaseName, dtos, true);
		}
		return this.environmentService.getVariables(environmentName, releaseName);
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
	@RequestMapping(method = RequestMethod.GET, value = "/environments/{environmentName}/{releaseName}/resolvedVariables.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<VariableDto> getResolvedVariables(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestParam(value = "serverName") String serverName,
			@RequestParam(value = "artifactName") String artifactName) throws ObjectNotExistException, ArtifactNotInReleaseException, MissingVariableException, CompatibilityException, NoActiveVersionException {
		LOG.debug("getResolvedVariables {}-{} {} {}", environmentName, releaseName, serverName, artifactName);
		return this.environmentService.getResolvedVariables(environmentName, releaseName, serverName, artifactName);
	}

	/**
	 * Sets the release variables.
	 * 
	 * @param environmentName
	 *            the name of the release's environment
	 * @param releaseName
	 *            the name of the release
	 * @param variables
	 *            a JSON array of variable objects
	 * @throws PackageRescanException
	 * @throws SnapshotReleaseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest SetReleaseVariables
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/environments/intro/snapshot/variables
	 * @exampleRequestBody [{name: 'CUSTOMER_NAME', value: 'test'}]
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/environments/{environmentName}/{releaseName}/variables", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ReleaseDto setVariables(
			@PathVariable(value = "environmentName") String environmentName,
			@PathVariable(value = "releaseName") String releaseName,
			@RequestBody Collection<VariableDto> dtos) throws ObjectNotExistException, SnapshotReleaseException, PackageRescanException {
		LOG.debug("setVariables {}-{}", environmentName, releaseName);
		for (VariableDto dto : dtos)
			DtoValidator.validate(dto);
		this.environmentService.updateVariables(environmentName, releaseName, dtos, false);
		return this.environmentService.getVariables(environmentName, releaseName);
	}
}