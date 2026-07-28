package com.ledger.salesmanager.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern GMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$");
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_.]{3,30}$");

    public static boolean isValidEmail(String s) { return s != null && EMAIL_PATTERN.matcher(s).matches(); }
    public static boolean isGmailAddress(String s) { return s != null && GMAIL_PATTERN.matcher(s).matches(); }
    public static boolean isValidUsername(String s) { return s != null && USERNAME_PATTERN.matcher(s).matches(); }
    public static boolean isStrongPassword(String s) { return s != null && s.length() >= 8; }
    public static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    public static boolean isNonNegativeNumber(String s) {
        if (isBlank(s)) return false;
        try {
            return Double.parseDouble(s) >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
