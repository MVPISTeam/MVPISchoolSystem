package com.mountainview.sms.common;

// Throws this when a record would duplicate one that already exist.
public class ConflictException extends RuntimeException {
    public ConflictException (String message){
        super(message);
    }
}
