package com.suse.tomcat.changes.changelog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

class ChangelogTest {

    private Changelog changelog;

    @BeforeEach
    void setUp() {
        changelog = new Changelog();
    }

    @Test
    void testAddChangelog_SingleEntry() {
        changelog.addChangelog("catalina", "Fix", "Fixed a bug");

        Map<String, List<ChangelogRecord>> records = changelog.getChangelog();
        assertEquals(1, records.size());
        assertTrue(records.containsKey("catalina"));
        assertEquals(1, records.get("catalina").size());

        ChangelogRecord record = records.get("catalina").get(0);
        assertEquals("Fix", record.getType());
        assertEquals("Fixed a bug", record.getDescription());
    }

    @Test
    void testAddChangelog_MultipleEntriesSameCategory() {
        changelog.addChangelog("coyote", "Fix", "First fix");
        changelog.addChangelog("coyote", "Add", "Added feature");
        changelog.addChangelog("coyote", "Update", "Updated library");

        Map<String, List<ChangelogRecord>> records = changelog.getChangelog();
        assertEquals(1, records.size());
        assertEquals(3, records.get("coyote").size());

        assertEquals("First fix", records.get("coyote").get(0).getDescription());
        assertEquals("Added feature", records.get("coyote").get(1).getDescription());
        assertEquals("Updated library", records.get("coyote").get(2).getDescription());
    }

    @Test
    void testAddChangelog_MultipleCategoriesAlphabeticalOrder() {
        changelog.addChangelog("jasper", "Fix", "JSP fix");
        changelog.addChangelog("catalina", "Add", "Catalina feature");
        changelog.addChangelog("coyote", "Update", "Coyote update");

        Map<String, List<ChangelogRecord>> records = changelog.getChangelog();
        assertEquals(3, records.size());

        // TreeMap should keep keys in alphabetical order
        List<String> keys = List.copyOf(records.keySet());
        assertEquals("catalina", keys.get(0));
        assertEquals("coyote", keys.get(1));
        assertEquals("jasper", keys.get(2));
    }

    @Test
    void testGetChangelog_EmptyInitially() {
        Map<String, List<ChangelogRecord>> records = changelog.getChangelog();
        assertNotNull(records);
        assertTrue(records.isEmpty());
    }

    @Test
    void testAddChangelog_DifferentTypes() {
        changelog.addChangelog("catalina", "Fix", "Bug fix");
        changelog.addChangelog("catalina", "Add", "New feature");
        changelog.addChangelog("catalina", "Update", "Library update");
        changelog.addChangelog("catalina", "Code", "Refactoring");

        List<ChangelogRecord> records = changelog.getChangelog().get("catalina");
        assertEquals(4, records.size());

        assertEquals("Fix", records.get(0).getType());
        assertEquals("Add", records.get(1).getType());
        assertEquals("Update", records.get(2).getType());
        assertEquals("Code", records.get(3).getType());
    }

    @Test
    void testAddChangelog_PreservesInsertionOrder() {
        changelog.addChangelog("catalina", "Fix", "First");
        changelog.addChangelog("catalina", "Fix", "Second");
        changelog.addChangelog("catalina", "Fix", "Third");

        List<ChangelogRecord> records = changelog.getChangelog().get("catalina");
        assertEquals("First", records.get(0).getDescription());
        assertEquals("Second", records.get(1).getDescription());
        assertEquals("Third", records.get(2).getDescription());
    }
}
