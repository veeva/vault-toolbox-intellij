package com.veeva.vault.toolbox.core.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Utility methods for formatting and validating dates and date-times in the
 * formats used throughout the toolbox.
 */
public final class Date {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    private static final String FILE_FORMAT = "yyyy-MM-dd-HHmmss";

    private Date() {
    }

    /**
     * Formats the given date as {@code yyyy-MM-dd}.
     *
     * @param value the date to format
     * @return the formatted date, or {@code null} if formatting fails
     */
    public static String getDateAsString(LocalDate value) {
        try {
            return value.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formats the given date-time as {@code yyyy-MM-dd HH:mm:ss,SSS}.
     *
     * @param value the date-time to format
     * @return the formatted date-time, or {@code null} if formatting fails
     */
    public static String getDateTimeAsString(ZonedDateTime value) {
        try {
            return value.format(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formats the given date-time as {@code yyyy-MM-dd-HHmmss}, suitable for
     * inclusion in a file name.
     *
     * @param value the date-time to format
     * @return the formatted date-time, or {@code null} if formatting fails
     */
    public static String getDateTimeAsFileName(ZonedDateTime value) {
        try {
            return value.format(DateTimeFormatter.ofPattern(FILE_FORMAT));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the given string can be parsed as {@code yyyy-MM-dd}.
     *
     * @param value the value to test
     * @return {@code true} if the value is a valid date; {@code false} otherwise
     */
    public static boolean isDate(String value) {
        return parseStrict(value, DATE_FORMAT);
    }

    /**
     * Returns {@code true} if the given string can be parsed as {@code yyyy-MM-dd HH:mm:ss,SSS}.
     *
     * @param value the value to test
     * @return {@code true} if the value is a valid date-time; {@code false} otherwise
     */
    public static boolean isDateTime(String value) {
        return parseStrict(value, DATETIME_FORMAT);
    }

    /**
     * Parses a Vault ISO 8601 datetime string (e.g. {@code 2024-01-15T10:30:00.000Z}) and
     * returns it formatted as {@code yyyy-MM-dd HH:mm:ss} in the system's local timezone.
     * Falls back to the raw string if parsing fails.
     *
     * @param dateStr the raw datetime string from the Vault API
     * @return a formatted, human-readable datetime string
     */
    public static String formatVaultDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "";
        try {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.parse(dateStr));
        } catch (DateTimeParseException e) {
            return dateStr;
        }
    }

    /**
     * Parses the given value strictly according to the specified pattern.
     *
     * @param value   the value to parse
     * @param pattern the pattern to use for parsing
     * @return {@code true} if the value matches the pattern and is a valid date; {@code false} otherwise
     */
    private static boolean parseStrict(String value, String pattern) {
        DateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
