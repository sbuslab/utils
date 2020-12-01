package com.sbuslab.utils;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


public class StringUtils {

    /**
     * Convert from "under_score" to "camelCase"
     */
    public static String toCamelCase(String s) {
        StringBuilder out = new StringBuilder();
        for (String part : s.split("_")) {
            if (out.length() == 0) {
                out = new StringBuilder(part.toLowerCase());
            } else {
                out.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
            }
        }
        return out.toString();
    }

    /**
     * Convert from "camelCase" to "under_score"
     */
    public static String toUnderscore(String s) {
        final StringBuilder buf = new StringBuilder(s.replace('.', '_'));
        for (int i = 1; i < buf.length() - 1; i++) {
            if (Character.isLowerCase(buf.charAt(i - 1))
                && Character.isUpperCase(buf.charAt(i))
                && Character.isLowerCase(buf.charAt(i + 1))) {
                buf.insert(i++, '_');
            }
        }
        return buf.toString().toLowerCase();
    }

    public static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
