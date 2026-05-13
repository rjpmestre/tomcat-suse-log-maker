package com.suse.tomcat.changes.changelog;

import com.suse.tomcat.changes.cve.CveProcessor;
import com.suse.tomcat.changes.cve.CveRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class ChangelogProcessor {

    public static final String OTHER = "other";
    public static final int LINE_MAX_LENGTH = 80;

    public static Changelog getTomcatChangelog(String inputFilePath) {
        return getTomcatChangelog(inputFilePath, false);
    }

    public static Changelog getTomcatChangelog(String inputFilePath, boolean quietMode) {
        try {
            Document doc = Jsoup.parse(new File(inputFilePath), "UTF-8");
            return getTomcatChangelog(doc, quietMode);
        }
        catch (IOException e) {
            throw new RuntimeException("Error processing HTML file: " + inputFilePath, e);
        }
    }

    public static Changelog getTomcatChangelog(Document doc) {
        return getTomcatChangelog(doc, false);
    }

    public static Changelog getTomcatChangelog(Document doc, boolean quietMode) {
        Changelog changelog = new Changelog();

        Elements subsections = doc.select(".subsection");
        for (Element subsection : subsections) {
            String sectionTitle = subsection.select("h4").text().toLowerCase();  // Extract section title (like "Catalina", "Coyote", etc.)

            Elements changelogItems = subsection.select(".changelog li");
            for (Element item : changelogItems) {
                String type = item.select("img").attr("alt").split(":")[0]; // Get text before ":"
                changelog.addChangelog(sectionTitle, type, item.text());
                if (!quietMode) {
                    System.out.println("Read an entry for " + sectionTitle + " -> " + type + ": " + item.text());
                }
            }
        }

        return changelog;
    }


    public static boolean printTomcatChangelog(Changelog changelog, List<CveRecord> cveRecords, String outputFilePath, String toVersion, boolean skipChangelog) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            // Write version header
            writer.write("- Update to Tomcat " + toVersion);
            writer.newLine();

            // Write CVE section if present
            if (cveRecords != null && !cveRecords.isEmpty()) {
                CveProcessor.formatCveSection(cveRecords, writer);
            }

            // Write regular changelog if not skipped
            if (!skipChangelog) {
                List<String> changeLogCategories =
                        changelog.getChangelog().keySet().stream()
                                .filter(p -> !p.equalsIgnoreCase(OTHER))
                                .collect(Collectors.toList());
                if (changelog.getChangelog().containsKey(OTHER)) {
                    changeLogCategories.add(OTHER);
                }

                for (String category : changeLogCategories) {
    //                writer.newLine();   //new line before each category
                    writer.write("  * " + StringUtils.capitalize(category));
                    writer.newLine();
                    for (ChangelogRecord record : changelog.getChangelog().get(category)) {
                        String recordLine = "    + " + record.getType() + ": " + record.getDescription();
                        // Ensure lines do not exceed 80 characters
                        String wrappedLine = wrapLineWithIndentation(recordLine);
                        writer.write(wrappedLine);
                        writer.newLine();
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static String wrapLineWithIndentation(String line) {
        // Check if the line exceeds the max length
        if (line.length() <= ChangelogProcessor.LINE_MAX_LENGTH) {
            return line;
        }

        // Find the part of the line that should be wrapped (after "    + ")
        String indentation = "    + ";  // the indentation to preserve
        String content = line.substring(indentation.length());

        // Wrap the content, but keep the initial indentation for subsequent lines
        String wrappedContent = WordUtils.wrap(content, ChangelogProcessor.LINE_MAX_LENGTH - indentation.length());

        // Rebuild the lines with the proper indentation
        StringBuilder finalLine = new StringBuilder();
        String[] lines = wrappedContent.split("\n");

        // First line gets the "    + " prefix
        finalLine.append(indentation).append(lines[0]);

        // Subsequent lines get the normal indentation (4 spaces), but no "+"
        for (int i = 1; i < lines.length; i++) {
            finalLine.append("\n").append("      ").append(lines[i]);
        }

        return finalLine.toString();
    }

    private ChangelogProcessor() {
        // Private constructor to prevent instantiation
    }

}