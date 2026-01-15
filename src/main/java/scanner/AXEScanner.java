package scanner;

import activesupport.IllegalBrowserException;
import activesupport.driver.Browser;
import com.deque.html.axecore.args.AxeRunOptions;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class AXEScanner {
    private static final Logger LOGGER = LogManager.getLogger(AXEScanner.class);

    private int totalViolationsCount = 0;
    private int numberOfViolationsFoundPerPage = 0;
    private List<String> tags;
    private final List<AccessibilityViolation> violations = new ArrayList<>();
    private final List<AccessibilityViolation> incomplete = new ArrayList<>();

    public AXEScanner() {
        this(getDefaultTags());
    }

    public AXEScanner(List<String> tags) {
        this.tags = (tags == null || tags.isEmpty()) ? getDefaultTags() : tags;
    }

    private static List<String> getDefaultTags() {
        String configuredTags = System.getProperty("standards.scan");
        if (configuredTags != null && !configuredTags.isEmpty()) {
            return Collections.singletonList(configuredTags);
        }
        return Arrays.asList(
                "wcag2a", "wcag2aa", "wcag21a", "wcag21aa",
                "wcag412", "wcag143", "best-practice", "ACT",
                "section508", "section508.*"
        );
    }

    public ScanResult scan(boolean includeIncomplete) throws IllegalBrowserException {
        String currentUrl = Browser.navigate().getCurrentUrl();
        LOGGER.info("Scanning page for accessibility violations: {}", currentUrl);

        AxeRunOptions runOptions = new AxeRunOptions();
        runOptions.setXPath(true);

        Results axeResponse = new AxeBuilder()
                .withOptions(runOptions)
                .withTags(tags)
                .analyze(Browser.navigate());

        List<Rule> violationRules = axeResponse.getViolations();
        List<Rule> incompleteRules = axeResponse.getIncomplete();

        numberOfViolationsFoundPerPage = violationRules.size();
        totalViolationsCount += violationRules.size();

        for (Rule rule : violationRules) {
            violations.add(AccessibilityViolation.fromRule(rule, currentUrl, "violation"));
        }

        if (includeIncomplete) {
            for (Rule rule : incompleteRules) {
                incomplete.add(AccessibilityViolation.fromRule(rule, currentUrl, "incomplete"));
            }
        }

        if (violationRules.isEmpty()) {
            LOGGER.info("No accessibility violations found on {}", currentUrl);
        } else {
            LOGGER.warn("Found {} accessibility violations on {}", violationRules.size(), currentUrl);
        }

        return new ScanResult(violationRules.size(), incompleteRules.size(), currentUrl);
    }


    public void logViolations() {
        if (totalViolationsCount == 0) {
            LOGGER.info("═══════════════════════════════════════════════════════");
            LOGGER.info("✓ NO ACCESSIBILITY VIOLATIONS DETECTED");
            LOGGER.info("═══════════════════════════════════════════════════════");
            return;
        }

        LOGGER.error("═══════════════════════════════════════════════════════");
        LOGGER.error("✗ ACCESSIBILITY VIOLATIONS DETECTED");
        LOGGER.error("═══════════════════════════════════════════════════════");
        LOGGER.error("Total Violations: {}", totalViolationsCount);
        LOGGER.error("Total Incomplete: {}", incomplete.size());
        LOGGER.error("");

        // Group violations by severity
        Map<String, List<AccessibilityViolation>> violationsBySeverity = violations.stream()
                .collect(Collectors.groupingBy(
                        v -> v.getImpact() != null ? v.getImpact().toUpperCase() : "MODERATE",
                        () -> new TreeMap<>(Comparator.comparing(this::getSeverityOrder)),
                        Collectors.toList()
                ));

        // Log violations by severity
        for (Map.Entry<String, List<AccessibilityViolation>> entry : violationsBySeverity.entrySet()) {
            String severity = entry.getKey();
            List<AccessibilityViolation> severityViolations = entry.getValue();

            LOGGER.error("{} {} ({}):", getSeverityEmoji(severity), severity, severityViolations.size());

            for (AccessibilityViolation violation : severityViolations) {
                LOGGER.error("  Rule ID: {}", violation.getId());
                LOGGER.error("  Description: {}", violation.getDescription());
                LOGGER.error("  Elements Affected: {}", violation.getNodeCount());
                LOGGER.error("  Help URL: {}", violation.getHelpUrl());
                LOGGER.error("  Page: {}", violation.getUrl());

                // Log example selectors
                List<String> selectors = violation.getSelectors();
                if (!selectors.isEmpty()) {
                    LOGGER.error("  Example Selectors:");
                    int displayCount = Math.min(3, selectors.size());
                    for (int i = 0; i < displayCount; i++) {
                        LOGGER.error("    - {}", selectors.get(i));
                    }
                    if (selectors.size() > 3) {
                        LOGGER.error("    ... and {} more", selectors.size() - 3);
                    }
                }
                LOGGER.error("");
            }
        }

        // Log incomplete/needs review items if any
        if (!incomplete.isEmpty()) {
            LOGGER.warn(":warning:  INCOMPLETE / NEEDS REVIEW ({}):", incomplete.size());
            for (AccessibilityViolation item : incomplete) {
                LOGGER.warn("  Rule ID: {}", item.getId());
                LOGGER.warn("  Description: {}", item.getDescription());
                LOGGER.warn("  Elements: {}", item.getNodeCount());
                LOGGER.warn("");
            }
        }

        LOGGER.error("═══════════════════════════════════════════════════════");
    }


    public String getViolationSummary() {
        if (totalViolationsCount == 0 && incomplete.isEmpty()) {
            return "No accessibility violations found";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("\n╔═══════════════════════════════════════════════════════╗\n");
        summary.append(String.format("║  ACCESSIBILITY VIOLATIONS: %-27d║\n", totalViolationsCount));
        if (!incomplete.isEmpty()) {
            summary.append(String.format("║  INCOMPLETE / NEEDS REVIEW: %-26d║\n", incomplete.size()));
        }
        summary.append("╚═══════════════════════════════════════════════════════╝\n\n");

        if (totalViolationsCount > 0) {
            // Group violations by severity
            Map<String, List<AccessibilityViolation>> violationsBySeverity = violations.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getImpact() != null ? v.getImpact().toUpperCase() : "MODERATE",
                            () -> new TreeMap<>(Comparator.comparing(this::getSeverityOrder)),
                            Collectors.toList()
                    ));

            // Add violations by severity
            for (Map.Entry<String, List<AccessibilityViolation>> entry : violationsBySeverity.entrySet()) {
                String severity = entry.getKey();
                List<AccessibilityViolation> severityViolations = entry.getValue();

                summary.append(String.format("\n%s %s (%d):\n",
                        getSeverityEmoji(severity), severity, severityViolations.size()));

                // Group by rule ID to avoid repetition
                Map<String, List<AccessibilityViolation>> byRule = severityViolations.stream()
                        .collect(Collectors.groupingBy(AccessibilityViolation::getId));

                for (Map.Entry<String, List<AccessibilityViolation>> ruleEntry : byRule.entrySet()) {
                    String ruleId = ruleEntry.getKey();
                    List<AccessibilityViolation> ruleViolations = ruleEntry.getValue();
                    AccessibilityViolation firstViolation = ruleViolations.get(0);

                    int totalElements = ruleViolations.stream()
                            .mapToInt(AccessibilityViolation::getNodeCount)
                            .sum();

                    Set<String> uniquePages = ruleViolations.stream()
                            .map(AccessibilityViolation::getUrl)
                            .collect(Collectors.toSet());

                    summary.append(String.format("  • %s\n", ruleId));
                    summary.append(String.format("    %s\n", firstViolation.getDescription()));
                    summary.append(String.format("    Elements: %d | Pages: %d | Info: %s\n",
                            totalElements, uniquePages.size(), firstViolation.getHelpUrl()));

                    List<String> selectors = firstViolation.getSelectors();
                    if (!selectors.isEmpty()) {
                        summary.append(String.format("    Example: %s\n", selectors.get(0)));
                    }
                }
            }
        }

        if (!incomplete.isEmpty()) {
            summary.append("\n:warning:  INCOMPLETE / NEEDS REVIEW:\n");
            Map<String, Long> incompleteByRule = incomplete.stream()
                    .collect(Collectors.groupingBy(AccessibilityViolation::getId, Collectors.counting()));

            for (Map.Entry<String, Long> entry : incompleteByRule.entrySet()) {
                summary.append(String.format("  • %s (%d items)\n", entry.getKey(), entry.getValue()));
            }
        }

        summary.append("\n═══════════════════════════════════════════════════════\n");
        summary.append("See Allure attachments for full HTML report and JSON data\n");
        summary.append("═══════════════════════════════════════════════════════\n");

        return summary.toString();
    }

    public int getTotalViolationsCount() {
        return totalViolationsCount;
    }

    public int getCriticalViolationsCount() {
        return (int) violations.stream()
                .filter(v -> "critical".equalsIgnoreCase(v.getImpact()))
                .count();
    }


    public int getNumberOfViolationsFoundPerPage() {
        return numberOfViolationsFoundPerPage;
    }

    public List<AccessibilityViolation> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public List<AccessibilityViolation> getIncomplete() {
        return Collections.unmodifiableList(incomplete);
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }


    public void reset() {
        totalViolationsCount = 0;
        numberOfViolationsFoundPerPage = 0;
        violations.clear();
        incomplete.clear();
        LOGGER.debug("AXE Scanner reset - all violations cleared");
    }


    public void attachToAllure() {
        AllureReporter.attachAccessibilityResults(this);
    }


    private String getSeverityEmoji(String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return ":red_circle:";
            case "SERIOUS":
                return ":large_orange_circle:";
            case "MODERATE":
                return ":large_yellow_circle:";
            case "MINOR":
                return ":large_green_circle:";
            default:
                return ":white_circle:";
        }
    }

    private int getSeverityOrder(String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return 1;
            case "SERIOUS":
                return 2;
            case "MODERATE":
                return 3;
            case "MINOR":
                return 4;
            default:
                return 5;
        }
    }


    public static class ScanResult {
        private final int violationCount;
        private final int incompleteCount;
        private final String url;

        public ScanResult(int violationCount, int incompleteCount, String url) {
            this.violationCount = violationCount;
            this.incompleteCount = incompleteCount;
            this.url = url;
        }

        public int getViolationCount() {
            return violationCount;
        }

        public int getIncompleteCount() {
            return incompleteCount;
        }

        public String getUrl() {
            return url;
        }

        public boolean hasViolations() {
            return violationCount > 0;
        }
    }
}
