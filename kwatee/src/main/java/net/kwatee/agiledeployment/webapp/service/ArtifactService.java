/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import net.kwatee.agiledeployment.common.Constants.LayerType;
import net.kwatee.agiledeployment.common.exception.CannotDeleteObjectInUseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.ObjectAlreadyExistsException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.core.Audit;
import net.kwatee.agiledeployment.core.repository.AdminRepository;
import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.core.repository.ServerRepository;
import net.kwatee.agiledeployment.core.service.FileStoreService;
import net.kwatee.agiledeployment.core.service.LayerService;
import net.kwatee.agiledeployment.core.service.PackageService;
import net.kwatee.agiledeployment.core.variable.VariableService;
import net.kwatee.agiledeployment.repository.dto.ArtifactDto;
import net.kwatee.agiledeployment.repository.dto.ExecutableDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.FilePropertiesDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VariableReference;
import net.kwatee.agiledeployment.repository.dto.VersionDto;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Executable;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;
import net.kwatee.agiledeployment.webapp.controller.ServletHelper;
import net.kwatee.agiledeployment.webapp.mapper.ArtifactMapper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ArtifactService {

	@Autowired
	private ArtifactRepository artifactRepository;
	@Autowired
	private ServerRepository serverRepository;
	@Autowired
	private AdminRepository adminRepository;
	@Autowired
	private ArtifactMapper artifactMapper;
	@Autowired
	private PackageService packageService;
	@Autowired
	private FileStoreService repositoryService;
	@Autowired
	private LayerService layerService;

	/**
	 * 
	 * @return
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<ArtifactDto> getArtifacts() {
		Collection<Artifact> artifacts = this.artifactRepository.getArtifacts();
		return this.artifactMapper.toShortArtifactDtos(artifacts);
	}

	/**
	 * 
	 * @param artifactName
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public ArtifactDto getArtifact(String artifactName) throws ObjectNotExistException {
		Artifact artifact = this.artifactRepository.getCheckedArtifact(artifactName);
		return this.artifactMapper.toArtifactDto(artifact);
	}

	/**
	 * 
	 * @param artifactName
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void deleteArtifact(String artifactName) throws CannotDeleteObjectInUseException {
		Artifact artifact = this.artifactRepository.getUncheckedArtifact(artifactName);
		if (artifact != null) {
			this.artifactRepository.deleteArtifact(artifact);
			Audit.log("Deleted artifact {}", artifactName);
		}
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void createArtifact(ArtifactDto dto) throws ObjectAlreadyExistsException, ObjectNotExistException {
		Artifact artifact = this.artifactRepository.getUncheckedArtifact(dto.getName());
		if (artifact != null) {
			throw new ObjectAlreadyExistsException("Artifact " + dto.getName());
		}
		artifact = new Artifact();
		artifact.setName(dto.getName());
		updateArtifact(artifact, dto);
		Audit.log("Created artifact {}", dto.getName());
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void updateArtifact(ArtifactDto dto) throws ObjectNotExistException {
		Artifact artifact = this.artifactRepository.getCheckedArtifact(dto.getName());
		updateArtifact(artifact, dto);
		Audit.log("Updated artifact {}", dto.getName());
	}

	private void updateArtifact(Artifact artifact, ArtifactDto dto) {
		if (dto.isDisabled() != null)
			artifact.setDisabled(dto.isDisabled());
		if (dto.getDescription() != null)
			artifact.setDescription(dto.getDescription());
		this.artifactRepository.saveArtifact(artifact);
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public VersionDto getVersion(String artifactName, String versionName) throws ObjectNotExistException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		VersionDto dto = this.artifactMapper.toVersionDto(version);
		return dto;
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void deleteVersion(String artifactName, String versionName) throws CannotDeleteObjectInUseException {
		Version version = this.artifactRepository.getUncheckedVersion(artifactName, versionName);
		if (version != null) {
			this.artifactRepository.deleteVersion(version, true);
			Audit.log("Deleted version {}", version.toString());
		}
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectNotExistException
	 * @throws ObjectAlreadyExistsException
	 * @throws CompatibilityException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void createVersion(VersionDto dto) throws ObjectNotExistException, ObjectAlreadyExistsException, CompatibilityException {
		Artifact artifact = this.artifactRepository.getCheckedArtifact(dto.getArtifactName());
		Version version = this.artifactRepository.getUncheckedVersion(artifact.getName(), dto.getName());
		if (version != null)
			throw new ObjectAlreadyExistsException("Version " + version.toString());
		version = new Version();
		version.setName(dto.getName());
		version.setArtifact(artifact);
		artifact.getVersions().add(version);
		this.artifactRepository.saveArtifact(artifact);
		updateVersion(version, dto);
		Audit.log("Created version {}", version.toString());
	}

	/**
	 * 
	 * @param dto
	 * @param fromVersionName
	 * @throws ObjectNotExistException
	 * @throws ObjectAlreadyExistsException
	 * @throws CompatibilityException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void duplicateVersion(VersionDto dto, String fromVersionName) throws ObjectNotExistException, ObjectAlreadyExistsException, CompatibilityException {
		Version version = this.artifactRepository.getCheckedVersion(dto.getArtifactName(), fromVersionName);
		Artifact artifact = version.getArtifact();
		if (this.artifactRepository.getUncheckedVersion(version.getArtifact().getName(), dto.getName()) != null)
			throw new ObjectAlreadyExistsException("VersionShortDto " + dto.getName());
		Version duplicateVersion = version.duplicate(dto.getName());
		artifact.getVersions().add(duplicateVersion);
		this.artifactRepository.saveArtifact(artifact);
		updateVersion(duplicateVersion, dto);
		Audit.log("Created version {}", duplicateVersion.toString());
	}

	/**
	 * 
	 * @param dto
	 * @throws ObjectNotExistException
	 * @throws CompatibilityException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void updateVersion(VersionDto dto) throws ObjectNotExistException, CompatibilityException {
		Version version = this.artifactRepository.getCheckedVersion(dto.getArtifactName(), dto.getName());
		updateVersion(version, dto);
		Audit.log("Updated version {}", version.toString());
	}

	private void updateVersion(Version version, VersionDto dto) throws CompatibilityException {
		if (dto.isDisabled() != null)
			version.setDisabled(dto.isDisabled());
		if (dto.getDescription() != null)
			version.setDescription(dto.getDescription());
		if (dto.getPlatforms() != null)
			updateVersionPlatforms(version, dto.getPlatforms());
		if (!this.artifactRepository.isVersionUsedInFrozenRelease(version)) {
			if (dto.getPreDeployAction() != null)
				version.setPreDeployAction(dto.getPreDeployAction());
			if (dto.getPostDeployAction() != null)
				version.setPostDeployAction(dto.getPostDeployAction());
			if (dto.getPreUndeployAction() != null)
				version.setPreUndeployAction(dto.getPreUndeployAction());
			if (dto.getPostUndeployAction() != null)
				version.setPostUndeployAction(dto.getPostUndeployAction());
			PermissionDto permissions = dto.getPermissions();
			if (permissions != null) {
				if (permissions.getFileOwner() != null)
					version.setFileOwner(permissions.getFileOwner().isEmpty() ? null : permissions.getFileOwner());
				if (permissions.getFileGroup() != null)
					version.setFileGroup(permissions.getFileGroup().isEmpty() ? null : permissions.getFileGroup());
				if (permissions.getFileMode() != null)
					version.setFileMode(permissions.getFileMode().isEmpty() ? null : Integer.parseInt(permissions.getFileMode(), 8));
				if (permissions.getDirMode() != null)
					version.setDirMode(permissions.getDirMode().isEmpty() ? null : Integer.parseInt(permissions.getDirMode(), 8));

			}
			if (dto.getExecutables() != null)
				updateVersionExecutables(version, dto.getExecutables());
		}
		this.artifactRepository.saveVersion(version);
	}

	private void updateVersionPlatforms(Version version, final Collection<Integer> platforms) throws CompatibilityException {
		if (platforms.isEmpty()) {
			version.getPlatforms().clear();
		} else {
			/*
			* Check server compatibility
			*/
			Collection<Server> artifactServers = this.serverRepository.getServersContainingVersion(version);
			for (final Server server : artifactServers) {
				if (CollectionUtils.find(platforms,
						new Predicate() {

							public boolean evaluate(Object p) {
								return server.getPlatform().equals(p);
							}
						}) == null) {
					throw new CompatibilityException(version.getArtifact().getName(), server.getName());
				}
			}
			/*
			* Remove obsolete
			*/
			CollectionUtils.filter(version.getPlatforms(),
					new Predicate() {

						public boolean evaluate(Object p) {
							return platforms.contains(p);
						}
					});
			/*
			* Add (it's a set)
			*/
			for (Integer platformId : platforms) {
				if (platformId != null)
					version.getPlatforms().add(platformId);
			}
		}
	}

	private void updateVersionExecutables(Version version, final Collection<ExecutableDto> dtos) {
		/*
		* Remove obsolete executables
		*/
		CollectionUtils.filter(version.getExecutables(),
				new Predicate() {

					public boolean evaluate(Object e) {
						for (ExecutableDto dto : dtos) {
							if (dto.getName().equals(((Executable) e).getName()))
								return true;
						}
						return false;
					}
				});
		/*
		* update & add values
		*/
		for (final ExecutableDto dto : dtos) {
			Executable exe = (Executable) CollectionUtils.find(version.getExecutables(),
					new Predicate() {

						public boolean evaluate(Object e2) {
							return dto.getName().equals(((Executable) e2).getName());
						}
					});
			if (exe == null) {
				exe = new Executable();
				exe.setVersion(version);
				exe.setName(dto.getName());
				version.getExecutables().add(exe);
			}
			if (dto.getDescription() != null)
				exe.setDescription(dto.getDescription());
			if (dto.getStartAction() != null)
				exe.setStartAction(dto.getStartAction());
			if (dto.getStopAction() != null)
				exe.setStopAction(dto.getStopAction());
			if (dto.getStatusAction() != null)
				exe.setStatusAction(dto.getStatusAction());
		}
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param uploadMultipart
	 * @param uploadUrl
	 * @param deleteOverlays
	 * @return
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public VersionDto uploadPackage(String artifactName, String versionName, MultipartFile uploadMultipart, String uploadUrl, boolean deleteOverlays) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		FileUpload upload = null;
		try {
			upload = new FileUpload(uploadUrl, uploadMultipart, true);
			Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
			this.artifactRepository.checkIfUsedInFrozenRelease(version); // force exception if not editable
			Set<String> excludedExtensions = this.adminRepository.getApplicationParameters().getExcludedExtensionsAsSet();
			this.packageService.updatePackage(
					version,
					upload.getName(),
					upload.getFile(),
					deleteOverlays,
					excludedExtensions);
			this.artifactRepository.saveVersion(version);
			Audit.log("Updated package of version {}", version.toString());
			return getVersion(artifactName, versionName);
		} finally {
			FileUpload.releaseQuietly(upload);
		}
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @return
	 * @throws ObjectNotExistException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public VersionDto rescanPackage(String artifactName, String versionName) throws ObjectNotExistException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		Set<String> excludedExtensions = this.adminRepository.getApplicationParameters().getExcludedExtensionsAsSet();
		this.packageService.updatePackage(version, excludedExtensions);
		this.artifactRepository.saveVersion(version);
		return getVersion(artifactName, versionName);
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void deletePackage(String artifactName, String versionName) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		this.artifactRepository.checkIfUsedInFrozenRelease(version);
		this.packageService.removePackage(version);
		version.getVariablesDefaultValues().clear();
		this.artifactRepository.saveVersion(version);
		Audit.log("Deleted package of version {}", version.toString());
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param path
	 * @return
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<FileDto> getPackageFiles(String artifactName, String versionName, String path) throws ObjectNotExistException, PackageRescanException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		// path = path == null ? "/" : path;
		if (path != null && !path.endsWith("/"))
			path += "/";
		Collection<RepositoryFile> files = this.layerService.getMergedVersionFiles(version, false, path);
		return this.artifactMapper.toFileDtos(files, path, false);
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public Collection<FileDto> getSpecialFiles(String artifactName, String versionName) throws ObjectNotExistException, PackageRescanException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		Collection<RepositoryFile> files = this.layerService.getMergedVersionFiles(version, false, null);
		ArrayList<RepositoryFile> specialFiles = new ArrayList<>();
		for (RepositoryFile file : files) {
			if (file.getLayerType() != LayerType.ARTIFACT ||
					file.ignoreIntegrity() || file.ignoreVariables() || file.dontDelete() || file.isTemplatized() ||
					file.getFileOwner() != null || file.getFileGroup() != null || file.getFileMode() != null || file.getDirMode() !=
					null) {
				specialFiles.add(file);
			}
		}

		return this.artifactMapper.toFileDtos(specialFiles, null, true);
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param filePath
	 * @param dto
	 * @return
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public FileDto updatePackageFileProperties(String artifactName, String versionName, String filePath, FilePropertiesDto dto) throws ObjectNotExistException, CannotDeleteObjectInUseException, PackageRescanException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		this.artifactRepository.checkIfUsedInFrozenRelease(version);
		if (version.isPackageRescanNeeded())
			throw new PackageRescanException(artifactName);
		RepositoryFile file = version.getFile(filePath);
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
			if (file.isDirectory()) {
				String dirMode = permissions.getDirMode();
				if (dirMode != null)
					file.setDirMode(dirMode.isEmpty() ? null : Integer.parseInt(dirMode, 8));
			}
		}
		//		this.persistenceService.saveEntity(file);
		this.artifactRepository.saveVersion(version); // to update version variables
		Audit.log("Updated file {} properties in version {}", filePath, version.toString());
		file = version.getFile(filePath);
		return this.artifactMapper.toFileDto(file);
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param path
	 * @param res
	 * @throws ObjectNotExistException
	 * @throws IOException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public void viewPackageFile(String artifactName, String versionName, String path, HttpServletResponse res) throws ObjectNotExistException, IOException {
		getPackageFile(artifactName, versionName, path, res);
	}

	void getPackageFile(String artifactName, String versionName, String packagePath, HttpServletResponse res) throws IOException, ObjectNotExistException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		RepositoryFile f = version.getFile(packagePath);
		boolean hasVariables = f.isTemplatized() && !f.ignoreVariables();
		String overlayPath = this.repositoryService.getVersionOverlayPath(artifactName, versionName);
		if (this.repositoryService.existsFile(overlayPath, packagePath)) {
			InputStream in = this.repositoryService.getFileInputStream(overlayPath, packagePath);
			int len = (int) this.repositoryService.getFileLength(overlayPath, packagePath);
			if (!hasVariables)
				ServletHelper.sendFile(res, in, len, null, "text/plain");
			else {
				Pattern pattern = VariableService.getVariablePattern(version.getVarPrefixChar());
				ServletHelper.sendHighlightedText(res, in, len, pattern);
			}
		} else {
			File tempFile = File.createTempFile("kwatee", null);
			OutputStream out = new FileOutputStream(tempFile);
			String fileName = this.packageService.extractPackageFileContents(version, packagePath, out);
			if (fileName == null)
				throw new FileNotFoundException();
			if (!hasVariables)
				ServletHelper.sendFile(res, new FileInputStream(tempFile), (int) tempFile.length(), null, "text/plain");
			else {
				Pattern pattern = VariableService.getVariablePattern(version.getVarPrefixChar());
				ServletHelper.sendHighlightedText(res, new FileInputStream(tempFile), (int) tempFile.length(), pattern);
			}
			tempFile.delete();
		}
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param path
	 * @param uploadMultipart
	 * @param uploadUrl
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 * @throws PackageRescanException
	 * @throws ObjectAlreadyExistsException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void uploadPackageOverlay(String artifactName, String versionName, String path, MultipartFile uploadMultipart, String uploadUrl) throws ObjectNotExistException, CannotDeleteObjectInUseException, PackageRescanException, ObjectAlreadyExistsException {
		FileUpload upload = new FileUpload(uploadUrl, uploadMultipart, true);
		String overlayFilePath = StringUtils.strip(path.replace("\\", "/"), "/");
		if (!overlayFilePath.isEmpty()) {
			overlayFilePath += "/";
		}
		try {
			overlayFilePath += upload.getName();
			Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
			this.artifactRepository.checkIfUsedInFrozenRelease(version);
			if (version.isPackageRescanNeeded())
				throw new PackageRescanException(artifactName);
			String overlayPath = this.repositoryService.getVersionOverlayPath(artifactName, versionName);
			try (OutputStream out = this.repositoryService.getFileOutputStream(overlayPath, overlayFilePath)) {
				IOUtils.copy(upload.getInputStream(), out);
			}
			this.packageService.updateVersionOverlay(version, overlayFilePath);
			this.artifactRepository.saveVersion(version);
			Audit.log("Added overlay {} to version {}", path, version.toString());
		} catch (IOException e) {
			throw new InternalErrorException(e);
		} finally {
			FileUpload.releaseQuietly(upload);
		}
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param filePath
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void deletePackageOverlay(String artifactName, String versionName, String filePath) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		this.artifactRepository.checkIfUsedInFrozenRelease(version);
		this.packageService.removeVersionOverlay(version, filePath);
		this.artifactRepository.saveVersion(version);
		String overlayPath = this.repositoryService.getVersionOverlayPath(artifactName, versionName);
		this.repositoryService.deleteFile(overlayPath, filePath, this.repositoryService.getVersionPath(artifactName, versionName));
		Audit.log("Deleted overlay {} of version {}", filePath, version.toString());
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public VersionDto getVariables(String artifactName, String versionName) throws ObjectNotExistException, PackageRescanException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		VersionDto dto = this.artifactMapper.toShortVersionDto(version);
		Map<String, Collection<VariableReference>> variables = this.artifactRepository.getVersionVariables(version);
		Collection<VariableDto> varDtos = this.artifactMapper.toVariableDtos(variables, version.getVariablesDefaultValues(), false);
		dto.setDefaultVariableValues(varDtos);
		return dto;
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @return
	 * @throws ObjectNotExistException
	 * @throws PackageRescanException
	 */
	@Secured("ROLE_USER")
	@Transactional(readOnly = true)
	public VersionDto getExportVariables(String artifactName, String versionName) throws ObjectNotExistException, PackageRescanException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		Map<String, Collection<VariableReference>> variables = this.artifactRepository.getVersionVariables(version);
		Collection<VariableDto> varDtos = this.artifactMapper.toVariableDtos(variables, version.getVariablesDefaultValues(), true);
		VersionDto dto = new VersionDto();
		dto.setArtifactName(artifactName);
		dto.setDefaultVariableValues(varDtos);
		return dto;
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param dtos
	 * @param updateOnly
	 *            if true, don't remove or create variables
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void updateVariables(String artifactName, String versionName, Collection<VariableDto> dtos, boolean updateOnly) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		Set<VersionVariable> variables = new java.util.HashSet<VersionVariable>();
		for (VariableDto dto : dtos) {
			if (dto.getValue() != null) {
				VersionVariable var = new VersionVariable();
				var.setName(dto.getName());
				var.setDescription(dto.getDescription());
				var.setDefaultValue(dto.getValue());
				variables.add(var);
				var.setVersion(version);
			}
		}
		this.artifactRepository.checkIfUsedInFrozenRelease(version);
		if (variables != null)
			updateVersionVariablesDefaultValues(version, variables, updateOnly);
		this.artifactRepository.saveVersion(version);
		Audit.log("Updated variables of version {}", version.toString());
	}

	private void updateVersionVariablesDefaultValues(Version version, final Collection<VersionVariable> variables, boolean updateOnly) {
		if (!updateOnly) {
			/*
			* remove obsolete definitions
			*/
			CollectionUtils.filter(version.getVariablesDefaultValues(),
					new Predicate() {

						public boolean evaluate(Object v) {
							return variables.contains(v);
						}
					});
		}
		/*
		* update & add values
		*/
		for (final VersionVariable var : variables) {
			VersionVariable v = (VersionVariable) CollectionUtils.find(version.getVariablesDefaultValues(),
					new Predicate() {

						public boolean evaluate(Object v) {
							return var.equals(v);
						}
					});
			if (v == null) {
				if (!updateOnly)
					version.getVariablesDefaultValues().add(var);
			}
			else {
				v.setDefaultValue(var.getDefaultValue());
				v.setDescription(var.getDescription());
			}
		}
	}

	/**
	 * 
	 * @param artifactName
	 * @param versionName
	 * @param variablePrefixChar
	 * @throws ObjectNotExistException
	 * @throws CannotDeleteObjectInUseException
	 */
	@Secured("ROLE_USER")
	@Transactional(rollbackFor = Exception.class)
	public void setVariablePrefixChar(String artifactName, String versionName, char variablePrefixChar) throws ObjectNotExistException, CannotDeleteObjectInUseException {
		Version version = this.artifactRepository.getCheckedVersion(artifactName, versionName);
		this.artifactRepository.checkIfUsedInFrozenRelease(version);
		version.setVarPrefixChar(variablePrefixChar);
		if (version.getPackageSize() > 0) {
			Set<String> excludedExtensions = this.adminRepository.getApplicationParameters().getExcludedExtensionsAsSet();
			this.packageService.rescanPackage(version, excludedExtensions);
		}
		this.artifactRepository.saveVersion(version);
		Audit.log("Changed prefixChar of version {} to {}", version.toString(), variablePrefixChar);
	}

}