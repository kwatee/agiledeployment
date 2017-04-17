/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit;

@SuppressWarnings("serial")
public class ServerInstanceException extends Exception {

	public ServerInstanceException(Throwable exception) {
		super(exception);
	}

	public ServerInstanceException(String message) {
		super(message);
	}
}
