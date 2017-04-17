package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

public class ServerOperationStatusDto {

	private String server;
	private String status;
	private Collection<ArtifactOperationStatusDto> artifacts;

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Collection<ArtifactOperationStatusDto> getArtifacts() {
		return this.artifacts;
	}

	public void setArtifacts(Collection<ArtifactOperationStatusDto> artifacts) {
		this.artifacts = artifacts;
	}
}
