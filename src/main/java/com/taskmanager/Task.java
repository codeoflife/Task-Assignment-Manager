package com.taskmanager;

import java.time.LocalDate;

public class Task {
    public enum Status { NEW, ASSIGNED, COMPLETE }

    private int id;
    private String assignedTo;
    private LocalDate createdDate;
    private LocalDate dueDate;
    private String summary;
    private String details;
    private Status status;

    public Task() {
        this.createdDate = LocalDate.now();
        this.status = Status.NEW;
    }

    public Task(int id, String assignedTo, LocalDate createdDate, LocalDate dueDate,
                String summary, String details, Status status) {
        this.id          = id;
        this.assignedTo  = assignedTo;
        this.createdDate = createdDate;
        this.dueDate     = dueDate;
        this.summary     = summary;
        this.details     = details;
        this.status      = status;
    }

    public boolean isOverdue() {
        return status != Status.COMPLETE && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }

    public boolean isDueToday() {
        return status != Status.COMPLETE && dueDate != null
                && dueDate.isEqual(LocalDate.now());
    }

    // Getters / Setters
    public int getId()                        { return id; }
    public void setId(int id)                 { this.id = id; }
    public String getAssignedTo()             { return assignedTo; }
    public void setAssignedTo(String v)       { this.assignedTo = v; }
    public LocalDate getCreatedDate()         { return createdDate; }
    public void setCreatedDate(LocalDate v)   { this.createdDate = v; }
    public LocalDate getDueDate()             { return dueDate; }
    public void setDueDate(LocalDate v)       { this.dueDate = v; }
    public String getSummary()                { return summary; }
    public void setSummary(String v)          { this.summary = v; }
    public String getDetails()                { return details; }
    public void setDetails(String v)          { this.details = v; }
    public Status getStatus()                 { return status; }
    public void setStatus(Status v)           { this.status = v; }
}