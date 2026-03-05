package com.taskmanager;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple CSV import / export – no external library needed.
 * Format: id, assigned_to, created_date, due_date, summary, details, status
 * Fields are double-quote escaped so commas and newlines inside details survive.
 */
public class CsvUtil {

    // CSV always stores dates as ISO yyyy-MM-dd for safe interchange
    private static final DateTimeFormatter CSV_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // Also accept MM/dd/yyyy if someone edits the CSV manually
    private static final DateTimeFormatter UI_FMT   = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final String HEADER   ="id,assigned_to,created_date,due_date,summary,details,status";

    // ── Export ────────────────────────────────────────────────────────────────

    public static void export(List<Task> tasks, File file) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            bw.write(HEADER);
            bw.newLine();
            for (Task t : tasks) {
                bw.write(buildRow(t));
                bw.newLine();
            }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Returns parsed tasks. Caller is responsible for duplicate checking via TaskDAO.exists().
     * Skips the header row and any malformed lines, collecting error messages.
     */
    public static ImportResult importFrom(File file) throws IOException {
        List<Task> tasks  = new ArrayList<>();
        List<String> errs = new ArrayList<>();
        int lineNo = 0;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (lineNo == 1) continue; // skip header
                line = line.trim();
                if (line.isEmpty()) continue;
                try {
                    tasks.add(parseLine(line));
                } catch (Exception e) {
                    errs.add("Line " + lineNo + ": " + e.getMessage());
                }
            }
        }
        return new ImportResult(tasks, errs);
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private static String buildRow(Task t) {
        return String.join(",",
            q(String.valueOf(t.getId())),
            q(t.getAssignedTo()),
            q(t.getCreatedDate().toString()),
            q(t.getDueDate().toString()),
            q(t.getSummary()),
            q(t.getDetails() == null ? "" : t.getDetails()),
            q(t.getStatus().name())
        );
    }

    /** Wrap in double-quotes; escape any embedded double-quotes as "". */
    private static String q(String v) {
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    // ── RFC-4180 parser ───────────────────────────────────────────────────────

    private static Task parseLine(String line) {
        List<String> cols = parseCSVLine(line);
        if (cols.size() < 7) throw new IllegalArgumentException("Expected 7 columns, got " + cols.size());

        // id (col 0) is informational only – DB will assign a new one on insert
        String assignedTo  = cols.get(1).trim();
        LocalDate created  = parseDate(cols.get(2).trim(), "created_date");
        LocalDate due      = parseDate(cols.get(3).trim(), "due_date");
        String summary     = cols.get(4).trim();
        String details     = cols.get(5);
        Task.Status status = parseStatus(cols.get(6).trim());

        if (assignedTo.isEmpty()) throw new IllegalArgumentException("assigned_to is empty");
        if (summary.isEmpty())    throw new IllegalArgumentException("summary is empty");

        Task t = new Task();
        t.setAssignedTo(assignedTo);
        t.setCreatedDate(created);
        t.setDueDate(due);
        t.setSummary(summary);
        t.setDetails(details);
        t.setStatus(status);
        return t;
    }

    private static List<String> parseCSVLine(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder sb  = new StringBuilder();
        boolean inQ       = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQ) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"'); i++; // escaped quote
                    } else {
                        inQ = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQ = true;
                } else if (c == ',') {
                    cols.add(sb.toString()); sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        cols.add(sb.toString());
        return cols;
    }

    private static LocalDate parseDate(String s, String field) {
        try { return LocalDate.parse(s, CSV_FMT); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(s, UI_FMT);  } catch (DateTimeParseException ignored) {}
        throw new IllegalArgumentException(field + " invalid date: " + s + " (expected yyyy-MM-dd or MM/dd/yyyy)");
    }

    private static Task.Status parseStatus(String s) {
        try { return Task.Status.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status: " + s);
        }
    }

    // ── Result carrier ────────────────────────────────────────────────────────

    public static class ImportResult {
        public final List<Task>   tasks;
        public final List<String> errors;
        public ImportResult(List<Task> tasks, List<String> errors) {
            this.tasks  = tasks;
            this.errors = errors;
        }
    }
}