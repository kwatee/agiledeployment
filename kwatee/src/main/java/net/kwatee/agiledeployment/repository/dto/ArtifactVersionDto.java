package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import javax.validation.constraints.Size;

public class ArtifactVersionDto {

	private Boolean disabled;
	@Size(max = 50)
	private String artifact;
	@Size(max = 50)
	private String version;
	private Boolean overlays;
	private Boolean unused;
	private Collection<FileDto> customFiles;

	public Boolean getDisabled() {
		return this.disabled;
	}

	public void setDisabled(Boolean disabled) {
		if (disabled != null && disabled.booleanValue())
			this.disabled = disabled;
		else
			this.disabled = null;
	}

	public String getArtifact() {
		return this.artifact;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Boolean hasOverlays() {
		return this.overlays;
	}

	public void setHasOverlays(Boolean hasOverlays) {
		if (hasOverlays != null && hasOverlays.booleanValue())
			this.overlays = hasOverlays;
		else
			this.overlays = null;
	}

	public Boolean getUnused() {
		return this.unused;
	}

	public void setUnused(Boolean unused) {
		if (unused != null && unused.booleanValue())
			this.unused = unused;
		else
			this.unused = null;
	}

	public Collection<FileDto> getCustomFiles() {
		return this.customFiles;
	}

	public void setCustomFiles(Collection<FileDto> customFiles) {
		this.customFiles = customFiles;
	}

	public String toString() {
		return ArtifactDto.getRef(this.artifact, this.version);
	}

	public static ArtifactVersionDto findByName(Collection<ArtifactVersionDto> artifacts, String name) {
		for (ArtifactVersionDto artifactVersion : artifacts) {
			if (artifactVersion.getArtifact().equals(name))
				return artifactVersion;
		}
		return null;
	}

}
