# Empacota a Bancada como app nativo (Tauri) com o backend Spring embutido (sidecar).
# Resultado: instalador NSIS em src-tauri/target/release/bundle/nsis/.
# Uso:  powershell -ExecutionPolicy Bypass -File package.ps1
#
# "Continue" de propósito: no PowerShell 5.1, sob "Stop", qualquer linha que um comando nativo
# escreva no stderr (o cargo imprime "Compiling..." lá) vira erro terminante. Cada passo confere
# o $LASTEXITCODE e interrompe quando falha.
$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
$jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-17" }
$env:JAVA_HOME = $jdk
$mvn = if ($env:MAVEN_CMD) { $env:MAVEN_CMD } else { "mvn" }

Write-Host "==> 1/4  build do frontend (React -> backend/static)"
Push-Location $root
& npm run build
if ($LASTEXITCODE -ne 0) { throw "npm build falhou" }
Pop-Location

Write-Host "==> 2/4  build do backend (fat jar, com testes)"
Push-Location (Join-Path $root "backend")
& $mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw "maven package falhou" }
Pop-Location

Write-Host "==> 3/4  jpackage do backend (JRE + jar) para src-tauri/resources"
$resources = Join-Path $root "src-tauri\resources"
$input = Join-Path $root "dist\input"
Remove-Item -Recurse -Force (Join-Path $resources "bancada-backend") -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $input | Out-Null
Get-ChildItem $input -File | Remove-Item -Force
Copy-Item (Join-Path $root "backend\target\bancada.jar") $input
New-Item -ItemType Directory -Force -Path $resources | Out-Null
& "$jdk\bin\jpackage.exe" `
  --type app-image `
  --name bancada-backend `
  --input $input `
  --main-jar bancada.jar `
  --main-class org.springframework.boot.loader.launch.JarLauncher `
  --dest $resources `
  --java-options "-Xmx384m"
if ($LASTEXITCODE -ne 0) { throw "jpackage falhou" }
# O jpackage marca os arquivos como somente leitura, e o Tauri não consegue sobrescrevê-los num
# segundo build ("Acesso negado"). Tira o atributo na origem.
Get-ChildItem -LiteralPath (Join-Path $resources "bancada-backend") -Recurse -Force -File -ErrorAction SilentlyContinue |
  Where-Object { $_.IsReadOnly } | ForEach-Object { $_.IsReadOnly = $false }

Write-Host "==> 4/4  app nativo (Tauri) com o backend embutido"
Push-Location $root
& npm run tauri build
if ($LASTEXITCODE -ne 0) { throw "tauri build falhou" }
Pop-Location

Write-Host "OK -> src-tauri\target\release\bundle\nsis\"
