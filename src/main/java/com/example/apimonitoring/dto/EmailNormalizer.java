package com.example.apimonitoring.dto;

import java.util.Locale;

/** " Jan@Example.COM " and "jan@example.com" are the same account. */
final class EmailNormalizer {

    private EmailNormalizer() {
    }

    static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
