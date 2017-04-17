/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class MissingDependencyException extends KwateeException {

	public MissingDependencyException(String serviceName) {
		super("Service " + serviceName + " cannot be instantiated");
	}
}
