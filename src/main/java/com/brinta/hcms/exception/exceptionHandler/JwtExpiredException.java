package com.brinta.hcms.exception.exceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtExpiredException extends RuntimeException {

    public JwtExpiredException(String message, Throwable cause) {
        super(message, cause);
        log.warn("Expired token passed");
    }

}
