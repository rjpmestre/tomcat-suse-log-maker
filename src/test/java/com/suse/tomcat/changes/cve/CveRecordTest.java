package com.suse.tomcat.changes.cve;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CveRecordTest {

    @Test
    void testConstructorAndGetters() {
        CveRecord record = new CveRecord(
            "CVE-2025-55752",
            "Moderate",
            "Directory traversal vulnerability",
            "Directory traversal via rewrite with possible RCE if PUT is enabled"
        );

        assertEquals("CVE-2025-55752", record.getCveId());
        assertEquals("Moderate", record.getSeverity());
        assertEquals("Directory traversal vulnerability", record.getTitle());
        assertEquals("Directory traversal via rewrite with possible RCE if PUT is enabled", record.getDescription());
    }

    @Test
    void testWithDifferentSeverities() {
        CveRecord critical = new CveRecord("CVE-2025-1", "Critical", "Critical issue", "Very serious");
        CveRecord low = new CveRecord("CVE-2025-2", "Low", "Low issue", "Not so serious");

        assertEquals("Critical", critical.getSeverity());
        assertEquals("Low", low.getSeverity());
    }

    @Test
    void testWithLongDescription() {
        String longDesc = "This is a very long description that spans multiple lines " +
                "and contains lots of technical details about the vulnerability that was discovered " +
                "and how it affects the Tomcat server.";

        CveRecord record = new CveRecord("CVE-2025-99999", "Important", "Long description", longDesc);

        assertEquals(longDesc, record.getDescription());
    }
}
