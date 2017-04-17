/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class NoDescriptorException extends KwateeException {

	public NoDescriptorException(String message, Throwable exception) {
		super("No cached descriptor: " + message, exception);
	}

	public NoDescriptorException(String message) {
		super("No cached descriptor: " + message);
	}
}
