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
import java.util.*;
import java.util.stream.Stream;

public class ReportGenerator {
    Set<String> uniqueImpactString = new HashSet<>();
    MustacheFactory mf = new DefaultMustacheFactory();

    private static final String userDirectory = System.getProperty("user.dir");

    private static final File temp = new File(userDirectory.concat("/target/urlScanned.txt"));
    private static final File tempDetailsFile = new File(userDirectory.concat("/target/details.html"));
    private static final File tempCompleteDetailsFile = new File(userDirectory.concat("/target/tempCompleteDetails.html"));
    private static final File tempCompleteDetailsWithPercentFile = new File(userDirectory.concat("/target/tempCompleteDetailsWithPercent.html"));

    private String executeTemplate(Mustache m, Map<String, Object> context) throws IOException {
        String impact;
        StringWriter writer = new StringWriter();
        m.execute(writer, context).flush();
        impact = readLinesAndRemoveDuplicate(writer.toString());
        return impact;
    }

    public void urlScannedReportSection(String scanURL) throws IOException {
        Mustache mustache = mf.compile("scanned_urls.mustache");
        mustache.execute(new FileWriter(temp, true),
                new MustacheSettings(null, scanURL, null)).flush();
    }

    public String createImpactHTML(AXEScanner scanner) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String impactStatus;

        Map<String, Object> context = new HashMap<>();
        Mustache impactTemplate = mf.compile("tags.mustache");
        List<MustacheSettings.Tags> tags;
        for (String impact : scanner.impact) {
            impactStatus = impact;
            switch (impactStatus) {
                case "critical":
                    tags = Collections.singletonList(
                            new MustacheSettings.Tags("critical", "red"));
                    context.put("tags", tags);
                    stringBuilder.append(executeTemplate(impactTemplate, context));
                    break;
                case "serious":
                    tags = Collections.singletonList(
                            new MustacheSettings.Tags("serious", "pink"));
                    context.put("tags", tags);
                    stringBuilder.append(executeTemplate(impactTemplate, context));
                    break;
                case "minor":
                    tags = Collections.singletonList(
                            new MustacheSettings.Tags("minor", "blue"));
                    context.put("tags", tags);
                    stringBuilder.append(executeTemplate(impactTemplate, context));
                    break;
                case "moderate":
                    tags = Collections.singletonList(
                            new MustacheSettings.Tags("moderate", "purple"));
                    context.put("tags", tags);
                    stringBuilder.append(executeTemplate(impactTemplate, context));
                    break;
                case "review":
                    tags = Collections.singletonList(
                            new MustacheSettings.Tags("review", "turquoise"));
                    context.put("tags", tags);
                    stringBuilder.append(executeTemplate(impactTemplate, context));
                    break;
            }
        }
        return stringBuilder.toString();
    }

    public void violationsReportSectionHTML(String scanURL, AXEScanner scanner) throws IOException {
        StringBuilder tags = new StringBuilder();
        createImpactHTML(scanner);
        for(String impact : uniqueImpactString){
            tags.append(impact);
        }
        Mustache violationsTemplate = mf.compile("violations.mustache");
        MustacheSettings updateSections = new MustacheSettings(scanner.axeFindings, scanURL, scanner.findingsThatNeedReviewing);
        StringWriter sectionWriter = new StringWriter();
        violationsTemplate.execute(sectionWriter, updateSections).flush();
        String html = sectionWriter.toString();
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempDetailsFile, true));
        writer.append(html);
        writer.flush();
        writer.close();
        try {
            String scannedURLs = readLines(userDirectory.concat("/target/details.html"));
            BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempCompleteDetailsFile, true));
            String formattedText = null;
            String[] placeHolderText = new String[]{"urlScanned", "tags"};
            String replacementString;
            for (String placeholder : placeHolderText) {
                if (placeholder.equals("urlScanned")) {
                    replacementString = scanURL;
                    formattedText = scannedURLs.replace(placeholder, replacementString);
                } else {
                    replacementString = tags.toString();
                    assert formattedText != null;
                    formattedText = formattedText.replace(placeholder, replacementString);
                }
            }
            tempWriter.append(formattedText);
            tempWriter.flush();
            tempWriter.close();

            String complianceSectionPercent = readLines(userDirectory.concat("/target/tempCompleteDetails.html"));
            String percentRegex = "sectionComp";
            String sectionComp = complianceSectionPercent.replace(percentRegex, Integer.toString(totalCompliancePercentage(scanner.getNumberOfViolationsFoundPerPage())));
            BufferedWriter tempDetailsWriter = new BufferedWriter(new FileWriter(tempCompleteDetailsWithPercentFile, true));
            tempDetailsWriter.append(sectionComp);
            tempDetailsWriter.flush();
            tempDetailsWriter.close();
        } catch (
                Exception e) {
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

    private String readLinesAndRemoveDuplicate(String content) {
        uniqueImpactString.add(content);
        return uniqueImpactString.toString();
    }

    public int totalCompliancePercentage(int totalNumberOfViolation) {
        int nonAccessiblePercentage = totalNumberOfViolation * 100 / 100;
        return 100 - nonAccessiblePercentage;
    }
}