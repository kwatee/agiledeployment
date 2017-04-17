/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public abstract class KwateeException extends Exception {

	public KwateeException(Throwable exception) {
		super(exception);
	}

	public KwateeException(String message, Throwable exception) {
		super(message, exception);
	}

	public KwateeException(String message) {
		super(message);
	}
}
