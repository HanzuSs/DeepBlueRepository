package com.deepblue.rescue.exception;

import com.deepblue.rescue.dto.response.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones de la aplicacion a un contrato HTTP consistente.
 * Todos los handlers retornan {@code ResponseEntity<ErrorResponse>}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404: el recurso solicitado no existe
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    // 409: el JSON es valido, pero la operacion viola una regla de negocio
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // 400: fallo @Valid / Bean Validation sobre el body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        // Si un campo incumple varias reglas (p. ej. @NotBlank y @Size) el
        // orden de las violaciones no esta garantizado; se ordena por campo y
        // mensaje para que la respuesta siempre sea la misma.
        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator
                        .comparing(FieldError::getField)
                        .thenComparing(GlobalExceptionHandler::messageOf))
                .collect(Collectors.toMap(
                        FieldError::getField,
                        GlobalExceptionHandler::messageOf,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                details);
    }

    // 400: JSON mal formado o con un valor de enum inexistente
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed or invalid JSON request",
                Map.of("body", "Check JSON syntax and enum values"));
    }

    // 400: query parameter o path variable con un valor que no se puede convertir
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Map<String, String> details = Map.of(
                ex.getName(),
                "Invalid value: " + ex.getValue());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request parameter",
                details);
    }

    // 500: cualquier error inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex) {

        // Los errores propios de Spring MVC (405, 415, 404 de ruta, falta un
        // @RequestParam obligatorio, ...) ya traen su codigo HTTP. Sin esta
        // rama el handler de Exception los convertiria en un 500 enganoso.
        if (ex instanceof org.springframework.web.ErrorResponse mvcError) {
            return handleSpringMvcError(mvcError);
        }

        // El detalle solo va al log del servidor, nunca al cliente.
        log.error("Unexpected error", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                Map.of());
    }

    private ResponseEntity<ErrorResponse> handleSpringMvcError(
            org.springframework.web.ErrorResponse mvcError) {

        HttpStatusCode statusCode = mvcError.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reason = status != null ? status.getReasonPhrase() : "Error";
        String detail = mvcError.getBody().getDetail();

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                statusCode.value(),
                reason,
                detail != null ? detail : reason,
                Map.of());

        return ResponseEntity
                .status(statusCode)
                .headers(mvcError.getHeaders())
                .body(error);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            Map<String, String> details) {

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);

        return ResponseEntity
                .status(status)
                .body(error);
    }

    private static String messageOf(FieldError error) {
        return error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : "Invalid value";
    }
}
