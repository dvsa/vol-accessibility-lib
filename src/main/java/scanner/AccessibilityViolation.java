package scanner;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class AccessibilityViolation {
    private final String id;
    private final String impact;
    private final String description;
    private final String help;
    private final String helpUrl;
    private final String url;
    private final String type;
    private final List<String> tags;
    private final List<ViolationNode> nodes;


    public static class ViolationNode {
        private final String html;
        private final String target;
        private final String failureSummary;

        public ViolationNode(String html, String target, String failureSummary) {
            this.html = html;
            this.target = target;
            this.failureSummary = failureSummary;
        }

        public String getHtml() {
            return html;
        }

        public String getTarget() {
            return target;
        }

        public String getFailureSummary() {
            return failureSummary;
        }
    }

    private AccessibilityViolation(String id, String impact, String description, String help,
                                   String helpUrl, String url, String type, List<String> tags,
                                   List<ViolationNode> nodes) {
        this.id = id;
        this.impact = impact;
        this.description = description;
        this.help = help;
        this.helpUrl = helpUrl;
        this.url = url;
        this.type = type;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.nodes = nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }


    public static AccessibilityViolation fromRule(Rule rule, String url, String type) {
        List<ViolationNode> nodes = new ArrayList<>();

        if (rule.getNodes() != null) {
            for (CheckedNode node : rule.getNodes()) {
                String html = node.getHtml();
                String target = node.getTarget() != null ? node.getTarget().toString() : "";
                String failureSummary = node.getFailureSummary();

                nodes.add(new ViolationNode(html, target, failureSummary));
            }
        }

        return new AccessibilityViolation(
                rule.getId(),
                rule.getImpact(),
                rule.getDescription(),
                rule.getHelp(),
                rule.getHelpUrl(),
                url,
                type,
                rule.getTags(),
                nodes
        );
    }



    public String getId() {
        return id;
    }

    public String getImpact() {
        return impact;
    }

    public String getDescription() {
        return description;
    }

    public String getHelp() {
        return help;
    }

    public String getHelpUrl() {
        return helpUrl;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public List<String> getTags() {
        return new ArrayList<>(tags);
    }

    public List<ViolationNode> getNodes() {
        return new ArrayList<>(nodes);
    }

    public int getNodeCount() {
        return nodes.size();
    }


    public List<String> getSelectors() {
        return nodes.stream()
                .map(ViolationNode::getTarget)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("AccessibilityViolation{id='%s', impact='%s', description='%s', nodes=%d}",
                id, impact, description, nodes.size());
    }
}