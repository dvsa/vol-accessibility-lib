package scanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MustacheSettings {

    private String urlsScanned;
    private String violations;

    public MustacheSettings(String violations, String urlsScanned){
        this.urlsScanned = urlsScanned;
        this.violations = violations;
    }

    List<Issue> issues() throws IOException {
        return Arrays.asList(
                new Issue(violations));
    }

    List<ScannedURL> scannedURLS(){
        return Arrays.asList(
                new ScannedURL(urlsScanned));
    }

    static class Issue {
        Issue(String violations) {
            this.violations = violations;
        }
        String violations;
    }

    static class ScannedURL {
        ScannedURL(String urlsScanned) {
            this.urlsScanned = urlsScanned;
        }
        String urlsScanned;
    }
}