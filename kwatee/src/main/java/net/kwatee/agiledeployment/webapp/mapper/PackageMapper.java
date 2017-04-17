/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.FilePropertiesDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;

public class PackageMapper {

	/**
	 * 
	 * @param files
	 * @param path
	 *            Include only files that are direct children of dirPath
	 * @return list of FileDto DTO objects from list of RepositoryFiles
	 */
	public static Collection<FileDto> toFileDtos(Collection<RepositoryFile> files, String path, boolean includePath) {
		if (files != null) {
			if (path != null && path.startsWith("/")) {
				path = path.substring(1);
			}
			return processOneDir(path, files, includePath);
		}
		return null;
	}

	static private Collection<FileDto> processOneDir(String filter, Collection<RepositoryFile> files, boolean includePath) {
		Collection<FileDto> dtos = new ArrayList<>();
		Set<String> notEmptyDirs = new HashSet<String>();
		Set<String> dirs = new HashSet<String>();
		boolean isFileFilter = filter != null && filter.length() > 0 && !filter.endsWith("/");
		int pathLen = filter == null ? 0 : filter.length();
		for (RepositoryFile layerFile : files) {
			String path = layerFile.getRelativePath();
			if (isFileFilter && !filter.equals(path)) {
				continue;
			}
			if (pathLen == 0 || (path.length() >= pathLen && path.startsWith(filter))) {
				int idx = path.indexOf('/', pathLen);
				if (idx > 0 && !isFileFilter && filter != null) {
					if (layerFile.isDirectory() && (path.length() == (idx + 1))) {
						FileDto dto = toFileDto(layerFile);
						dto.setSize(layerFile.getSize());
						if (includePath) {
							dto.setPath(path);
							dto.setVariables(layerFile.getVariables());
							dto.setSignature(layerFile.getSignature());
						}
						dtos.add(dto);
						notEmptyDirs.add(path.substring(0, idx));
					} else {
						dirs.add(path.substring(0, idx));
					}
				} else if (path.length() > pathLen) {
					FileDto dto = toFileDto(layerFile);
					dto.setSize(layerFile.getSize());
					if (includePath) {
						dto.setPath(path);
						dto.setVariables(layerFile.getVariables());
						dto.setSignature(layerFile.getSignature());
					}
					dtos.add(dto);
				}
			}
		}
		/*
		 * Patch to handle packages that did not have FileDto entries for directories
		 */
		for (String dir : dirs) {
			if (!notEmptyDirs.contains(dir)) {
				FileDto dto = new FileDto();
				dto.setDir(true);
				dto.setName(dir.substring(pathLen));
				dto.setPath(dir);
				dtos.add(dto);
			}
		}
		return dtos;
	}

	static public FileDto toFileDto(RepositoryFile layerFile) {
		FileDto dto = new FileDto();
		String path = layerFile.getRelativePath();
		int startIdx, endIdx;
		if (layerFile.isDirectory()) {
			endIdx = path.length() - 1;
			startIdx = path.lastIndexOf('/', endIdx - 1);
			dto.setDir(true);
		} else {
			endIdx = path.length();
			startIdx = path.lastIndexOf('/');
			dto.setHasVariables(layerFile.isTemplatized());
		}
		dto.setName(path.substring(startIdx + 1, endIdx));
		dto.setId(layerFile.getId());
		FilePropertiesDto properties = new FilePropertiesDto();
		properties.setIgnoreVariables(layerFile.ignoreVariables());
		properties.setIgnoreIntegrity(layerFile.ignoreIntegrity());
		properties.setDontDelete(layerFile.dontDelete());
		if (layerFile.getFileOwner() != null || layerFile.getFileGroup() != null || layerFile.getFileMode() != null || layerFile.getDirMode() != null) {
			PermissionDto permissions = new PermissionDto();
			permissions.setFileOwner(layerFile.getFileOwner());
			permissions.setFileGroup(layerFile.getFileGroup());
			if (layerFile.getFileMode() != null)
				permissions.setFileMode(Integer.toOctalString(layerFile.getFileMode()));
			if (layerFile.getDirMode() != null)
				permissions.setDirMode(Integer.toOctalString(layerFile.getDirMode()));
			properties.setPermissions(permissions);
		}
		if (layerFile.getOriginalMode() != null) {
			PermissionDto permissions = new PermissionDto();
			permissions.setFileOwner(layerFile.getOriginalOwner());
			permissions.setFileGroup(layerFile.getOriginalGroup());
			permissions.setFileMode(Integer.toOctalString(layerFile.getOriginalMode() & 0x01FF));
			properties.setOriginalPermissions(permissions);
		}
		dto.setProperties(properties);
		switch (layerFile.getLayerType()) {
			case ARTIFACT_OVERLAY:
			case COMMON_OVERLAY:
			case SERVER_OVERLAY:
				dto.setLayer(layerFile.getLayerType().ordinal());
			break;
			default:
			break;
		}
		return dto;
	}
}
