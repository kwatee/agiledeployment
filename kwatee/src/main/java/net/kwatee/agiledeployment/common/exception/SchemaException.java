/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

import org.springframework.security.core.AuthenticationException;

@SuppressWarnings("serial")
public class SchemaException extends AuthenticationException {

	public SchemaException(String message, Throwable exception) {
		super(message, exception);
	}

	public SchemaException(String message) {
		super(message);
	}
}
