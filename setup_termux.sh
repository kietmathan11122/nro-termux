#!/data/data/com.termux/files/usr/bin/bash

echo "==========================================="
echo "   TOOL CAI DAT NRO SERVER TREN TERMUX    "
echo "==========================================="

echo "[1/4] Cap nhat goi va cai dat OpenJDK, MariaDB..."
pkg update -y
pkg install openjdk-17 mariadb git nano -y

echo "[2/4] Khoi tao va bat MySQL / MariaDB..."
if [ ! -d "/data/data/com.termux/files/usr/var/lib/mysql" ]; then
    mysql_install_db
fi

# Khoi dong mysql_safe
mysqld_safe -u root > /dev/null 2>&1 &
sleep 5

echo "[3/4] Tao Database 'yeupich' (trung voi Config.properties)..."
mariadb -u root -e "CREATE DATABASE IF NOT EXISTS yeupich DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mariadb -u root -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' IDENTIFIED BY '' WITH GRANT OPTION; FLUSH PRIVILEGES;"

echo "[4/4] Cap quyen thuc thi cho run.sh..."
chmod +x run.sh

echo "==========================================="
echo "CAI DAT HOAN TAT!"
echo "- De import file database .sql: mariadb -u root yeupich < file_du_lieu.sql"
echo "- De khoi chay server: ./run.sh"
echo "==========================================="
