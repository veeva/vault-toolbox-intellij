package com.veeva.vault.toolbox.intellij.tasks;

import com.veeva.vault.toolbox.core.utils.Date;

import java.io.File;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper methods for deriving filename suffixes from a set of log files based on the
 * date pattern embedded in their names.
 */
final class LogFileNameUtils {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final String ANALYSIS_FOLDER = "analysis";

    private LogFileNameUtils() {
    }

    /**
     * Builds a date-range suffix for an output filename based on the date stamps in
     * the filenames found within the given directory. The {@code analysis} subfolder
     * is excluded from the scan.
     *
     * @param directory the directory to scan
     * @return a suffix of the form {@code yyyy-MM-dd}, {@code yyyy-MM-dd_to_yyyy-MM-dd},
     * or a timestamp fallback if no dates are found
     */
    static String getDateRangeSuffix(File directory) {
        List<LocalDate> dates = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (ANALYSIS_FOLDER.equals(file.getName())) {
                    continue;
                }
                addParsedDate(dates, file.getName());
            }
        }
        return formatRange(dates);
    }

    /**
     * Builds a date-range suffix for an output filename based on the date stamps in
     * the given list of files.
     *
     * @param files the files to scan
     * @return a suffix of the form {@code yyyy-MM-dd}, {@code yyyy-MM-dd_to_yyyy-MM-dd},
     * or a timestamp fallback if no dates are found
     */
    static String getDateRangeSuffix(List<File> files) {
        List<LocalDate> dates = new ArrayList<>();
        for (File file : files) {
            addParsedDate(dates, file.getName());
        }
        return formatRange(dates);
    }

    /**
     * Extracts a date from the given filename and adds it to the list of dates.
     *
     * @param dates    the list of dates to add to
     * @param fileName the filename to parse
     */
    private static void addParsedDate(List<LocalDate> dates, String fileName) {
        Matcher matcher = DATE_PATTERN.matcher(fileName);
        if (matcher.find()) {
            try {
                dates.add(LocalDate.parse(matcher.group()));
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Formats a list of dates into a date-range string.
     *
     * @param dates the list of dates to format
     * @return a date-range string or a timestamp fallback
     */
    private static String formatRange(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return Date.getDateTimeAsFileName(ZonedDateTime.now());
        }
        LocalDate min = Collections.min(dates);
        LocalDate max = Collections.max(dates);
        return min.equals(max) ? min.toString() : min + "_to_" + max;
    }
}
