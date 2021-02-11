package scanner;

import activesupport.IllegalBrowserException;
import activesupport.driver.Browser;
import com.deque.html.axecore.axeargs.AxeRunOptions;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AXEScanner {

    private int totalViolationsCount;
    private int numberOfViolationsFoundPerPage;

    private String[] standards = new String[]{System.getProperty("standards.scan")};
    private String[] rules = new String[]{System.getProperty("rules.scan")};

    public String[] getStandards() {
        return standards;
    }

    public static List<Rule> axeFindings;

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

    public void scan() throws IOException, IllegalBrowserException {
        AxeRunOptions runOptions = new AxeRunOptions();
        runOptions.setXPath(true);
        runOptions.setResultTypes(Collections.singletonList("violations"));

        Results axeResponse = new AxeBuilder().withOptions(runOptions)
                .withTags(Arrays.asList("wcag2aa"))
                .withRules(Arrays.asList("accesskeys", "bypass", "focus-order-semantics", "region", "skip-link", "tabindex"))
                .exclude(Collections.singletonList("#global-footer"))
                .analyze(Browser.navigate());

        List<Rule> violations = axeResponse.getViolations();

        if (violations.size() == 0) {
            assertTrue(true, "No new issues found on page");
        } else {
            numberOfViolationsFoundPerPage = violations.size();
            totalViolationsCount += violations.size();
        }
        axeFindings = violations;
    }
}