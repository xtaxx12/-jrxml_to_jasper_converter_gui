import net.sf.jasperreports.engine.JasperCompileManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JasperCompiler {
    private static final Pattern ROOT_UUID_PATTERN = Pattern.compile(
        "(?s)(<jasperReport\\b[^>]*?)\\s+uuid\\s*=\\s*(\"[^\"]*\"|'[^']*')"
    );
    private static final Pattern OPEN_QUERY_PATTERN = Pattern.compile("<query(\\s[^>]*)?>");
    private static final Pattern CLOSE_QUERY_PATTERN = Pattern.compile("</query>");
    private static final Pattern VARIABLE_BLOCK_PATTERN = Pattern.compile("(?s)<variable\\b[^>]*>.*?</variable>");
    private static final Pattern GROUP_BLOCK_PATTERN = Pattern.compile("(?s)<group\\b[^>]*>.*?</group>");
    private static final Pattern OPEN_EXPRESSION_PATTERN = Pattern.compile("<expression(\\s[^>]*)?>");
    private static final Pattern CLOSE_EXPRESSION_PATTERN = Pattern.compile("</expression>");

    public static void main(String[] args) {
        try {
            // Compile all JRXML files in input folder
            File inputDir = new File("input");
            File[] jrxmlFiles = inputDir.listFiles((dir, name) -> name.endsWith(".jrxml"));
            new File("output").mkdirs();
            
            if (jrxmlFiles == null || jrxmlFiles.length == 0) {
                System.err.println("No JRXML files found in input folder");
                return;
            }
            
            for (File jrxmlFile : jrxmlFiles) {
                File effectiveInput = jrxmlFile;
                File tempSanitizedFile = null;
                String outputFile = "output/" + jrxmlFile.getName().replace(".jrxml", ".jasper");

                System.out.println("\nCompiling: " + jrxmlFile.getName());
                long start = System.currentTimeMillis();

                try {
                    SanitizeResult sanitizeResult = createSanitizedJrxmlCopyIfNeeded(jrxmlFile);
                    effectiveInput = sanitizeResult.file;
                    if (!effectiveInput.equals(jrxmlFile)) {
                        tempSanitizedFile = effectiveInput;
                        if (sanitizeResult.removedRootUuid) {
                            System.out.println("  ! Root uuid detected: automatic sanitization applied");
                        }
                        if (sanitizeResult.convertedQueryTag) {
                            System.out.println("  ! <query> tag detected: converted to <queryString>");
                        }
                        if (sanitizeResult.convertedVariableExpressionTag) {
                            System.out.println("  ! <expression> tag in variable detected: converted to <variableExpression>");
                        }
                        if (sanitizeResult.convertedGroupExpressionTag) {
                            System.out.println("  ! <expression> tag in group detected: converted to <groupExpression>");
                        }
                    }

                    JasperCompileManager.compileReportToFile(effectiveInput.getPath(), outputFile);
                } finally {
                    if (tempSanitizedFile != null && tempSanitizedFile.exists() && !tempSanitizedFile.delete()) {
                        System.out.println("  ! Warning: could not delete temp file " + tempSanitizedFile.getName());
                    }
                }

                long end = System.currentTimeMillis();
                System.out.println("✓ Compilation successful!");
                System.out.println("  Output: " + outputFile);
                System.out.println("  Time: " + (end - start) + "ms");
            }
        } catch (Exception e) {
            System.err.println("Error compiling report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static SanitizeResult createSanitizedJrxmlCopyIfNeeded(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        boolean removedRootUuid = false;
        boolean convertedQueryTag = false;
        boolean convertedVariableExpressionTag = false;
        boolean convertedGroupExpressionTag = false;

        Matcher uuidMatcher = ROOT_UUID_PATTERN.matcher(content);
        if (uuidMatcher.find()) {
            content = uuidMatcher.replaceFirst("$1");
            removedRootUuid = true;
        }

        Matcher openQueryMatcher = OPEN_QUERY_PATTERN.matcher(content);
        if (openQueryMatcher.find()) {
            content = openQueryMatcher.replaceAll("<queryString$1>");
            content = CLOSE_QUERY_PATTERN.matcher(content).replaceAll("</queryString>");
            convertedQueryTag = true;
        }

        Matcher variableBlockMatcher = VARIABLE_BLOCK_PATTERN.matcher(content);
        StringBuffer convertedContent = new StringBuffer();
        while (variableBlockMatcher.find()) {
            String variableBlock = variableBlockMatcher.group();
            String updatedBlock = OPEN_EXPRESSION_PATTERN.matcher(variableBlock).replaceAll("<variableExpression$1>");
            updatedBlock = CLOSE_EXPRESSION_PATTERN.matcher(updatedBlock).replaceAll("</variableExpression>");
            if (!updatedBlock.equals(variableBlock)) {
                convertedVariableExpressionTag = true;
            }
            variableBlockMatcher.appendReplacement(convertedContent, Matcher.quoteReplacement(updatedBlock));
        }
        variableBlockMatcher.appendTail(convertedContent);
        if (convertedVariableExpressionTag) {
            content = convertedContent.toString();
        }

        Matcher groupBlockMatcher = GROUP_BLOCK_PATTERN.matcher(content);
        StringBuffer groupConvertedContent = new StringBuffer();
        while (groupBlockMatcher.find()) {
            String groupBlock = groupBlockMatcher.group();
            String updatedBlock = OPEN_EXPRESSION_PATTERN.matcher(groupBlock).replaceAll("<groupExpression$1>");
            updatedBlock = CLOSE_EXPRESSION_PATTERN.matcher(updatedBlock).replaceAll("</groupExpression>");
            if (!updatedBlock.equals(groupBlock)) {
                convertedGroupExpressionTag = true;
            }
            groupBlockMatcher.appendReplacement(groupConvertedContent, Matcher.quoteReplacement(updatedBlock));
        }
        groupBlockMatcher.appendTail(groupConvertedContent);
        if (convertedGroupExpressionTag) {
            content = groupConvertedContent.toString();
        }

        if (!removedRootUuid && !convertedQueryTag && !convertedVariableExpressionTag && !convertedGroupExpressionTag) {
            return new SanitizeResult(file, false, false, false, false);
        }
        Path tempDir = new File("output/.tmp").toPath();
        Files.createDirectories(tempDir);

        String baseName = file.getName().replace(".jrxml", "");
        Path tempFile = Files.createTempFile(tempDir, baseName + "-sanitized-", ".jrxml");
        Files.write(tempFile, content.getBytes(StandardCharsets.UTF_8));
        return new SanitizeResult(tempFile.toFile(), removedRootUuid, convertedQueryTag,
            convertedVariableExpressionTag, convertedGroupExpressionTag);
    }

    private static class SanitizeResult {
        private final File file;
        private final boolean removedRootUuid;
        private final boolean convertedQueryTag;
        private final boolean convertedVariableExpressionTag;
        private final boolean convertedGroupExpressionTag;

        private SanitizeResult(File file, boolean removedRootUuid, boolean convertedQueryTag,
                              boolean convertedVariableExpressionTag, boolean convertedGroupExpressionTag) {
            this.file = file;
            this.removedRootUuid = removedRootUuid;
            this.convertedQueryTag = convertedQueryTag;
            this.convertedVariableExpressionTag = convertedVariableExpressionTag;
            this.convertedGroupExpressionTag = convertedGroupExpressionTag;
        }
    }
}