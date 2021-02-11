package scanner;

import activesupport.IllegalBrowserException;
import activesupport.driver.Browser;
import com.deque.html.axecore.axeargs.AxeRunOptions;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.deque.html.axecore.selenium.AxeReporter;
import net.sf.json.JSONArray;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AXEScanner {

    private int totalViolationsCount;
    private int numberOfViolationsFoundPerPage;

    public static JSONArray scannerFindings;

    private String[] standards = new String[]{System.getProperty("standards.scan")};
    private String[] rules = new String[]{System.getProperty("rules.scan")};

    private JSONArray multi = null;


    public String[] getStandards() {
        return standards;
    }

    public void setStandards(String[] standards) {
        this.standards = standards;
    }

    public String[] getRules() {
        return rules;
    }

    public void setRules(String[] rules) {
        this.rules = rules;
    }


    public int getTotalViolationsCount() {
        return totalViolationsCount;
    }


    public int getNumberOfViolationsFoundPerPage() {
        return numberOfViolationsFoundPerPage;
    }

    public AXEScanner() {
        if (getStandards() == null) {
            setStandards(new String[]{"wcag2a", "wcag412", "wcag2aa"});
        }
        if (getRules().length == 0) {
            setRules(new String[]{"accesskeys", "bypass", "focus-order-semantics", "region", "skip-link", "tabindex", "cat.color"});
        }
    }
    public void scan() throws IOException, IllegalBrowserException {
        AxeRunOptions runOptions = new AxeRunOptions();
        runOptions.setXPath(true);
        runOptions.setResultTypes(Collections.singletonList("violations"));

        AxeBuilder axeResponse = new AxeBuilder().withOptions(runOptions)
                .withTags(Arrays.asList(getStandards()))
                .withRules(Arrays.asList(getRules()))
                .exclude(Collections.singletonList("#global-footer"));

        Results results = axeResponse.analyze(Browser.navigate());
        List<Rule> violations = results.getViolations();

        if (violations.size() == 0) {
            assertTrue(true, "No new issues found on page");
        } else {
            numberOfViolationsFoundPerPage = violations.size();
            totalViolationsCount += violations.size();
            multi = (JSONArray) violations;
        }
        AxeReporter.writeResultsToTextFile("target/results/testAccessibilityResults", results);
    }
}