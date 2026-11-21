package com.mountainview.sms.common;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException ex) {
        return ApiError.of(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(ConflictException ex) {
        return ApiError.of(HttpStatus.CONFLICT, ex.getMessage());
    }

    // The user is logged in, but their role fails the controller's @PreAuthorize rule.
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleForbidden(AccessDeniedException ex) {
        return ApiError.of(HttpStatus.FORBIDDEN, "Your role is not allowed to do this");
    }

    // A @Valid request body broke one or more validation rules. \
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        return ApiError.of(HttpStatus.BAD_REQUEST, "Some fields are invalid", fieldErrors);
    }

    // A business rule we check ourselves in a service was broken.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequest(IllegalArgumentException ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // The JSON could not be read: a misspelt dropdown value, a bad date, text where a number goes.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadable(HttpMessageNotReadableException ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST,
                "The request body could not be read. Check dates (YYYY-MM-DD), numbers and dropdown values.");
    }

    // A URL value was the wrong type, e.g. ?date=tomorrow or ?month=eleven.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleWrongType(MethodArgumentTypeMismatchException ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST,
                "'" + ex.getValue() + "' is not a valid value for '" + ex.getName() + "'. Dates look like 2026-11-18.");
    }

    // A required URL value was left out, e.g. no ?date= at all.
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParameter(MissingServletRequestParameterException ex) {
        return ApiError.of(HttpStatus.BAD_REQUEST, "The URL parameter '" + ex.getParameterName() + "' is required");
    }
}
