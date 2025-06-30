package com.brinta.hcms.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Centralized Logger Utility used throughout the application.
 * Helps in adding traceId/requestId or any context fields (MDC) automatically.
 */
public final class LoggerUtil {

    private LoggerUtil() {
        // Prevent instantiation
    }

    /**
     * Returns SLF4J Logger instance for the given class.
     */
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

    // ---------------- MDC Message Enhancer ----------------
    private static String buildMessage(String message) {
        StringBuilder builder = new StringBuilder();

        // Add requestId if available
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            builder.append("[requestId=").append(requestId).append("] ");
        }

        // Add userId if available
        String userId = MDC.get("userId");
        if (userId != null) {
            builder.append("[userId=").append(userId).append("] ");
        }

        builder.append(message);
        return builder.toString();
    }

}