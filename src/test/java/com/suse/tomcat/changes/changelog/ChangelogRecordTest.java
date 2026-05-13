package com.suse.tomcat.changes.changelog;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChangelogRecordTest {

    @Test
    void testConstructorAndGetters() {
        ChangelogRecord record = new ChangelogRecord("Fix", "Fixed a critical bug");

        assertEquals("Fix", record.getType());
        assertEquals("Fixed a critical bug", record.getDescription());
    }

    @Test
    void testWithEmptyStrings() {
        ChangelogRecord record = new ChangelogRecord("", "");

        assertEquals("", record.getType());
        assertEquals("", record.getDescription());
    }

    @Test
    void testWithLongDescription() {
        String longDesc = "This is a very long description that spans multiple lines " +
                "and contains lots of technical details about the fix that was applied " +
                "to resolve the issue in the Tomcat codebase.";

        ChangelogRecord record = new ChangelogRecord("Update", longDesc);

        assertEquals("Update", record.getType());
        assertEquals(longDesc, record.getDescription());
    }

    @Test
    void testDifferentTypes() {
        ChangelogRecord fix = new ChangelogRecord("Fix", "Bug fix");
        ChangelogRecord add = new ChangelogRecord("Add", "New feature");
        ChangelogRecord update = new ChangelogRecord("Update", "Library update");
        ChangelogRecord code = new ChangelogRecord("Code", "Refactoring");

        assertEquals("Fix", fix.getType());
        assertEquals("Add", add.getType());
        assertEquals("Update", update.getType());
        assertEquals("Code", code.getType());
    }
}
