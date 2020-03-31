package scanner;

import activesupport.IllegalBrowserException;
import activesupport.driver.Browser;
import com.deque.axe.AXE;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class AXEScanner {

    private int totalViolationsCount;
    private int numberOfViolationsFoundPerPage;

    private String standards = System.getProperty("standards.scan");
    private String[] rules = new String[]{System.getProperty("rules.scan")};

    private String urlsList;

    private JSONArray multi = null;
    private JSONArray violationsFound;

    private String findings;

    public String getUrlsList() {
        return urlsList;
    }

    public String setUrlsList(String urlsList) {
        this.urlsList = urlsList;
        return urlsList;
    }

    public String getStandards() {
        return standards;
    }

    public void setStandards(String standards) {
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

    public void setTotalViolationsCount(int totalViolationsCount) {
        this.totalViolationsCount = totalViolationsCount;
    }

    public int getNumberOfViolationsFoundPerPage() {
        return numberOfViolationsFoundPerPage;
    }

    public void setNumberOfViolationsFoundPerPage(int numberOfViolationsFoundPerPage) {
        this.numberOfViolationsFoundPerPage = numberOfViolationsFoundPerPage;
    }

    private static final URL scriptUrl = AXEScanner.class.getResource("/axe/axe.min.js");

    public AXEScanner() {
        if (getStandards() == null) {
            setStandards("wcag21aa");
        }
        if (getRules().length == 0) {
            setRules(new String[]{"accesskeys', 'bypass', 'focus-order-semantics', 'region', 'skip-link','tabindex'"});
        }
    }

    public void scan() throws IOException, IllegalBrowserException {
        JSONObject axeResponse = new AXE.Builder(Browser.navigate(), scriptUrl)
                .options("{runOnly:{type: 'tag', values:" + getStandards() + "}}")
                .options("{runOnly:{type: 'rule', values:" + getRules() + "}}")
                .exclude("#global-footer")
                .options("{resultTypes:['violations']}")
                .analyze();

        JSONArray violationsFound = axeResponse.getJSONArray("violations");

        if (violationsFound.length() == 0) {
            assertTrue(true, "No new issues found on page");
        } else {
            numberOfViolationsFoundPerPage = violationsFound.length();
            totalViolationsCount += violationsFound.length();
            multi = violationsFound;
        }
    }

    public String axeFindings() throws IOException {
       return AXE.report((multi));
    }
}
