package com.brinta.hcms.exception;

public class JwtInvalidException extends RuntimeException {
    public JwtInvalidException(String message, Throwable cause) {
        super(message, cause);
        System.out.println("🚫 Invalid JWT Token");
    }
}

