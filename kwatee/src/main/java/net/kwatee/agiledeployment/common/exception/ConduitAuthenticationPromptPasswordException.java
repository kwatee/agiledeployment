/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class ConduitAuthenticationPromptPasswordException extends ConduitAuthenticationFailedException {

	private String environmentName;
	private String serverName;
	private String login;
	private String accessLevel;

	public ConduitAuthenticationPromptPasswordException(String serverName, String login, String accessLevel, String message) {
		this(null, serverName, login, accessLevel, message);
	}

	public ConduitAuthenticationPromptPasswordException(String environmentName, String serverName, String login, String accessLevel, String message) {
		super(message);
		this.environmentName = environmentName;
		this.serverName = serverName;
		this.login = login;
		this.accessLevel = accessLevel;
	}

	public String getEnvironmentName() {
		return this.environmentName;
	}

	public String getServerName() {
		return this.serverName;
	}

	public String getLogin() {
		return this.login;
	}

	public String getAccessLevel() {
		return this.accessLevel;
	}
}
