package com.suse.tomcat.changes.changelog;

import com.suse.tomcat.changes.cve.CveRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChangelogProcessorTest {

    @Test
    void testGetTomcatChangelog_SimpleHTML() {
        String html = "<html>" +
            "<body>" +
                "<div class=\"subsection\">" +
                    "<h4>Catalina</h4>" +
                    "<div class=\"changelog\">" +
                        "<ul>" +
                            "<li><img alt=\"Fix: \" src=\"fix.gif\">Fixed a bug in servlet handling</li>" +
                            "<li><img alt=\"Add: \" src=\"add.gif\">Added new feature for sessions</li>" +
                        "</ul>" +
                    "</div>" +
                "</div>" +
            "</body>" +
            "</html>";

        Document doc = Jsoup.parse(html);
        Changelog changelog = ChangelogProcessor.getTomcatChangelog(doc, true);

        assertEquals(1, changelog.getChangelog().size());
        assertTrue(changelog.getChangelog().containsKey("catalina"));

        List<ChangelogRecord> records = changelog.getChangelog().get("catalina");
        assertEquals(2, records.size());
        assertEquals("Fix", records.get(0).getType());
        assertEquals("Fixed a bug in servlet handling", records.get(0).getDescription());
        assertEquals("Add", records.get(1).getType());
        assertEquals("Added new feature for sessions", records.get(1).getDescription());
    }

    @Test
    void testGetTomcatChangelog_MultipleCategories() {
        String html = "<html><body>" +
                "<div class=\"subsection\"><h4>Catalina</h4><div class=\"changelog\"><ul>" +
                "<li><img alt=\"Fix: \" src=\"fix.gif\">Catalina fix</li></ul></div></div>" +
                "<div class=\"subsection\"><h4>Coyote</h4><div class=\"changelog\"><ul>" +
                "<li><img alt=\"Update: \" src=\"update.gif\">Coyote update</li></ul></div></div>" +
                "<div class=\"subsection\"><h4>Jasper</h4><div class=\"changelog\"><ul>" +
                "<li><img alt=\"Add: \" src=\"add.gif\">Jasper addition</li></ul></div></div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        Changelog changelog = ChangelogProcessor.getTomcatChangelog(doc, true);

        assertEquals(3, changelog.getChangelog().size());
        assertTrue(changelog.getChangelog().containsKey("catalina"));
        assertTrue(changelog.getChangelog().containsKey("coyote"));
        assertTrue(changelog.getChangelog().containsKey("jasper"));
    }

    @Test
    void testGetTomcatChangelog_EmptyHTML() {
        String html = "<html><body></body></html>";

        Document doc = Jsoup.parse(html);
        Changelog changelog = ChangelogProcessor.getTomcatChangelog(doc, true);

        assertEquals(0, changelog.getChangelog().size());
    }

    @Test
    void testGetTomcatChangelog_MixedChangeTypes() {
        String html = "<html><body>" +
                "<div class=\"subsection\"><h4>Coyote</h4><div class=\"changelog\"><ul>" +
                "<li><img alt=\"Fix: \" src=\"fix.gif\">Bug fix</li>" +
                "<li><img alt=\"Add: \" src=\"add.gif\">New feature</li>" +
                "<li><img alt=\"Update: \" src=\"update.gif\">Library update</li>" +
                "<li><img alt=\"Code: \" src=\"code.gif\">Refactoring</li>" +
                "</ul></div></div></body></html>";

        Document doc = Jsoup.parse(html);
        Changelog changelog = ChangelogProcessor.getTomcatChangelog(doc, true);

        List<ChangelogRecord> records = changelog.getChangelog().get("coyote");
        assertEquals(4, records.size());
        assertEquals("Fix", records.get(0).getType());
        assertEquals("Add", records.get(1).getType());
        assertEquals("Update", records.get(2).getType());
        assertEquals("Code", records.get(3).getType());
    }

    @Test
    void testPrintTomcatChangelog_BasicFormatting(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("catalina", "Fix", "Fixed a bug");
        changelog.addChangelog("coyote", "Add", "Added a feature");

        Path outputFile = tempDir.resolve("output.txt");
        boolean success = ChangelogProcessor.printTomcatChangelog(changelog, new java.util.ArrayList<>(), outputFile.toString(), "10.1.48", false);

        assertTrue(success);
        assertTrue(Files.exists(outputFile));

        String content = Files.readString(outputFile);
        assertTrue(content.contains("- Update to Tomcat 10.1.48"));
        assertTrue(content.contains("* Catalina"));
        assertTrue(content.contains("+ Fix: Fixed a bug"));
        assertTrue(content.contains("* Coyote"));
        assertTrue(content.contains("+ Add: Added a feature"));
    }

    @Test
    void testPrintTomcatChangelog_LongLineWrapping(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        String longDescription = "This is a very long description that definitely exceeds the 80 character " +
                "line length limit and should be wrapped properly with correct indentation on subsequent lines";
        changelog.addChangelog("catalina", "Fix", longDescription);

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, new java.util.ArrayList<>(), outputFile.toString(), "10.1.48", false);

        String content = Files.readString(outputFile);
        String[] lines = content.split("\n");

        // Check that no line exceeds 80 characters
        for (String line : lines) {
            assertTrue(line.length() <= 80, "Line exceeds 80 characters: " + line);
        }

        // Check proper indentation (continuation lines should have 6 spaces)
        boolean foundContinuation = false;
        for (String line : lines) {
            // Skip version header, category headers, entry starts, and empty lines
            if (!line.startsWith("- ") && !line.startsWith("  *") && !line.startsWith("    +") && !line.isEmpty()) {
                assertTrue(line.startsWith("      "), "Continuation line should start with 6 spaces: " + line);
                foundContinuation = true;
            }
        }
        assertTrue(foundContinuation, "Expected to find wrapped continuation lines");
    }

    @Test
    void testPrintTomcatChangelog_OtherCategoryLast(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("other", "Update", "Other update");
        changelog.addChangelog("catalina", "Fix", "Catalina fix");
        changelog.addChangelog("coyote", "Add", "Coyote add");

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, new java.util.ArrayList<>(), outputFile.toString(), "10.1.48", false);

        String content = Files.readString(outputFile);

        // Find positions of each category
        int catalinaPos = content.indexOf("* Catalina");
        int coyotePos = content.indexOf("* Coyote");
        int otherPos = content.indexOf("* Other");

        // "other" category should appear last
        assertTrue(catalinaPos < otherPos);
        assertTrue(coyotePos < otherPos);
    }

    @Test
    void testPrintTomcatChangelog_IndentationCorrect(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("catalina", "Fix", "Short fix");

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, new java.util.ArrayList<>(), outputFile.toString(), "10.1.48", false);

        String content = Files.readString(outputFile);

        // Category should start with "  * " (2 spaces + asterisk)
        assertTrue(content.contains("  * Catalina"));

        // Entry should start with "    + " (4 spaces + plus)
        assertTrue(content.contains("    + Fix: Short fix"));
    }

    @Test
    void testLineMaxLength() {
        assertEquals(80, ChangelogProcessor.LINE_MAX_LENGTH);
    }

    @Test
    void testOtherConstant() {
        assertEquals("other", ChangelogProcessor.OTHER);
    }

    @Test
    void testPrintTomcatChangelog_WithVersionHeader(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("catalina", "Fix", "Test fix");

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, new java.util.ArrayList<>(), outputFile.toString(), "10.1.48", false);

        String content = Files.readString(outputFile);
        String[] lines = content.split("\n");

        // First line should be the version header
        assertEquals("- Update to Tomcat 10.1.48", lines[0]);
    }

    @Test
    void testPrintTomcatChangelog_WithCvesAndChangelog(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("catalina", "Fix", "Changelog fix");

        java.util.List<CveRecord> cves = new java.util.ArrayList<>();
        cves.add(new CveRecord("CVE-2025-1", "Important", "CVE title", "CVE description"));

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, cves, outputFile.toString(), "9.0.100", false);

        String content = Files.readString(outputFile);

        // Should contain version header, CVE section, and changelog section
        assertTrue(content.contains("- Update to Tomcat 9.0.100"));
        assertTrue(content.contains("* Fixed CVE:"));
        assertTrue(content.contains("CVE-2025-1"));
        assertTrue(content.contains("* Catalina"));
        assertTrue(content.contains("Changelog fix"));
    }

    @Test
    void testPrintTomcatChangelog_SkipChangelogFlag(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("catalina", "Fix", "This should not appear");
        changelog.addChangelog("coyote", "Add", "This should also not appear");

        java.util.List<CveRecord> cves = new java.util.ArrayList<>();
        cves.add(new CveRecord("CVE-2025-1", "Low", "CVE title", "Description"));

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, cves, outputFile.toString(), "11.0.5", true);

        String content = Files.readString(outputFile);

        // Should contain version header and CVEs only
        assertTrue(content.contains("- Update to Tomcat 11.0.5"));
        assertTrue(content.contains("* Fixed CVE:"));
        assertTrue(content.contains("CVE-2025-1"));

        // Should NOT contain changelog categories
        assertFalse(content.contains("* Catalina"));
        assertFalse(content.contains("* Coyote"));
        assertFalse(content.contains("This should not appear"));
    }

    @Test
    void testPrintTomcatChangelog_NoCvesButHasChangelog(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("jasper", "Update", "JSP update");

        java.util.List<CveRecord> cves = new java.util.ArrayList<>(); // Empty CVE list

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, cves, outputFile.toString(), "8.5.100", false);

        String content = Files.readString(outputFile);

        // Should have version header and changelog, but no CVE section
        assertTrue(content.contains("- Update to Tomcat 8.5.100"));
        assertTrue(content.contains("* Jasper"));
        assertFalse(content.contains("* Fixed CVE"));
    }

    @Test
    void testPrintTomcatChangelog_EmptyChangelogAndCves(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog(); // Empty

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, new java.util.ArrayList<>(), outputFile.toString(), "10.1.1", false);

        String content = Files.readString(outputFile);

        // Should only have version header
        assertEquals("- Update to Tomcat 10.1.1\n", content);
    }

    @Test
    void testPrintTomcatChangelog_MultipleCvesAndCategories(@TempDir Path tempDir) throws IOException {
        Changelog changelog = new Changelog();
        changelog.addChangelog("catalina", "Fix", "Fix 1");
        changelog.addChangelog("catalina", "Add", "Add 1");
        changelog.addChangelog("coyote", "Update", "Update 1");

        java.util.List<CveRecord> cves = new java.util.ArrayList<>();
        cves.add(new CveRecord("CVE-2025-1", "Critical", "First CVE", "Desc 1"));
        cves.add(new CveRecord("CVE-2025-2", "Moderate", "Second CVE", "Desc 2"));
        cves.add(new CveRecord("CVE-2025-3", "Low", "Third CVE", "Desc 3"));

        Path outputFile = tempDir.resolve("output.txt");
        ChangelogProcessor.printTomcatChangelog(changelog, cves, outputFile.toString(), "10.1.50", false);

        String content = Files.readString(outputFile);

        // Check structure
        assertTrue(content.contains("- Update to Tomcat 10.1.50"));
        assertTrue(content.contains("* Fixed CVEs:")); // Plural
        assertTrue(content.contains("CVE-2025-1"));
        assertTrue(content.contains("CVE-2025-2"));
        assertTrue(content.contains("CVE-2025-3"));
        assertTrue(content.contains("* Catalina"));
        assertTrue(content.contains("* Coyote"));

        // Verify ordering: version header, then CVEs, then changelog
        int versionPos = content.indexOf("- Update to Tomcat");
        int cvePos = content.indexOf("* Fixed CVEs:");
        int catalinaPos = content.indexOf("* Catalina");
        int coyotePos = content.indexOf("* Coyote");

        assertTrue(versionPos < cvePos);
        assertTrue(cvePos < catalinaPos);
        assertTrue(catalinaPos < coyotePos);
    }
}
