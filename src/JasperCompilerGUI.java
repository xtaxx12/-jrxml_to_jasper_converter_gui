import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.view.JasperViewer;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JasperCompilerGUI extends JFrame {
    private JTextArea logArea;
    private JButton selectFilesButton;
    private JButton selectFolderButton;
    private JButton compileButton;
    private JButton previewButton;
    private JButton clearButton;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private List<File> selectedFiles;

    public JasperCompilerGUI() {
        setTitle("Jasper Report Compiler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 550);
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
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setBorder(BorderFactory.createTitledBorder("Archivos JRXML seleccionados (0) - Arrastre archivos aquí"));
        fileScrollPane.setPreferredSize(new Dimension(730, 150));

        // Log de compilación
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Compilación"));
        
        splitPane.setTopComponent(fileScrollPane);
        splitPane.setBottomComponent(logScrollPane);
        
        // Panel inferior con botones de acción
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        compileButton = new JButton("Compilar Todo");
        compileButton.setEnabled(false);
        compileButton.setPreferredSize(new Dimension(140, 35));
        compileButton.addActionListener(e -> compileReports());
        
        previewButton = new JButton("Vista Previa");
        previewButton.setEnabled(false);
        previewButton.setPreferredSize(new Dimension(140, 35));
        previewButton.setToolTipText("Seleccione un archivo de la lista para previsualizar");
        previewButton.addActionListener(e -> previewReport());
        
        bottomPanel.add(compileButton);
        bottomPanel.add(previewButton);
        
        // Listener para habilitar vista previa cuando se selecciona un archivo
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreviewButton();
            }
        });
        
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Configurar Drag & Drop
        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (isDragAcceptable(dtde)) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                    fileList.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 2));
                } else {
                    dtde.rejectDrag();
                }
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // No action needed
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                // No action needed
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                fileList.setBorder(null);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                fileList.setBorder(null);
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();
                    
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        processDroppedFiles(droppedFiles);
                    }
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    log("✗ Error al procesar archivos arrastrados: " + e.getMessage());
                    dtde.dropComplete(false);
                }
            }

            private boolean isDragAcceptable(DropTargetDragEvent dtde) {
                return dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
        }, true);
    }

    private void processDroppedFiles(List<File> droppedFiles) {
        List<File> jrxmlFiles = new ArrayList<>();
        
        for (File file : droppedFiles) {
            if (file.isDirectory()) {
                File[] filesInDir = file.listFiles((dir, name) -> name.toLowerCase().endsWith(".jrxml"));
                if (filesInDir != null) {
                    for (File f : filesInDir) {
                        jrxmlFiles.add(f);
                    }
                }
            } else if (file.getName().toLowerCase().endsWith(".jrxml")) {
                jrxmlFiles.add(file);
            }
        }
        
        if (jrxmlFiles.isEmpty()) {
            log("⚠ No se encontraron archivos JRXML en los elementos arrastrados");
            return;
        }
        
        addFilesToList(jrxmlFiles.toArray(new File[0]));
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
        previewButton.setEnabled(false);
        log("Lista de archivos limpiada.");
    }

    private void updateFileListTitle() {
        JScrollPane scrollPane = (JScrollPane) fileList.getParent().getParent();
        String title = "Archivos JRXML seleccionados (" + selectedFiles.size() + ")";
        if (selectedFiles.isEmpty()) {
            title += " - Arrastre archivos aquí";
        }
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
    }

    private void updatePreviewButton() {
        int selectedIndex = fileList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < selectedFiles.size()) {
            File selectedFile = selectedFiles.get(selectedIndex);
            String jasperPath = "output/" + selectedFile.getName().replace(".jrxml", ".jasper");
            File jasperFile = new File(jasperPath);
            previewButton.setEnabled(jasperFile.exists());
            if (!jasperFile.exists()) {
                previewButton.setToolTipText("Compile primero el archivo seleccionado");
            } else {
                previewButton.setToolTipText("Ver vista previa de: " + jasperFile.getName());
            }
        } else {
            previewButton.setEnabled(false);
            previewButton.setToolTipText("Seleccione un archivo de la lista para previsualizar");
        }
    }

    private void previewReport() {
        int selectedIndex = fileList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= selectedFiles.size()) {
            JOptionPane.showMessageDialog(this, 
                "Por favor seleccione un archivo de la lista para previsualizar",
                "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File selectedFile = selectedFiles.get(selectedIndex);
        String jasperPath = "output/" + selectedFile.getName().replace(".jrxml", ".jasper");
        File jasperFile = new File(jasperPath);

        if (!jasperFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "El archivo .jasper no existe. Compile primero el reporte.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        log("Generando vista previa de: " + jasperFile.getName() + "...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    JasperPrint jasperPrint = JasperFillManager.fillReport(
                        jasperFile.getPath(),
                        new HashMap<>(),
                        new JREmptyDataSource()
                    );
                    
                    SwingUtilities.invokeLater(() -> {
                        JasperViewer viewer = new JasperViewer(jasperPrint, false);
                        viewer.setTitle("Vista Previa - " + jasperFile.getName());
                        viewer.setVisible(true);
                        log("✓ Vista previa abierta correctamente");
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        log("✗ Error al generar vista previa: " + e.getMessage());
                        JOptionPane.showMessageDialog(JasperCompilerGUI.this,
                            "Error al generar vista previa:\n" + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                setButtonsEnabled(true);
                updatePreviewButton();
            }
        };
        worker.execute();
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
                updatePreviewButton();
            }
        };
        
        worker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        compileButton.setEnabled(enabled && !selectedFiles.isEmpty());
        selectFilesButton.setEnabled(enabled);
        selectFolderButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        previewButton.setEnabled(enabled && fileList.getSelectedIndex() >= 0);
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
