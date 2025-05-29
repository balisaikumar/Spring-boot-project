package com.brinta.hcms.exception.exceptionHandler;

public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException (String message) {
        super(message);
    }

}

