#!/usr/bin/env bash
set -euo pipefail

ADMIN_IP="${1:-}"
SSH_PORT="${2:-22}"
GAME_PORT="${GAME_PORT:-14445}"

if [[ ! "$ADMIN_IP" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}$ ]]; then
  echo "Usage: sudo bash $0 <ADMIN_PUBLIC_IP> [SSH_PORT]"
  exit 1
fi
if [[ ! "$SSH_PORT" =~ ^[0-9]+$ ]] || (( SSH_PORT < 1 || SSH_PORT > 65535 )); then
  echo "SSH port is invalid"
  exit 1
fi
if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script with sudo/root"
  exit 1
fi

if ! command -v ufw >/dev/null 2>&1; then
  apt-get update
  apt-get install -y ufw
fi

# Mở quyền quản trị trước khi bật default deny để tránh tự khóa VPS.
ufw allow from "$ADMIN_IP" to any port "$SSH_PORT" proto tcp comment 'NRO admin SSH'
ufw allow "$GAME_PORT"/tcp comment 'NRO game server'

# Không cho truy cập database và các dashboard thử nghiệm từ Internet.
ufw deny 3306/tcp comment 'Block public MySQL'
for port in 3000 8080 8443 9090; do
  ufw deny "$port"/tcp comment 'Block internal dashboard/API'
done

ufw default deny incoming
ufw default allow outgoing
ufw logging low
ufw --force enable

echo "Firewall enabled. Keep this SSH session open and test a second SSH login now."
ufw status numbered
