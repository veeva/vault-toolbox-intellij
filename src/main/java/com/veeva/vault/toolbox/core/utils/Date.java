package com.veeva.vault.toolbox.core.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Date {

    private static String DATE_FORMAT = "yyyy-MM-dd";
    private static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss,SSS";
    private static String FILE_FORMAT = "yyyy-MM-dd-HHmmss";

    public static String getDateAsString(LocalDate value) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        try {
            return value.format(dateTimeFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDateTimeAsString(ZonedDateTime value) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
        try {
            return value.format(dateTimeFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getDateTimeAsFileName(ZonedDateTime value) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(FILE_FORMAT);
        try {
            return value.format(dateTimeFormatter);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isDate(String value) {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(value);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public static boolean isDateTime(String value) {
        DateFormat dateFormat = new SimpleDateFormat(DATETIME_FORMAT);
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(value);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }


}
