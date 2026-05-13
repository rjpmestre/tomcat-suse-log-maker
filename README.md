# Tomcat SUSE Log Maker

Tool to fetch and format Apache Tomcat changelogs and CVE security advisories for SUSE package maintenance.

## Features

- Fetches changelogs directly from Apache Tomcat documentation
- Fetches and parses CVE security advisories from Apache Tomcat security pages
- Extracts changes between two versions
- Formats output for SUSE changelog format (80-character lines)
- Supports Tomcat 8.5, 9.0, 10.1, and 11.0 series
- Flexible output options (changelog only, CVEs only, or both)

## Requirements

- Java 17 or later
- Maven 3.6 or later
- Internet connection (to fetch changelogs and security advisories from apache.org :) )

## Building

```bash
mvn clean package
```

This creates `target/tomcat-changelog-processor.jar`

## Usage

```bash
./tomcat-gen-changes [OPTIONS] <from-version> <to-version>
```

### Options

- `-o <path>` - Custom output file path (default: `output/output_tomcat<series>_<from>-<to>.txt`)
- `-s, --save-html` - Save intermediate HTML to `input/` directory for debugging
- `-q, --quiet` - Quiet mode - minimal output (no progress messages)
- `--skip-changelog` - Only show CVE fixes, skip regular changelog entries
- `--skip-cves` - Only show regular changelog, skip CVE fixes
- `-h, --help` - Show help message

### Examples

```bash
# Basic usage (includes both changelog and CVEs)
./tomcat-gen-changes 10.1.51 10.1.54

# Quiet mode
./tomcat-gen-changes -q 9.0.115 9.0.117

# Custom output path
./tomcat-gen-changes -o /tmp/changes.txt 11.0.18 11.0.21

# Save intermediate HTML for debugging
./tomcat-gen-changes -s 8.5.100 8.5.105

# Only show CVE fixes
./tomcat-gen-changes --skip-changelog 10.1.45 10.1.48

# Combine flags
./tomcat-gen-changes -q -s -o /tmp/output.txt 10.1.45 10.1.48
```

## Output

The tool generates formatted changelog files by default:

1. **Formatted changelog**: `output/output_tomcatX_Y_Y-Z.txt` (or custom path with `-o`)
   - Formatted according to SUSE standards
   - 80-character line wrapping
   - Includes CVE security fixes with severity and titles
   - Organized by category (Catalina, Coyote, Jasper, etc.)
   
2. **Filtered HTML** (optional with `-s`): 
   - `input/changes_tomcat_X_Y-X_Z.html` - Raw changelog HTML
   - `input/security_tomcat_X_Y-X_Z.html` - Raw security advisory HTML
   - Useful for debugging parsing issues

## Supported Versions

- Tomcat 8.5.x
- Tomcat 9.0.x
- Tomcat 10.1.x
- Tomcat 11.0.x


## Dependencies

- **jsoup 1.17.2** - HTML parsing
- **commons-lang3 3.14.0** - String utilities
- **commons-text 1.11.0** - Text wrapping
