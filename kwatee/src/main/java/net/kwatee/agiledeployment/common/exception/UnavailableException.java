/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class UnavailableException extends KwateeException {

	public UnavailableException(Throwable exception) {
		super(exception);
	}

	public UnavailableException(String message, Throwable exception) {
		super(message, exception);
	}

	public UnavailableException(String message) {
		super(message);
	}
}
