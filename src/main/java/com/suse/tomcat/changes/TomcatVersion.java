package com.suse.tomcat.changes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TomcatVersion {
    TOMCAT_8_5("8.5",
            "https://tomcat.apache.org/tomcat-8.5-doc/changelog.html",
            "https://tomcat.apache.org/security-8.html"),
    TOMCAT_9_0("9.0",
            "https://tomcat.apache.org/tomcat-9.0-doc/changelog.html",
            "https://tomcat.apache.org/security-9.html"),
    TOMCAT_10_1("10.1",
            "https://tomcat.apache.org/tomcat-10.1-doc/changelog.html",
            "https://tomcat.apache.org/security-10.html"),
    TOMCAT_11_0("11.0",
            "https://tomcat.apache.org/tomcat-11.0-doc/changelog.html",
            "https://tomcat.apache.org/security-11.html");

    private final String series;
    private final String changelogUrl;
    private final String securityUrl;
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    TomcatVersion(String series, String changelogUrl, String securityUrl) {
        this.series = series;
        this.changelogUrl = changelogUrl;
        this.securityUrl = securityUrl;
    }

    public String getSeries() {
        return series;
    }

    public String getChangelogUrl() {
        return changelogUrl;
    }

    public String getSecurityUrl() {
        return securityUrl;
    }

    /**
     * Detects the Tomcat version series from a version string like "10.1.54"
     * @param version The version string (e.g., "10.1.54")
     * @return The corresponding TomcatVersion enum
     * @throws IllegalArgumentException if the version format is invalid or not supported
     */
    public static TomcatVersion fromVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid version format: " + version + ". Expected format: X.Y.Z");
        }

        String major = matcher.group(1);
        String minor = matcher.group(2);
        String series = major + "." + minor;

        for (TomcatVersion tv : values()) {
            if (tv.getSeries().equals(series)) {
                return tv;
            }
        }

        throw new IllegalArgumentException("Unsupported Tomcat version series: " + series +
                ". Supported series: 8.5, 9.0, 10.1, 11.0");
    }

    @Override
    public String toString() {
        return "Tomcat " + series;
    }
}
