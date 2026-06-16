package com.veeva.vault.toolbox.intellij.language.vql;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reformats a VQL statement for readability: each top-level clause on its own line,
 * one {@code SELECT} field per indented line, and top-level {@code AND}/{@code OR}
 * conditions in {@code WHERE} broken onto their own lines. Clause keywords are
 * upper-cased; field and value text keep their original case.
 *
 * <p>The formatter is whitespace-, string-, and parenthesis-aware, so commas, keywords,
 * and connectors inside string literals or sub-queries are left untouched (sub-queries
 * stay inline on a single line).</p>
 */
public final class VqlFormatter {

    private VqlFormatter() {
    }

    private static final String INDENT = "    ";

    /** Single-word clauses that begin a new top-level line. {@code ORDER}/{@code GROUP} are handled as {@code ... BY}. */
    private static final Set<String> CLAUSE_KEYWORDS = Set.of(
            "select", "from", "where", "having", "limit", "offset", "skip", "pagesize", "maxrows", "find");

    /** Operator/value keywords upper-cased wherever they appear as whole words (outside strings). */
    private static final Set<String> OPERATOR_KEYWORDS = Set.of(
            "and", "or", "not", "in", "is", "like", "between", "contains", "as",
            "null", "true", "false", "asc", "desc", "nulls", "first", "last", "by");

    /**
     * @return the formatted query, or the input (whitespace-normalised) unchanged when it
     * does not look like a recognisable statement.
     */
    public static String format(String vql) {
        if (vql == null) {
            return "";
        }
        String s = normalizeWhitespace(vql).trim();
        if (s.isEmpty()) {
            return s;
        }

        List<int[]> bounds = clauseBoundaries(s);
        if (bounds.isEmpty()) {
            return s;
        }

        StringBuilder out = new StringBuilder();
        for (int b = 0; b < bounds.size(); b++) {
            int start = bounds.get(b)[0];
            int keywordLength = bounds.get(b)[1];
            int end = (b + 1 < bounds.size()) ? bounds.get(b + 1)[0] : s.length();

            String keyword = s.substring(start, start + keywordLength).toUpperCase();
            String body = s.substring(start + keywordLength, end).trim();

            if (b > 0) {
                out.append('\n');
            }
            if (keyword.equals("SELECT")) {
                out.append("SELECT");
                appendFieldList(out, body);
            } else if (keyword.equals("WHERE")) {
                out.append("WHERE");
                appendConditions(out, body);
            } else {
                out.append(keyword);
                if (!body.isEmpty()) {
                    out.append(' ').append(upperKeywords(body));
                }
            }
        }
        return out.toString();
    }

    /** Emits each top-level, comma-separated field on its own indented line. */
    private static void appendFieldList(StringBuilder out, String body) {
        List<String> fields = splitTopLevel(body, ',');
        for (int i = 0; i < fields.size(); i++) {
            String field = fields.get(i).trim();
            if (field.isEmpty()) {
                continue;
            }
            out.append('\n').append(INDENT).append(upperKeywords(field));
            if (i < fields.size() - 1) {
                out.append(',');
            }
        }
    }

    /** Emits the first condition after {@code WHERE}, then each top-level AND/OR on its own indented line. */
    private static void appendConditions(StringBuilder out, String body) {
        if (body.isEmpty()) {
            return;
        }
        List<String[]> conditions = splitConditions(body);
        for (int i = 0; i < conditions.size(); i++) {
            String connector = conditions.get(i)[0];
            String condition = conditions.get(i)[1].trim();
            if (i == 0) {
                out.append(' ').append(upperKeywords(condition));
            } else {
                out.append('\n').append(INDENT).append(connector).append(' ').append(upperKeywords(condition));
            }
        }
    }

    /** Locates the start offset and length of each top-level clause keyword, in order. */
    private static List<int[]> clauseBoundaries(String s) {
        String lower = s.toLowerCase();
        List<int[]> result = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\'') {
                    inString = false;
                }
                i++;
            } else if (c == '\'') {
                inString = true;
                i++;
            } else if (c == '(') {
                depth++;
                i++;
            } else if (c == ')') {
                depth--;
                i++;
            } else if (depth == 0 && isWordStart(c) && boundaryBefore(s, i)) {
                int j = i;
                while (j < n && isWordChar(s.charAt(j))) {
                    j++;
                }
                String word = lower.substring(i, j);
                if (word.equals("order") || word.equals("group")) {
                    int k = j;
                    while (k < n && s.charAt(k) == ' ') {
                        k++;
                    }
                    int m = k;
                    while (m < n && isWordChar(s.charAt(m))) {
                        m++;
                    }
                    if (lower.substring(k, m).equals("by")) {
                        result.add(new int[]{i, m - i});
                        i = m;
                        continue;
                    }
                } else if (CLAUSE_KEYWORDS.contains(word)) {
                    result.add(new int[]{i, j - i});
                }
                i = j;
            } else {
                i++;
            }
        }
        return result;
    }

    /** Splits on top-level {@code AND}/{@code OR}; each entry is {connector, conditionText} (first connector is empty). */
    private static List<String[]> splitConditions(String body) {
        String lower = body.toLowerCase();
        List<String[]> parts = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int last = 0;
        String connector = "";
        int i = 0;
        int n = body.length();
        while (i < n) {
            char c = body.charAt(i);
            if (inString) {
                if (c == '\'') {
                    inString = false;
                }
                i++;
            } else if (c == '\'') {
                inString = true;
                i++;
            } else if (c == '(') {
                depth++;
                i++;
            } else if (c == ')') {
                depth--;
                i++;
            } else if (depth == 0 && isWordStart(c) && boundaryBefore(body, i)) {
                int j = i;
                while (j < n && isWordChar(body.charAt(j))) {
                    j++;
                }
                String word = lower.substring(i, j);
                if (word.equals("and") || word.equals("or")) {
                    parts.add(new String[]{connector, body.substring(last, i)});
                    connector = word.toUpperCase();
                    last = j;
                }
                i = j;
            } else {
                i++;
            }
        }
        parts.add(new String[]{connector, body.substring(last)});
        return parts;
    }

    /** Splits on a top-level delimiter, ignoring delimiters inside parentheses or strings. */
    private static List<String> splitTopLevel(String input, char delimiter) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        int last = 0;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (inString) {
                if (c == '\'') {
                    inString = false;
                }
            } else if (c == '\'') {
                inString = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == delimiter && depth == 0) {
                parts.add(input.substring(last, i));
                last = i + 1;
            }
        }
        parts.add(input.substring(last));
        return parts;
    }

    /** Upper-cases {@link #OPERATOR_KEYWORDS} appearing as whole words, leaving strings and identifiers alone. */
    private static String upperKeywords(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        boolean inString = false;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (inString) {
                sb.append(c);
                if (c == '\'') {
                    inString = false;
                }
                i++;
            } else if (c == '\'') {
                inString = true;
                sb.append(c);
                i++;
            } else if (isWordStart(c) && boundaryBefore(text, i)) {
                int j = i;
                while (j < n && isWordChar(text.charAt(j))) {
                    j++;
                }
                String word = text.substring(i, j);
                sb.append(OPERATOR_KEYWORDS.contains(word.toLowerCase()) ? word.toUpperCase() : word);
                i = j;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /** Collapses runs of whitespace to a single space, preserving content inside string literals. */
    private static String normalizeWhitespace(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean inString = false;
        boolean prevSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                sb.append(c);
                if (c == '\'') {
                    inString = false;
                }
            } else if (c == '\'') {
                inString = true;
                sb.append(c);
                prevSpace = false;
            } else if (Character.isWhitespace(c)) {
                if (!prevSpace) {
                    sb.append(' ');
                    prevSpace = true;
                }
            } else {
                sb.append(c);
                prevSpace = false;
            }
        }
        return sb.toString();
    }

    private static boolean boundaryBefore(String s, int i) {
        return i == 0 || !isWordChar(s.charAt(i - 1));
    }

    private static boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
