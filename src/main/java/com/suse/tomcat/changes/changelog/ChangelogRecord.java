package com.suse.tomcat.changes.changelog;


public class ChangelogRecord {
    private final String type;
    private final String description;

    public ChangelogRecord(String typeIn, String descriptionIn) {
        type = typeIn;
        description = descriptionIn;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}