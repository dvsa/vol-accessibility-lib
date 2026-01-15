package scanner;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for attaching accessibility results to Allure reports
 */
public class AllureReporter {
    private static final Logger LOGGER = LogManager.getLogger(AllureReporter.class);

    /**
     * Attach accessibility scan results to Allure report
     * Creates 3 attachments: Summary (text), HTML report, JSON data
     */
    public static void attachAccessibilityResults(AXEScanner scanner) {
        LOGGER.debug("Attaching accessibility results to Allure report");

        // 1. Attach summary text
        String summaryText = createSummaryText(scanner);
        Allure.addAttachment("Accessibility Summary", "text/plain",
                new ByteArrayInputStream(summaryText.getBytes(StandardCharsets.UTF_8)), ".txt");

        // 2. Attach HTML report
        String htmlReport = createHtmlReport(scanner);
        Allure.addAttachment("Accessibility Report", "text/html",
                new ByteArrayInputStream(htmlReport.getBytes(StandardCharsets.UTF_8)), ".html");

        // 3. Attach JSON data
        String jsonData = createJsonReport(scanner);
        Allure.addAttachment("Accessibility Results", "application/json",
                new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8)), ".json");

        LOGGER.debug("Successfully attached {} violation(s) to Allure", scanner.getTotalViolationsCount());
    }

    /**
     * Create plain text summary
     */
    private static String createSummaryText(AXEScanner scanner) {
        StringBuilder text = new StringBuilder();
        text.append("ACCESSIBILITY SCAN SUMMARY\n");
        text.append("=".repeat(60)).append("\n\n");
        text.append("Total Violations: ").append(scanner.getTotalViolationsCount()).append("\n");
        text.append("Incomplete Checks: ").append(scanner.getIncomplete().size()).append("\n\n");

        if (scanner.getTotalViolationsCount() > 0) {
            text.append("VIOLATIONS BY SEVERITY:\n");
            text.append("-".repeat(60)).append("\n");

            Map<String, List<AccessibilityViolation>> bySeverity = scanner.getViolations().stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getImpact() != null ? v.getImpact().toUpperCase() : "MODERATE"
                    ));

            for (String severity : Arrays.asList("CRITICAL", "SERIOUS", "MODERATE", "MINOR")) {
                List<AccessibilityViolation> violations = bySeverity.get(severity);
                if (violations != null && !violations.isEmpty()) {
                    text.append(String.format("\n%s (%d):\n", severity, violations.size()));
                    for (AccessibilityViolation v : violations) {
                        text.append(String.format("  • %s - %s (%d elements)\n",
                                v.getId(), v.getDescription(), v.getNodeCount()));
                    }
                }
            }
        }

        if (!scanner.getIncomplete().isEmpty()) {
            text.append("\n\nINCOMPLETE / NEEDS REVIEW:\n");
            text.append("-".repeat(60)).append("\n");
            for (AccessibilityViolation v : scanner.getIncomplete()) {
                text.append(String.format("  • %s - %s (%d elements)\n",
                        v.getId(), v.getDescription(), v.getNodeCount()));
            }
        }

        return text.toString();
    }

    /**
     * Create HTML report with interactive styling
     */
    private static String createHtmlReport(AXEScanner scanner) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>Accessibility Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }\n");
        html.append("h1 { color: #d32f2f; margin-bottom: 10px; }\n");
        html.append("h2 { color: #1976d2; border-bottom: 2px solid #1976d2; padding-bottom: 5px; margin-top: 30px; }\n");
        html.append(".summary { background: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append(".stats { display: flex; gap: 20px; margin-top: 15px; }\n");
        html.append(".stat-box { flex: 1; padding: 15px; border-radius: 5px; text-align: center; }\n");
        html.append(".stat-box.violations { background: #ffebee; border: 2px solid #d32f2f; }\n");
        html.append(".stat-box.incomplete { background: #fff3e0; border: 2px solid #f57c00; }\n");
        html.append(".stat-number { font-size: 36px; font-weight: bold; margin: 10px 0; }\n");
        html.append(".stat-label { font-size: 14px; color: #666; }\n");
        html.append(".violation { background: white; padding: 20px; margin: 15px 0; border-left: 5px solid #d32f2f; border-radius: 5px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        html.append(".violation.critical { border-left-color: #d32f2f; }\n");
        html.append(".violation.serious { border-left-color: #f57c00; }\n");
        html.append(".violation.moderate { border-left-color: #fbc02d; }\n");
        html.append(".violation.minor { border-left-color: #388e3c; }\n");
        html.append(".badge { display: inline-block; padding: 4px 12px; border-radius: 4px; font-size: 12px; font-weight: bold; margin-right: 10px; }\n");
        html.append(".badge-critical { background: #d32f2f; color: white; }\n");
        html.append(".badge-serious { background: #f57c00; color: white; }\n");
        html.append(".badge-moderate { background: #fbc02d; color: black; }\n");
        html.append(".badge-minor { background: #388e3c; color: white; }\n");
        html.append(".violation-header { font-size: 18px; font-weight: 600; margin-bottom: 10px; }\n");
        html.append(".violation-description { color: #555; margin-bottom: 15px; line-height: 1.5; }\n");
        html.append(".violation-meta { display: flex; gap: 20px; margin-bottom: 15px; font-size: 14px; color: #666; }\n");
        html.append(".meta-item { display: flex; align-items: center; gap: 5px; }\n");
        html.append(".help-link { color: #1976d2; text-decoration: none; }\n");
        html.append(".help-link:hover { text-decoration: underline; }\n");
        html.append(".nodes-section { margin-top: 15px; }\n");
        html.append("details { margin-top: 10px; }\n");
        html.append("summary { cursor: pointer; font-weight: 600; padding: 10px; background: #f5f5f5; border-radius: 4px; user-select: none; }\n");
        html.append("summary:hover { background: #e0e0e0; }\n");
        html.append(".node { background: #fafafa; padding: 15px; margin: 10px 0; border-radius: 4px; border: 1px solid #e0e0e0; font-family: 'Courier New', monospace; font-size: 13px; }\n");
        html.append(".node-label { font-weight: bold; color: #666; margin-right: 10px; }\n");
        html.append(".node-target { color: #1976d2; word-break: break-all; }\n");
        html.append(".node-html { color: #666; margin-top: 8px; word-break: break-all; }\n");
        html.append(".no-violations { text-align: center; padding: 40px; color: #4caf50; font-size: 24px; }\n");
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // Summary section
        html.append("<div class=\"summary\">\n");
        html.append("<h1>:mag: Accessibility Test Report</h1>\n");
        html.append(String.format("<p>Generated: %s</p>\n", new Date()));
        html.append("<div class=\"stats\">\n");
        html.append("<div class=\"stat-box violations\">\n");
        html.append(String.format("<div class=\"stat-number\">%d</div>\n", scanner.getTotalViolationsCount()));
        html.append("<div class=\"stat-label\">Total Violations</div>\n");
        html.append("</div>\n");
        html.append("<div class=\"stat-box incomplete\">\n");
        html.append(String.format("<div class=\"stat-number\">%d</div>\n", scanner.getIncomplete().size()));
        html.append("<div class=\"stat-label\">Incomplete Checks</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");

        if (scanner.getTotalViolationsCount() == 0) {
            html.append("<div class=\"no-violations\">✓ No accessibility violations found!</div>\n");
        } else {
            // Group violations by URL
            Map<String, List<AccessibilityViolation>> byUrl = scanner.getViolations().stream()
                    .collect(Collectors.groupingBy(AccessibilityViolation::getUrl));

            for (Map.Entry<String, List<AccessibilityViolation>> entry : byUrl.entrySet()) {
                String url = entry.getKey();
                List<AccessibilityViolation> violations = entry.getValue();

                html.append(String.format("<h2>:page_facing_up: %s</h2>\n", escapeHtml(url)));
                html.append(String.format("<p>Violations: %d</p>\n", violations.size()));

                for (AccessibilityViolation violation : violations) {
                    String impact = violation.getImpact() != null ? violation.getImpact().toLowerCase() : "moderate";

                    html.append(String.format("<div class=\"violation %s\">\n", impact));
                    html.append("<div class=\"violation-header\">\n");
                    html.append(String.format("<span class=\"badge badge-%s\">%s</span>\n",
                            impact, impact.toUpperCase()));
                    html.append(String.format("<strong>%s</strong>\n", escapeHtml(violation.getId())));
                    html.append("</div>\n");
                    html.append(String.format("<div class=\"violation-description\">%s</div>\n",
                            escapeHtml(violation.getDescription())));

                    html.append("<div class=\"violation-meta\">\n");
                    html.append(String.format("<div class=\"meta-item\">:bar_chart: Elements: %d</div>\n",
                            violation.getNodeCount()));
                    html.append(String.format("<div class=\"meta-item\">:book: <a href=\"%s\" target=\"_blank\" class=\"help-link\">Documentation</a></div>\n",
                            violation.getHelpUrl()));
                    html.append("</div>\n");

                    // Affected elements
                    if (!violation.getNodes().isEmpty()) {
                        html.append("<div class=\"nodes-section\">\n");
                        html.append("<details>\n");
                        html.append(String.format("<summary>Show Affected Elements (%d)</summary>\n",
                                violation.getNodeCount()));

                        for (AccessibilityViolation.ViolationNode node : violation.getNodes()) {
                            html.append("<div class=\"node\">\n");
                            html.append("<div><span class=\"node-label\">Selector:</span><span class=\"node-target\">");
                            html.append(escapeHtml(node.getTarget()));
                            html.append("</span></div>\n");

                            if (node.getHtml() != null && !node.getHtml().isEmpty()) {
                                html.append("<div class=\"node-html\"><span class=\"node-label\">HTML:</span>");
                                html.append(escapeHtml(node.getHtml()));
                                html.append("</div>\n");
                            }
                            html.append("</div>\n");
                        }

                        html.append("</details>\n");
                        html.append("</div>\n");
                    }

                    html.append("</div>\n");
                }
            }
        }

        // Incomplete section
        if (!scanner.getIncomplete().isEmpty()) {
            html.append("<h2>:warning: Incomplete / Needs Review</h2>\n");
            for (AccessibilityViolation item : scanner.getIncomplete()) {
                html.append("<div class=\"violation moderate\">\n");
                html.append(String.format("<div class=\"violation-header\"><strong>%s</strong></div>\n",
                        escapeHtml(item.getId())));
                html.append(String.format("<div class=\"violation-description\">%s</div>\n",
                        escapeHtml(item.getDescription())));
                html.append(String.format("<div class=\"violation-meta\"><div class=\"meta-item\">Elements: %d</div></div>\n",
                        item.getNodeCount()));
                html.append("</div>\n");
            }
        }

        html.append("</body>\n</html>");
        return html.toString();
    }

    /**
     * Create JSON report
     */
    private static String createJsonReport(AXEScanner scanner) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"summary\": {\n");
        json.append(String.format("    \"totalViolations\": %d,\n", scanner.getTotalViolationsCount()));
        json.append(String.format("    \"incompleteChecks\": %d,\n", scanner.getIncomplete().size()));
        json.append(String.format("    \"timestamp\": \"%s\"\n", new Date()));
        json.append("  },\n");
        json.append("  \"violations\": [\n");

        List<String> violationJsons = new ArrayList<>();
        for (AccessibilityViolation violation : scanner.getViolations()) {
            StringBuilder vJson = new StringBuilder();
            vJson.append("    {\n");
            vJson.append(String.format("      \"id\": \"%s\",\n", escapeJson(violation.getId())));
            vJson.append(String.format("      \"impact\": \"%s\",\n",
                    violation.getImpact() != null ? violation.getImpact() : "moderate"));
            vJson.append(String.format("      \"description\": \"%s\",\n", escapeJson(violation.getDescription())));
            vJson.append(String.format("      \"helpUrl\": \"%s\",\n", escapeJson(violation.getHelpUrl())));
            vJson.append(String.format("      \"url\": \"%s\",\n", escapeJson(violation.getUrl())));
            vJson.append(String.format("      \"elementCount\": %d\n", violation.getNodeCount()));
            vJson.append("    }");
            violationJsons.add(vJson.toString());
        }

        json.append(String.join(",\n", violationJsons));
        json.append("\n  ]\n");
        json.append("}\n");

        return json.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}