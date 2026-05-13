package com.suse.tomcat.changes.cve;

import org.apache.commons.text.WordUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class CveProcessor {

    private static final int LINE_MAX_LENGTH = 80;

    /**
     * Formats and writes the CVE section to the output
     */
    public static void formatCveSection(List<CveRecord> cves, BufferedWriter writer) throws IOException {
        if (cves == null || cves.isEmpty()) {
            return;
        }

        // Write section header
        writer.write("  * Fixed CVE");
        if (cves.size() > 1) {
            writer.write("s");
        }
        writer.write(":");
        writer.newLine();

        // Write each CVE entry
        for (CveRecord cve : cves) {
            String cveText = cve.getTitle() + " (bsc#)";
            String prefix = "    + " + cve.getCveId() + ": ";
            int prefixLength = prefix.length();
            int availableFirstLine = LINE_MAX_LENGTH - prefixLength;

            if (cveText.length() <= availableFirstLine) {
                // Fits on one line
                writer.write(prefix + cveText);
                writer.newLine();
            } else {
                // Needs wrapping - wrap at different widths for first line vs continuation
                // First, wrap the full text at continuation line width (80 - 6 = 74)
                String wrapped = WordUtils.wrap(cveText, LINE_MAX_LENGTH - 6, "\n", false);
                String[] wrappedLines = wrapped.split("\n");

                // Now check if first wrapped line fits on the first line
                if (wrappedLines[0].length() <= availableFirstLine) {
                    // First wrapped line fits
                    writer.write(prefix + wrappedLines[0]);
                    writer.newLine();

                    // Write remaining lines with 6-space indentation
                    for (int i = 1; i < wrappedLines.length; i++) {
                        writer.write("      " + wrappedLines[i]);
                        writer.newLine();
                    }
                } else {
                    // First wrapped line is still too long for the first line
                    // Re-wrap first line separately
                    String firstLineText = WordUtils.wrap(cveText, availableFirstLine, "\n", false).split("\n")[0];
                    writer.write(prefix + firstLineText);
                    writer.newLine();

                    // Wrap the rest at continuation width
                    String remainder = cveText.substring(firstLineText.length()).trim();
                    if (!remainder.isEmpty()) {
                        String remainderWrapped = WordUtils.wrap(remainder, LINE_MAX_LENGTH - 6, "\n", false);
                        String[] remainderLines = remainderWrapped.split("\n");
                        for (String line : remainderLines) {
                            writer.write("      " + line);
                            writer.newLine();
                        }
                    }
                }
            }
        }
    }

    private CveProcessor() {
        // Private constructor to prevent instantiation
    }
}
