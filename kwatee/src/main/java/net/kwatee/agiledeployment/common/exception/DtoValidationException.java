/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.common.exception;

import java.util.Set;

import javax.validation.ConstraintViolation;

@SuppressWarnings("serial")
public class DtoValidationException extends RuntimeException {

	private final Set<ConstraintViolation<Object>> violations;

	public DtoValidationException(Set<ConstraintViolation<Object>> violations) {
		super("Validation error");
		this.violations = violations;
	}

	public Set<ConstraintViolation<Object>> getViolations() {
		return this.violations;
	}
}
