package com.kestrel.booking.web;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.kestrel.booking.exception.CarrierCapacityExhaustedException;
import com.kestrel.booking.exception.CarrierUnavailableException;
import com.kestrel.booking.exception.InvalidShipmentException;
import com.kestrel.booking.exception.ReservationConflictException;
import com.kestrel.booking.exception.ShipmentNotFoundException;
import com.kestrel.booking.exception.UnknownCarrierException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Maps domain and framework exceptions onto RFC 7807 problem responses. */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final PropertyNamingStrategies.SnakeCaseStrategy SNAKE =
            (PropertyNamingStrategies.SnakeCaseStrategy) PropertyNamingStrategies.SNAKE_CASE;
    private static final String TYPE_BASE = "https://errors.kestrel.example/booking/";

    @ExceptionHandler(ShipmentNotFoundException.class)
    ResponseEntity<Object> notFound(ShipmentNotFoundException ex, WebRequest request) {
        return respond(ex, HttpStatus.NOT_FOUND, "shipment-not-found", "Shipment not found", request);
    }

    @ExceptionHandler(UnknownCarrierException.class)
    ResponseEntity<Object> unknownCarrier(UnknownCarrierException ex, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "unknown-carrier",
                "Unknown carrier", ex.getMessage());
        problem.setProperty("carrier_code", ex.getCarrierCode());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(CarrierUnavailableException.class)
    ResponseEntity<Object> carrierUnavailable(CarrierUnavailableException ex, WebRequest request) {
        return respond(ex, HttpStatus.UNPROCESSABLE_ENTITY, "carrier-unavailable",
                "Carrier unavailable", request);
    }

    @ExceptionHandler(InvalidShipmentException.class)
    ResponseEntity<Object> invalidShipment(InvalidShipmentException ex, WebRequest request) {
        return respond(ex, HttpStatus.UNPROCESSABLE_ENTITY, "invalid-shipment", "Invalid shipment", request);
    }

    @ExceptionHandler(ReservationConflictException.class)
    ResponseEntity<Object> reservationConflict(ReservationConflictException ex, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "reservation-conflict",
                "Reservation conflict", ex.getMessage());
        problem.setProperty("current_status", ex.getCurrentStatus().name());
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(CarrierCapacityExhaustedException.class)
    ResponseEntity<Object> capacityExhausted(CarrierCapacityExhaustedException ex, WebRequest request) {
        long retryAfterSeconds = Math.max(1, (ex.getRetryAfter().toMillis() + 999) / 1000);
        ProblemDetail problem = problem(HttpStatus.TOO_MANY_REQUESTS, "carrier-capacity-exhausted",
                "Too many requests", ex.getMessage());
        problem.setProperty("carrier_code", ex.getCarrierCode());
        problem.setProperty("retry_after_seconds", retryAfterSeconds);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.TOO_MANY_REQUESTS, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> integrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "duplicate-reference", "Conflict",
                "a shipment with this reference already exists");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception ex, WebRequest request) {
        log.error("unhandled exception", ex);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Internal error", "unexpected error");
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(error(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError objectError : ex.getBindingResult().getGlobalErrors()) {
            errors.add(error(objectError.getObjectName(), objectError.getDefaultMessage()));
        }
        return validationFailure(ex, errors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getAllValidationResults().forEach(result -> result.getResolvableErrors().forEach(
                e -> errors.add(error(result.getMethodParameter().getParameterName(), e.getDefaultMessage()))));
        return validationFailure(ex, errors, headers, request);
    }

    private ResponseEntity<Object> validationFailure(Exception ex, List<Map<String, String>> errors,
                                                     HttpHeaders headers, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation-failed", "Validation failed",
                "request validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private ResponseEntity<Object> respond(RuntimeException ex, HttpStatus status, String slug,
                                           String title, WebRequest request) {
        return handleExceptionInternal(ex, problem(status, slug, title, ex.getMessage()),
                new HttpHeaders(), status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String slug, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_BASE + slug));
        problem.setProperty("error", detail);
        return problem;
    }

    private static Map<String, String> error(String field, String message) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("field", SNAKE.translate(field));
        entry.put("message", message);
        return entry;
    }
}
