/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.repository.dto.ArtifactDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.FilePropertiesDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VersionDto;
import net.kwatee.agiledeployment.webapp.service.ArtifactService;

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

import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping(value = "/api")
/**
 * 
 * @author mac
 *
 */
public class ArtifactController {

	@Autowired
	private ArtifactService artifactService;

	@Autowired
	private ObjectMapper mapper;

	private final static Logger LOG = LoggerFactory.getLogger(ArtifactController.class);

	/**
	 * Retrieves the list of artifacts in the repository.
	 * 
	 * @return <code>application/json</code> array of artifact objects
	 * 
	 * @rest GetArtifacts
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/artifacts.json?activeOnly=true
	 * @exampleResponse 200
	 * @exampleResponseBody [{description: 'Demo PHP web site', name: 'demowebsite'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<ArtifactDto> getArtifacts() {
		LOG.debug("getArtifacts");
		return this.artifactService.getArtifacts();
	}

	/**
	 * Retrieves an artifact's properties.
	 * 
	 * @param artifactName
	 *            the name of an artifact
	 * @return <code>application/json</code> artifact object
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetArtifact
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/artifacts/demowebsite.json
	 * @exampleResponse 200
	 * @exampleResponseBody {name: 'demowebsite', description: 'Demo PHP web site', versions: [{name: '1.0',
	 *                      description: 'initial release', platforms: [1, 2, 7]}]}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ArtifactDto getArtifact(
			@PathVariable(value = "artifactName") String artifactName) throws ObjectNotExistException {
		LOG.debug("getArtifact {}", artifactName);
		return this.artifactService.getArtifact(artifactName);
	}

	/**
	 * Updates an artifact's properties.
	 * 
	 * @param artifactName
	 *            the name of an artifact
	 * @param artifactProperties
	 *            the JSON artifact's properties to update
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateArtifact
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/artifacts/demowebsite
	 * @exampleRequestBody {description:'new description'}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/artifacts/{artifactName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateArtifact(
			@PathVariable(value = "artifactName") String artifactName,
			@RequestBody ArtifactDto dto) throws ObjectNotExistException {
		LOG.debug("updateArtifact {}", artifactName);
		dto.setName(artifactName);
		DtoValidator.validate(dto);
		this.artifactService.updateArtifact(dto);
	}

	/**
	 * Creates a new artifact and optionally set additional properties.
	 * 
	 * @param artifactName
	 *            the name of the artifact to create
	 * @param artifactProperties
	 *            the JSON artifact's properties to update or empty
	 * @throws ObjectNotExistException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @rest CreateArtifact
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/testArtifact
	 * @exampleRequestBody {description:'initial description'}
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void createArtifact(
			@PathVariable(value = "artifactName") String artifactName,
			@RequestBody(required = false) ArtifactDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException {
		LOG.debug("createArtifact {}", artifactName);
		if (dto == null)
			dto = new ArtifactDto();
		dto.setName(artifactName);
		DtoValidator.validate(dto);
		this.artifactService.createArtifact(dto);
	}

	/**
	 * Deletes an artifact. The operation is successful even if the artifact does not exist.
	 * 
	 * @param artifactName
	 *            the name of an artifact
	 * @throws CannotDeleteObjectInUseException
	 * 
	 * @rest DeleteArtifact
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/testArtifact
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/artifacts/{artifactName:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteArtifact(@PathVariable(value = "artifactName") String artifactName) throws CannotDeleteObjectInUseException {
		LOG.debug("deleteArtifact {}", artifactName);
		this.artifactService.deleteArtifact(artifactName);
	}

	/**
	 * Retrieves the properties of an artifact's version.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @return <code>application/json</code> version object
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetVersion
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0.json
	 * @exampleResponse 200
	 * @exampleResponseBody {name: '1.0', description: 'initial release', frozen: true, platforms: [1, 2, 7],
	 *                      packageInfo: {name: 'mywebsite.zip', size: '10.0 KB'}, executables: [], varPrefixChar: 37}
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}/{versionName}.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VersionDto getVersion(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName) throws ObjectNotExistException {
		LOG.debug("getVersion {}[{}]", artifactName, versionName);
		return this.artifactService.getVersion(artifactName, versionName);
	}

	/**
	 * Updates an artifact's version properties.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param versionProperties
	 *            the JSON version properties
	 * @throws CompatibilityException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateVersion
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0
	 * @exampleRequestBody {description:'new description'}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/artifacts/{artifactName}/{versionName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updateVersion(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestBody VersionDto dto) throws ObjectNotExistException, CompatibilityException {
		LOG.debug("updateVersion {}[{}]", artifactName, versionName);
		dto.setArtifactName(artifactName);
		dto.setName(versionName);
		DtoValidator.validate(dto);
		this.artifactService.updateVersion(dto);
	}

	/**
	 * Creates or duplicates a version in an artifact and optionally set additional properties.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of the version to create
	 * @param duplicateFrom
	 *            the optional name of a version to duplicate
	 * @param versionProperties
	 *            the optional JSON version properties
	 * @throws CompatibilityException
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotExistException
	 * 
	 * @rest CreateOrDuplicateVersion
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @example Duplicate existing version 1.0
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/2.0?duplicateFrom=1.0
	 * @exampleRequestBody {description: 'duplicated from version 1.0'}
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName}/{versionName:.+}", consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public void createVersion(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "duplicateFrom", required = false) String duplicateFrom,
			@RequestBody(required = false) VersionDto dto) throws ObjectNotExistException, ObjectAlreadyExistsException, CompatibilityException {
		LOG.debug("createVersion {}[{}] from ", artifactName, versionName, StringUtils.defaultString(duplicateFrom, "<none>"));
		if (dto == null)
			dto = new VersionDto();
		dto.setArtifactName(artifactName);
		dto.setName(versionName);
		DtoValidator.validate(dto);
		if (duplicateFrom == null) {
			this.artifactService.createVersion(dto);
		} else {
			this.artifactService.duplicateVersion(dto, duplicateFrom);
		}
	}

	/**
	 * Deletes an artifact's version. The operation is successful even if the version does not exist.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @throws CannotDeleteObjectInUseException
	 * 
	 * @rest DeleteVersion
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/2.0
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/artifacts/{artifactName}/{versionName:.+}")
	@ResponseStatus(HttpStatus.OK)
	public void deleteVersion(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName) throws CannotDeleteObjectInUseException {
		LOG.debug("deleteVersion {}[{}]", artifactName, versionName);
		this.artifactService.deleteVersion(artifactName, versionName);
	}

	/**
	 * Uploads a package to an artifact's version. <br/>
	 * Replaces whatever existing package there is but retains previously
	 * uploaded overlays unless <code>deleteOverlays=true</code>.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param file
	 *            the package file to upload as POST data
	 * @param deleteOverlays
	 *            if <code>true</code>, removes pre-existing overlays ( <code>false</code> if omitted)
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UploadArtifactPackage
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0
	 * @exampleRequestBody multipart/form-data: ...Content-Disposition: form-data;
	 *                     name="file";filename="newarchive.tar.gz"
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName}/{versionName}/package", headers = "Content-Type=multipart/*", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VersionDto uploadPackageMultipart(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "file") MultipartFile file,
			@RequestParam(value = "deleteOverlays", required = false) boolean deleteOverlays) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("uploadPackageMultipart {}[{}] {}", artifactName, versionName, file.getOriginalFilename());
		return this.artifactService.uploadPackage(artifactName, versionName, file, null, deleteOverlays);
	}

	/**
	 * Uploads a package to an artifact's version. <br/>
	 * Replaces whatever existing package there is but retains previously
	 * uploaded overlays unless <code>deleteOverlays=true</code>.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param url
	 *            url or server path of package to upload
	 * @param deleteOverlays
	 *            if <code>true</code>, removes pre-existing overlays ( <code>false</code> if omitted)
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 * 
	 * @rest UploadArtifactPackage
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0?url=file:///newarchive.tar.gz
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName}/{versionName}/package")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VersionDto uploadPackage(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "url", required = false) String url,
			@RequestParam(value = "deleteOverlays", required = false) boolean deleteOverlays) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("uploadPackage {}[{}] {}", artifactName, versionName, StringUtils.defaultString(url));
		if (url == null)
			return this.artifactService.rescanPackage(artifactName, versionName);
		else
			return this.artifactService.uploadPackage(artifactName, versionName, null, url, deleteOverlays);
	}

	/**
	 * Deletes an existing version package.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest DeleteArtifactPackage
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/artifacts/{artifactName}/{versionName}/package")
	@ResponseStatus(HttpStatus.OK)
	public void deletePackage(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("deletePackage {}[{}]", artifactName, versionName);
		this.artifactService.deletePackage(artifactName, versionName);
	}

	/**
	 * Retrieves the files present in a version's package at a given relative path.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the directory to list within package (if omitted, root of the package)
	 * @return <code>application/json</code> array of file objects
	 * @throws PackageRescanException
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetArtifactPackageFiles
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/files.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 1, name: 'index.php', hasVariables: true}, {id: 2, name: 'logo.gif'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}/{versionName}/package/files.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<FileDto> getPackageFiles(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "path", required = false) String path) throws ObjectNotExistException, PackageRescanException {
		LOG.debug("getPackageFiles {}[{}] {}", artifactName, versionName, StringUtils.defaultString(path, "<root>"));
		return this.artifactService.getPackageFiles(artifactName, versionName, path);
	}

	/**
	 * Retrieves all the <i>special files</i> within the package. Files with custom properties,
	 * containing variables or overlays are considered "special".
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @return <code>application/json</code> array of file objects
	 * @throws PackageRescanException
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetArtifactSpecialFiles
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/specialFiles.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{id: 1, name: 'index.php', hasVariables: true, path: 'index.php'}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}/{versionName}/package/specialFiles.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<FileDto> getSpecialFiles(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName) throws ObjectNotExistException, PackageRescanException {
		LOG.debug("getSpecialFiles {}[{}]", artifactName, versionName);
		return this.artifactService.getSpecialFiles(artifactName, versionName);
	}

	/**
	 * Updates custom flags of a file within a package.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the file within the package
	 * @param fileProperties
	 *            the JSON file's properties to update
	 * @throws PackageRescanException
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UpdateArtifactPackageFileProperties
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/file?path=index.php
	 * @exampleRequestBody {ignoreVariables: true}
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/artifacts/{artifactName}/{versionName}/package/file", consumes = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public void updatePackageFileProperties(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "path") String path,
			@RequestBody FilePropertiesDto dto) throws ObjectNotExistException, CannotDeleteObjectInUseException, PackageRescanException {
		LOG.debug("updatePackageFileProperties {}[{}] {}", artifactName, versionName, path);
		DtoValidator.validate(dto);
		this.artifactService.updatePackageFileProperties(artifactName, versionName, path, dto);
	}

	/**
	 * Downloads a file within the package in the specified location.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the file within the package
	 * @return <code>text/plain</code> or <code>application/octet-stream</code> file contents
	 * @throws ObjectNotExistException
	 * @throws IOException
	 * 
	 * @rest DownloadArtifactPackageFile
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/file?path=index.php
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}/{versionName}/package/file")
	@ResponseStatus(HttpStatus.OK)
	public void downloadPackageFile(HttpServletResponse res,
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "path") String path) throws ObjectNotExistException, IOException {
		LOG.debug("downloadPackageFile {}[{}] {}", artifactName, versionName, path);
		this.artifactService.viewPackageFile(artifactName, versionName, path, res);
	}

	/**
	 * Uploads an overlay at a relative path within the package.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param file
	 *            overlay file to upload as POST data
	 * @throws ObjectAlreadyExistsException
	 * @throws PackageRescanException
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UploadArtifactPackageOverlay
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @example Upload an overlay at the root of the archive named 'logo.gif'
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/overlay
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName}/{versionName}/package/overlay", headers = "Content-Type=multipart/*")
	@ResponseStatus(HttpStatus.CREATED)
	public void uploadPackageOverlayMultipart(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "path", required = false) String path,
			@RequestParam(value = "file") MultipartFile file) throws ObjectNotExistException, CannotDeleteObjectInUseException, PackageRescanException, ObjectAlreadyExistsException {
		LOG.debug("uploadPackageOverlayMultipart {}[{}] {} {}", artifactName, versionName, StringUtils.defaultString(path, "<root>"), file.getOriginalFilename());
		this.artifactService.uploadPackageOverlay(artifactName, versionName, path == null ? StringUtils.EMPTY : path, file, null);
	}

	/**
	 * Uploads an overlay at a relative path within the package.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the overlay directory within the package
	 * @param url
	 *            url or server path of overlay file to upload
	 * @throws ObjectAlreadyExistsException
	 * @throws PackageRescanException
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest UploadArtifactPackageOverlay
	 * @restStatus <code>201</code> created
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @example Upload an overlay at the root of the archive named 'logo.gif'
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/overlay?url=file:///logo.gif
	 * @exampleResponse 201
	 */
	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName}/{versionName}/package/overlay")
	@ResponseStatus(HttpStatus.CREATED)
	public void uploadPackageOverlay(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "path", required = false) String path,
			@RequestParam(value = "url") String url) throws ObjectNotExistException, CannotDeleteObjectInUseException, PackageRescanException, ObjectAlreadyExistsException {
		LOG.debug("uploadPackageOverlay {}[{}] {} {}", artifactName, versionName, StringUtils.defaultString(path, "<root>"), path);
		this.artifactService.uploadPackageOverlay(artifactName, versionName, path == null ? StringUtils.EMPTY : path, null, url);
	}

	/**
	 * Deletes an existing version overlay.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param path
	 *            the relative path of the file within the package
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest DeleteArtifactPackageOverlay
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/package/overlay?path=logo.gif
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.DELETE, value = "/artifacts/{artifactName}/{versionName}/package/overlay")
	@ResponseStatus(HttpStatus.OK)
	public void deletePackageOverlay(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "path") String path) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("deletePackageOverlay {}[{}] {}", artifactName, versionName, path);
		this.artifactService.deletePackageOverlay(artifactName, versionName, path);
	}

	/**
	 * Retrieves the list of version variables.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @return <code>application/json</code> array of version variable objects
	 * @throws PackageRescanException
	 * @throws ObjectNotExistException
	 * 
	 * @rest GetArtifactVariables
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/variables.json
	 * @exampleResponse 200
	 * @exampleResponseBody [{name: 'CUSTOMER_NAME', reference: ['demowebsite - index.php']}]
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}/{versionName}/variables.json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VersionDto getVariables(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName) throws ObjectNotExistException, PackageRescanException {
		LOG.debug("getVariables {}[{}]", artifactName, versionName);
		return this.artifactService.getVariables(artifactName, versionName);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/artifacts/{artifactName}/{versionName}/variables.export", produces = "application/octet-stream")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String exportVariables(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName) throws ObjectNotExistException, PackageRescanException, IOException {
		LOG.debug("exportVariables {}[{}]", artifactName, versionName);
		VersionDto dto = this.artifactService.getExportVariables(artifactName, versionName);
		return this.mapper.writeValueAsString(dto);
	}

	@RequestMapping(method = RequestMethod.POST, value = "/artifacts/{artifactName}/{versionName}/variables.import", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VersionDto importVariables(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "file") MultipartFile file) throws ObjectNotExistException, PackageRescanException, IOException, CannotDeleteObjectInUseException {
		LOG.debug("importVariables {}[{}]", artifactName, versionName);
		Collection<VariableDto> dtos = this.mapper.readValue(file.getBytes(), VersionDto.class).getDefaultVariableValues();
		for (VariableDto dto : dtos)
			DtoValidator.validate(dto);
		this.artifactService.updateVariables(artifactName, versionName, dtos, true);
		return this.artifactService.getVariables(artifactName, versionName);
	}

	/**
	 * Sets version variables.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param variables
	 *            a JSON array of variable objects
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 * 
	 * @rest SetArtifactVariables
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/variables
	 * @exampleRequestBody [{name: 'CUSTOMER_NAME', value: 'ACME Corp.'}]
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/artifacts/{artifactName}/{versionName}/variables", consumes = "application/json", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VersionDto setVariables(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestBody Collection<VariableDto> dtos) throws ObjectNotExistException, CannotDeleteObjectInUseException, PackageRescanException {
		LOG.debug("setVariables {}[{}]", artifactName, versionName);
		for (VariableDto dto : dtos)
			DtoValidator.validate(dto);
		this.artifactService.updateVariables(artifactName, versionName, dtos, false);
		return this.artifactService.getVariables(artifactName, versionName);
	}

	/**
	 * Sets the version's variable prefix character.
	 * 
	 * @param artifactName
	 *            the name of the version's artifact
	 * @param versionName
	 *            the name of a version
	 * @param char
	 *        the variable prefix character
	 * @throws CannotDeleteObjectInUseException
	 * @throws ObjectNotExistException
	 * 
	 * @rest SetArtifactVariablePrefixCharacter
	 * @restStatus <code>200</code> success
	 * @restStatus <code class='error'>401</code> authentication token missing or expired
	 * @restStatus <code class='error'>404</code> not found
	 * @restStatus <code class='error'>420</code> unspecified kwatee error (check logs)
	 * @restStatus <code class='error'>500</code> internal error
	 * 
	 * @exampleRequest http://kwatee.local:8080/kwate/api/demowebsite/1.0/variablePrefix?char=@
	 * @exampleResponse 200
	 */
	@RequestMapping(method = RequestMethod.PUT, value = "/artifacts/{artifactName}/{versionName}/variablePrefix")
	@ResponseStatus(HttpStatus.OK)
	public void setVariablePrefix(
			@PathVariable(value = "artifactName") String artifactName,
			@PathVariable(value = "versionName") String versionName,
			@RequestParam(value = "char") char variablePrefixChar) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		LOG.debug("setVariablePrefix {}[{}] '{}'", artifactName, versionName, String.valueOf(variablePrefixChar));
		this.artifactService.setVariablePrefixChar(artifactName, versionName, variablePrefixChar);
	}
}