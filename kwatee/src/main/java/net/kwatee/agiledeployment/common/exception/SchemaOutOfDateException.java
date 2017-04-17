/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class SchemaOutOfDateException extends SchemaException {

	public SchemaOutOfDateException(String message) {
		super("Upgrade required from version " + message);
	}
}
