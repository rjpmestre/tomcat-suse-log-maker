package com.suse.tomcat.changes;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TomcatVersionTest {

    @Test
    void testFromVersion_Tomcat85() {
        TomcatVersion version = TomcatVersion.fromVersion("8.5.100");
        assertEquals(TomcatVersion.TOMCAT_8_5, version);
        assertEquals("8.5", version.getSeries());
        assertEquals("https://tomcat.apache.org/tomcat-8.5-doc/changelog.html", version.getChangelogUrl());
        assertEquals("https://tomcat.apache.org/security-8.html", version.getSecurityUrl());
    }

    @Test
    void testFromVersion_Tomcat90() {
        TomcatVersion version = TomcatVersion.fromVersion("9.0.117");
        assertEquals(TomcatVersion.TOMCAT_9_0, version);
        assertEquals("9.0", version.getSeries());
        assertEquals("https://tomcat.apache.org/tomcat-9.0-doc/changelog.html", version.getChangelogUrl());
    }

    @Test
    void testFromVersion_Tomcat101() {
        TomcatVersion version = TomcatVersion.fromVersion("10.1.54");
        assertEquals(TomcatVersion.TOMCAT_10_1, version);
        assertEquals("10.1", version.getSeries());
        assertEquals("https://tomcat.apache.org/tomcat-10.1-doc/changelog.html", version.getChangelogUrl());
    }

    @Test
    void testFromVersion_Tomcat110() {
        TomcatVersion version = TomcatVersion.fromVersion("11.0.21");
        assertEquals(TomcatVersion.TOMCAT_11_0, version);
        assertEquals("11.0", version.getSeries());
        assertEquals("https://tomcat.apache.org/tomcat-11.0-doc/changelog.html", version.getChangelogUrl());
    }

    @Test
    void testFromVersion_InvalidFormat_NoMinorVersion() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TomcatVersion.fromVersion("9.117");
        });
        assertTrue(exception.getMessage().contains("Invalid version format"));
    }

    @Test
    void testFromVersion_InvalidFormat_TooManyParts() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TomcatVersion.fromVersion("9.0.117.1");
        });
        assertTrue(exception.getMessage().contains("Invalid version format"));
    }

    @Test
    void testFromVersion_InvalidFormat_NonNumeric() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TomcatVersion.fromVersion("9.0.abc");
        });
        assertTrue(exception.getMessage().contains("Invalid version format"));
    }

    @Test
    void testFromVersion_UnsupportedSeries() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TomcatVersion.fromVersion("7.0.100");
        });
        assertTrue(exception.getMessage().contains("Unsupported Tomcat version series"));
        assertTrue(exception.getMessage().contains("7.0"));
    }

    @Test
    void testFromVersion_UnsupportedSeries_12x() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            TomcatVersion.fromVersion("12.0.1");
        });
        assertTrue(exception.getMessage().contains("Unsupported Tomcat version series"));
    }

    @Test
    void testToString() {
        assertEquals("Tomcat 9.0", TomcatVersion.TOMCAT_9_0.toString());
        assertEquals("Tomcat 10.1", TomcatVersion.TOMCAT_10_1.toString());
    }

    @Test
    void testAllSupportedVersions() {
        // Verify all enum values are accessible
        TomcatVersion[] versions = TomcatVersion.values();
        assertEquals(4, versions.length);

        assertEquals(TomcatVersion.TOMCAT_8_5, versions[0]);
        assertEquals(TomcatVersion.TOMCAT_9_0, versions[1]);
        assertEquals(TomcatVersion.TOMCAT_10_1, versions[2]);
        assertEquals(TomcatVersion.TOMCAT_11_0, versions[3]);
    }

    @Test
    void testSecurityUrls_AllVersions() {
        assertEquals("https://tomcat.apache.org/security-8.html", TomcatVersion.TOMCAT_8_5.getSecurityUrl());
        assertEquals("https://tomcat.apache.org/security-9.html", TomcatVersion.TOMCAT_9_0.getSecurityUrl());
        assertEquals("https://tomcat.apache.org/security-10.html", TomcatVersion.TOMCAT_10_1.getSecurityUrl());
        assertEquals("https://tomcat.apache.org/security-11.html", TomcatVersion.TOMCAT_11_0.getSecurityUrl());
    }

    @Test
    void testSecurityUrl_MatchesMajorVersion() {
        // Verify that 10.1 series uses security-10.html (major version only)
        TomcatVersion version = TomcatVersion.fromVersion("10.1.45");
        assertTrue(version.getSecurityUrl().contains("security-10.html"));
        assertFalse(version.getSecurityUrl().contains("security-10.1.html"));
    }

    @Test
    void testChangelogAndSecurityUrl_NotNull() {
        for (TomcatVersion version : TomcatVersion.values()) {
            assertNotNull(version.getChangelogUrl(), "Changelog URL should not be null for " + version);
            assertNotNull(version.getSecurityUrl(), "Security URL should not be null for " + version);
            assertTrue(version.getChangelogUrl().startsWith("https://"), "Changelog URL should use HTTPS for " + version);
            assertTrue(version.getSecurityUrl().startsWith("https://"), "Security URL should use HTTPS for " + version);
        }
    }
}
