package scanner;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Collections;
import java.util.Scanner;
import java.util.stream.Stream;

public class ReportGenerator {

    MustacheFactory mf = new DefaultMustacheFactory();

    private static final String userDirectory = System.getProperty("user.dir");

    private static final File temp = new File(userDirectory.concat("/target/urlScanned.txt"));
    private static final File tempDetailsFile = new File(userDirectory.concat("/target/details.html"));
    private static final File tempCompleteDetailsFile = new File(userDirectory.concat("/target/tempCompleteDetails.html"));
    private static final File tempCompleteDetailsWithPercentFile = new File(userDirectory.concat("/target/tempCompleteDetailsWithPercent.html"));

    public void urlScannedReportSection(String scanURL) throws IOException {
        Mustache mustache = mf.compile("scanned_urls.mustache");
        mustache.execute(new FileWriter(temp, true),
                new MustacheSettings(null, scanURL)).flush();
    }

    public void violationDetailsReportSection(String scanURL, AXEScanner scanner) throws IOException {
        File violationTemp = new File(userDirectory.concat("/target/violations.txt"));
        Mustache mustache = mf.compile("violations.mustache");
        Scanner textScanner = new Scanner(String.valueOf(AXEScanner.axeFindings));
        while (textScanner.hasNext()) {
            String violations = String.valueOf((textScanner.nextLine()));
            mustache.execute(new FileWriter(violationTemp, true),
                    new MustacheSettings(violations, null)).flush();
        }
        textScanner.close();
        try {
            String details = readLinesAsInputStream(ReportGenerator.class.getClassLoader().getResourceAsStream("violations.html"));
            String detailsRegex = "violations";
            String violationDetails = details.replace(detailsRegex, readLines(userDirectory.concat("/target/violations.txt")));

            BufferedWriter writer = new BufferedWriter(new FileWriter(tempDetailsFile, true));
            writer.append(violationDetails);
            writer.flush();
            writer.close();
            violationTemp.delete();

            String scannedURLs = readLines(userDirectory.concat("/target/details.html"));
            String urlRegex = "urlScanned";
            String completeDetails = scannedURLs.replace(urlRegex, scanURL);

            BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempCompleteDetailsFile, true));
            tempWriter.append(completeDetails);
            tempWriter.flush();
            tempWriter.close();

            String complianceSectionPercent = readLines(userDirectory.concat("/target/tempCompleteDetails.html"));
            String percentRegex = "sectionComp";
            String sectionComp = complianceSectionPercent.replace(percentRegex, Integer.toString(totalCompliancePercentage(scanner.getNumberOfViolationsFoundPerPage())));
            BufferedWriter tempDetailsWriter = new BufferedWriter(new FileWriter(tempCompleteDetailsWithPercentFile, true));
            tempDetailsWriter.append(sectionComp);
            tempDetailsWriter.flush();
            tempDetailsWriter.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tempDetailsFile.exists() && (tempCompleteDetailsFile.exists())) {
            tempDetailsFile.delete();
            tempCompleteDetailsFile.delete();
        }
    }

    public void createReport(AXEScanner scanner) throws IOException, URISyntaxException {
        try {
            File urls = new File(userDirectory.concat("/target/urls.txt"));
            File totalComp = new File(userDirectory.concat("/target/totalComp.txt"));

            String urlList = readLinesAsInputStream(ReportGenerator.class.getClassLoader().getResourceAsStream("index.html"));
            String urlRegex = "urlList";
            String index = urlList.replace(urlRegex, readLines(userDirectory.concat("/target/urlScanned.txt")));

            BufferedWriter writer = new BufferedWriter(new FileWriter(urls, false));
            writer.append(index);
            writer.flush();
            temp.delete();

            String totalCompPercent = readLines(userDirectory.concat("/target/urls.txt"));
            String percentRegex = "totalComp";
            String totalCompPer = totalCompPercent.replace(percentRegex, Integer.toString(totalCompliancePercentage(scanner.getTotalViolationsCount())));

            writer = new BufferedWriter(new FileWriter(totalComp, false));
            writer.append(totalCompPer);
            writer.flush();

            String details = readLines(userDirectory.concat("/target/totalComp.txt"));
            String detailRegex = "violation_details";
            String detailsOfViolations = details.replace(detailRegex, readLines(userDirectory.concat("/target/tempCompleteDetailsWithPercent.html")));

            createDirectory();
            copyFromJar("/public/", Paths.get("Reports/public/"));

            writer = new BufferedWriter(new FileWriter("Reports/" + String.valueOf(Instant.now().getEpochSecond()).concat("index.html"), true));
            writer.append(detailsOfViolations);
            writer.close();

            if (urls.exists() && (totalComp.exists())) {
                urls.delete();
                totalComp.delete();
                tempCompleteDetailsWithPercentFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDirectory() {
        File file = new File("Reports");
        if (!file.exists()) {
            file.mkdir();
        }
    }

    private void copyFromJar(String source, final Path target) throws URISyntaxException, IOException {
        URI resource = getClass().getResource("").toURI();
        FileSystem fileSystem = FileSystems.newFileSystem(
                resource,
                Collections.<String, String>emptyMap()
        );

        final Path jarPath = fileSystem.getPath(source);

        Files.walkFileTree(jarPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path currentTarget = target.resolve(jarPath.relativize(dir).toString());
                Files.createDirectories(currentTarget);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(jarPath.relativize(file).toString()), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

        });
    }

    private static String readLinesAsInputStream(InputStream inputStream) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        while (reader.ready()) {
            String line = reader.readLine();
            contentBuilder.append(line).append("\n");
        }
        return contentBuilder.toString();
    }

    private static String readLines(String filePath) {
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
        return 100 - nonAccessiblePercentage;
    }
}