/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class OperationInProgressException extends RuntimeException {

	public OperationInProgressException(String ref) {
		super(ref);
	}
}
