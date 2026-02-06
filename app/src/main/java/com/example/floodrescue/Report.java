package com.example.floodrescue;

public class Report {
    private String description;
    private String type;
    private long timestamp;

    public Report() {
        // Needed for Firestore
    }

    public Report(String description, String type, long timestamp) {
        this.description = description;
        this.type = type;
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}