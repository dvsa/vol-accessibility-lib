package scanner;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Scanner;
import java.util.stream.Stream;

public class ReportGenerator {

    AXEScanner scanner = new AXEScanner();
    MustacheFactory mf = new DefaultMustacheFactory();

    private String violations;
    private String violationDetails;

    private static File temp = new File("urlScanned.txt");
    private static File tempDetailsFile = new File("details.html");
    private static File tempCompleteDetailsFile = new File("tempCompleteDetails.html");
    private static File tempCompleteDetailsWithPercentFile = new File("tempCompleteDetailsWithPercent.html");

    public void urlScannedReportSection(String scanURL) throws IOException {
        Mustache mustache = mf.compile("src/main/resources/scanned_urls.mustache");
        mustache.execute(new FileWriter(temp, true),
                new MustacheSettings(null, scanURL)).flush();
    }

    public void violationDetailsReportSection(String scanURL) throws IOException {
        File violationTemp = new File("violations.txt");
        Mustache mustache = mf.compile("src/main/resources/violations.mustache");
        Scanner sc1 = new Scanner(scanner.axeFindings());
        while (sc1.hasNext()) {
            this.violations = String.valueOf((sc1.nextLine()));
            mustache.execute(new FileWriter(violationTemp, true),
                    new MustacheSettings(violations, null)).flush();
        }
        sc1.close();

        String details = readLineByLineJava8("src/main/resources/violations.html");
        String detailsRegex = "violations";
        violationDetails = details.replace(detailsRegex, readLineByLineJava8("violations.txt"));

        BufferedWriter writer = new BufferedWriter(new FileWriter(tempDetailsFile, true));
        writer.append(violationDetails);
        writer.flush();
        writer.close();
        violationTemp.delete();


        String scannedURLs = readLineByLineJava8("details.html");
        String urlRegex = "urlScanned";
        String completeDetails = scannedURLs.replace(urlRegex, scanURL);

        BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempCompleteDetailsFile, true));
        tempWriter.append(completeDetails);
        tempWriter.flush();
        tempWriter.close();

        String complianceSectionPercent = readLineByLineJava8("tempCompleteDetails.html");
        String percentRegex = "sectionComp";
        String sectionComp = complianceSectionPercent.replace(percentRegex, Integer.toString(totalCompliancePercentage(scanner.getNumberOfViolationsFoundPerPage())));

        BufferedWriter tempDetailsWriter = new BufferedWriter(new FileWriter(tempCompleteDetailsWithPercentFile, true));
        tempDetailsWriter.append(sectionComp);
        tempDetailsWriter.flush();
        tempDetailsWriter.close();

        if (tempDetailsFile.exists() && (tempCompleteDetailsFile.exists())) {
            tempDetailsFile.delete();
            tempCompleteDetailsFile.delete();
        }
    }

    public void createReport() throws IOException {
        File urls = new File("urls.txt");
        File totalComp = new File("totalComp.txt");

        String urlList = readLineByLineJava8("src/main/resources/index.html");
        String urlRegex = "urlList";
        String index = urlList.replace(urlRegex, readLineByLineJava8("urlScanned.txt"));

        BufferedWriter writer = new BufferedWriter(new FileWriter(urls, false));
        writer.append(index);
        writer.flush();

        String totalCompPercent = readLineByLineJava8("urls.txt");
        String percentRegex = "totalComp";
        String totalCompPer = totalCompPercent.replace(percentRegex, Integer.toString(totalCompliancePercentage(scanner.getTotalViolationsCount())));

        writer = new BufferedWriter(new FileWriter(totalComp, false));
        writer.append(totalCompPer);
        writer.flush();

        String details = readLineByLineJava8("totalComp.txt");
        String detailRegex = "violation_details";
        String detailsOfViolations = details.replace(detailRegex, readLineByLineJava8("tempCompleteDetailsWithPercent.html"));

        writer = new BufferedWriter(new FileWriter(createDirectories() + "/" + String.valueOf(Instant.now().getEpochSecond()).concat("index.html"), true));
        writer.append(detailsOfViolations);
        writer.close();

        copyDirectory();

        if (urls.exists() && (totalComp.exists())) {
            urls.delete();
            totalComp.delete();
            tempCompleteDetailsWithPercentFile.delete();
            temp.delete();
        }
    }

    public File createDirectories() throws IOException {
        File directory = new File("Reports/");
        FileUtils.forceMkdir(directory);
        return directory;
    }

    public void copyDirectory() throws IOException {
        File sourceLocation = new File("src/main/resources/public");
        File targetLocation = new File("Reports/public");

        FileUtils.copyDirectory(sourceLocation, targetLocation);
    }

    private static String readLineByLineJava8(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }

    public int totalCompliancePercentage(int totalNumberOfViolation) {
        int nonAccessiblePercentage = totalNumberOfViolation * 100 / 100;
        int accessiblePercentage = 100 - nonAccessiblePercentage;
        return accessiblePercentage;
    }
}
