# Firewall VPS cho NRO

Bộ rule này bảo vệ cổng game TCP `14445`, không thay thế lớp chống flood packet
trong Java server.

## Linux Ubuntu/Debian

Chạy bằng root và truyền IP công cộng của máy quản trị:

```bash
sudo bash install-linux-ufw.sh 1.2.3.4 22
```

- `1.2.3.4`: IP máy được phép SSH vào VPS.
- `22`: cổng SSH hiện tại của VPS.
- Không đóng phiên SSH đang dùng trước khi kiểm tra một phiên SSH mới.
- Gỡ rule bằng: `sudo bash remove-linux-ufw.sh`.

## Windows Server

Mở PowerShell bằng Run as Administrator:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\Install-WindowsFirewall.ps1 -AdminIP "1.2.3.4" -GamePort 14445 -RdpPort 3389
```

Gỡ các rule do bộ này tạo:

```powershell
.\Remove-WindowsFirewall.ps1
```

## Cổng được áp dụng

- TCP `14445`: cho phép công khai để client vào game.
- TCP `3306`: chặn truy cập MySQL từ Internet.
- TCP `8443`, `9090`, `3000`, `8080`: chặn các dashboard/API firewall thử nghiệm.
- SSH hoặc RDP: chỉ cho phép từ `AdminIP`.

Firewall không thể che IP VPS. Muốn che IP gốc cần TCP proxy ở một máy khác.
