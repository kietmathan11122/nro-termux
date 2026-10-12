@ECHO OFF
cd /d "%~dp0"
REM Cau hinh VPS 2-4 GB RAM. Dat JVM options truoc -jar de Java ap dung dung.
REM Panel Java se tu dong mo sau khi server khoi dong thanh cong.
set "SERVER_JAR=dist\server-run-%RANDOM%-%RANDOM%.jar"
copy /Y "dist\NgocRongOnline.jar" "%SERVER_JAR%" >nul
if errorlevel 1 goto :end
java -server -Xms256M -Xmx1200M -XX:+UseG1GC -XX:MaxGCPauseMillis=150 -XX:+UseStringDeduplication -XX:+DisableExplicitGC -Dfile.encoding=UTF-8 -jar "%SERVER_JAR%"
del /Q "%SERVER_JAR%"
:end
PAUSE
