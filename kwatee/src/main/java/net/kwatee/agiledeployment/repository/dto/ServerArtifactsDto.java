package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class ServerArtifactsDto {

	private Boolean disabled;
	@Size(max = 20)
	private String server;
	private Boolean unused;
	@Valid
	private Collection<ArtifactVersionDto> artifacts;

	public Boolean getDisabled() {
		return this.disabled;
	}

	public void setDisabled(Boolean disabled) {
		if (disabled != null && disabled.booleanValue())
			this.disabled = disabled;
		else
			this.disabled = null;
	}

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		if (StringUtils.isNotEmpty(server))
			this.server = server;
		else
			this.server = null;
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

	public Collection<ArtifactVersionDto> getArtifacts() {
		return this.artifacts;
	}

	public void setArtifacts(Collection<ArtifactVersionDto> artifacts) {
		if (CollectionUtils.isNotEmpty(artifacts))
			this.artifacts = artifacts;
		else
			this.artifacts = null;
	}

	public static ServerArtifactsDto findByName(Collection<ServerArtifactsDto> servers, String name) {
		for (ServerArtifactsDto serverArtifactsDto : servers) {
			if (serverArtifactsDto.getServer().equals(name))
				return serverArtifactsDto;
		}
		return null;
	}
}
