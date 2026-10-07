package com.libraryms.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IsbnValidator implements ConstraintValidator<ValidIsbn, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String cleaned = value.replaceAll("[-\\s]", "").trim();
        if (cleaned.length() == 10) {
            return isValidIsbn10(cleaned);
        } else if (cleaned.length() == 13) {
            return isValidIsbn13(cleaned);
        }
        return false;
    }

    public static boolean isValidIsbn10(String cleaned) {
        if (cleaned.length() != 10) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            char c = cleaned.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            sum += (c - '0') * (10 - i);
        }
        char checkChar = cleaned.charAt(9);
        int checkDigit;
        if (checkChar == 'X' || checkChar == 'x') {
            checkDigit = 10;
        } else if (Character.isDigit(checkChar)) {
            checkDigit = checkChar - '0';
        } else {
            return false;
        }
        sum += checkDigit;
        return (sum % 11) == 0;
    }

    public static boolean isValidIsbn13(String cleaned) {
        if (cleaned.length() != 13 || (!cleaned.startsWith("978") && !cleaned.startsWith("979"))) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char c = cleaned.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
            int digit = c - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (sum % 10) == 0;
    }

    /**
     * Normalizes an ISBN (10 or 13) to an official 13-digit string for database storage.
     */
    public static String normalizeToIsbn13(String rawIsbn) {
        if (rawIsbn == null) {
            return null;
        }
        String cleaned = rawIsbn.replaceAll("[-\\s]", "").trim();
        if (cleaned.length() == 13 && isValidIsbn13(cleaned)) {
            return cleaned;
        }
        if (cleaned.length() == 10 && isValidIsbn10(cleaned)) {
            String prefix = "978" + cleaned.substring(0, 9);
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                int digit = prefix.charAt(i) - '0';
                sum += (i % 2 == 0) ? digit : digit * 3;
            }
            int remainder = sum % 10;
            int checkDigit = (remainder == 0) ? 0 : 10 - remainder;
            return prefix + checkDigit;
        }
        return cleaned;
    }
}
