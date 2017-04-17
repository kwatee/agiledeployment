/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.repository.dto;

import javax.validation.constraints.Size;

public class CredentialsDto {

	@Size(max = 20)
	private String accessLevel;
	private Boolean promptPassword;
	@Size(max = 20)
	private String login;
	@Size(max = 80)
	private String password;
	@Size(max = 4000)
	private String pem;

	public String getAccessLevel() {
		return this.accessLevel;
	}

	public void setAccessLevel(String accessLevel) {
		this.accessLevel = accessLevel;
	}

	public String getLogin() {
		return this.login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public Boolean getPromptPassword() {
		return this.promptPassword;
	}

	public void setPromptPassword(Boolean promptPassword) {
		if (promptPassword != null)
			this.promptPassword = promptPassword;
		else
			this.promptPassword = null;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPem() {
		return this.pem;
	}

	public void setPem(String pem) {
		this.pem = pem;
	}
}
