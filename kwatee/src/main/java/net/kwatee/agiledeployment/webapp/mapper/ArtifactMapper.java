/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.repository.dto.ArtifactDto;
import net.kwatee.agiledeployment.repository.dto.ExecutableDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.PackageDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VariableReference;
import net.kwatee.agiledeployment.repository.dto.VersionDto;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Executable;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArtifactMapper {

	@Autowired
	private ArtifactRepository artifactRepository;

	/**
	 * @param artifacts
	 * @return artifact dtos
	 */
	public Collection<ArtifactDto> toShortArtifactDtos(Collection<Artifact> artifacts) {
		List<Artifact> sortedArtifacts = new ArrayList<>(artifacts);
		Collections.sort(sortedArtifacts, new Comparator<Artifact>() {

			@Override
			public int compare(Artifact artifact1, Artifact artifact2) {
				return artifact1.getName().compareToIgnoreCase(artifact2.getName());
			}
		});
		Collection<ArtifactDto> dtos = new ArrayList<>(sortedArtifacts.size());
		for (Artifact artifact : sortedArtifacts) {
			dtos.add(toShortArtifactDto(artifact));
		}
		return dtos;
	}

	/**
	 * @param artifact
	 * @return detailed artifact dto
	 */
	public ArtifactDto toArtifactDto(Artifact artifact) {
		ArtifactDto dto = (ArtifactDto) toShortArtifactDto(artifact);
		dto.setVersions(toShortVersionDtos(artifact.getVersions()));
		return dto;
	}

	private ArtifactDto toShortArtifactDto(Artifact artifact) {
		ArtifactDto dto = new ArtifactDto();
		dto.setName(artifact.getName());
		dto.setDescription(artifact.getDescription());
		dto.setDisabled(artifact.isDisabled());
		return dto;
	}

	private Collection<VersionDto> toShortVersionDtos(Collection<Version> versions) {
		List<Version> sortedVersions = new ArrayList<>(versions);
		Collections.sort(sortedVersions, new Comparator<Version>() {

			@Override
			public int compare(Version v1, Version v2) {
				if (v1.getCreationTs() < v2.getCreationTs()) {
					return 1;
				}
				if (v1.getCreationTs() > v2.getCreationTs()) {
					return -1;
				}
				return 0;
			}

		});
		Collection<VersionDto> dtos = new ArrayList<>(sortedVersions.size());
		for (Version version : sortedVersions) {
			dtos.add(toShortVersionDto(version));
		}
		return dtos;
	}

	/**
	 * 
	 * @param version
	 * @return
	 */
	public VersionDto toShortVersionDto(Version version) {
		VersionDto dto = new VersionDto();
		dto.setArtifactName(version.getArtifact().getName());
		dto.setName(version.getName());
		dto.setDescription(version.getDescription());
		dto.setDisabled(version.isDisabled());
		dto.setPlatforms(version.getPlatforms());
		if (this.artifactRepository.isVersionUsedInFrozenRelease(version))
			dto.setFrozen(true);
		return dto;
	}

	/**
	 * @param version
	 * @return detailed version dto
	 */
	public VersionDto toVersionDto(Version version) {
		VersionDto dto = toShortVersionDto(version);
		PackageDto packageInfo = toPackageDto(version);
		dto.setPackageInfo(packageInfo);
		if (packageInfo != null && version.isPackageRescanNeeded())
			packageInfo.setRescanNeeded(true);
		dto.setPreDeployAction(version.getPreDeployAction());
		dto.setPostDeployAction(version.getPostDeployAction());
		dto.setPreUndeployAction(version.getPreUndeployAction());
		dto.setPostUndeployAction(version.getPostUndeployAction());
		if (version.getFileOwner() != null || version.getFileGroup() != null || version.getFileMode() != null || version.getDirMode() != null) {
			PermissionDto permissions = new PermissionDto();
			permissions.setFileOwner(version.getFileOwner());
			permissions.setFileGroup(version.getFileGroup());
			if (version.getFileMode() != null)
				permissions.setFileMode(Integer.toOctalString(version.getFileMode()));
			if (version.getDirMode() != null)
				permissions.setDirMode(Integer.toOctalString(version.getDirMode()));
			dto.setPermissions(permissions);
		}

		List<Executable> sortedExes = new ArrayList<>(version.getExecutables());
		Collections.sort(sortedExes);
		Collection<ExecutableDto> executables = new ArrayList<>(sortedExes.size());
		for (Executable e : sortedExes) {
			ExecutableDto executable = new ExecutableDto();
			executable.setName(e.getName());
			executable.setDescription(e.getDescription());
			executable.setStartAction(e.getStartAction());
			executable.setStopAction(e.getStopAction());
			executable.setStatusAction(e.getStatusAction());
			executables.add(executable);
		}
		dto.setExecutables(executables);
		dto.setVarPrefixChar(version.getVarPrefixChar());
		return dto;
	}

	/**
	 * 
	 * @param version
	 * @return package dto
	 */
	public PackageDto toPackageDto(Version version) {
		if (version.getPackageFileName() == null)
			return null;
		PackageDto dto = new PackageDto();
		String formattedSize;
		double len = (double) version.getPackageSize();
		if (len >= 1000000000.0) {
			formattedSize = String.format("%1.1f GB", len / (1024.0 * 1024.0 * 1024.0));
		} else if (len >= 1000000.0) {
			formattedSize = String.format("%1.1f MB", len / (1024.0 * 1024.0));
		} else if (len >= 1000.0) {
			formattedSize = String.format("%1.1f KB", len / 1024.0);
		} else {
			formattedSize = String.format("%1.1f Bytes", len);
		}
		String packageName = version.getPackageFileName() == null ? StringUtils.EMPTY : version.getPackageFileName();
		dto.setName(packageName);
		dto.setSize(formattedSize);
		dto.setUploadDate(version.getPackageUploadDate());
		return dto;
	}

	/**
	 * 
	 * @param variables
	 * @param defaultValues
	 * @return
	 */
	public Collection<VariableDto> toVariableDtos(Map<String, Collection<VariableReference>> variables, Set<VersionVariable> defaultValues, boolean onlyDefinedVariables) {
		Collection<VariableDto> vars = new ArrayList<>();
		ArrayList<VersionVariable> sortedVersionVars = new ArrayList<>(defaultValues);
		Collections.sort(sortedVersionVars, new Comparator<VersionVariable>() {

			@Override
			public int compare(VersionVariable v1, VersionVariable v2) {
				return v1.getName().compareToIgnoreCase(v2.getName());
			}

		});
		ArrayList<String> sortedPackageVarNames = new ArrayList<>(variables.keySet());
		Collections.sort(sortedPackageVarNames);
		for (final String varName : sortedPackageVarNames) {
			VariableDto var = new VariableDto();
			var.setName(varName);
			var.setReferences(variables.get(varName));
			VersionVariable variable = (VersionVariable) CollectionUtils.find(sortedVersionVars, new Predicate() {

				@Override
				public boolean evaluate(Object v) {
					return ((VersionVariable) v).getName().equals(varName);
				}
			});
			if (variable != null) {
				var.setDescription(variable.getDescription());
				try {
					// Don't use JsonHelper.set because in this case we must
					// distinguish between null and StringUtils.EMPTY
					var.setValue(variable.getDefaultValue());
				} catch (JSONException e) {}
				sortedVersionVars.remove(variable);
			}
			if (!onlyDefinedVariables || var.getValue() != null)
				vars.add(var);
		}
		for (final VersionVariable variable : sortedVersionVars) {
			VariableDto var = new VariableDto();
			var.setName(variable.getName());
			var.setDescription(variable.getDescription());
			try {
				// Don't use JsonHelper.set because in this case we must
				// distinguish between null and StringUtils.EMPTY
				var.setValue(variable.getDefaultValue());
			} catch (JSONException e) {}
			if (!onlyDefinedVariables || var.getValue() != null)
				vars.add(var);
		}
		return vars;
	}

	/**
	 * 
	 * @param files
	 * @param path
	 *            Include only files that are direct children of dirPath
	 * @return list of FileDto DTO objects from list of RepositoryFiles
	 */
	public Collection<FileDto> toFileDtos(Collection<RepositoryFile> files, String path, boolean includePath) {
		return PackageMapper.toFileDtos(files, path, includePath);
	}

	/**
	 * 
	 * @param file
	 * @return
	 */
	public FileDto toFileDto(RepositoryFile file) {
		return PackageMapper.toFileDto(file);
	}
}
