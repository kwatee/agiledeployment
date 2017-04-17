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

public class VersionDto {

	private Boolean disabled;
	@Size(max = 50)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 255)
	private String description;
	private Collection<Integer> platforms;
	private String artifactName;
	@Valid
	private PackageDto packageInfo;
	@Size(max = 2000)
	private String preDeployAction;
	@Size(max = 2000)
	private String postDeployAction;
	@Size(max = 2000)
	private String preUndeployAction;
	@Size(max = 2000)
	private String postUndeployAction;
	@Valid
	private PermissionDto permissions;
	@Valid
	private Collection<ExecutableDto> executables;
	private Character varPrefixChar;
	private Collection<VariableDto> defaultVariableValues;
	private Boolean frozen;

	public Boolean isDisabled() {
		return this.disabled;
	}

	public void setDisabled(Boolean disabled) {
		if (disabled != null && disabled.booleanValue())
			this.disabled = disabled;
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

	public Collection<Integer> getPlatforms() {
		return this.platforms;
	}

	public void setPlatforms(Collection<Integer> platforms) {
		if (CollectionUtils.isNotEmpty(platforms))
			this.platforms = platforms;
		else
			this.platforms = null;
	}

	public String getArtifactName() {
		return this.artifactName;
	}

	public void setArtifactName(String artifactName) {
		this.artifactName = artifactName;
	}

	public PackageDto getPackageInfo() {
		return this.packageInfo;
	}

	public void setPackageInfo(PackageDto packageInfo) {
		this.packageInfo = packageInfo;
	}

	public String getPreDeployAction() {
		return this.preDeployAction;
	}

	public void setPreDeployAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.preDeployAction = action;
		else
			this.preDeployAction = null;
	}

	public String getPostDeployAction() {
		return this.postDeployAction;
	}

	public void setPostDeployAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.postDeployAction = action;
		else
			this.postDeployAction = null;
	}

	public String getPreUndeployAction() {
		return this.preUndeployAction;
	}

	public void setPreUndeployAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.preUndeployAction = action;
		else
			this.preUndeployAction = null;
	}

	public String getPostUndeployAction() {
		return this.postUndeployAction;
	}

	public void setPostUndeployAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.postUndeployAction = action;
		else
			this.postUndeployAction = null;
	}

	public PermissionDto getPermissions() {
		return this.permissions;
	}

	public void setPermissions(PermissionDto permissions) {
		this.permissions = permissions;
	}

	public Collection<ExecutableDto> getExecutables() {
		return this.executables;
	}

	public void setExecutables(Collection<ExecutableDto> executables) {
		if (CollectionUtils.isNotEmpty(executables))
			this.executables = executables;
		else
			this.executables = null;
	}

	public Character getVarPrefixChar() {
		return this.varPrefixChar;
	}

	public void setVarPrefixChar(char varPrefixChar) {
		this.varPrefixChar = varPrefixChar;
	}

	public Boolean isFrozen() {
		return this.frozen;
	}

	public void setFrozen(Boolean frozen) {
		if (frozen != null && frozen.booleanValue())
			this.frozen = frozen;
		else
			this.frozen = null;
	}

	public Collection<VariableDto> getDefaultVariableValues() {
		return this.defaultVariableValues;
	}

	public void setDefaultVariableValues(Collection<VariableDto> defaultVariableValues) {
		this.defaultVariableValues = defaultVariableValues;
	}

	public String toString() {
		return ArtifactDto.getRef(this.artifactName, this.name);
	}
}
