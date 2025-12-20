# JRXML to Jasper Converter GUI

Herramienta con interfaz gráfica para compilar plantillas de reportes JasperReports (.jrxml) a archivos compilados (.jasper).

## Características

- Interfaz gráfica intuitiva con selector de archivos
- Compilación de reportes JRXML a formato Jasper
- Log de compilación en tiempo real
- Soporte para códigos de barras (ZXing)

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
   - Clic en "Seleccionar..." para elegir un archivo .jrxml
   - Clic en "Compilar" para generar el archivo .jasper
   - El archivo compilado se guarda en la carpeta `output/`

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
