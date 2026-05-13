package com.suse.tomcat.changes.cve;

public class CveRecord {
    private final String cveId;
    private final String severity;
    private final String title;
    private final String description;

    public CveRecord(String cveId, String severity, String title, String description) {
        this.cveId = cveId;
        this.severity = severity;
        this.title = title;
        this.description = description;
    }

    public String getCveId() {
        return cveId;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
