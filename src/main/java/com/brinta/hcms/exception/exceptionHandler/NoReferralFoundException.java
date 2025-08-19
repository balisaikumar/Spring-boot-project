package com.brinta.hcms.exception.exceptionHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoReferralFoundException extends RuntimeException {

    public NoReferralFoundException(String message) {
        super(message);
    }

}

