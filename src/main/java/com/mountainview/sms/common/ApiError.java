package com.mountainview.sms.common;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;


// The JSON body every error response uses
public record ApiError(
int status,
String error,
String message,
Map<String, String> fieldErrors,
Instant timestamp) {
    public static ApiError of(HttpStatus status, String message){
        return new ApiError(status.value(), status.getReasonPhrase(), message, Map.of(), Instant.now());
    }

    public static ApiError of(HttpStatus status, String message, Map<String, String> fieldErrors){
        return new ApiError(status.value(), status.getReasonPhrase(), message, fieldErrors, Instant.now());
    }
}
