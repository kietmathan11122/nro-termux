param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^([0-9]{1,3}\.){3}[0-9]{1,3}$')]
    [string]$AdminIP,
    [ValidateRange(1, 65535)]
    [int]$GamePort = 14445,
    [ValidateRange(1, 65535)]
    [int]$RdpPort = 3389
)

$ErrorActionPreference = 'Stop'
$prefix = 'NRO-FW-'

if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Run PowerShell as Administrator.'
}

# Chỉ thay các rule mang prefix riêng, không đụng rule hệ thống của VPS.
Get-NetFirewallRule -DisplayName "$prefix*" -ErrorAction SilentlyContinue | Remove-NetFirewallRule

New-NetFirewallRule -DisplayName "${prefix}Game-TCP-$GamePort" -Direction Inbound `
    -Action Allow -Protocol TCP -LocalPort $GamePort -Profile Any

New-NetFirewallRule -DisplayName "${prefix}Admin-RDP-$RdpPort" -Direction Inbound `
    -Action Allow -Protocol TCP -LocalPort $RdpPort -RemoteAddress $AdminIP -Profile Any

New-NetFirewallRule -DisplayName "${prefix}Block-MySQL-3306" -Direction Inbound `
    -Action Block -Protocol TCP -LocalPort 3306 -Profile Any

New-NetFirewallRule -DisplayName "${prefix}Block-Internal-APIs" -Direction Inbound `
    -Action Block -Protocol TCP -LocalPort 3000,8080,8443,9090 -Profile Any

Set-NetFirewallProfile -Profile Domain,Private,Public -Enabled True

Write-Host "Firewall rules installed. Game TCP port: $GamePort" -ForegroundColor Green
Write-Host "RDP port $RdpPort is allowed only from $AdminIP." -ForegroundColor Yellow
Get-NetFirewallRule -DisplayName "$prefix*" | Format-Table DisplayName,Enabled,Direction,Action
