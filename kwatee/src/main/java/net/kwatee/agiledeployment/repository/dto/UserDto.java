/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

public class UserDto {

	@Size(max = 20)
	@Pattern(regexp = "(?:[a-zA-Z0-9]||[a-zA-Z0-9][a-zA-Z0-9_\\-\\.]*)\\z", message = "invalid character")
	private String name;
	private Boolean disabled;
	@Size(max = 255)
	private String description;
	@Size(max = 40)
	private String password;
	@Size(max = 40)
	private String email;
	private Boolean operator;
	private Boolean srm;
	private Boolean admin;

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

	public String getPassword() {
		return this.password;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		if (StringUtils.isNotEmpty(email))
			this.email = email;
		else
			this.email = null;
	}

	public Boolean isOperator() {
		return this.operator;
	}

	public void setOperator(Boolean operator) {
		if (operator != null && operator.booleanValue())
			this.operator = operator;
		else
			this.operator = null;
	}

	public Boolean isSrm() {
		return this.srm;
	}

	public void setSrm(Boolean srm) {
		if (srm != null && srm.booleanValue())
			this.srm = srm;
		else
			this.srm = null;
	}

	public Boolean isAdmin() {
		return this.admin;
	}

	public void setAdmin(Boolean admin) {
		if (admin != null && admin.booleanValue())
			this.admin = admin;
		else
			this.admin = null;
	}
}
