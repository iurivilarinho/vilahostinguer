#Requires -RunAsAdministrator
<#
  Prepara este PC para o Bancada criar máquinas virtuais (Hyper-V). Roda como administrador:

    powershell -ExecutionPolicy Bypass -File tools\habilitar-hyperv.ps1 -Usuario "DOMINIO\usuario"

  1ª vez: ativa o Hyper-V e põe o usuário no grupo "Administradores do Hyper-V" (para o painel
          controlar as VMs sem ser administrador). Pede para reiniciar.
  2ª vez (depois de reiniciar): cria a rede das VMs — um switch interno "Bancada", o PC em
          10.77.0.1 e NAT para a internet. Cada VM ganha um IP fixo nessa rede.

  Rodar de novo não estraga nada: o que já existe fica como está.
  Sem -Usuario, usa quem abriu o script (com "Executar como administrador" em outra conta, informe a sua).
#>
param(
    [string]$Usuario = "$env:USERDOMAIN\$env:USERNAME",
    [string]$Rede = '10.77.0'
)

$ErrorActionPreference = 'Stop'
$hyperVAdmins = 'S-1-5-32-578'   # o nome do grupo muda com o idioma do Windows; o SID não
$switchName = 'Bancada'
$reiniciar = $false

Write-Host "== Hyper-V =="
$recurso = Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All
if ($recurso.State -ne 'Enabled') {
    Write-Host "Ativando o Hyper-V e as ferramentas de gerenciamento..."
    $resultado = Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All -All -NoRestart
    $reiniciar = $true
} else {
    Write-Host "Ativo."
}

Write-Host "== Permissão para $Usuario =="
$membros = Get-LocalGroupMember -SID $hyperVAdmins -ErrorAction SilentlyContinue | ForEach-Object { $_.Name }
if ($membros -contains $Usuario) {
    Write-Host "Já está em $((Get-LocalGroup -SID $hyperVAdmins).Name)."
} else {
    Add-LocalGroupMember -SID $hyperVAdmins -Member $Usuario
    Write-Host "Adicionado a $((Get-LocalGroup -SID $hyperVAdmins).Name). Vale depois de sair e entrar de novo."
    $reiniciar = $true
}

if (-not (Get-Command New-VMSwitch -ErrorAction SilentlyContinue)) {
    Write-Host ""
    Write-Host "Reinicie o PC e rode este script de novo para criar a rede das VMs." -ForegroundColor Yellow
    exit 0
}

Write-Host "== Rede das VMs ($Rede.0/24) =="
if (-not (Get-VMSwitch -Name $switchName -ErrorAction SilentlyContinue)) {
    New-VMSwitch -Name $switchName -SwitchType Internal | Out-Null
    Write-Host "Switch interno '$switchName' criado."
} else {
    Write-Host "Switch '$switchName' já existe."
}
$adaptador = Get-NetAdapter | Where-Object { $_.Name -eq "vEthernet ($switchName)" }
if (-not (Get-NetIPAddress -InterfaceIndex $adaptador.ifIndex -IPAddress "$Rede.1" -ErrorAction SilentlyContinue)) {
    New-NetIPAddress -InterfaceIndex $adaptador.ifIndex -IPAddress "$Rede.1" -PrefixLength 24 | Out-Null
    Write-Host "PC em $Rede.1."
} else {
    Write-Host "PC já está em $Rede.1."
}
if (-not (Get-NetNat -Name 'BancadaNAT' -ErrorAction SilentlyContinue)) {
    New-NetNat -Name 'BancadaNAT' -InternalIPInterfaceAddressPrefix "$Rede.0/24" | Out-Null
    Write-Host "NAT para a internet criado."
} else {
    Write-Host "NAT já existe."
}
# as VMs falam com o Bancada (discos do PC) por esta rede: ela é privada, não pública
Set-NetConnectionProfile -InterfaceIndex $adaptador.ifIndex -NetworkCategory Private -ErrorAction SilentlyContinue

Write-Host ""
if ($reiniciar) {
    Write-Host "Pronto. Reinicie o PC para a permissão valer." -ForegroundColor Yellow
} else {
    Write-Host "Pronto." -ForegroundColor Green
}
