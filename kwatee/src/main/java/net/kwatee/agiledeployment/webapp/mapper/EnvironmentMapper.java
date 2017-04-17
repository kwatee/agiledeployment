/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kwatee.agiledeployment.common.exception.ArtifactNotInReleaseException;
import net.kwatee.agiledeployment.common.exception.CompatibilityException;
import net.kwatee.agiledeployment.common.exception.NoActiveVersionException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.PackageRescanException;
import net.kwatee.agiledeployment.core.repository.ArtifactRepository;
import net.kwatee.agiledeployment.core.repository.EnvironmentRepository;
import net.kwatee.agiledeployment.core.repository.ServerRepository;
import net.kwatee.agiledeployment.core.service.LayerService;
import net.kwatee.agiledeployment.repository.dto.ArtifactVersionDto;
import net.kwatee.agiledeployment.repository.dto.EnvironmentDto;
import net.kwatee.agiledeployment.repository.dto.FileDto;
import net.kwatee.agiledeployment.repository.dto.PermissionDto;
import net.kwatee.agiledeployment.repository.dto.ReleaseDto;
import net.kwatee.agiledeployment.repository.dto.ServerArtifactsDto;
import net.kwatee.agiledeployment.repository.dto.VariableDto;
import net.kwatee.agiledeployment.repository.dto.VariableReference;
import net.kwatee.agiledeployment.repository.entity.Artifact;
import net.kwatee.agiledeployment.repository.entity.Environment;
import net.kwatee.agiledeployment.repository.entity.EnvironmentArtifact;
import net.kwatee.agiledeployment.repository.entity.EnvironmentServer;
import net.kwatee.agiledeployment.repository.entity.Release;
import net.kwatee.agiledeployment.repository.entity.ReleaseArtifact;
import net.kwatee.agiledeployment.repository.entity.ReleaseVariable;
import net.kwatee.agiledeployment.repository.entity.RepositoryFile;
import net.kwatee.agiledeployment.repository.entity.Server;
import net.kwatee.agiledeployment.repository.entity.Version;
import net.kwatee.agiledeployment.repository.entity.VersionVariable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentMapper {

	@Autowired
	private ArtifactRepository artifactRepository;
	@Autowired
	private ServerRepository serverRepository;
	@Autowired
	private EnvironmentRepository environmentRepository;
	@Autowired
	private LayerService layerService;

	/**
	 * 
	 * @param environments
	 * @return environment dtos
	 */
	public Collection<EnvironmentDto> toShortEnvironmentDtos(Collection<Environment> environments) {
		List<Environment> sortedEnvironments = new ArrayList<>(environments);
		Collections.sort(sortedEnvironments, new Comparator<Environment>() {

			@Override
			public int compare(Environment environment1, Environment environment2) {
				return environment1.getName().compareToIgnoreCase(environment2.getName());
			}
		});
		Collection<EnvironmentDto> dtos = new ArrayList<>(sortedEnvironments.size());
		for (Environment environment : sortedEnvironments) {
			dtos.add(toShortEnvironmentDto(environment));
		}
		return dtos;
	}

	/**
	 * @param environment
	 * @return detailed environment dto
	 */
	public EnvironmentDto toEnvironmentDto(Environment environment) {
		EnvironmentDto dto = toShortEnvironmentDto(environment);
		dto.setSequentialDeployment(environment.isDeploymentSequential());
		Collection<String> serverNames = new ArrayList<>(environment.getServers().size());
		for (EnvironmentServer es : environment.getServers()) {
			serverNames.add(es.getServer().getName());
		}
		dto.setServers(serverNames);
		Collection<String> artifactNames = new ArrayList<>(environment.getArtifacts().size());
		for (EnvironmentArtifact ea : environment.getArtifacts()) {
			artifactNames.add(ea.getArtifact().getName());
		}
		dto.setArtifacts(artifactNames);
		Collection<ReleaseDto> releases = toShortReleaseDtos(environment.getReleases());
		dto.setReleases(releases);
		return dto;
	}

	private Collection<ReleaseDto> toShortReleaseDtos(Collection<Release> releases) {
		ArrayList<Release> sortedReleases = new ArrayList<>(releases);
		Collections.sort(sortedReleases, new Comparator<Release>() {

			@Override
			public int compare(Release r1, Release r2) {
				if (r1.getCreationTs() == r2.getCreationTs()) {
					return 0;
				}
				if (r1.getCreationTs() == 0) {
					return -1;
				}
				if (r2.getCreationTs() == 0 || r1.getCreationTs() < r2.getCreationTs()) {
					return 1;
				}
				return -1;
			}
		});
		Collection<ReleaseDto> dtos = new ArrayList<>(sortedReleases.size());
		for (Release release : sortedReleases) {
			ReleaseDto dto = toShortReleaseDto(release);
			dto.setEditable(release.isSnapshot());
			dtos.add(dto);
		}
		return dtos;
	}

	/**
	 * 
	 * @param release
	 * @return
	 */
	public ReleaseDto toShortReleaseDto(Release release) {
		ReleaseDto dto = new ReleaseDto();
		dto.setEnvironmentName(release.getEnvironment().getName());
		dto.setName(release.getName());
		dto.setDescription(release.getDescription());
		dto.setDisabled(release.isDisabled());
		dto.setEditable(release.isSnapshot());
		dto.setHasErrors(release.hasErrors());
		return dto;
	}

	private EnvironmentDto toShortEnvironmentDto(Environment environment) {
		EnvironmentDto dto = new EnvironmentDto();
		dto.setName(environment.getName());
		dto.setDescription(environment.getDescription());
		dto.setDisabled(environment.isDisabled());
		return dto;
	}

	/**
	 * 
	 * @param release
	 * @return short release dto
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 */
	public ReleaseDto toReleaseDto(Release release) throws CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		ReleaseDto dto = toShortReleaseDto(release);
		dto.setPreSetupAction(release.getPreSetupAction());
		dto.setPostSetupAction(release.getPostSetupAction());
		dto.setPreCleanupAction(release.getPreCleanupAction());
		dto.setPostCleanupAction(release.getPostCleanupAction());
		if (release.getFileOwner() != null || release.getFileGroup() != null || release.getFileMode() != null || release.getDirMode() != null) {
			PermissionDto permissions = new PermissionDto();
			permissions.setFileOwner(release.getFileOwner());
			permissions.setFileGroup(release.getFileGroup());
			if (release.getFileMode() != null)
				permissions.setFileMode(Integer.toOctalString(release.getFileMode()));
			if (release.getDirMode() != null)
				permissions.setDirMode(Integer.toOctalString(release.getDirMode()));
			dto.setPermissions(permissions);
		}

		/*
		 * Build the list of default versions, that is all environment artifacts with the release-defined active version
		 */
		dto.setDefaultArtifacts(toDefaultArtifactDtos(release));
		/*
		 * Build a list of release artifacts per server and put into a map
		 */
		toServerArtifactDtos(dto, release, false);
		return dto;
	}

	private Collection<ArtifactVersionDto> toDefaultArtifactDtos(Release release) throws CompatibilityException {
		Collection<ArtifactVersionDto> dtos = new ArrayList<>();
		for (EnvironmentArtifact a : release.getEnvironment().getArtifacts()) {
			Artifact artifact = a.getArtifact();
			ReleaseArtifact defaultArtifact = null;
			boolean isUsed = false;
			for (ReleaseArtifact releaseArtifact : release.getReleaseArtifacts()) {
				if (releaseArtifact.getArtifact() == artifact) {
					if (releaseArtifact.getServer() == null)
						defaultArtifact = releaseArtifact;
					else
						isUsed = true;
					if (defaultArtifact != null && isUsed)
						break;
				}
			}
			ArtifactVersionDto dto = new ArtifactVersionDto();
			dto.setArtifact(artifact.getName());
			dto.setDisabled(artifact.isDisabled());
			String versionName = (defaultArtifact == null || defaultArtifact.getVersion() == null) ? null : defaultArtifact.getVersion().getName();
			dto.setVersion(versionName);
			if (defaultArtifact != null)
				dto.setHasOverlays(defaultArtifact.hasOverlays());
			dto.setUnused(!isUsed);
			dtos.add(dto);
		}
		return dtos;
	}

	private void toServerArtifactDtos(ReleaseDto dto, Release release, boolean realActiveVersion) throws CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		Collection<ServerArtifactsDto> servers = new ArrayList<>();
		boolean emptyRelease = true;
		Collection<ArtifactVersionDto> artifactVersions = null;
		for (EnvironmentServer s : release.getEnvironment().getServers()) {
			ServerArtifactsDto serverArtifacts = new ServerArtifactsDto();
			serverArtifacts.setServer(s.getServer().getName());
			serverArtifacts.setDisabled(s.getServer().isDisabled());
			artifactVersions = new ArrayList<>();
			boolean unused = true;
			for (ReleaseArtifact releaseArtifact : release.getReleaseArtifacts()) {
				Server server = releaseArtifact.getServer();
				if (s.getServer().equals(server)) {
					Version activeVersion;
					if (realActiveVersion) {
						activeVersion = releaseArtifact.getActiveVersion();
					} else {
						activeVersion = releaseArtifact.getVersion();
					}
					ArtifactVersionDto artifactVersion = new ArtifactVersionDto();
					artifactVersion.setArtifact(releaseArtifact.getArtifact().getName());
					artifactVersion.setVersion(activeVersion == null ? null : activeVersion.getName());
					artifactVersion.setDisabled(releaseArtifact.getArtifact().isDisabled());
					artifactVersion.setHasOverlays(releaseArtifact.hasOverlays());
					artifactVersions.add(artifactVersion);
					if (!releaseArtifact.getArtifact().isDisabled() && (activeVersion == null || !activeVersion.isDisabled())) {
						unused = false;
					}
				}
			}
			serverArtifacts.setArtifacts(artifactVersions);
			serverArtifacts.setUnused(unused);
			if (!realActiveVersion || !unused) {
				servers.add(serverArtifacts);
			}
			if (!s.getServer().isDisabled() && !unused) {
				emptyRelease = false;
			}
		}
		dto.setEmpty(emptyRelease);
		dto.setServers(servers);
	}

	/**
	 * Returns artifact versions actually used in release (after resolving inherited defaults).
	 * 
	 * @param release
	 * @return artifact version dtos
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 */
	public Collection<ArtifactVersionDto> toEffectiveReleaseArtifactDtos(Release release) throws CompatibilityException, NoActiveVersionException {
		HashSet<Version> uniqueVersions = new HashSet<Version>();
		for (ReleaseArtifact releaseArtifact : release.getReleaseArtifacts()) {
			Server server = releaseArtifact.getServer();
			if (server != null) {
				uniqueVersions.add(releaseArtifact.getActiveVersion());
			}
		}
		ArrayList<Version> sortedVersions = new ArrayList<>(uniqueVersions);
		Collections.sort(sortedVersions, new Comparator<Version>() {

			@Override
			public int compare(Version version1, Version version2) {
				int comp = version1.getArtifact().getName().compareToIgnoreCase(version2.getArtifact().getName());
				if (comp != 0) {
					return comp;
				}
				return version1.getName().compareToIgnoreCase(version2.getName());
			}
		});
		Collection<ArtifactVersionDto> dtos = new ArrayList<>();
		for (Version version : sortedVersions) {
			ArtifactVersionDto dto = new ArtifactVersionDto();
			dto.setArtifact(version.getArtifact().getName());
			dto.setVersion(version.getName());
			dtos.add(dto);
		}
		return dtos;
	}

	/**
	 * 
	 * @param environments
	 * @return deployment dtos
	 * @throws ObjectNotExistException
	 */
	public Collection<EnvironmentDto> toDeploymentDtos(Collection<Environment> environments) throws ObjectNotExistException {
		List<Environment> sortedEnvironments = new ArrayList<>(environments);
		Collections.sort(sortedEnvironments, new Comparator<Environment>() {

			@Override
			public int compare(Environment environment1, Environment environment2) {
				return environment1.getName().compareToIgnoreCase(environment2.getName());
			}
		});
		Collection<EnvironmentDto> dtos = new ArrayList<>();
		for (Environment environment : sortedEnvironments) {
			EnvironmentDto dto = toShortEnvironmentDto(environment);
			Collection<Release> manageableReleases = getReleases(environment.getName(), true);
			dto.setReleases(toShortReleaseDtos(manageableReleases));
			dtos.add(dto);
		}
		return dtos;
	}

	private Collection<Release> getReleases(String environmentName, boolean manageableOnly) throws ObjectNotExistException {
		Environment environment = this.environmentRepository.getCheckedEnvironment(environmentName);
		Collection<Release> releases;
		if (!manageableOnly) {
			releases = environment.getReleases();
		} else {
			releases = new ArrayList<>(environment.getReleases());
			CollectionUtils.filter(releases,
					new Predicate() {

						public boolean evaluate(Object r) {
							return releaseIsManageable((Release) r);
						}
					});
		}
		return releases;
	}

	public boolean releaseIsManageable(Release release) {
		if (release.isDisabled() || release.hasErrors() || release.getReleaseArtifacts().size() <= 1) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param release
	 * @return detailed release dto
	 * @throws CompatibilityException
	 * @throws NoActiveVersionException
	 * @throws PackageRescanException
	 * @throws ArtifactNotInReleaseException
	 */
	public ReleaseDto toDeploymentDto(Release release) throws CompatibilityException, NoActiveVersionException, ArtifactNotInReleaseException, PackageRescanException {
		ReleaseDto dto = toShortReleaseDto(release);
		/*
		 * Build a list of release artifacts per server and put into a map
		 */
		toServerArtifactDtos(dto, release, true);
		return dto;
	}

	/**
	 * 
	 * @param files
	 * @param path
	 *            Include only files that are direct children of dirPath
	 * @return list of FileDto DTO objects from list of RepositoryFiles
	 */
	public Collection<FileDto> toFileDtos(Collection<RepositoryFile> files, String path) {
		return PackageMapper.toFileDtos(files, path, false);
	}

	/**
	 * 
	 * @param defaultValues
	 * @return variable dtos
	 */
	public Collection<VariableDto> toVersionVariableDtos(Set<VersionVariable> defaultValues) {
		ArrayList<VersionVariable> sortedVersionVars = new ArrayList<>(defaultValues);
		Collections.sort(sortedVersionVars, new Comparator<VersionVariable>() {

			@Override
			public int compare(VersionVariable v1, VersionVariable v2) {
				int c = v1.getVersion().getArtifact().getName().compareTo(v2.getVersion().getArtifact().getName());
				if (c != 0) {
					return c;
				}
				return v1.getName().compareToIgnoreCase(v2.getName());
			}

		});
		Collection<VariableDto> dtos = new ArrayList<>();
		for (VersionVariable var : sortedVersionVars) {
			if (var.getDefaultValue() != null) {
				VariableDto dto = new VariableDto();
				dto.setName(var.getName());
				dto.setDescription(var.getDescription());
				dto.setValue(var.getDefaultValue());
				dto.setArtifact(var.getVersion().getArtifact().getName());
				dtos.add(dto);
			}
		}
		return dtos;
	}

	/**
	 * 
	 * @param variables
	 * @return variable dtos
	 * @throws ObjectNotExistException
	 */
	public Collection<VariableDto> toReleaseVariableDtos(Collection<ReleaseVariable> variables, boolean onlyDefinedVariables) throws ObjectNotExistException {
		final HashMap<Long, String> artifactMap = new HashMap<>();
		final HashMap<Long, String> serverMap = new HashMap<>();
		for (ReleaseVariable var : variables) {
			if (var.getArtifactId() != null && !artifactMap.containsKey(var.getArtifactId())) {
				Artifact artifact = this.artifactRepository.getCheckedArtifact(var.getArtifactId());
				artifactMap.put(var.getArtifactId(), artifact.getName());
			}
			if (var.getServerId() != null && !serverMap.containsKey(var.getServerId())) {
				Server server = this.serverRepository.getCheckedServer(var.getServerId());
				serverMap.put(var.getServerId(), server.getName());
			}
		}

		ArrayList<ReleaseVariable> sortedReleaseVars = new ArrayList<>(variables);
		Collections.sort(sortedReleaseVars, new Comparator<ReleaseVariable>() {

			@Override
			public int compare(ReleaseVariable v1, ReleaseVariable v2) {
				int comp = v1.getName().compareToIgnoreCase(v2.getName());
				if (comp != 0) {
					return comp;
				}
				String v1ServerName = serverMap.get(v1.getServerId());
				String v2ServerName = serverMap.get(v2.getServerId());
				if (v1ServerName == null ^ v2ServerName == null) {
					return v1ServerName == null ? -1 : 1;
				}
				if (v1ServerName != null) {
					comp = v1ServerName.compareTo(v2ServerName);
					if (comp != 0) {
						return comp;
					}
				}
				String v1ArtifactName = artifactMap.get(v1.getArtifactId());
				String v2ArtifactName = artifactMap.get(v2.getArtifactId());
				if (v1ArtifactName == null ^ v2ArtifactName == null) {
					return v1ArtifactName == null ? -1 : 1;
				}
				if (v1ArtifactName == null) {
					return 0;
				}
				return v1ArtifactName.compareTo(v2ArtifactName);
			}

		});

		Collection<VariableDto> dtos = new ArrayList<>(sortedReleaseVars.size());
		for (ReleaseVariable variable : sortedReleaseVars) {
			VariableDto dto = new VariableDto();
			dto.setName(variable.getName());
			dto.setDescription(variable.getDescription());
			dto.setValue(variable.getValue());
			if (variable.getArtifactId() != null) {
				String artifactName = artifactMap.get(variable.getArtifactId());
				dto.setArtifact(artifactName);
			}
			if (variable.getServerId() != null) {
				String serverName = serverMap.get(variable.getServerId());
				dto.setServer(serverName);
			}
			// !!!variableInfo.setReadOnly(variable.isFrozenSystemProperty());
			if (!onlyDefinedVariables || dto.getValue() != null)
				dtos.add(dto);
		}
		return dtos.size() == 0 ? null : dtos;
	}

	/**
	 * 
	 * @param missingVars
	 * @return variable dtos
	 */
	public Collection<VariableDto> toMissingVariableDtos(Map<String, Collection<VariableReference>> missingVars) {
		if (MapUtils.isEmpty(missingVars))
			return null;
		ArrayList<String> sortedMissingVarNames = new ArrayList<>(missingVars.keySet());
		Collections.sort(sortedMissingVarNames);
		Collection<VariableDto> dtos = new ArrayList<>();
		for (String varName : sortedMissingVarNames) {
			VariableDto dto = new VariableDto();
			dto.setName(varName);
			dto.setReferences(missingVars.get(varName));
			dtos.add(dto);
		}
		return dtos;
	}
}
