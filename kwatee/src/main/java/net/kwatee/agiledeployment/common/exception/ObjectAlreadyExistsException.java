/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class ObjectAlreadyExistsException extends KwateeException {

	public ObjectAlreadyExistsException(String name) {
		super(name + " already exists");
	}
}
