package com.nextdoor.nextdoor.domain.rentalreservation.domain.util;

public class ValidationUtils {

    public static void validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수 값입니다.");
        }
    }
}