package com.brinta.hcms.exception.exceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtInvalidException extends RuntimeException {

    public JwtInvalidException(String message, Throwable cause) {
        super(message, cause);
        log.warn("Invalid token passed"); // Logged in the console and log file (if configured)
    }

}
