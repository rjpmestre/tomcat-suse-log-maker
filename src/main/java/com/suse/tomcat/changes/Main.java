package com.suse.tomcat.changes;

import com.suse.tomcat.changes.changelog.Changelog;
import com.suse.tomcat.changes.changelog.ChangelogFetcher;
import com.suse.tomcat.changes.changelog.ChangelogProcessor;
import com.suse.tomcat.changes.cve.CveRecord;
import com.suse.tomcat.changes.cve.SecurityFetcher;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        // Parse arguments
        String fromVersion = null;
        String toVersion = null;
        String customOutputPath = null;
        boolean saveHtml = false;
        boolean quietMode = false;
        boolean skipChangelog = false;
        boolean skipCves = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("-o") && i + 1 < args.length) {
                customOutputPath = args[++i];
            } else if (arg.equals("-s") || arg.equals("--save-html")) {
                saveHtml = true;
            } else if (arg.equals("-q") || arg.equals("--quiet")) {
                quietMode = true;
            } else if (arg.equals("--skip-changelog")) {
                skipChangelog = true;
            } else if (arg.equals("--skip-cves")) {
                skipCves = true;
            } else if (arg.equals("-h") || arg.equals("--help")) {
                printUsage();
                System.exit(0);
            } else if (!arg.startsWith("-")) {
                if (fromVersion == null) {
                    fromVersion = arg;
                } else if (toVersion == null) {
                    toVersion = arg;
                } else {
                    System.err.println("Error: Too many arguments");
                    printUsage();
                    System.exit(1);
                }
            } else {
                System.err.println("Error: Unknown option: " + arg);
                printUsage();
                System.exit(1);
            }
        }

        if (fromVersion == null || toVersion == null) {
            System.err.println("Error: Both from-version and to-version are required");
            printUsage();
            System.exit(1);
        }

        try {
            // Detect Tomcat version series from one of the versions (they should be the same series)
            TomcatVersion tomcatVersion = TomcatVersion.fromVersion(toVersion);

            // Validate that both versions are from the same series
            TomcatVersion fromTomcatVersion = TomcatVersion.fromVersion(fromVersion);
            if (tomcatVersion != fromTomcatVersion) {
                System.err.println("Error: Both versions must be from the same Tomcat series.");
                System.err.println("  From version: " + fromVersion + " (series " + fromTomcatVersion.getSeries() + ")");
                System.err.println("  To version: " + toVersion + " (series " + tomcatVersion.getSeries() + ")");
                System.exit(1);
            }

            // Validate that at least one output type is enabled
            if (skipChangelog && skipCves) {
                System.err.println("Error: Cannot skip both changelog and CVEs. At least one must be enabled.");
                System.exit(1);
            }

            if (!quietMode) {
                System.out.println("Processing changelog for " + tomcatVersion + " from " + fromVersion + " to " + toVersion);
                System.out.println();
            }

            // Fetch and filter the changelog
            Document changelogDoc = ChangelogFetcher.fetchChangelogBetweenVersions(
                    tomcatVersion, fromVersion, toVersion, quietMode);

            // Optionally save the filtered HTML to input/ directory
            if (saveHtml) {
                String inputFileName = "input/changes_tomcat" + fromVersion.replace(".", "_") +
                                       "-" + toVersion.replace(".", "_") + ".html";
                ensureDirectoryExists("input");
                ChangelogFetcher.saveToFile(changelogDoc, inputFileName);
                if (!quietMode) {
                    System.out.println("Saved filtered HTML to: " + inputFileName);
                    System.out.println();
                }
            }

            // Fetch and parse security advisories (CVEs)
            java.util.List<CveRecord> cveRecords = new java.util.ArrayList<>();
            Document securityDoc = null;

            if (!skipCves) {
                try {
                    if (!quietMode) {
                        System.out.println();
                    }
                    securityDoc = SecurityFetcher.fetchSecurityPage(tomcatVersion, quietMode);

                    if (saveHtml) {
                        String securityFileName = "input/security_tomcat" + tomcatVersion.getSeries().replace(".", "_") +
                                                  "_" + extractPatchVersion(fromVersion) +
                                                  "-" + extractPatchVersion(toVersion) + ".html";
                        ensureDirectoryExists("input");
                        SecurityFetcher.saveToFile(securityDoc, securityFileName);
                        if (!quietMode) {
                            System.out.println("Saved security HTML to: " + securityFileName);
                            System.out.println();
                        }
                    }

                    cveRecords = SecurityFetcher.parseSecurityRecords(securityDoc, fromVersion, toVersion, quietMode);

                    if (!quietMode) {
                        System.out.println();
                        System.out.println("Found " + cveRecords.size() + " CVE(s) fixed in this range");
                    }
                } catch (IOException e) {
                    System.err.println("Warning: Failed to fetch security data: " + e.getMessage());
                    if (!quietMode) {
                        System.err.println("Continuing without CVE information...");
                    }
                }
            }

            // Process the changelog
            Changelog changelog = ChangelogProcessor.getTomcatChangelog(changelogDoc, quietMode);

            // Determine output file path
            String outputFileName;
            if (customOutputPath != null) {
                outputFileName = customOutputPath;
            } else {
                outputFileName = "output/output_tomcat" + tomcatVersion.getSeries().replace(".", "_") +
                                "_" + extractPatchVersion(fromVersion) +
                                "-" + extractPatchVersion(toVersion) + ".txt";
                ensureDirectoryExists("output");
            }

            boolean success = ChangelogProcessor.printTomcatChangelog(changelog, cveRecords, outputFileName, toVersion, skipChangelog);

            if (success) {
                if (!quietMode) {
                    System.out.println();
                    System.out.println("Successfully generated changelog: " + outputFileName);
                }
            } else {
                System.err.println("Failed to generate changelog");
                System.exit(1);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Error fetching changelog: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("Tomcat SUSE Log Maker");
        System.out.println("======================");
        System.out.println();
        System.out.println("Usage: tomcat-changelog [OPTIONS] <from-version> <to-version>");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -o <path>          Custom output file path");
        System.out.println("  -s, --save-html    Save intermediate HTML to input/ directory");
        System.out.println("  -q, --quiet        Quiet mode (minimal output)");
        System.out.println("  --skip-changelog   Only show CVE fixes, skip regular changelog entries");
        System.out.println("  --skip-cves        Only show regular changelog, skip CVE fixes");
        System.out.println("  -h, --help         Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  tomcat-changelog 10.1.51 10.1.54");
        System.out.println("  tomcat-changelog -q 9.0.115 9.0.117");
        System.out.println("  tomcat-changelog -o /tmp/changes.txt 11.0.18 11.0.21");
        System.out.println("  tomcat-changelog -s 8.5.100 8.5.105");
        System.out.println();
        System.out.println("Supported Tomcat series:");
        System.out.println("  - 8.5.x");
        System.out.println("  - 9.0.x");
        System.out.println("  - 10.1.x");
        System.out.println("  - 11.0.x");
        System.out.println();
        System.out.println("Default output: output/output_tomcat<series>_<from>-<to>.txt");
    }

    /**
     * Extracts the patch version number from a full version string.
     * E.g., "10.1.54" -> "54"
     */
    private static String extractPatchVersion(String version) {
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            return parts[2];
        }
        return version;
    }

    /**
     * Ensures a directory exists, creates it if it doesn't
     */
    private static void ensureDirectoryExists(String directory) throws IOException {
        if (!Files.exists(Paths.get(directory))) {
            Files.createDirectories(Paths.get(directory));
        }
    }
}
