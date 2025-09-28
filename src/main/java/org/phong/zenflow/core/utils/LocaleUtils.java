package org.phong.zenflow.core.utils;

import java.util.Locale;

public class LocaleUtils {
    public static Locale getLocale(String localeStr) {
        try {
            if (localeStr == null || localeStr.isEmpty()) {
                return Locale.getDefault();
            }

            String[] parts = localeStr.split("[_-]");
            if (parts.length == 1) {
                return Locale.of(parts[0]);
            } else if (parts.length == 2) {
                return Locale.of(parts[0], parts[1]);
            } else {
                return Locale.getDefault();
            }
        } catch (Exception e) {
            return Locale.getDefault();
        }
    }
}
