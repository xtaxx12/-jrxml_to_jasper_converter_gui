import net.sf.jasperreports.engine.JasperCompileManager;
import java.io.File;

public class JasperCompiler {
    public static void main(String[] args) {
        try {
            // Compile all JRXML files in input folder
            File inputDir = new File("input");
            File[] jrxmlFiles = inputDir.listFiles((dir, name) -> name.endsWith(".jrxml"));
            
            if (jrxmlFiles == null || jrxmlFiles.length == 0) {
                System.err.println("No JRXML files found in input folder");
                return;
            }
            
            for (File jrxmlFile : jrxmlFiles) {
                String inputFile = jrxmlFile.getPath();
                String outputFile = "output/" + jrxmlFile.getName().replace(".jrxml", ".jasper");

                System.out.println("\nCompiling: " + jrxmlFile.getName());
                long start = System.currentTimeMillis();

                JasperCompileManager.compileReportToFile(inputFile, outputFile);

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
}