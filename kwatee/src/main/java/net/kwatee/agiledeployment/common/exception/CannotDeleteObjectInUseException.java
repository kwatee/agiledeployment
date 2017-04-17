/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class CannotDeleteObjectInUseException extends KwateeException {

	public CannotDeleteObjectInUseException(String name, String where) {
		super("Cannot alter or delete " + name + " that is referenced in " + where);
	}
}
