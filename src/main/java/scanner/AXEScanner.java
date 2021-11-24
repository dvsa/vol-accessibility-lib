package scanner;

import activesupport.IllegalBrowserException;
import activesupport.driver.Browser;
import com.deque.html.axecore.axeargs.AxeRuleOptions;
import com.deque.html.axecore.axeargs.AxeRunOptions;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.deque.html.axecore.selenium.AxeReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AXEScanner {

    public String axeFindings;
    public String findingsThatNeedReviewing;

    private int totalViolationsCount;
    private int numberOfViolationsFoundPerPage;

    private List<String> tags = Collections.singletonList(System.getProperty("standards.scan"));
    private List<String> rules = Collections.singletonList(System.getProperty("rules.scan"));
    public List<String> impact = new ArrayList<>();

    private static final Logger LOGGER = LogManager.getLogger(AXEScanner.class);

    public int getTotalViolationsCount() {
        return totalViolationsCount;
    }

    public int getNumberOfViolationsFoundPerPage() {
        return numberOfViolationsFoundPerPage;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public AXEScanner() {
        if (getTags().get(0) == null) {
            setTags(Arrays.asList("wcag2a", "wcag412", "wcag143", "wcag2aa", "cat.**","best-practice","ACT","section508",
                    "section508.*"));
        } else {
            setTags(tags);
        }
    }

    public void scan() throws IOException, IllegalBrowserException {
        AxeRunOptions runOptions = new AxeRunOptions();
        runOptions.setXPath(true);

        AxeRuleOptions enabledRules = new AxeRuleOptions();
        enabledRules.setEnabled(true);

        Results axeResponse = new AxeBuilder().withOptions(runOptions)
                .withTags(tags)
                .analyze(Browser.navigate());

        List<Rule> inapplicable = axeResponse.getInapplicable();
        List<Rule> violations = axeResponse.getViolations();

        if (violations.size() == 0) {
            LOGGER.info("No issues found");
        } else {
            numberOfViolationsFoundPerPage = violations.size();
            totalViolationsCount += violations.size();
        }
        List<List<Rule>> multiList = Arrays.asList(violations, inapplicable);
        for (List<Rule> ruleList : multiList) {
            for (Rule rule : ruleList) {
                if (!rule.getImpact().equals("")) {
                    AxeReporter.getReadableAxeResults("VIOLATIONS", Browser.navigate(), ruleList);
                    axeFindings = AxeReporter.getAxeResultString();
                    impact.add(rule.getImpact());
                } else {
                    rule.setImpact("review");
                    impact.add("review");
                    AxeReporter.getReadableAxeResults("PLEASE REVIEW", Browser.navigate(), ruleList);
                    findingsThatNeedReviewing = AxeReporter.getAxeResultString();
                }
            }
        }
    }
}