/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class ArtifactDto {

	private Boolean disabled;
	@Size(max = 50)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 255)
	private String description;
	private Collection<VersionDto> versions;

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

	public static String getRef(String artifactName, String versionName) {
		return artifactName + "[" + versionName + "]";
	}

	public Collection<VersionDto> getVersions() {
		return this.versions;
	}

	public void setVersions(Collection<VersionDto> versions) {
		if (CollectionUtils.isNotEmpty(versions))
			this.versions = versions;
		else
			this.versions = null;
	}
}
