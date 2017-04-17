/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class NoActiveVersionException extends KwateeException {

	public NoActiveVersionException(Throwable t) {
		super(t);
	}

	public NoActiveVersionException(String artifactName, String serverName) {
		super("Missing active version for " + artifactName + "@" + serverName);
	}
}
