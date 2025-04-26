package com.apnabaazar.apnabaazar.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ParentCategoryNotFoundException extends RuntimeException {
    public ParentCategoryNotFoundException(String message) {
        super(message);
    }
}
