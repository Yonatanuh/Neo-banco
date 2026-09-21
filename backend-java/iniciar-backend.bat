@echo off
color 0b
echo ========================================================
echo   Iniciando Backend de NeoBanco (Java Spring Boot)
echo ========================================================
echo.
echo Configuracion automatica: Usando JDK 21 Portable...
set "JAVA_HOME=%~dp0jdk21\jdk-21.0.12.1+1"

echo Ejecutando Maven...
call .\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
pause

