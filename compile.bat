@echo off
:: ============================================================================
:: JRXML to Jasper Compiler - Script de Compilación Forzada
:: Descripción: Fuerza la recompilación de todos los archivos Java
:: ============================================================================

setlocal enabledelayedexpansion

set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "CYAN=[96m"
set "RESET=[0m"

set "SRC_DIR=src"
set "LIB_DIR=lib"
set "OUTPUT_DIR=output"

echo.
echo %CYAN%============================================================================%RESET%
echo %CYAN%          JRXML to Jasper Compiler - Compilador%RESET%
echo %CYAN%============================================================================%RESET%
echo.

:: Verificar javac
where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[ERROR]%RESET% javac no encontrado. Instala Java JDK.
    pause
    exit /b 1
)

echo %GREEN%[INFO]%RESET% Version de javac:
javac -version
echo.

:: Limpiar clases anteriores
echo %YELLOW%[LIMPIANDO]%RESET% Eliminando archivos .class anteriores...
del /q "%OUTPUT_DIR%\*.class" 2>nul

:: Compilar todos los archivos Java
echo.
echo %CYAN%[COMPILANDO]%RESET% Todos los archivos en %SRC_DIR%\...
echo.

javac -encoding UTF-8 -cp "%LIB_DIR%\*" -d "%OUTPUT_DIR%" "%SRC_DIR%\*.java"

if %errorlevel% neq 0 (
    echo.
    echo %RED%[ERROR]%RESET% La compilacion fallo.
    pause
    exit /b 1
)

echo.
echo %GREEN%============================================================================%RESET%
echo %GREEN%  COMPILACION EXITOSA!%RESET%
echo %GREEN%============================================================================%RESET%
echo.
echo Archivos generados en %OUTPUT_DIR%\:
dir /b "%OUTPUT_DIR%\*.class" 2>nul
echo.
echo Ejecuta %CYAN%run.bat%RESET% para iniciar la aplicacion.
echo.

endlocal
pause
