package com.brinta.hcms.utility;

public final class LoggerUtil {

    // Masking for email and phone numbers
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