@java -cp "h2-1.4.199.jar;%H2DRIVERS%;%CLASSPATH%" org.h2.tools.Server %*
@if errorlevel 1 pause