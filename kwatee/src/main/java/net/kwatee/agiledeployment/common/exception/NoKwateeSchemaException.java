/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class NoKwateeSchemaException extends SchemaException {

	public NoKwateeSchemaException() {
		super("No kwatee schema");
	}
}
