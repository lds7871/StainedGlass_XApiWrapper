@echo off
set JAVA_HOME=%~dp0jdk-17.0.12
set PATH=%JAVA_HOME%\bin;%PATH%
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dfile.encoding=GBK"
