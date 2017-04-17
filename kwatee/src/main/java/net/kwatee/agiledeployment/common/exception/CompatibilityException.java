/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class CompatibilityException extends KwateeException {

	public CompatibilityException(String message) {
		super(message);
	}

	public CompatibilityException(String artifactName, String serverName) {
		super("Incompatible inherited version for " + artifactName + "@" + serverName);
	}
}
