/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class VariableDto {

	@Size(max = 200)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 2000)
	private String value;
	@Size(max = 200)
	private String description;
	@Size(max = 50)
	private String artifact;
	@Size(max = 50)
	private String server;
	private Collection<VariableReference> references;

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		if (StringUtils.isNotEmpty(description))
			this.description = description;
		else
			this.description = null;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getArtifact() {
		return (StringUtils.isNotEmpty(this.artifact)) ? this.artifact : null;
	}

	public void setArtifact(String artifact) {
		if (StringUtils.isNotEmpty(artifact))
			this.artifact = artifact;
		else
			this.artifact = null;
	}

	public String getServer() {
		return (StringUtils.isNotEmpty(this.server)) ? this.server : null;
	}

	public void setServer(String server) {
		if (StringUtils.isNotEmpty(server))
			this.server = server;
		else
			this.server = null;
	}

	public Collection<VariableReference> getReferences() {
		return this.references;
	}

	public void setReferences(Collection<VariableReference> references) {
		if (CollectionUtils.isNotEmpty(references))
			this.references = references;
		else
			this.references = null;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() + (this.server == null ? 0 : this.server.hashCode()) + (this.artifact == null ? 0 : this.artifact.hashCode());
	}

	@Override
	public boolean equals(Object o) {
		VariableDto that = (VariableDto) o;
		if (that == null || !this.name.equals(that.name))
			return false;
		return StringUtils.equals(this.server, that.server) && StringUtils.equals(this.artifact, that.artifact);
	}
}
