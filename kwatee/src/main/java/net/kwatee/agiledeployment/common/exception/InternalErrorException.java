/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class InternalErrorException extends RuntimeException {

	public InternalErrorException(Throwable t) {
		super(t);
	}

	public InternalErrorException(String msg) {
		super(msg);
	}
}
