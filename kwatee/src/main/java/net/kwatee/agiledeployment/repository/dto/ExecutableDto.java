/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

public class ExecutableDto {

	@Size(max = 50)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	@Size(max = 255)
	private String description;
	@Size(max = 2000)
	private String startAction;
	@Size(max = 2000)
	private String stopAction;
	@Size(max = 2000)
	private String statusAction;

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

	public String getStartAction() {
		return this.startAction;
	}

	public void setStartAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.startAction = action;
		else
			this.startAction = null;
	}

	public String getStopAction() {
		return this.stopAction;
	}

	public void setStopAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.stopAction = action;
		else
			this.stopAction = null;
	}

	public String getStatusAction() {
		return this.statusAction;
	}

	public void setStatusAction(String action) {
		if (StringUtils.isNotEmpty(action))
			this.statusAction = action;
		else
			this.statusAction = null;
	}
}
