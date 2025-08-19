package com.brinta.hcms.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Centralized Logger Utility used throughout the application.
 * Helps in adding traceId/requestId or any context fields (MDC) automatically,
 * and supports secure logging with masking of sensitive fields.
 */
public final class LoggerUtil {

    private LoggerUtil() {
        // Prevent instantiation
    }

    // ---------------- Get Logger ----------------
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }

    // ---------------- INFO ----------------
    public static void info(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).info(buildMessage(message), args);
    }

    // ---------------- DEBUG ----------------
    public static void debug(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).debug(buildMessage(message), args);
    }

    // ---------------- WARN ----------------
    public static void warn(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).warn(buildMessage(message), args);
    }

    // ---------------- ERROR ----------------
    public static void error(Class<?> clazz, String message, Object... args) {
        getLogger(clazz).error(buildMessage(message), args);
    }

    public static void error(Class<?> clazz, String message, Throwable throwable) {
        getLogger(clazz).error(buildMessage(message), throwable);
    }

    // ---------------- Masked INFO Logger ----------------
    public static void infoMasked(Class<?> clazz, String message, String email, String contactNumber) {
        getLogger(clazz).info(buildMessage(message),
                mask(email), mask(contactNumber));
    }

    // ---------------- MDC Enhancer ----------------
    private static String buildMessage(String message) {
        StringBuilder builder = new StringBuilder();

        String requestId = MDC.get("requestId");
        if (requestId != null) {
            builder.append("[requestId=").append(requestId).append("] ");
        }

        String userId = MDC.get("userId");
        if (userId != null) {
            builder.append("[userId=").append(userId).append("] ");
        }

        builder.append(message);
        return builder.toString();
    }

    // ---------------- Masking Method ----------------
    public static String mask(String input) {
        if (input == null || input.isEmpty()) return "***";

        if (input.contains("@")) {
            String[] parts = input.split("@");
            String namePart = parts[0];
            return (namePart.length() > 2 ? namePart.substring(0, 2) : "*") + "***@" + parts[1];
        } else if (input.matches("\\d{10}")) {
            return input.substring(0, 2) + "****" + input.substring(6);
        }

        return "***";
    }

}

