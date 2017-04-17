package net.kwatee.agiledeployment.webapp.controller;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import net.kwatee.agiledeployment.common.exception.DtoValidationException;

import org.apache.commons.collections.CollectionUtils;

public class DtoValidator {

	static private Validator validator;

	static public void validate(Object o) {
		Set<ConstraintViolation<Object>> violations = getValidator().validate(o);
		if (CollectionUtils.isNotEmpty(violations)) {
			throw new DtoValidationException(violations);
		}
	}

	private static Validator getValidator() {
		if (validator == null) {
			ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
			validator = factory.getValidator();
		}
		return validator;
	}

}
