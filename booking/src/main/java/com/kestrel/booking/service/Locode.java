package com.kestrel.booking.service;

import java.util.Locale;
import java.util.regex.Pattern;

/** Helpers for 5-character UN/LOCODE endpoints such as {@code NLRTM} or {@code SGSIN}. */
public final class Locode {

    public static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}[A-Z0-9]{3}$");

    private Locode() {
    }

    public static String normalize(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String locode) {
        return locode != null && FORMAT.matcher(locode).matches();
    }

    public static String country(String locode) {
        return locode.substring(0, 2);
    }

    public static boolean isDomestic(String origin, String destination) {
        return country(origin).equals(country(destination));
    }
}
