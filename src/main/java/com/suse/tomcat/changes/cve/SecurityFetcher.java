package com.suse.tomcat.changes.cve;

import com.suse.tomcat.changes.TomcatVersion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SecurityFetcher {

    private static final Pattern VERSION_HEADER_PATTERN = Pattern.compile("Fixed in Apache Tomcat ([\\d.]+(?:-M\\d+)?)");
    private static final Pattern CVE_PATTERN = Pattern.compile("CVE-\\d{4}-\\d+");
    private static final Pattern SEVERITY_PATTERN = Pattern.compile("^(Critical|Important|High|Moderate|Low):\\s*(.+)$");

    /**
     * Fetches the security advisory page for the given Tomcat version
     */
    public static Document fetchSecurityPage(TomcatVersion version, boolean quietMode) throws IOException {
        String url = version.getSecurityUrl();

        if (!quietMode) {
            System.out.println("Fetching security advisories from: " + url);
        }

        return Jsoup.connect(url).timeout(10000).get();
    }

    /**
     * Parses security records from the fetched document for versions in the given range
     */
    public static List<CveRecord> parseSecurityRecords(Document doc, String fromVersion, String toVersion, boolean quietMode) {
        List<CveRecord> cveRecords = new ArrayList<>();

        // Find all h3 headers that indicate a fixed version
        Elements headers = doc.select("h3");

        for (Element header : headers) {
            String headerText = header.text();
            Matcher versionMatcher = VERSION_HEADER_PATTERN.matcher(headerText);

            if (versionMatcher.find()) {
                String fixedVersion = versionMatcher.group(1);

                // Check if this fixed version is in our range
                if (isVersionInRange(fixedVersion, fromVersion, toVersion)) {
                    if (!quietMode) {
                        System.out.println("Found security fixes in version: " + fixedVersion);
                    }

                    // Parse CVEs under this header
                    List<CveRecord> cvesForVersion = parseCvesUnderHeader(header, quietMode);
                    cveRecords.addAll(cvesForVersion);
                }
            }
        }

        return cveRecords;
    }

    /**
     * Parses all CVE entries that appear after a given header until the next h3
     */
    private static List<CveRecord> parseCvesUnderHeader(Element header, boolean quietMode) {
        List<CveRecord> cves = new ArrayList<>();

        // Check if there's a div.text after the header (new structure)
        Element textDiv = header.nextElementSibling();
        Elements paragraphs;

        if (textDiv != null && textDiv.tagName().equals("div") && textDiv.hasClass("text")) {
            // New structure: paragraphs are inside a div.text
            paragraphs = textDiv.select("p");
        } else {
            // Old structure or direct paragraphs: collect all paragraphs until next h3
            paragraphs = new Elements();
            Element currentElement = header.nextElementSibling();
            while (currentElement != null && !currentElement.tagName().equals("h3")) {
                if (currentElement.tagName().equals("p")) {
                    paragraphs.add(currentElement);
                }
                currentElement = currentElement.nextElementSibling();
            }
        }

        // Parse CVE entries from the paragraphs
        for (int i = 0; i < paragraphs.size(); i++) {
            Element paragraph = paragraphs.get(i);
            Element strongElem = paragraph.selectFirst("strong");
            Element cveLink = paragraph.selectFirst("a[href*=cve.mitre.org]");

            if (strongElem != null && cveLink != null) {
                // This is a CVE entry header
                String strongText = strongElem.text();
                String cveId = extractCveId(cveLink.text());

                if (cveId != null) {
                    Matcher severityMatcher = SEVERITY_PATTERN.matcher(strongText);
                    String severity = "Unknown";
                    String title = strongText;

                    if (severityMatcher.matches()) {
                        severity = severityMatcher.group(1);
                        title = severityMatcher.group(2);
                    }

                    // Collect description from following paragraphs
                    String description = collectDescriptionFromParagraphs(paragraphs, i + 1);

                    CveRecord cve = new CveRecord(cveId, severity, title, description);
                    cves.add(cve);

                    if (!quietMode) {
                        System.out.println("  Found " + cveId + " - " + severity);
                    }
                }
            }
        }

        return cves;
    }

    /**
     * Collects the description text for a CVE from a list of paragraphs
     */
    private static String collectDescriptionFromParagraphs(Elements paragraphs, int startIndex) {
        StringBuilder description = new StringBuilder();

        for (int i = startIndex; i < paragraphs.size(); i++) {
            Element paragraph = paragraphs.get(i);
            String text = paragraph.text();

            // Stop if we hit metadata lines
            if (text.startsWith("This was fixed with commit") ||
                text.startsWith("This issue was reported") ||
                text.startsWith("Affects:")) {
                break;
            }

            // Check if this paragraph contains CVE/strong (next CVE entry)
            if (paragraph.selectFirst("strong") != null &&
                paragraph.selectFirst("a[href*=cve.mitre.org]") != null) {
                break;
            }

            if (description.length() > 0) {
                description.append(" ");
            }
            description.append(text);
        }

        return description.toString().trim();
    }

    /**
     * Extracts CVE ID from text
     */
    private static String extractCveId(String text) {
        Matcher matcher = CVE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Checks if a version is within the given range (inclusive)
     */
    private static boolean isVersionInRange(String version, String fromVersion, String toVersion) {
        try {
            return compareVersions(version, fromVersion) >= 0 &&
                   compareVersions(version, toVersion) <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compares two version strings
     * Returns: negative if v1 < v2, zero if v1 == v2, positive if v1 > v2
     */
    private static int compareVersions(String v1, String v2) {
        // Remove milestone suffixes for comparison
        String[] parts1 = v1.replace("-M", ".").split("\\.");
        String[] parts2 = v2.replace("-M", ".").split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return num1 - num2;
            }
        }

        return 0;
    }

    /**
     * Saves the HTML document to a file
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

    private SecurityFetcher() {
        // Private constructor to prevent instantiation
    }
}
