# JRXML to Jasper Converter GUI

Herramienta con interfaz gráfica para compilar plantillas de reportes JasperReports (.jrxml) a archivos compilados (.jasper).

## Características

- Interfaz gráfica intuitiva
- Drag & Drop de archivos y carpetas directamente a la ventana
- Selección múltiple de archivos JRXML
- Selección de carpeta completa (compila todos los .jrxml)
- Compilación en lote con resumen de resultados
- Barra de progreso visual durante la compilación
- Historial de archivos recientes
- Tema oscuro/claro (persistente)
- Vista previa de reportes compilados con datos de prueba
- Log de compilación en tiempo real
- Soporte para códigos de barras (ZXing)

## Atajos de Teclado

| Atajo | Acción |
|-------|--------|
| Ctrl+O | Abrir archivos |
| Ctrl+Enter | Compilar |
| Ctrl+L | Limpiar lista |
| Ctrl+T | Cambiar tema |

## Estructura del Proyecto

```
├── input/    # Archivos .jrxml de entrada
├── lib/      # Dependencias de JasperReports
├── output/   # Archivos .jasper compilados
└── src/      # Código fuente
    ├── JasperCompiler.java      # Compilador por línea de comandos
    └── JasperCompilerGUI.java   # Interfaz gráfica
```

## Requisitos

- Java JDK 8 o superior

## Uso

### Interfaz Gráfica (Recomendado)

1. Compilar:
   ```cmd
   javac -cp "lib/*" -d output src/JasperCompilerGUI.java
   ```

2. Ejecutar:
   ```cmd
   java -cp "output;lib/*" JasperCompilerGUI
   ```

3. En la interfaz:
   - Arrastre archivos .jrxml o carpetas directamente a la ventana
   - Use Ctrl+O o "Abrir Archivos" para seleccionar archivos
   - "Abrir Carpeta" para agregar todos los .jrxml de una carpeta
   - Seleccione archivos recientes desde el menú desplegable
   - Use Ctrl+Enter o "Compilar" para generar los archivos .jasper
   - "Vista Previa" para ver el reporte compilado
   - Cambie el tema con el botón 🌙/☀️ o Ctrl+T
   - Los archivos compilados se guardan en la carpeta `output/`

### Línea de Comandos

Compila todos los archivos .jrxml en la carpeta `input/`:

```cmd
javac -cp "lib/*" -d output src/JasperCompiler.java
java -cp "output;lib/*" JasperCompiler
```

## Dependencias Incluidas

- JasperReports 6.0.0
- ZXing (códigos de barras)
- iText PDF
- Apache POI (Excel)
- Y otras dependencias en `lib/`
