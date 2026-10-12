#!/usr/bin/env bash
set -euo pipefail
if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script with sudo/root"
  exit 1
fi
ufw --force disable
ufw --force reset
echo "UFW rules removed and UFW disabled."
