#!/data/data/com.termux/files/usr/bin/bash

# Di chuyen den thu muc hien tai cua script
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

echo "=== DANG KHOI CHAY SERVER NRO TREN TERMUX ==="

# Kiem tra xem MariaDB da chay chua, neu chua thi bat
if ! pgrep -x "mariadbd" > /dev/null && ! pgrep -x "mysqld" > /dev/null; then
    echo "[!] MariaDB chua chay, dang khoi dong..."
    mysqld_safe -u root > /dev/null 2>&1 &
    sleep 3
fi

# Chay Server NRO voi bo nho phu hop voi dien thoai (RAM 256MB - 1GB)
java -server -Xms256M -Xmx1024M -XX:+UseG1GC -XX:MaxGCPauseMillis=150 -Dfile.encoding=UTF-8 -jar "dist/NgocRongOnline.jar"
