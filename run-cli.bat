@echo off
:: ============================================================================
:: JRXML to Jasper Compiler - Modo Línea de Comandos
:: Descripción: Compila todos los JRXML en la carpeta input/ sin interfaz gráfica
:: ============================================================================

setlocal

set "GREEN=[92m"
set "RED=[91m"
set "CYAN=[96m"
set "RESET=[0m"

set "LIB_DIR=lib"
set "OUTPUT_DIR=output"
set "MAIN_CLASS=JasperCompiler"

echo.
echo %CYAN%============================================================================%RESET%
echo %CYAN%          JRXML to Jasper Compiler - Modo CLI%RESET%
echo %CYAN%============================================================================%RESET%
echo.

:: Verificar Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%RESET% Java no esta instalado.
    pause
    exit /b 1
)

:: Verificar que existe la clase compilada
if not exist "%OUTPUT_DIR%\%MAIN_CLASS%.class" (
    echo %RED%[ERROR]%RESET% No se encontro %MAIN_CLASS%.class
    echo         Ejecuta primero: compile.bat
    pause
    exit /b 1
)

:: Verificar que hay archivos en input/
if not exist "input\*.jrxml" (
    echo %RED%[ERROR]%RESET% No hay archivos .jrxml en la carpeta input\
    echo         Coloca tus archivos JRXML en la carpeta input\ y vuelve a ejecutar.
    pause
    exit /b 1
)

echo %GREEN%[INFO]%RESET% Archivos JRXML encontrados en input\:
dir /b "input\*.jrxml"
echo.

echo %CYAN%[COMPILANDO]%RESET% Procesando reportes...
echo.

java -Dfile.encoding=UTF-8 -cp "%OUTPUT_DIR%;%LIB_DIR%\*" %MAIN_CLASS%

echo.
echo %GREEN%[FIN]%RESET% Proceso completado.
echo.

endlocal
pause
