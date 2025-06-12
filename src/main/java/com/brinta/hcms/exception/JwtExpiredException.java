package com.brinta.hcms.exception;

public class JwtExpiredException extends RuntimeException {

    public JwtExpiredException(String message, Throwable cause) {
        super(message, cause);
        System.out.println("Expired JWT Token");
    }

}
