package org.example.jobportalappbackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One place that turns exceptions into HTTP responses.
 *
 * <p>{@code @RestControllerAdvice} applies to every {@code @RestController}. Most handlers return
 * {@link ProblemDetail}, which Spring writes as {@code application/problem+json} (RFC 9457):
 * {@code type}, {@code title}, {@code status}, {@code detail}, and {@code instance}.
 *
 * <p>Login is the exception to that rule. The sibling frontend stores {@code response.text()} as the token
 * unless the body is exactly {@code Invalid username or password}. A JSON problem body would be saved as a token.
 *
 * <p>{@code @Order} puts this advice ahead of Boot's {@code ProblemDetailsExceptionHandler}. That built-in advice
 * also handles validation failures, but its body is only "Invalid request content." and has no field list.
 * Advice beans are tried in order, and the first match wins, so ours has to be first.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String INVALID_LOGIN = "Invalid username or password";

    @ExceptionHandler(PortalException.class)
    ProblemDetail handlePortal(PortalException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception) {
            case JobNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ForbiddenJobAccessException ignored -> HttpStatus.FORBIDDEN;
            case DuplicateUsernameException ignored -> HttpStatus.CONFLICT;
        };
        return problem(status, exception.getMessage(), request);
    }

    /**
     * {@code @Valid} failed on a request body. Field names and messages go in an extra {@code errors} property.
     * Problem Details allows extension members; {@code errors} is one of those.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> handleInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        if (isLogin(request)) {
            return invalidLogin();
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Thrown when the controller class is {@code @Validated} and a constraint on a parameter fails
     * ({@code @Positive} on a path variable, for example). Field errors from a {@code @Valid} body usually
     * arrive as {@link MethodArgumentNotValidException} instead. Both are turned into the same {@code errors} map.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail handleMethodValidation(HandlerMethodValidationException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                for (FieldError fieldError : parameterErrors.getFieldErrors()) {
                    errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
                }
            } else {
                String name = result.getMethodParameter().getParameterName();
                errors.putIfAbsent(name == null ? "request" : name, firstMessage(result));
            }
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }

    /** {@code @Positive} (and similar) when method validation is not in use. */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException exception, HttpServletRequest request) {
        String detail = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .reduce((left, right) -> left + "; " + right)
                .orElse("Request validation failed");
        return problem(HttpStatus.BAD_REQUEST, detail, request);
    }

    /** JSON that is not an object, or a value that cannot be converted (for example experience: "two"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> handleUnreadable(HttpMessageNotReadableException exception, HttpServletRequest request) {
        if (isLogin(request)) {
            return invalidLogin();
        }
        String detail = "Request body could not be read";
        if (exception.getCause() instanceof InvalidFormatException invalid) {
            detail = "Invalid value for " + invalid.getPathReference();
        }
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, detail, request));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    ResponseEntity<String> handleBadCredentials() {
        return invalidLogin();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    private static ResponseEntity<String> invalidLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.TEXT_PLAIN)
                .body(INVALID_LOGIN);
    }

    private static String firstMessage(ParameterValidationResult result) {
        return result.getResolvableErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("Invalid value");
    }

    private static boolean isLogin(HttpServletRequest request) {
        return "/login".equals(request.getServletPath());
    }

    private static ProblemDetail problem(HttpStatus status, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
