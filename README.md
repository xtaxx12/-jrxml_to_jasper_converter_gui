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

### 🚀 Inicio Rápido (Recomendado)

Simplemente ejecuta el script correspondiente:

| Script | Descripción |
|--------|-------------|
| `run.bat` | Inicia la interfaz gráfica (compila automáticamente si es necesario) |
| `run-cli.bat` | Compila todos los JRXML en `input/` (modo consola) |
| `compile.bat` | Fuerza recompilación limpia del código fuente |

```cmd
# Doble clic en run.bat o desde terminal:
.\run.bat
```

### Interfaz Gráfica

En la interfaz puedes:
- Arrastrar archivos .jrxml o carpetas directamente a la ventana
- Usar Ctrl+O o "Abrir Archivos" para seleccionar archivos
- "Abrir Carpeta" para agregar todos los .jrxml de una carpeta
- Seleccionar archivos recientes desde el menú desplegable
- Usar Ctrl+Enter o "Compilar" para generar los archivos .jasper
- "Vista Previa" para ver el reporte compilado
- Cambiar el tema con el botón 🌙/☀️ o Ctrl+T
- Los archivos compilados se guardan en la carpeta `output/`

### Línea de Comandos (Modo CLI)

Para compilar todos los archivos .jrxml en la carpeta `input/` sin interfaz:

```cmd
.\run-cli.bat
```

### Compilación Manual (Avanzado)

Si prefieres compilar manualmente:

```cmd
javac -encoding UTF-8 -cp "lib/*" -d output src/*.java
java -cp "output;lib/*" JasperCompilerGUI
```

## Dependencias Incluidas

- JasperReports 6.0.0
- ZXing (códigos de barras)
- iText PDF
- Apache POI (Excel)
- Y otras dependencias en `lib/`
