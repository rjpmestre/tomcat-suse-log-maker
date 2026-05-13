package com.suse.tomcat.changes.cve;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CveProcessorTest {

    @Test
    void testFormatCveSection_SingleCve(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        cves.add(new CveRecord("CVE-2025-55752", "Moderate", "Short title", "Short description"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        assertTrue(content.contains("* Fixed CVE:"));
        assertTrue(content.contains("+ CVE-2025-55752: Short title (bsc#)"));
    }

    @Test
    void testFormatCveSection_MultipleCves(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        cves.add(new CveRecord("CVE-2025-1", "Critical", "Title1", "Description 1"));
        cves.add(new CveRecord("CVE-2025-2", "Low", "Title2", "Description 2"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        assertTrue(content.contains("* Fixed CVEs:"));
        assertTrue(content.contains("CVE-2025-1"));
        assertTrue(content.contains("CVE-2025-2"));
        assertTrue(content.contains("(bsc#)"));
    }

    @Test
    void testFormatCveSection_LongDescription(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        String longTitle = "This is a very long title that definitely exceeds the 80 character " +
                "line length limit and should be wrapped properly with correct indentation on subsequent lines";
        cves.add(new CveRecord("CVE-2025-55752", "Moderate", longTitle, "Description text"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        String[] lines = content.split("\n");

        // Check that no line exceeds 80 characters
        for (String line : lines) {
            assertTrue(line.length() <= 80, "Line exceeds 80 characters: " + line);
        }

        // Check proper indentation (continuation lines should have 6 spaces)
        boolean foundContinuation = false;
        for (String line : lines) {
            if (!line.startsWith("  *") && !line.startsWith("    +") && !line.isEmpty()) {
                assertTrue(line.startsWith("      "), "Continuation line should start with 6 spaces: " + line);
                foundContinuation = true;
            }
        }
        assertTrue(foundContinuation, "Expected to find wrapped continuation lines");
    }

    @Test
    void testFormatCveSection_EmptyList(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        assertEquals("", content);
    }

    @Test
    void testFormatCveSection_NullList(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(null, writer);
        }

        String content = Files.readString(outputFile);
        assertEquals("", content);
    }

    @Test
    void testFormatCveSection_SingularHeader(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        cves.add(new CveRecord("CVE-2025-1", "Critical", "Single CVE title", "Description"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        // Should be singular "Fixed CVE:" not "Fixed CVEs:"
        assertTrue(content.contains("* Fixed CVE:"));
        assertFalse(content.contains("* Fixed CVEs:"));
    }

    @Test
    void testFormatCveSection_PluralHeader(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        cves.add(new CveRecord("CVE-2025-1", "Critical", "First CVE", "Description 1"));
        cves.add(new CveRecord("CVE-2025-2", "Low", "Second CVE", "Description 2"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        // Should be plural "Fixed CVEs:"
        assertTrue(content.contains("* Fixed CVEs:"));
    }

    @Test
    void testFormatCveSection_VeryLongTitle(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        String veryLongTitle = "This is an extremely long CVE title that will definitely need to be wrapped " +
                "across multiple lines because it significantly exceeds the maximum line length of 80 characters " +
                "and we need to ensure proper wrapping and indentation is applied";
        cves.add(new CveRecord("CVE-2025-99999", "Important", veryLongTitle, "Description"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        String[] lines = content.split("\n");

        // Verify no line exceeds 80 characters
        for (String line : lines) {
            assertTrue(line.length() <= 80, "Line exceeds 80 characters: '" + line + "' (length=" + line.length() + ")");
        }

        // Verify CVE ID appears
        assertTrue(content.contains("CVE-2025-99999"));
        // Verify (bsc#) placeholder appears
        assertTrue(content.contains("(bsc#)"));
    }

    @Test
    void testFormatCveSection_ExactlyFittingTitle(@TempDir Path tempDir) throws IOException {
        List<CveRecord> cves = new ArrayList<>();
        // Create a title that exactly fits on one line: "    + CVE-ID: " = 20 chars, so 60 chars left
        String exactTitle = "A".repeat(49) + " (bsc#)"; // 49 + 7 = 56 chars (fits with prefix)
        cves.add(new CveRecord("CVE-2025-12345", "Low", exactTitle.substring(0, 49), "Description"));

        Path outputFile = tempDir.resolve("output.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.toFile()))) {
            CveProcessor.formatCveSection(cves, writer);
        }

        String content = Files.readString(outputFile);
        String[] lines = content.split("\n");

        // Should fit on 2 lines: header + CVE entry
        assertEquals(2, lines.length);
        assertTrue(lines[1].length() <= 80);
    }
}
