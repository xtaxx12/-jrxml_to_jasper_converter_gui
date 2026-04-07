import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.view.JasperViewer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private JPanel topPanel;
    private JPanel headerPanel;
    private JPanel buttonsPanel;
    private JPanel historyPanel;
    private JPanel bottomPanel;
    private JPanel actionPanel;
    private JPanel statusPanel;
    private JLabel statusLabel;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JSplitPane splitPane;
    private JScrollPane fileScrollPane;
    private JScrollPane logScrollPane;
    private boolean isDarkTheme = false;
    private Preferences prefs;
    private static final int MAX_HISTORY = 10;
    private static final Pattern ROOT_UUID_PATTERN = Pattern.compile(
        "(?s)(<jasperReport\\b[^>]*?)\\s+uuid\\s*=\\s*(\"[^\"]*\"|'[^']*')"
    );
    private static final Pattern OPEN_QUERY_PATTERN = Pattern.compile("<query(\\s[^>]*)?>");
    private static final Pattern CLOSE_QUERY_PATTERN = Pattern.compile("</query>");
    private static final Pattern VARIABLE_BLOCK_PATTERN = Pattern.compile("(?s)<variable\\b[^>]*>.*?</variable>");
    private static final Pattern GROUP_BLOCK_PATTERN = Pattern.compile("(?s)<group\\b[^>]*>.*?</group>");
    private static final Pattern OPEN_EXPRESSION_PATTERN = Pattern.compile("<expression(\\s[^>]*)?>");
    private static final Pattern CLOSE_EXPRESSION_PATTERN = Pattern.compile("</expression>");

    // Colores y estilos de tema
    private static final Color DARK_BG = new Color(24, 26, 31);
    private static final Color DARK_PANEL = new Color(34, 37, 43);
    private static final Color DARK_SURFACE = new Color(40, 44, 52);
    private static final Color DARK_FG = new Color(230, 235, 242);
    private static final Color DARK_BORDER = new Color(74, 79, 90);
    private static final Color DARK_ACCENT = new Color(88, 166, 255);
    private static final Color DARK_MUTED = new Color(161, 169, 181);

    private static final Color LIGHT_BG = new Color(241, 244, 249);
    private static final Color LIGHT_PANEL = new Color(252, 253, 255);
    private static final Color LIGHT_SURFACE = new Color(255, 255, 255);
    private static final Color LIGHT_FG = new Color(28, 34, 45);
    private static final Color LIGHT_BORDER = new Color(202, 211, 224);
    private static final Color LIGHT_ACCENT = new Color(21, 115, 230);
    private static final Color LIGHT_MUTED = new Color(95, 108, 128);

    private Color currentTextColor;
    private Color currentBorderColor;
    private Color currentAccentColor;
    private Color currentMutedTextColor;

    private static final Font UI_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font UI_BOLD_FONT = new Font("SansSerif", Font.BOLD, 13);
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 20);
    private static final Font SUBTITLE_FONT = new Font("SansSerif", Font.PLAIN, 13);
    private static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

    public JasperCompilerGUI() {
        setTitle("Jasper Report Compiler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(920, 680);
        setMinimumSize(new Dimension(840, 620));
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

        headerPanel = createHeaderPanel();
        
        // Panel superior con botones de selección
        topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        selectFilesButton = new RoundedButton("Abrir Archivos (Ctrl+O)");
        selectFilesButton.setFont(UI_BOLD_FONT);
        selectFilesButton.addActionListener(e -> selectFiles());
        
        selectFolderButton = new RoundedButton("Abrir Carpeta");
        selectFolderButton.setFont(UI_BOLD_FONT);
        selectFolderButton.addActionListener(e -> selectFolder());
        
        clearButton = new RoundedButton("Limpiar");
        clearButton.setFont(UI_BOLD_FONT);
        clearButton.addActionListener(e -> clearFileList());
        
        themeButton = new RoundedButton("🌙");
        themeButton.setToolTipText("Cambiar tema");
        themeButton.setPreferredSize(new Dimension(46, 30));
        themeButton.setFont(new Font("Dialog", Font.PLAIN, 14));
        themeButton.addActionListener(e -> toggleTheme());
        
        buttonsPanel.add(selectFilesButton);
        buttonsPanel.add(selectFolderButton);
        buttonsPanel.add(clearButton);
        buttonsPanel.add(Box.createHorizontalStrut(20));
        buttonsPanel.add(themeButton);

        // Panel de historial
        historyPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JLabel historyLabel = new JLabel("Recientes:");
        historyLabel.setFont(UI_BOLD_FONT);
        historyModel = new DefaultComboBoxModel<>();
        historyComboBox = new JComboBox<>(historyModel);
        historyComboBox.setFont(UI_FONT);
        historyComboBox.setPreferredSize(new Dimension(285, 30));
        historyComboBox.addActionListener(e -> loadFromHistory());
        historyPanel.add(historyLabel);
        historyPanel.add(historyComboBox);
        
        topPanel.add(buttonsPanel, BorderLayout.WEST);
        topPanel.add(historyPanel, BorderLayout.EAST);

        // Panel central dividido: lista de archivos y log
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.4);
        splitPane.setBorder(null);
        splitPane.setContinuousLayout(true);
        
        // Lista de archivos seleccionados
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(MONO_FONT);
        fileList.setFixedCellHeight(24);
        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(new EmptyBorder(0, 8, 0, 8));
                return label;
            }
        });
        fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setBorder(BorderFactory.createTitledBorder("Archivos JRXML (0) - Arrastre aquí o Ctrl+O"));
        fileScrollPane.setPreferredSize(new Dimension(780, 150));

        // Log de compilación
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(MONO_FONT);
        logArea.setMargin(new Insets(8, 10, 8, 10));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Log de Compilación"));
        this.logScrollPane = logScrollPane;
        
        splitPane.setTopComponent(fileScrollPane);
        splitPane.setBottomComponent(logScrollPane);
        
        // Panel inferior con barra de progreso y botones
        bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        // Barra de progreso
        progressBar = new JProgressBar(0, 100);
        progressBar.setFont(UI_BOLD_FONT);
        progressBar.setStringPainted(true);
        progressBar.setString("Listo");
        progressBar.setValue(0);
        progressBar.setBorderPainted(false);
        
        // Botones de acción
        actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        
        compileButton = new RoundedButton("Compilar (Ctrl+Enter)");
        compileButton.setFont(UI_BOLD_FONT);
        compileButton.setEnabled(false);
        compileButton.setPreferredSize(new Dimension(160, 35));
        compileButton.addActionListener(e -> compileReports());
        
        previewButton = new RoundedButton("Vista Previa");
        previewButton.setFont(UI_BOLD_FONT);
        previewButton.setEnabled(false);
        previewButton.setPreferredSize(new Dimension(140, 35));
        previewButton.setToolTipText("Seleccione un archivo de la lista para previsualizar");
        previewButton.addActionListener(e -> previewReport());
        
        actionPanel.add(compileButton);
        actionPanel.add(previewButton);
        
        statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusLabel = new JLabel();
        statusLabel.setFont(UI_BOLD_FONT);
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        statusPanel.add(statusLabel);

        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(actionPanel, BorderLayout.CENTER);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        
        // Listener para habilitar vista previa cuando se selecciona un archivo
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreviewButton();
            }
        });
        
        JPanel northContainer = new JPanel(new BorderLayout(0, 0));
        northContainer.add(headerPanel, BorderLayout.NORTH);
        northContainer.add(topPanel, BorderLayout.SOUTH);

        add(northContainer, BorderLayout.NORTH);
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
        updateStatusLabel();
    }

    private JPanel createHeaderPanel() {
        JPanel container = new GradientHeaderPanel();
        container.setLayout(new BorderLayout(8, 6));
        container.setBorder(new EmptyBorder(14, 14, 12, 14));

        titleLabel = new JLabel("JRXML to Jasper Compiler");
        titleLabel.setFont(TITLE_FONT);

        subtitleLabel = new JLabel("Compilacion visual, lotes y saneamiento automatico de compatibilidad");
        subtitleLabel.setFont(SUBTITLE_FONT);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(subtitleLabel);

        container.add(textPanel, BorderLayout.WEST);
        return container;
    }

    private void applyTheme() {
        Color bg = isDarkTheme ? DARK_BG : LIGHT_BG;
        Color fg = isDarkTheme ? DARK_FG : LIGHT_FG;
        Color panelBg = isDarkTheme ? DARK_PANEL : Color.WHITE;
        
        getContentPane().setBackground(bg);
        applyThemeToComponent(getContentPane(), bg, fg, panelBg);
        
        if (logArea != null) {
            Color surface = isDarkTheme ? DARK_SURFACE : LIGHT_SURFACE;
            logArea.setBackground(surface);
            logArea.setForeground(currentTextColor);
            logArea.setCaretColor(currentTextColor);
        }
        if (fileList != null) {
            Color surface = isDarkTheme ? DARK_SURFACE : LIGHT_SURFACE;
            fileList.setBackground(surface);
            fileList.setForeground(currentTextColor);
            fileList.setSelectionBackground(currentAccentColor);
            fileList.setSelectionForeground(Color.WHITE);
        }

        stylePrimaryComponents();
        updateFileListTitle();
        setSectionBorder(logScrollPane, "Log de Compilación", currentBorderColor, currentTextColor);
        
        themeButton.setText(isDarkTheme ? "☀️" : "🌙");
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void stylePrimaryComponents() {
        Color bg = isDarkTheme ? DARK_BG : LIGHT_BG;
        Color panel = isDarkTheme ? DARK_PANEL : LIGHT_PANEL;
        Color surface = isDarkTheme ? DARK_SURFACE : LIGHT_SURFACE;

        currentTextColor = isDarkTheme ? DARK_FG : LIGHT_FG;
        currentBorderColor = isDarkTheme ? DARK_BORDER : LIGHT_BORDER;
        currentAccentColor = isDarkTheme ? DARK_ACCENT : LIGHT_ACCENT;
        currentMutedTextColor = isDarkTheme ? DARK_MUTED : LIGHT_MUTED;

        getContentPane().setBackground(bg);

        stylePanel(topPanel, panel);
        stylePanel(statusPanel, panel);
        stylePanel(buttonsPanel, panel);
        stylePanel(historyPanel, panel);
        stylePanel(bottomPanel, panel);
        stylePanel(actionPanel, panel);

        splitPane.setBackground(bg);
        splitPane.setForeground(currentBorderColor);

        if (fileScrollPane != null) {
            fileScrollPane.getViewport().setBackground(surface);
        }
        if (logScrollPane != null) {
            logScrollPane.getViewport().setBackground(surface);
        }

        styleButton(selectFilesButton, true);
        styleButton(selectFolderButton, false);
        styleButton(clearButton, false);
        styleButton(compileButton, true);
        styleButton(previewButton, false);
        styleButton(themeButton, false);

        historyComboBox.setBackground(surface);
        historyComboBox.setForeground(currentTextColor);
        historyComboBox.setBorder(new LineBorder(currentBorderColor, 1, true));

        progressBar.setBackground(isDarkTheme ? DARK_PANEL : LIGHT_PANEL);
        progressBar.setForeground(currentAccentColor);
        progressBar.setString(progressBar.getString() == null ? "Listo" : progressBar.getString());
        progressBar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        statusLabel.setForeground(isDarkTheme ? DARK_BG : Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(currentAccentColor);
        titleLabel.setForeground(isDarkTheme ? new Color(240, 244, 250) : new Color(21, 35, 56));
        subtitleLabel.setForeground(currentMutedTextColor);
        updateStatusLabel();
    }

    private void stylePanel(JPanel panel, Color color) {
        if (panel != null) {
            panel.setBackground(color);
            panel.setOpaque(true);
        }
    }

    private void styleButton(JButton button, boolean primary) {
        if (button == null) {
            return;
        }

        button.setFocusPainted(false);
        button.setOpaque(true);

        Color enabledBg = primary ? currentAccentColor : (isDarkTheme ? DARK_SURFACE : LIGHT_SURFACE);
        Color enabledFg = primary ? Color.WHITE : currentTextColor;
        Color disabledBg = isDarkTheme ? new Color(55, 58, 66) : new Color(232, 237, 245);
        Color disabledFg = isDarkTheme ? new Color(130, 137, 150) : new Color(145, 153, 169);

        if (button.isEnabled()) {
            button.setBackground(enabledBg);
            button.setForeground(enabledFg);
            button.setBorder(new LineBorder(primary ? currentAccentColor.darker() : currentBorderColor, 1, true));
        } else {
            button.setBackground(disabledBg);
            button.setForeground(disabledFg);
            button.setBorder(new LineBorder(currentBorderColor, 1, true));
        }
    }

    private void setSectionBorder(JScrollPane scrollPane, String title, Color borderColor, Color titleColor) {
        if (scrollPane == null) {
            return;
        }

        TitledBorder border = BorderFactory.createTitledBorder(new LineBorder(borderColor, 1, true), title);
        border.setTitleColor(titleColor);
        border.setTitleFont(UI_BOLD_FONT);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            new EmptyBorder(4, 4, 4, 4),
            border
        ));
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
                    setSectionBorder(fileScrollPane, getFileListTitle(), currentAccentColor, currentAccentColor);
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
                updateFileListTitle();
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                updateFileListTitle();
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
        styleButton(compileButton, true);
        updateStatusLabel();
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
        updateStatusLabel();
        log("Lista limpiada.");
    }

    private void updateFileListTitle() {
        setSectionBorder(fileScrollPane, getFileListTitle(), currentBorderColor, currentTextColor);
    }

    private String getFileListTitle() {
        String title = "Archivos JRXML (" + selectedFiles.size() + ")";
        if (selectedFiles.isEmpty()) {
            title += " - Arrastre aquí o Ctrl+O";
        }
        return title;
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
                File effectiveInput = file;
                File tempSanitizedFile = null;
                try {
                    SanitizeResult sanitizeResult = createSanitizedJrxmlCopyIfNeeded(file);
                    effectiveInput = sanitizeResult.file;
                    if (!effectiveInput.equals(file)) {
                        tempSanitizedFile = effectiveInput;
                        if (sanitizeResult.removedRootUuid) {
                            publish(new Object[]{"log", "  ! UUID detectado en raiz: se aplico saneamiento automatico"});
                        }
                        if (sanitizeResult.convertedQueryTag) {
                            publish(new Object[]{"log", "  ! Etiqueta <query> detectada: convertida a <queryString>"});
                        }
                        if (sanitizeResult.convertedVariableExpressionTag) {
                            publish(new Object[]{"log", "  ! Etiqueta <expression> en variable: convertida a <variableExpression>"});
                        }
                        if (sanitizeResult.convertedGroupExpressionTag) {
                            publish(new Object[]{"log", "  ! Etiqueta <expression> en group: convertida a <groupExpression>"});
                        }
                    }

                    String inputFile = effectiveInput.getPath();
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
                } finally {
                    if (tempSanitizedFile != null && tempSanitizedFile.exists() && !tempSanitizedFile.delete()) {
                        publish(new Object[]{"log", "  ! Aviso: no se pudo eliminar temporal " + tempSanitizedFile.getName()});
                    }
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

    private SanitizeResult createSanitizedJrxmlCopyIfNeeded(File file) throws IOException {
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

    private void setButtonsEnabled(boolean enabled) {
        compileButton.setEnabled(enabled && !selectedFiles.isEmpty());
        selectFilesButton.setEnabled(enabled);
        selectFolderButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
        previewButton.setEnabled(enabled && fileList.getSelectedIndex() >= 0);
        historyComboBox.setEnabled(enabled);

        styleButton(selectFilesButton, true);
        styleButton(selectFolderButton, false);
        styleButton(clearButton, false);
        styleButton(compileButton, true);
        styleButton(previewButton, false);
        styleButton(themeButton, false);
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        if (statusLabel == null) {
            return;
        }
        String themeName = isDarkTheme ? "Oscuro" : "Claro";
        String mode = compileButton != null && compileButton.isEnabled() ? "Listo para compilar" : "Sin archivos";
        statusLabel.setText("  " + themeName + "  |  " + selectedFiles.size() + " archivo(s)  |  " + mode + "  ");
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

    private static class GradientHeaderPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color c1 = new Color(33, 87, 188);
            Color c2 = new Color(15, 132, 147);
            GradientPaint paint = new GradientPaint(0, 0, c1, getWidth(), getHeight(), c2);
            g2.setPaint(paint);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

            g2.setColor(new Color(255, 255, 255, 35));
            g2.fillOval(getWidth() - 180, -40, 220, 160);
            g2.fillOval(getWidth() - 90, 25, 140, 120);
            g2.dispose();
        }
    }

    private static class RoundedButton extends JButton {
        private static final int ARC = 12;

        RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMargin(new Insets(6, 14, 6, 14));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg = getBackground();
            if (getModel().isArmed() || getModel().isPressed()) {
                bg = bg.darker();
            } else if (getModel().isRollover()) {
                bg = bg.brighter();
            }

            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
            super.paintComponent(g2);
            g2.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getForeground().equals(Color.WHITE) ? new Color(0, 0, 0, 60) : new Color(0, 0, 0, 30));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
            g2.dispose();
        }
    }
}
