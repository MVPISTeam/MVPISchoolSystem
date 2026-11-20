package com.mountainview.sms.common;

// Throws this when a record the user asked for does not exist.
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
