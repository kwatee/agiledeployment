/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class OperationNotExistException extends KwateeException {

	public OperationNotExistException() {
		super("Deploy operation not found");
	}
}
