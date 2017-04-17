/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class ServerNotInReleaseException extends KwateeException {

	public ServerNotInReleaseException(String name) {
		super("Server " + name + " does not exist in release");
	}
}
