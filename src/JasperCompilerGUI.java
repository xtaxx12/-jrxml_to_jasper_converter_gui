import net.sf.jasperreports.engine.JasperCompileManager;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JasperCompilerGUI extends JFrame {
    private JTextArea logArea;
    private JButton selectFilesButton;
    private JButton selectFolderButton;
    private JButton compileButton;
    private JButton clearButton;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private List<File> selectedFiles;

    public JasperCompilerGUI() {
        setTitle("Jasper Report Compiler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        selectedFiles = new ArrayList<>();
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Panel superior con botones de selección
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        selectFilesButton = new JButton("Seleccionar Archivos...");
        selectFilesButton.addActionListener(e -> selectFiles());
        
        selectFolderButton = new JButton("Seleccionar Carpeta...");
        selectFolderButton.addActionListener(e -> selectFolder());
        
        clearButton = new JButton("Limpiar Lista");
        clearButton.addActionListener(e -> clearFileList());
        
        topPanel.add(selectFilesButton);
        topPanel.add(selectFolderButton);
        topPanel.add(clearButton);
        
        // Panel central dividido: lista de archivos y log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        
        // Lista de archivos seleccionados
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setBorder(BorderFactory.createTitledBorder("Archivos JRXML seleccionados (0)"));
        fileScrollPane.setPreferredSize(new Dimension(680, 150));

        // Log de compilación
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Compilación"));
        
        splitPane.setTopComponent(fileScrollPane);
        splitPane.setBottomComponent(logScrollPane);
        
        // Panel inferior con botón de compilar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        compileButton = new JButton("Compilar Todo");
        compileButton.setEnabled(false);
        compileButton.setPreferredSize(new Dimension(150, 35));
        compileButton.addActionListener(e -> compileReports());
        
        bottomPanel.add(compileButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("input"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JRXML (*.jrxml)", "jrxml"));
        fileChooser.setMultiSelectionEnabled(true);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            addFilesToList(files);
        }
    }

    private void selectFolder() {
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setCurrentDirectory(new File("input"));
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int result = folderChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = folderChooser.getSelectedFile();
            File[] jrxmlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jrxml"));
            
            if (jrxmlFiles == null || jrxmlFiles.length == 0) {
                log("⚠ No se encontraron archivos JRXML en: " + folder.getName());
                return;
            }
            addFilesToList(jrxmlFiles);
        }
    }

    private void addFilesToList(File[] files) {
        for (File file : files) {
            if (!selectedFiles.contains(file)) {
                selectedFiles.add(file);
                fileListModel.addElement(file.getAbsolutePath());
            }
        }
        updateFileListTitle();
        compileButton.setEnabled(!selectedFiles.isEmpty());
        log("✓ " + files.length + " archivo(s) agregado(s). Total: " + selectedFiles.size());
    }

    private void clearFileList() {
        selectedFiles.clear();
        fileListModel.clear();
        updateFileListTitle();
        compileButton.setEnabled(false);
        log("Lista de archivos limpiada.");
    }

    private void updateFileListTitle() {
        JScrollPane scrollPane = (JScrollPane) fileList.getParent().getParent();
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            "Archivos JRXML seleccionados (" + selectedFiles.size() + ")"));
    }

    private void compileReports() {
        if (selectedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor seleccione al menos un archivo JRXML",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            private int successCount = 0;
            private int errorCount = 0;

            @Override
            protected Void doInBackground() {
                new File("output").mkdirs();
                publish("\n═══════════════════════════════════════════");
                publish("Iniciando compilación de " + selectedFiles.size() + " archivo(s)...");
                publish("═══════════════════════════════════════════\n");
                
                long totalStart = System.currentTimeMillis();
                
                for (File file : selectedFiles) {
                    compileFile(file);
                }
                
                long totalEnd = System.currentTimeMillis();
                publish("\n═══════════════════════════════════════════");
                publish("RESUMEN DE COMPILACIÓN");
                publish("───────────────────────────────────────────");
                publish("  ✓ Exitosos: " + successCount);
                publish("  ✗ Errores:  " + errorCount);
                publish("  Tiempo total: " + (totalEnd - totalStart) + "ms");
                publish("═══════════════════════════════════════════\n");
                
                return null;
            }

            private void compileFile(File file) {
                try {
                    String inputFile = file.getPath();
                    String outputFile = "output/" + file.getName().replace(".jrxml", ".jasper");
                    
                    publish("Compilando: " + file.getName() + "...");
                    long start = System.currentTimeMillis();
                    
                    JasperCompileManager.compileReportToFile(inputFile, outputFile);
                    
                    long end = System.currentTimeMillis();
                    publish("  ✓ OK (" + (end - start) + "ms) → " + outputFile);
                    successCount++;
                } catch (Exception e) {
                    publish("  ✗ ERROR: " + e.getMessage());
                    errorCount++;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    log(message);
                }
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
            }
        };
        
        worker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        compileButton.setEnabled(enabled && !selectedFiles.isEmpty());
        selectFilesButton.setEnabled(enabled);
        selectFolderButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Error al configurar Look and Feel: " + e.getMessage());
            }
            new JasperCompilerGUI().setVisible(true);
        });
    }
}
