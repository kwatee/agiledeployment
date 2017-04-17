/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

@SuppressWarnings("serial")
public class NoKwateeDBException extends SchemaException {

	public NoKwateeDBException() {
		super("Kwatee database unreachable");
	}
}
