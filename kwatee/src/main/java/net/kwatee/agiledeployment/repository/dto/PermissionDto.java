/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

public class PermissionDto {

	@Size(max = 32)
	@Pattern(regexp = "([a-z_][a-z0-9_]*)?", message = "invalid owner value")
	private String fileOwner;
	@Size(max = 32)
	@Pattern(regexp = "([a-z_][a-z0-9_]*)?", message = "invalid group value")
	private String fileGroup;
	@Size(max = 3)
	@Pattern(regexp = "[0-7]*", message = "invalid octal value")
	private String fileMode;
	@Size(max = 3)
	@Pattern(regexp = "[0-7]*", message = "invalid octal value")
	private String dirMode;

	public String getFileOwner() {
		return this.fileOwner;
	}

	public void setFileOwner(String fileOwner) {
		if (StringUtils.isNotEmpty(fileOwner))
			this.fileOwner = fileOwner;
		else
			this.fileOwner = null;
	}

	public String getFileGroup() {
		return this.fileGroup;
	}

	public void setFileGroup(String fileGroup) {
		if (StringUtils.isNotEmpty(fileGroup))
			this.fileGroup = fileGroup;
		else
			this.fileGroup = null;
	}

	public String getFileMode() {
		return this.fileMode;
	}

	public void setFileMode(String fileMode) {
		if (fileMode != null)
			this.fileMode = fileMode;
		else
			this.fileMode = null;
	}

	public String getDirMode() {
		return this.dirMode;
	}

	public void setDirMode(String dirMode) {
		if (dirMode != null)
			this.dirMode = dirMode;
		else
			this.dirMode = null;
	}
}
