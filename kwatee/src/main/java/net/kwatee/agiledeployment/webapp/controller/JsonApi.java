/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.webapp.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.ConstraintViolation;

import net.kwatee.agiledeployment.common.exception.DtoValidationException;
import net.kwatee.agiledeployment.common.exception.InternalErrorException;
import net.kwatee.agiledeployment.common.exception.KwateeException;
import net.kwatee.agiledeployment.common.exception.ObjectNotExistException;
import net.kwatee.agiledeployment.common.exception.OperationInProgressException;
import net.kwatee.agiledeployment.common.exception.UnauthorizedException;
import net.kwatee.agiledeployment.common.exception.UnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 
 * @author mac
 * 
 */
@ControllerAdvice
public class JsonApi extends ResponseEntityExceptionHandler {

	private final static Logger LOG = LoggerFactory.getLogger(JsonApi.class);

	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	@ExceptionHandler(UnavailableException.class)
	public ResponseEntity<Object> unavailableError(UnavailableException e, WebRequest request) {
		return errorResponse(HttpStatus.SERVICE_UNAVAILABLE, e, null, request);
	}

	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<Object> unauthorizedError(UnauthorizedException e, WebRequest request) {
		return errorResponse(HttpStatus.UNAUTHORIZED, e, null, request);
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(InternalErrorException.class)
	public ResponseEntity<Object> internalError(InternalErrorException e, WebRequest request) {
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e, null, request);
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(ObjectNotExistException.class)
	public ResponseEntity<Object> notFound(ObjectNotExistException e, WebRequest request) {
		return errorResponse(HttpStatus.NOT_FOUND, e, null, request);
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	@ExceptionHandler(OperationInProgressException.class)
	public ResponseEntity<Object> notFound(OperationInProgressException e, WebRequest request) {
		return errorResponse(HttpStatus.CONFLICT, e, null, request);
	}

	@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
	@ExceptionHandler(DtoValidationException.class)
	public ResponseEntity<Object> badParameterError(DtoValidationException e, WebRequest request) {
		LOG.error("Validation error", e);
		//		ModelAndView model = new ModelAndView("error/validation_template");
		//		model.addObject("status", HttpStatus.NOT_ACCEPTABLE.value());
		//		model.addObject("violations", e.getViolations());
		//		return model;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> error = new HashMap<>();
		error.put("status", HttpStatus.NOT_ACCEPTABLE.value());
		List<Map<String, String>> violations = new ArrayList<>();
		for (ConstraintViolation<Object> v : e.getViolations()) {
			Map<String, String> violation = new HashMap<>();
			violation.put("field", v.getPropertyPath().toString());
			violation.put("message", v.getMessage());
			violations.add(violation);
		}
		error.put("violations", violations);
		return handleExceptionInternal(e, error, headers, HttpStatus.NOT_ACCEPTABLE, request);

	}

	@SuppressWarnings("deprecation")
	@ResponseStatus(value = HttpStatus.METHOD_FAILURE)
	@ExceptionHandler(KwateeException.class)
	public ResponseEntity<Object> kwateeError(KwateeException e, WebRequest request) {
		return errorResponse(HttpStatus.METHOD_FAILURE, e, null, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAllException(Exception e, WebRequest request) {
		return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e, "Unexpected error", request);
	}

	private ResponseEntity<Object> errorResponse(HttpStatus status, Exception e, String message, WebRequest request) {
		LOG.error(message, e);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, Object> error = new HashMap<>();
		error.put("status", status.value());
		error.put("message", message == null ? e.getMessage() : message);
		return handleExceptionInternal(e, error, headers, status, request);

	}
}