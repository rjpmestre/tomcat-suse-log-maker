package com.suse.tomcat.changes.changelog;

import com.suse.tomcat.changes.TomcatVersion;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import java.io.IOException;

public class ChangelogFetcher {

    // Timeout for HTTP requests in ms
    private static final int TIMEOUT = 10000; 

    /**
     * Fetches the changelog HTML from Apache Tomcat website and extracts
     * entries between two versions.
     *
     * @param tomcatVersion The Tomcat version series (determines which changelog URL to use)
     * @param fromVersion The starting version (e.g., "10.1.51")
     * @param toVersion The ending version (e.g., "10.1.54")
     * @param quietMode If true, suppress informational messages
     * @return HTML document containing only the relevant changelog sections
     * @throws IOException if fetching fails
     */
    public static Document fetchChangelogBetweenVersions(
        TomcatVersion tomcatVersion,
        String fromVersion,
        String toVersion,
        boolean quietMode
    ) throws IOException {

        if (!quietMode) {
            System.out.println("Fetching changelog from: " + tomcatVersion.getChangelogUrl());
        }

        // Fetch the full changelog
        Document fullDoc = Jsoup.connect(tomcatVersion.getChangelogUrl())
                .timeout(TIMEOUT)
                .get();

        // Create a new document to hold filtered results
        Document filteredDoc = Document.createShell("");
        Element body = filteredDoc.body();

        // Extract version sections between fromVersion and toVersion
        boolean capturing = false;
        Elements h3Headers = fullDoc.select("h3");

        for (Element h3 : h3Headers) {
            String sectionText = h3.text();
            String versionInSection = extractVersionFromHeader(sectionText);

            if (versionInSection == null) {
                continue;
            }

            // Start capturing when we hit toVersion (newer version first in changelog)
            if (versionInSection.equals(toVersion)) {
                capturing = true;
            }

            // If we're capturing, add the subsections for this version
            if (capturing) {
                // The subsections are in the next sibling div.text
                Element nextDiv = h3.nextElementSibling();
                if (nextDiv != null && nextDiv.hasClass("text")) {
                    // Clone all subsections from this div
                    Elements subsections = nextDiv.select(".subsection");
                    for (Element subsection : subsections) {
                        body.appendChild(subsection.clone());
                    }
                }

                // Stop capturing after including fromVersion (inclusive)
                if (versionInSection.equals(fromVersion)) {
                    break;
                }
            }
        }

        if (!capturing) {
            throw new IllegalArgumentException(
                "Could not find version " + toVersion + " in changelog. " +
                "Please verify the version exists in " + tomcatVersion.getChangelogUrl()
            );
        }

        return filteredDoc;
    }

    /**
     * Extracts version number from a header like "Tomcat 10.1.54 (rjung)" or "2024-11-13 Tomcat 10.1.54 (rjung)"
     *
     * @param headerText The header text
     * @return The version string (e.g., "10.1.54") or null if not found
     */
    private static String extractVersionFromHeader(String headerText) {
        // Pattern: look for "Tomcat X.Y.Z"
        Pattern pattern = Pattern.compile("Tomcat\\s+(\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(headerText);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * Saves the HTML document to a file for debugging purposes
     */
    public static void saveToFile(Document doc, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(
            path,
            doc.html(),
            StandardCharsets.UTF_8
        );
    }

}
