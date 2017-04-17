/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class EnvironmentDto {

	private Boolean disabled;
	@Size(max = 50)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 255)
	private String description;
	private Boolean sequentialDeployment;
	private Collection<String> servers;
	private Collection<String> artifacts;
	private Collection<ReleaseDto> releases;

	public Boolean isDisabled() {
		return this.disabled;
	}

	public void setDisabled(Boolean disabled) {
		if (disabled != null && disabled.booleanValue())
			this.disabled = disabled;
		else
			this.disabled = null;
	}

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

	public Boolean isSequentialDeployment() {
		return this.sequentialDeployment;
	}

	public void setSequentialDeployment(Boolean sequential) {
		if (sequential != null && sequential.booleanValue())
			this.sequentialDeployment = sequential;
		else
			this.sequentialDeployment = null;
	}

	public Collection<String> getServers() {
		return this.servers;
	}

	public void setServers(Collection<String> servers) {
		if (CollectionUtils.isNotEmpty(servers))
			this.servers = servers;
		else
			this.servers = null;
	}

	public Collection<String> getArtifacts() {
		return this.artifacts;
	}

	public void setArtifacts(Collection<String> artifacts) {
		if (CollectionUtils.isNotEmpty(artifacts))
			this.artifacts = artifacts;
		else
			this.artifacts = null;
	}

	public Collection<ReleaseDto> getReleases() {
		return this.releases;
	}

	public void setReleases(Collection<ReleaseDto> releases) {
		if (CollectionUtils.isNotEmpty(releases))
			this.releases = releases;
		else
			this.releases = null;
	}
}
