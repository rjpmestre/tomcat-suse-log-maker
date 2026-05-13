package com.suse.tomcat.changes.changelog;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Changelog {

    private final Map<String, List<ChangelogRecord>> changelogRecords = new TreeMap<>();

    public void addChangelog(String category, String type, String description) {
        changelogRecords.computeIfAbsent(category, k -> new ArrayList<>())
        .add(new ChangelogRecord(type, description));
    }

    public Map<String, List<ChangelogRecord>> getChangelog() {
        return changelogRecords;
    }

}