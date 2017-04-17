/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class DescriptorOutOfDateException extends KwateeException {

	public DescriptorOutOfDateException(String message, Throwable exception) {
		super("Descriptor out of date: " + message, exception);
	}

	public DescriptorOutOfDateException(String message) {
		super("Descriptor out of date: " + message);
	}
}
