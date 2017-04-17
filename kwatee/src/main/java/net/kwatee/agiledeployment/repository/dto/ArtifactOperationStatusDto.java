package net.kwatee.agiledeployment.repository.dto;

import javax.validation.constraints.Size;

public class ArtifactOperationStatusDto {

	@Size(max = 50)
	private String artifact;
	private String status;

	public String getArtifact() {
		return this.artifact;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
