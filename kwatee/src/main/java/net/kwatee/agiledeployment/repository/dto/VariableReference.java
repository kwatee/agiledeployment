package net.kwatee.agiledeployment.repository.dto;

public class VariableReference {

	private String artifact;
	private String location;

	@SuppressWarnings("unused")
	private VariableReference() {
		// need empty constructor for deserialization
	}

	public VariableReference(String artifact, String location) {
		this.artifact = artifact;
		this.location = location;
	}

	public String getArtifact() {
		return this.artifact;
	}

	public String getLocation() {
		return this.location;
	}
}
