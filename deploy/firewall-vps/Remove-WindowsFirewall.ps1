$ErrorActionPreference = 'Stop'
if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Run PowerShell as Administrator.'
}

Get-NetFirewallRule -DisplayName 'NRO-FW-*' -ErrorAction SilentlyContinue | Remove-NetFirewallRule
Write-Host 'Removed all NRO-FW firewall rules.' -ForegroundColor Green
