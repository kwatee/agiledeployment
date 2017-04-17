/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class ConduitAuthenticationFailedException extends KwateeException {

	public ConduitAuthenticationFailedException(String message) {
		super(message);
	}

	public ConduitAuthenticationFailedException(String deploymentRef, String environmentName, String serverName, String login, String accessLevel, String message) {
		super(message);
	}
}
