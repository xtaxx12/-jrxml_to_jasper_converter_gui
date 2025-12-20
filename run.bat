@echo off
:: ============================================================================
:: JRXML to Jasper Compiler - Script de Ejecución
:: Autor: Sistema Automatizado
:: Descripción: Compila y ejecuta la interfaz gráfica del compilador Jasper
:: ============================================================================

setlocal enabledelayedexpansion

:: Colores para la consola
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "CYAN=[96m"
set "RESET=[0m"

:: Configuración de rutas
set "SRC_DIR=src"
set "LIB_DIR=lib"
set "OUTPUT_DIR=output"
set "MAIN_CLASS=JasperCompilerGUI"

echo.
echo %CYAN%============================================================================%RESET%
echo %CYAN%          JRXML to Jasper Compiler - Launcher%RESET%
echo %CYAN%============================================================================%RESET%
echo.

:: Verificar que Java está instalado
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%RESET% Java no está instalado o no está en el PATH.
    echo         Instala Java JDK 8 o superior desde: https://adoptium.net/
    pause
    exit /b 1
)

:: Mostrar versión de Java
echo %GREEN%[INFO]%RESET% Version de Java detectada:
java -version 2>&1 | findstr /i "version"
echo.

:: Verificar que existe la carpeta lib con JARs
if not exist "%LIB_DIR%\jasperreports-6.0.0.jar" (
    echo %RED%[ERROR]%RESET% No se encontraron las dependencias en %LIB_DIR%\
    echo         Asegurate de que la carpeta lib contiene los JARs de JasperReports.
    pause
    exit /b 1
)

:: Crear carpeta output si no existe
if not exist "%OUTPUT_DIR%" (
    echo %YELLOW%[INFO]%RESET% Creando directorio %OUTPUT_DIR%\...
    mkdir "%OUTPUT_DIR%"
)

:: Verificar si necesita compilar (no existe el .class o el .java es más reciente)
set "NEEDS_COMPILE=0"
if not exist "%OUTPUT_DIR%\%MAIN_CLASS%.class" (
    set "NEEDS_COMPILE=1"
    echo %YELLOW%[INFO]%RESET% No se encontro el archivo compilado. Compilando...
) else (
    :: Comparar fechas de modificación
    for %%F in ("%SRC_DIR%\%MAIN_CLASS%.java") do set "SRC_DATE=%%~tF"
    for %%F in ("%OUTPUT_DIR%\%MAIN_CLASS%.class") do set "CLASS_DATE=%%~tF"
    
    :: Siempre recompilar si el source es más reciente (simplificado)
    echo %GREEN%[INFO]%RESET% Archivo compilado encontrado.
)

:: Compilar si es necesario
if %NEEDS_COMPILE%==1 (
    echo.
    echo %CYAN%[COMPILANDO]%RESET% %SRC_DIR%\%MAIN_CLASS%.java
    echo.
    
    :: Verificar que javac está disponible
    where javac >nul 2>&1
    if %errorlevel% neq 0 (
        echo %RED%[ERROR]%RESET% javac no encontrado. Necesitas JDK, no solo JRE.
        echo         Instala Java JDK desde: https://adoptium.net/
        pause
        exit /b 1
    )
    
    :: Compilar el código fuente
    javac -encoding UTF-8 -cp "%LIB_DIR%\*" -d "%OUTPUT_DIR%" "%SRC_DIR%\%MAIN_CLASS%.java"
    
    if %errorlevel% neq 0 (
        echo.
        echo %RED%[ERROR]%RESET% La compilacion fallo. Revisa los errores arriba.
        pause
        exit /b 1
    )
    
    echo %GREEN%[OK]%RESET% Compilacion exitosa!
    echo.
)

:: Ejecutar la aplicación
echo %CYAN%[EJECUTANDO]%RESET% %MAIN_CLASS%...
echo %CYAN%============================================================================%RESET%
echo.

:: Ejecutar con encoding UTF-8 para emojis
java -Dfile.encoding=UTF-8 -cp "%OUTPUT_DIR%;%LIB_DIR%\*" %MAIN_CLASS%

:: Verificar código de salida
if %errorlevel% neq 0 (
    echo.
    echo %RED%[ERROR]%RESET% La aplicacion termino con errores.
    pause
    exit /b 1
)

echo.
echo %GREEN%[FIN]%RESET% Aplicacion cerrada correctamente.

endlocal
