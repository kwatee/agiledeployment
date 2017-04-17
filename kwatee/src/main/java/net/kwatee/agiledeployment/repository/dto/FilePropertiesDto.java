/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import javax.validation.Valid;

import org.apache.commons.lang3.BooleanUtils;

public class FilePropertiesDto {

	private Boolean ignoreVariables;
	private Boolean ignoreIntegrity;
	private Boolean dontDelete;
	@Valid
	private PermissionDto permissions;
	private PermissionDto originalPermissions;
	private Boolean pristine = true;

	public Boolean getIgnoreVariables() {
		return this.ignoreVariables;
	}

	public void setIgnoreVariables(Boolean ignoreVariables) {
		if (BooleanUtils.isTrue(ignoreVariables)) {
			this.ignoreVariables = true;
			this.pristine = null;
		} else
			this.ignoreVariables = null;
	}

	public Boolean getIgnoreIntegrity() {
		return this.ignoreIntegrity;
	}

	public void setIgnoreIntegrity(Boolean ignoreIntegrity) {
		if (BooleanUtils.isTrue(ignoreIntegrity)) {
			this.ignoreIntegrity = true;
			this.pristine = null;
		} else
			this.ignoreIntegrity = null;
	}

	public Boolean getDontDelete() {
		return this.dontDelete;
	}

	public void setDontDelete(Boolean dontDelete) {
		if (BooleanUtils.isTrue(dontDelete)) {
			this.dontDelete = true;
			this.pristine = null;
		} else
			this.dontDelete = null;
	}

	public PermissionDto getPermissions() {
		return this.permissions;
	}

	public void setPermissions(PermissionDto permissions) {
		if (permissions != null)
			this.pristine = null;
		this.permissions = permissions;
	}

	public PermissionDto getOriginalPermissions() {
		return this.originalPermissions;
	}

	public void setOriginalPermissions(PermissionDto permissions) {
		this.originalPermissions = permissions;
	}

	public boolean isPristine() {
		return BooleanUtils.isTrue(this.pristine);
	}
}
