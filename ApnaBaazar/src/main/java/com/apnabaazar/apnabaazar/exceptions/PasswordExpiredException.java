package com.apnabaazar.apnabaazar.exceptions;

public class PasswordExpiredException extends RuntimeException {
    public PasswordExpiredException(String message) {
        super(message);
    }
}
