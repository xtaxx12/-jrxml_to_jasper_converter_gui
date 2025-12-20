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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;

public class JasperCompilerGUI extends JFrame {
    private JTextArea logArea;
    private JButton selectFilesButton;
    private JButton selectFolderButton;
    private JButton compileButton;
    private JButton previewButton;
    private JButton clearButton;
    private JButton themeButton;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private List<File> selectedFiles;
    private JProgressBar progressBar;
    private JComboBox<String> historyComboBox;
    private DefaultComboBoxModel<String> historyModel;
    private boolean isDarkTheme = false;
    private Preferences prefs;
    private static final int MAX_HISTORY = 10;
    
    // Colores para temas
    private static final Color DARK_BG = new Color(45, 45, 45);
    private static final Color DARK_FG = new Color(220, 220, 220);
    private static final Color DARK_PANEL = new Color(60, 60, 60);
    private static final Color LIGHT_BG = new Color(240, 240, 240);
    private static final Color LIGHT_FG = new Color(30, 30, 30);

    public JasperCompilerGUI() {
        setTitle("Jasper Report Compiler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        selectedFiles = new ArrayList<>();
        prefs = Preferences.userNodeForPackage(JasperCompilerGUI.class);
        isDarkTheme = prefs.getBoolean("darkTheme", false);
        initComponents();
        setupKeyboardShortcuts();
        loadHistory();
        applyTheme();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Panel superior con botones de selección
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        selectFilesButton = new JButton("Abrir Archivos (Ctrl+O)");
        selectFilesButton.addActionListener(e -> selectFiles());
        
        selectFolderButton = new JButton("Abrir Carpeta");
        selectFolderButton.addActionListener(e -> selectFolder());
        
        clearButton = new JButton("Limpiar");
        clearButton.addActionListener(e -> clearFileList());
        
        themeButton = new JButton("🌙");
        themeButton.setToolTipText("Cambiar tema");
        themeButton.setPreferredSize(new Dimension(45, 25));
        themeButton.addActionListener(e -> toggleTheme());
        
        buttonsPanel.add(selectFilesButton);
        buttonsPanel.add(selectFolderButton);
        buttonsPanel.add(clearButton);
        buttonsPanel.add(Box.createHorizontalStrut(20));
        buttonsPanel.add(themeButton);

        // Panel de historial
        JPanel historyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JLabel historyLabel = new JLabel("Recientes:");
        historyModel = new DefaultComboBoxModel<>();
        historyComboBox = new JComboBox<>(historyModel);
        historyComboBox.setPreferredSize(new Dimension(250, 25));
        historyComboBox.addActionListener(e -> loadFromHistory());
        historyPanel.add(historyLabel);
        historyPanel.add(historyComboBox);
        
        topPanel.add(buttonsPanel, BorderLayout.WEST);
        topPanel.add(historyPanel, BorderLayout.EAST);

        // Panel central dividido: lista de archivos y log
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        
        // Lista de archivos seleccionados
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setBorder(BorderFactory.createTitledBorder("Archivos JRXML (0) - Arrastre aquí o Ctrl+O"));
        fileScrollPane.setPreferredSize(new Dimension(780, 150));

        // Log de compilación
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Compilación"));
        
        splitPane.setTopComponent(fileScrollPane);
        splitPane.setBottomComponent(logScrollPane);
        
        // Panel inferior con barra de progreso y botones
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        // Barra de progreso
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Listo");
        progressBar.setValue(0);
        
        // Botones de acción
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        
        compileButton = new JButton("Compilar (Ctrl+Enter)");
        compileButton.setEnabled(false);
        compileButton.setPreferredSize(new Dimension(160, 35));
        compileButton.addActionListener(e -> compileReports());
        
        previewButton = new JButton("Vista Previa");
        previewButton.setEnabled(false);
        previewButton.setPreferredSize(new Dimension(140, 35));
        previewButton.setToolTipText("Seleccione un archivo de la lista para previsualizar");
        previewButton.addActionListener(e -> previewReport());
        
        actionPanel.add(compileButton);
        actionPanel.add(previewButton);
        
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(actionPanel, BorderLayout.CENTER);
        
        // Listener para habilitar vista previa cuando se selecciona un archivo
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreviewButton();
            }
        });
        
        add(topPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        setupDragAndDrop();
    }

    private void setupKeyboardShortcuts() {
        // Ctrl+O para abrir archivos
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK), "openFiles");
        getRootPane().getActionMap().put("openFiles", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selectFilesButton.isEnabled()) selectFiles();
            }
        });
        
        // Ctrl+Enter para compilar
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "compile");
        getRootPane().getActionMap().put("compile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (compileButton.isEnabled()) compileReports();
            }
        });
        
        // Ctrl+L para limpiar
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), "clear");
        getRootPane().getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (clearButton.isEnabled()) clearFileList();
            }
        });
        
        // Ctrl+T para cambiar tema
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK), "toggleTheme");
        getRootPane().getActionMap().put("toggleTheme", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleTheme();
            }
        });
    }

    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        prefs.putBoolean("darkTheme", isDarkTheme);
        applyTheme();
        themeButton.setText(isDarkTheme ? "☀️" : "🌙");
        log("Tema cambiado a: " + (isDarkTheme ? "Oscuro" : "Claro"));
    }

    private void applyTheme() {
        Color bg = isDarkTheme ? DARK_BG : LIGHT_BG;
        Color fg = isDarkTheme ? DARK_FG : LIGHT_FG;
        Color panelBg = isDarkTheme ? DARK_PANEL : Color.WHITE;
        
        getContentPane().setBackground(bg);
        applyThemeToComponent(getContentPane(), bg, fg, panelBg);
        
        if (logArea != null) {
            logArea.setBackground(panelBg);
            logArea.setForeground(fg);
            logArea.setCaretColor(fg);
        }
        if (fileList != null) {
            fileList.setBackground(panelBg);
            fileList.setForeground(fg);
        }
        
        themeButton.setText(isDarkTheme ? "☀️" : "🌙");
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void applyThemeToComponent(Container container, Color bg, Color fg, Color panelBg) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                comp.setBackground(bg);
                applyThemeToComponent((Container) comp, bg, fg, panelBg);
            } else if (comp instanceof JButton) {
                comp.setBackground(panelBg);
                comp.setForeground(fg);
            } else if (comp instanceof JLabel) {
                comp.setForeground(fg);
            } else if (comp instanceof JComboBox) {
                comp.setBackground(panelBg);
                comp.setForeground(fg);
            } else if (comp instanceof JSplitPane) {
                ((JSplitPane) comp).setBackground(bg);
                applyThemeToComponent((Container) comp, bg, fg, panelBg);
            } else if (comp instanceof JScrollPane) {
                comp.setBackground(bg);
                applyThemeToComponent((Container) comp, bg, fg, panelBg);
            }
        }
    }


    private void loadHistory() {
        String history = prefs.get("recentFiles", "");
        historyModel.removeAllElements();
        historyModel.addElement("-- Seleccionar archivo reciente --");
        if (!history.isEmpty()) {
            String[] files = history.split("\\|");
            for (String file : files) {
                if (!file.isEmpty() && new File(file).exists()) {
                    historyModel.addElement(file);
                }
            }
        }
    }

    private void saveToHistory(File file) {
        String path = file.getAbsolutePath();
        String history = prefs.get("recentFiles", "");
        List<String> files = new ArrayList<>();
        
        files.add(path);
        if (!history.isEmpty()) {
            for (String f : history.split("\\|")) {
                if (!f.equals(path) && files.size() < MAX_HISTORY) {
                    files.add(f);
                }
            }
        }
        
        prefs.put("recentFiles", String.join("|", files));
        loadHistory();
    }

    private void loadFromHistory() {
        int index = historyComboBox.getSelectedIndex();
        if (index > 0) {
            String path = (String) historyComboBox.getSelectedItem();
            File file = new File(path);
            if (file.exists()) {
                addFilesToList(new File[]{file});
            } else {
                log("⚠ El archivo ya no existe: " + path);
            }
            historyComboBox.setSelectedIndex(0);
        }
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
            public void dragOver(DropTargetDragEvent dtde) {}

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {}

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
                    log("✗ Error al procesar archivos: " + e.getMessage());
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
            log("⚠ No se encontraron archivos JRXML");
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
            addFilesToList(fileChooser.getSelectedFiles());
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
                saveToHistory(file);
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
        progressBar.setValue(0);
        progressBar.setString("Listo");
        log("Lista limpiada.");
    }

    private void updateFileListTitle() {
        JScrollPane scrollPane = (JScrollPane) fileList.getParent().getParent();
        String title = "Archivos JRXML (" + selectedFiles.size() + ")";
        if (selectedFiles.isEmpty()) {
            title += " - Arrastre aquí o Ctrl+O";
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
            previewButton.setToolTipText(jasperFile.exists() ? 
                "Ver: " + jasperFile.getName() : "Compile primero");
        } else {
            previewButton.setEnabled(false);
            previewButton.setToolTipText("Seleccione un archivo");
        }
    }


    private void previewReport() {
        int selectedIndex = fileList.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= selectedFiles.size()) {
            JOptionPane.showMessageDialog(this, 
                "Seleccione un archivo de la lista", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File selectedFile = selectedFiles.get(selectedIndex);
        String jasperPath = "output/" + selectedFile.getName().replace(".jrxml", ".jasper");
        File jasperFile = new File(jasperPath);

        if (!jasperFile.exists()) {
            JOptionPane.showMessageDialog(this,
                "Compile primero el reporte.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        log("Abriendo vista previa: " + jasperFile.getName() + "...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    JasperPrint jasperPrint = JasperFillManager.fillReport(
                        jasperFile.getPath(), new HashMap<>(), new JREmptyDataSource());
                    
                    SwingUtilities.invokeLater(() -> {
                        JasperViewer viewer = new JasperViewer(jasperPrint, false);
                        viewer.setTitle("Vista Previa - " + jasperFile.getName());
                        viewer.setVisible(true);
                        log("✓ Vista previa abierta");
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        log("✗ Error: " + e.getMessage());
                        JOptionPane.showMessageDialog(JasperCompilerGUI.this,
                            "Error:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Seleccione al menos un archivo JRXML",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Compilando...");
        
        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            private int successCount = 0;
            private int errorCount = 0;
            private int current = 0;
            private final int total = selectedFiles.size();

            @Override
            protected Void doInBackground() {
                new File("output").mkdirs();
                publish(new Object[]{"log", "\n═══════════════════════════════════════════"});
                publish(new Object[]{"log", "Compilando " + total + " archivo(s)..."});
                publish(new Object[]{"log", "═══════════════════════════════════════════\n"});
                
                long totalStart = System.currentTimeMillis();
                
                for (File file : selectedFiles) {
                    current++;
                    compileFile(file);
                    int progress = (current * 100) / total;
                    publish(new Object[]{"progress", progress, current + "/" + total});
                }
                
                long totalEnd = System.currentTimeMillis();
                publish(new Object[]{"log", "\n═══════════════════════════════════════════"});
                publish(new Object[]{"log", "RESUMEN: ✓ " + successCount + " exitosos, ✗ " + errorCount + " errores"});
                publish(new Object[]{"log", "Tiempo: " + (totalEnd - totalStart) + "ms"});
                publish(new Object[]{"log", "═══════════════════════════════════════════\n"});
                
                return null;
            }

            private void compileFile(File file) {
                try {
                    String inputFile = file.getPath();
                    String outputFile = "output/" + file.getName().replace(".jrxml", ".jasper");
                    
                    publish(new Object[]{"log", "Compilando: " + file.getName() + "..."});
                    long start = System.currentTimeMillis();
                    
                    JasperCompileManager.compileReportToFile(inputFile, outputFile);
                    
                    long end = System.currentTimeMillis();
                    publish(new Object[]{"log", "  ✓ OK (" + (end - start) + "ms)"});
                    successCount++;
                } catch (Exception e) {
                    publish(new Object[]{"log", "  ✗ ERROR: " + e.getMessage()});
                    errorCount++;
                }
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] data : chunks) {
                    String type = (String) data[0];
                    if ("log".equals(type)) {
                        log((String) data[1]);
                    } else if ("progress".equals(type)) {
                        progressBar.setValue((Integer) data[1]);
                        progressBar.setString((String) data[2]);
                    }
                }
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                progressBar.setString("Completado");
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
        historyComboBox.setEnabled(enabled);
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
                System.err.println("Error Look and Feel: " + e.getMessage());
            }
            new JasperCompilerGUI().setVisible(true);
        });
    }
}
