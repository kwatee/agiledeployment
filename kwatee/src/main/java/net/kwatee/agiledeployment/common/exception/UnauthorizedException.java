/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException() {

	}

	public UnauthorizedException(Throwable exception) {
		super(exception);
	}

	public UnauthorizedException(String message, Throwable exception) {
		super(message, exception);
	}

	public UnauthorizedException(String message) {
		super(message);
	}
}
