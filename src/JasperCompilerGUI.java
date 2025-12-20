import net.sf.jasperreports.engine.JasperCompileManager;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

public class JasperCompilerGUI extends JFrame {
    private JTextField filePathField;
    private JTextArea logArea;
    private JButton selectButton;
    private JButton compileButton;
    private File selectedFile;

    public JasperCompilerGUI() {
        setTitle("Jasper Report Compiler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        
        // Panel superior para selección de archivo
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel label = new JLabel("Archivo JRXML:");
        filePathField = new JTextField();
        filePathField.setEditable(false);
        
        selectButton = new JButton("Seleccionar...");
        selectButton.addActionListener(e -> selectFile());
        
        topPanel.add(label, BorderLayout.WEST);
        topPanel.add(filePathField, BorderLayout.CENTER);
        topPanel.add(selectButton, BorderLayout.EAST);
        
        // Panel central para log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Log de Compilación"));
        
        // Panel inferior con botón de compilar
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        
        compileButton = new JButton("Compilar");
        compileButton.setEnabled(false);
        compileButton.setPreferredSize(new Dimension(150, 35));
        compileButton.addActionListener(e -> compileReport());
        
        bottomPanel.add(compileButton);
        
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("input"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Archivos JRXML (*.jrxml)", "jrxml"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedFile.getAbsolutePath());
            compileButton.setEnabled(true);
            log("Archivo seleccionado: " + selectedFile.getName());
        }
    }

    private void compileReport() {
        if (selectedFile == null || !selectedFile.exists()) {
            JOptionPane.showMessageDialog(this, "Por favor seleccione un archivo JRXML válido", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        compileButton.setEnabled(false);
        selectButton.setEnabled(false);
        
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String inputFile = selectedFile.getPath();
                    String outputFile = "output/" + selectedFile.getName().replace(".jrxml", ".jasper");
                    
                    // Crear carpeta output si no existe
                    new File("output").mkdirs();
                    
                    publish("Compilando: " + selectedFile.getName() + "...");
                    long start = System.currentTimeMillis();
                    
                    JasperCompileManager.compileReportToFile(inputFile, outputFile);
                    
                    long end = System.currentTimeMillis();
                    publish("✓ Compilación exitosa!");
                    publish("  Archivo generado: " + outputFile);
                    publish("  Tiempo: " + (end - start) + "ms");
                } catch (Exception e) {
                    publish("✗ Error de compilación: " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    log(message);
                }
            }

            @Override
            protected void done() {
                compileButton.setEnabled(true);
                selectButton.setEnabled(true);
            }
        };
        
        worker.execute();
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            
            new JasperCompilerGUI().setVisible(true);
        });
    }
}
