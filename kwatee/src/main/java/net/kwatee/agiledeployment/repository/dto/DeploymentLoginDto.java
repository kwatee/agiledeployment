package net.kwatee.agiledeployment.repository.dto;

import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

public class DeploymentLoginDto {

	@Size(max = 50)
	private String environment;
	@Size(max = 50)
	private String server;
	@Size(max = 20)
	private String login;
	@Size(max = 80)
	private String password;
	@Size(max = 20)
	private String access;
	private String message;

	public String getEnvironment() {
		return this.environment;
	}

	public void setEnvironment(String environment) {
		if (StringUtils.isNotEmpty(environment))
			this.environment = environment;
		else
			this.environment = null;
	}

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		if (StringUtils.isNotEmpty(server))
			this.server = server;
		else
			this.server = null;
	}

	public String getLogin() {
		return this.login;
	}

	public void setLogin(String login) {
		if (StringUtils.isNotEmpty(login))
			this.login = login;
		else
			this.login = null;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		if (StringUtils.isNotEmpty(password))
			this.password = password;
		else
			this.password = null;
	}

	public String getAccess() {
		return this.access;
	}

	public void setAccess(String access) {
		if (StringUtils.isNotEmpty(access))
			this.access = access;
		else
			this.access = null;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		if (StringUtils.isNotEmpty(message))
			this.message = message;
		else
			this.message = null;
	}
}
