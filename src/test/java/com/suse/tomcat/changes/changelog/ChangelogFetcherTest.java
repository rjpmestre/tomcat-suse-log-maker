package com.suse.tomcat.changes.changelog;

import com.suse.tomcat.changes.TomcatVersion;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChangelogFetcherTest {

    @Test
    void testSaveToFile(@TempDir Path tempDir) throws IOException {
        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("Test content");

        Path outputFile = tempDir.resolve("test.html");
        ChangelogFetcher.saveToFile(doc, outputFile.toString());

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("Test content"));
        assertTrue(content.contains("<html>"));
    }

    @Test
    void testSaveToFile_OverwritesExisting(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("test.html");
        Files.writeString(outputFile, "Old content");

        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("New content");
        ChangelogFetcher.saveToFile(doc, outputFile.toString());

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

        ChangelogFetcher.saveToFile(doc, outputFile.toString());

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.isDirectory(subDir));
    }

    @Test
    void testSaveToFile_UTF8Encoding(@TempDir Path tempDir) throws IOException {
        Document doc = Document.createShell("");
        doc.body().appendElement("p").text("Test with UTF-8: spëcial chärs");

        Path outputFile = tempDir.resolve("test.html");
        ChangelogFetcher.saveToFile(doc, outputFile.toString());

        String content = Files.readString(outputFile);
        assertTrue(content.contains("spëcial chärs"));
    }
}
