$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

Write-Host "Iniciando Servidor NeoBanco (Java Spring Boot) en el puerto 5000..." -ForegroundColor Cyan

.\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
