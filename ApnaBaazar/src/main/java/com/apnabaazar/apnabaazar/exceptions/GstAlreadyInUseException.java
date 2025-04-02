package com.apnabaazar.apnabaazar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class GstAlreadyInUseException extends RuntimeException {
    public GstAlreadyInUseException(String message) {
        super(message);
    }
}
