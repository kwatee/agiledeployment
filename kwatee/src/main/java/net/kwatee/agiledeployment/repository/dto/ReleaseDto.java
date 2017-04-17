/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class ReleaseDto {

	private Boolean disabled;
	@Size(max = 50)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 255)
	private String description;
	private Boolean editable;
	private String environmentName;
	@Size(max = 2000)
	private String preSetupAction;
	@Size(max = 2000)
	private String postSetupAction;
	@Size(max = 2000)
	private String preCleanupAction;
	@Size(max = 2000)
	private String postCleanupAction;
	@Valid
	private PermissionDto permissions;
	private Collection<ArtifactVersionDto> defaultArtifacts;
	private Collection<ServerArtifactsDto> servers;
	private Boolean empty;
	private Collection<VariableDto> artifactVariables;
	private Collection<VariableDto> variables;
	private Collection<VariableDto> missingVariables;
	private boolean hasErrors;

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

	public Boolean isEditable() {
		return this.editable;
	}

	public void setEditable(Boolean editable) {
		if (editable != null && editable.booleanValue())
			this.editable = editable;
		else
			this.editable = null;
	}

	public static String getRef(String environmentName, String releaseName) {
		return releaseName + "-" + environmentName;
	}

	public String getEnvironmentName() {
		return this.environmentName;
	}

	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}

	public String getPreSetupAction() {
		return this.preSetupAction;
	}

	public void setPreSetupAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.preSetupAction = action;
		else
			this.preSetupAction = null;
	}

	public String getPostSetupAction() {
		return this.postSetupAction;
	}

	public void setPostSetupAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.postSetupAction = action;
		else
			this.postSetupAction = null;
	}

	public String getPreCleanupAction() {
		return this.preCleanupAction;
	}

	public void setPreCleanupAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.preCleanupAction = action;
		else
			this.preCleanupAction = null;
	}

	public String getPostCleanupAction() {
		return this.postCleanupAction;
	}

	public void setPostCleanupAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.postCleanupAction = action;
		else
			this.postCleanupAction = null;
	}

	public PermissionDto getPermissions() {
		return this.permissions;
	}

	public void setPermissions(PermissionDto permissions) {
		this.permissions = permissions;
	}

	public Collection<ArtifactVersionDto> getDefaultArtifacts() {
		return this.defaultArtifacts;
	}

	public void setDefaultArtifacts(Collection<ArtifactVersionDto> defaultArtifacts) {
		if (CollectionUtils.isNotEmpty(defaultArtifacts))
			this.defaultArtifacts = defaultArtifacts;
		else
			this.defaultArtifacts = null;
	}

	public Collection<ServerArtifactsDto> getServers() {
		return this.servers;
	}

	public void setServers(Collection<ServerArtifactsDto> servers) {
		if (CollectionUtils.isNotEmpty(servers))
			this.servers = servers;
		else
			this.servers = null;
	}

	public Boolean getEmpty() {
		return this.empty;
	}

	public void setEmpty(Boolean empty) {
		if (empty != null && empty.booleanValue())
			this.empty = empty;
	}

	public Collection<VariableDto> getArtifactVariables() {
		return this.artifactVariables;
	}

	public void setArtifactVariables(Collection<VariableDto> variables) {
		this.artifactVariables = variables;
	}

	public Collection<VariableDto> getVariables() {
		return this.variables;
	}

	public void setVariables(Collection<VariableDto> variables) {
		this.variables = variables;
	}

	public Collection<VariableDto> getMissingVariables() {
		return this.missingVariables;
	}

	public void setMissingVariables(Collection<VariableDto> missingVariables) {
		if (CollectionUtils.isNotEmpty(missingVariables))
			this.missingVariables = missingVariables;
		else
			this.missingVariables = null;
	}

	public String toString() {
		return ReleaseDto.getRef(this.environmentName, this.name);
	}

	public boolean hasErrors() {
		return this.hasErrors;
	}

	public void setHasErrors(boolean hasErrors) {
		this.hasErrors = hasErrors;
	}
}
