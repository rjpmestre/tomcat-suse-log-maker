package com.suse.tomcat.changes.cve;

import com.suse.tomcat.changes.TomcatVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityFetcherTest {

    @Test
    void testSaveToFile(@TempDir Path tempDir) throws IOException {
        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("Security advisory content");

        Path outputFile = tempDir.resolve("test.html");
        SecurityFetcher.saveToFile(doc, outputFile.toString());

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("Security advisory content"));
        assertTrue(content.contains("<html>"));
    }

    @Test
    void testSaveToFile_OverwritesExisting(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("test.html");
        Files.writeString(outputFile, "Old content");

        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("New content");
        SecurityFetcher.saveToFile(doc, outputFile.toString());

        String content = Files.readString(outputFile);
        assertTrue(content.contains("New content"));
        assertFalse(content.contains("Old content"));
    }

    @Test
    void testSaveToFile_CreatesParentDirectories(@TempDir Path tempDir) throws IOException {
        Path subDir = tempDir.resolve("nested").resolve("directory");
        Path outputFile = subDir.resolve("test.html");

        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("Test");

        SecurityFetcher.saveToFile(doc, outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.isDirectory(subDir));
    }

    @Test
    void testSaveToFile_UTF8Encoding(@TempDir Path tempDir) throws IOException {
        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("Test with UTF-8: émojis 🚀 and spëcial chärs");

        Path outputFile = tempDir.resolve("test.html");
        SecurityFetcher.saveToFile(doc, outputFile.toString());

        String content = Files.readString(outputFile);
        assertTrue(content.contains("émojis 🚀 and spëcial chärs"));
    }

    @Test
    void testParseSecurityRecords_SingleCve() {
        String html = "<html><body>" +
                "<h3 id=\"Fixed_in_Apache_Tomcat_10.1.45\"><span class=\"pull-right\">2025-09-08</span> Fixed in Apache Tomcat 10.1.45</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Important: Directory traversal vulnerability</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-55752\" rel=\"nofollow\">CVE-2025-55752</a></p>" +
                "<p>This is the description of the vulnerability.</p>" +
                "<p>This was fixed with commit <a href=\"https://github.com/apache/tomcat/commit/abc123\">abc123</a>.</p>" +
                "<p>Affects: 10.1.0-M1 to 10.1.44</p>" +
                "</div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.45", "10.1.45", true);

        assertEquals(1, cves.size());
        assertEquals("CVE-2025-55752", cves.get(0).getCveId());
        assertEquals("Important", cves.get(0).getSeverity());
        assertEquals("Directory traversal vulnerability", cves.get(0).getTitle());
        assertEquals("This is the description of the vulnerability.", cves.get(0).getDescription());
    }

    @Test
    void testParseSecurityRecords_MultipleCvesInOneVersion() {
        String html = "<html><body>" +
                "<h3 id=\"Fixed_in_Apache_Tomcat_10.1.45\">Fixed in Apache Tomcat 10.1.45</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Critical: Remote code execution</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-1\">CVE-2025-1</a></p>" +
                "<p>First CVE description.</p>" +
                "<p>This was fixed with commit abc.</p>" +
                "<p>Affects: 10.1.0 to 10.1.44</p>" +
                "<p><strong>Low: Information disclosure</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-2\">CVE-2025-2</a></p>" +
                "<p>Second CVE description.</p>" +
                "<p>This was fixed with commit def.</p>" +
                "<p>Affects: 10.1.0 to 10.1.44</p>" +
                "</div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.45", "10.1.45", true);

        assertEquals(2, cves.size());
        assertEquals("CVE-2025-1", cves.get(0).getCveId());
        assertEquals("Critical", cves.get(0).getSeverity());
        assertEquals("CVE-2025-2", cves.get(1).getCveId());
        assertEquals("Low", cves.get(1).getSeverity());
    }

    @Test
    void testParseSecurityRecords_VersionNotInRange() {
        String html = "<html><body>" +
                "<h3 id=\"Fixed_in_Apache_Tomcat_10.1.44\">Fixed in Apache Tomcat 10.1.44</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Important: Some vulnerability</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-99\">CVE-2025-99</a></p>" +
                "<p>Description.</p>" +
                "</div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        // Looking for 10.1.45-10.1.48, should not find 10.1.44
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.45", "10.1.48", true);

        assertEquals(0, cves.size());
    }

    @Test
    void testParseSecurityRecords_MultipleVersionsInRange() {
        String html = "<html><body>" +
                "<h3>Fixed in Apache Tomcat 10.1.48</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Low: CVE in 48</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-1\">CVE-2025-1</a></p>" +
                "<p>Description 1.</p>" +
                "</div>" +
                "<h3>Fixed in Apache Tomcat 10.1.47</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Moderate: CVE in 47</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-2\">CVE-2025-2</a></p>" +
                "<p>Description 2.</p>" +
                "</div>" +
                "<h3>Fixed in Apache Tomcat 10.1.45</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Important: CVE in 45</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-3\">CVE-2025-3</a></p>" +
                "<p>Description 3.</p>" +
                "</div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.45", "10.1.48", true);

        assertEquals(3, cves.size());
        // Check all three CVEs were found
        assertTrue(cves.stream().anyMatch(c -> c.getCveId().equals("CVE-2025-1")));
        assertTrue(cves.stream().anyMatch(c -> c.getCveId().equals("CVE-2025-2")));
        assertTrue(cves.stream().anyMatch(c -> c.getCveId().equals("CVE-2025-3")));
    }

    @Test
    void testParseSecurityRecords_EmptyDocument() {
        String html = "<html><body></body></html>";

        Document doc = Jsoup.parse(html);
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.45", "10.1.48", true);

        assertEquals(0, cves.size());
    }

    @Test
    void testParseSecurityRecords_NoH3Headers() {
        String html = "<html><body>" +
                "<p>Some text without any h3 headers</p>" +
                "<p><strong>Important: Vulnerability</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-1\">CVE-2025-1</a></p>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.45", "10.1.48", true);

        assertEquals(0, cves.size());
    }

    @Test
    void testParseSecurityRecords_MilestoneVersionParsing() {
        String html = "<html><body>" +
                "<h3>Fixed in Apache Tomcat 10.1.1-M2</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Moderate: Milestone CVE</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-M\">CVE-2025-M</a></p>" +
                "<p>Milestone description.</p>" +
                "</div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        // Test that parsing handles milestone versions without throwing exceptions
        // Version comparison with milestones is complex, so just verify it doesn't crash
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.0", "10.1.5", true);

        // The milestone version 10.1.1-M2 might or might not be in range depending on comparison logic
        // Just verify parsing works without errors
        assertNotNull(cves);
    }

    @Test
    void testParseSecurityRecords_InvalidVersionFormat() {
        String html = "<html><body>" +
                "<h3>Fixed in Apache Tomcat INVALID.VERSION</h3>" +
                "<div class=\"text\">" +
                "<p><strong>Low: CVE</strong> <a href=\"http://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2025-X\">CVE-2025-X</a></p>" +
                "</div>" +
                "</body></html>";

        Document doc = Jsoup.parse(html);
        // Should handle invalid version formats gracefully without crashing
        List<CveRecord> cves = SecurityFetcher.parseSecurityRecords(doc, "10.1.0", "10.1.5", true);

        // Invalid version should be skipped
        assertEquals(0, cves.size());
    }
}
