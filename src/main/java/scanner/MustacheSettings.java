package scanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MustacheSettings {

    private final String urlsScanned;
    private final String violations;
    private final String reviews;
    private String impact;
    private String colour;


    public MustacheSettings(String violations, String urlsScanned, String reviews){
        this.urlsScanned = urlsScanned;
        this.violations = violations;
        this.reviews = reviews;
    }

    List<Issue> issues() throws IOException {
        return Arrays.asList(
                new Issue(violations, reviews));
    }

    List<ScannedURL> scannedURLS(){
        return Arrays.asList(
                new ScannedURL(urlsScanned));
    }

    List<Tags> tags(){
       return Collections.singletonList(
               new Tags(impact, colour));
    }

    static class Issue {
        Issue(String violations, String reviews) {
            this.violations = violations;
            this.reviews = reviews;
        }
        String violations;
        String reviews;
    }

    static class Tags {
        Tags(String impact, String colour) {
            this.impact = impact;
            this.colour = colour;
        }
        String impact;
        String colour;
    }

    static class ScannedURL {
        ScannedURL(String urlsScanned) {
            this.urlsScanned = urlsScanned;
        }
        String urlsScanned;
    }
}