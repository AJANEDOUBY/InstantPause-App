#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Construit, signe et publie une release GitHub d'Instant Pause.

.DESCRIPTION
    Enchaîne :
      1. lecture de la version depuis app/build.gradle.kts (versionName)
      2. tests unitaires + assembleRelease
      3. zipalign + signature avec la clé de release (keystore/)
      4. vérification de la signature et calcul du SHA-256
      5. tag git annoté + push
      6. release GitHub (brouillon -> upload de l'APK -> publication)

    Le token GitHub est lu depuis $env:GITHUB_TOKEN, sinon depuis le
    gestionnaire d'identifiants git (git credential fill).

.PARAMETER Notes
    Chemin d'un fichier Markdown servant de notes de release.
    Sinon un contenu par défaut est généré.

.PARAMETER Tag
    Nom du tag à créer. Par défaut "v<version>".

.PARAMETER DryRun
    Fait tout sauf le tag, le push et la publication.
    L'APK est produit dans le dossier temporaire, pas à la racine du dépôt.

.PARAMETER SkipTests
    Saute les tests unitaires (déconseillé).

.EXAMPLE
    ./release.ps1 -DryRun
    ./release.ps1 -Notes .\notes-1.1.md
#>
[CmdletBinding()]
param(
    [string]$Notes,
    [string]$Tag,
    [switch]$DryRun,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ---------------------------------------------------------------- utilitaires
function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    Write-Host "    OK  $Message" -ForegroundColor Green
}

function Fail([string]$Message) {
    Write-Host ""
    Write-Host "ECHEC : $Message" -ForegroundColor Red
    exit 1
}

function Invoke-Checked([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments
    if ($LASTEXITCODE -ne 0) { Fail "$(Split-Path $Exe -Leaf) a retourne le code $LASTEXITCODE" }
}

function Get-GitHubToken {
    if ($env:GITHUB_TOKEN) { return $env:GITHUB_TOKEN.Trim() }

    $filled = "protocol=https`nhost=github.com`n`n" | & git credential fill 2>$null
    if ($LASTEXITCODE -eq 0) {
        $line = $filled | Where-Object { $_ -like 'password=*' } | Select-Object -First 1
        if ($line) { return $line.Substring(9).Trim() }
    }
    Fail "Aucun token GitHub. Definir `$env:GITHUB_TOKEN ou enregistrer les identifiants git (git credential approve)."
}

# ---------------------------------------------------------------- preparation
$root = $PSScriptRoot
Set-Location $root

$gradleFile = Join-Path $root 'app/build.gradle.kts'
if (-not (Test-Path $gradleFile)) { Fail "app/build.gradle.kts introuvable - le script doit vivre a la racine du depot." }

$versionName = (Select-String -Path $gradleFile -Pattern 'versionName\s*=\s*"([^"]+)"').Matches[0].Groups[1].Value
$versionCode = (Select-String -Path $gradleFile -Pattern 'versionCode\s*=\s*(\d+)').Matches[0].Groups[1].Value
if (-not $versionName) { Fail "versionName introuvable dans app/build.gradle.kts" }

if (-not $Tag) { $Tag = "v$versionName" }

$ksDir    = Join-Path $root 'keystore'
$ksFile   = Join-Path $ksDir 'cooldown-release.jks'
$ksInfo   = Join-Path $ksDir 'KEYSTORE-INFO.txt'
$apkName  = "InstantPause-$versionName.apk"

Write-Host ""
Write-Host "Instant Pause $versionName (versionCode $versionCode)  ->  tag $Tag" -ForegroundColor White

if (-not (Test-Path $ksFile) -or -not (Test-Path $ksInfo)) {
    Fail "Keystore manquant : $ksFile / $ksInfo. Voir README (section Signature)."
}

$ksPassword = (Get-Content $ksInfo | Where-Object { $_ -like 'Store/key:*' } | Select-Object -First 1)
if (-not $ksPassword) { Fail "Mot de passe introuvable dans KEYSTORE-INFO.txt" }
$ksPassword = $ksPassword.Substring(10).Trim()

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
       elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
       elseif (Test-Path (Join-Path $root 'local.properties')) {
           # sdk.dir=C\:\\Users\\... -> C:\Users\...
           $line = (Get-Content (Join-Path $root 'local.properties') | Where-Object { $_ -like 'sdk.dir=*' } | Select-Object -First 1)
           if ($line) { ($line -replace '^sdk\.dir=', '') -replace '\\(.)', '$1' }
       }
       else { $null }
if (-not $sdk -or -not (Test-Path $sdk)) { Fail "SDK Android introuvable (ANDROID_HOME ou local.properties)." }

$buildTools = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { Fail "Aucun build-tools dans $sdk" }

$zipalign  = Join-Path $buildTools.FullName 'zipalign.exe'
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
$aapt2     = Join-Path $buildTools.FullName 'aapt2.exe'

# ---------------------------------------------------------------- build + tests
if (-not $SkipTests) {
    Write-Step "Tests unitaires"
    Invoke-Checked (Join-Path $root 'gradlew.bat') @(':app:testDebugUnitTest', '--offline', '--console=plain')
    $results = Join-Path $root 'app/build/test-results/testDebugUnitTest'
    if (Test-Path $results) {
        $total = 0; $failed = 0
        Get-ChildItem $results -Filter *.xml | ForEach-Object {
            [xml]$x = Get-Content $_.FullName
            $total  += [int]$x.testsuite.tests
            $failed += [int]$x.testsuite.failures + [int]$x.testsuite.errors
        }
        if ($failed -gt 0) { Fail "$failed test(s) en echec sur $total" }
        Write-Ok "$total tests verts"
    }
}

Write-Step "Assemblage de la release (R8)"
Invoke-Checked (Join-Path $root 'gradlew.bat') @(':app:assembleRelease', '--offline', '--console=plain')

$unsigned = Join-Path $root 'app/build/outputs/apk/release/app-release-unsigned.apk'
if (-not (Test-Path $unsigned)) { Fail "APK non signee introuvable : $unsigned" }

# ---------------------------------------------------------------- signature
Write-Step "Signature"
$aligned = Join-Path $env:TEMP "cooldown-aligned-$versionName.apk"
Invoke-Checked $zipalign @('-p', '-f', '4', $unsigned, $aligned)
Write-Ok "zipalign"

if ($DryRun) {
    $outApk = Join-Path $env:TEMP $apkName
} else {
    $outApk = Join-Path $root $apkName
}

Invoke-Checked $apksigner @(
    'sign',
    '--ks', $ksFile,
    '--ks-key-alias', 'cooldown',
    '--ks-pass', "pass:$ksPassword",
    '--key-pass', "pass:$ksPassword",
    '--v1-signing-enabled', 'false',
    '--v2-signing-enabled', 'true',
    '--v3-signing-enabled', 'true',
    '--v4-signing-enabled', 'false',
    '--out', $outApk,
    $aligned
)
Write-Ok "APK signee : $outApk"

$verify = & $apksigner verify --verbose $outApk 2>&1
if ($LASTEXITCODE -ne 0) { Fail "Verification de signature en echec" }
Write-Ok (($verify | Where-Object { $_ -like 'Verified using*true*' }) -join ' | ')

$badging = & $aapt2 dump badging $outApk 2>&1
$pkgLine = $badging | Where-Object { $_ -like 'package:*' } | Select-Object -First 1
$minSdk  = (($badging | Where-Object { $_ -like 'minSdkVersion*' } | Select-Object -First 1) -replace '.*:', '').Trim().Trim("'")
$sizeMb  = [math]::Round((Get-Item $outApk).Length / 1MB, 2)
$sha256  = (Get-FileHash $outApk -Algorithm SHA256).Hash.ToLower()

Write-Host ""
Write-Host "    $pkgLine"
Write-Host "    minSdk $minSdk  |  $sizeMb Mo  |  sha256 $sha256"

if ($DryRun) {
    Write-Host ""
    Write-Host "DryRun : ni tag, ni push, ni publication." -ForegroundColor Yellow
    exit 0
}

# ---------------------------------------------------------------- tag + push
Write-Step "Tag git $Tag"
$env:GIT_TERMINAL_PROMPT = '0'
$existing = git ls-remote --tags origin "refs/tags/$Tag" 2>$null
if ($existing) { Fail "Le tag $Tag existe deja sur origin. Incrementer versionName ou passer -Tag." }

if (-not (git rev-parse -q --verify "refs/tags/$Tag" 2>$null)) {
    Invoke-Checked 'git' @('tag', '-a', $Tag, '-m', "Instant Pause $versionName")
}
Invoke-Checked 'git' @('push', 'origin', $Tag)
Write-Ok "tag pousse"

# ---------------------------------------------------------------- release
Write-Step "Release GitHub"

if ($Notes -and (Test-Path $Notes)) {
    $body = Get-Content $Notes -Raw
} else {
    $body = @"
Instant Pause $versionName

Application Android qui intercepter les appels sortants impulsifs pendant des plages
horaires definies ou un mode Zen, puis propose un ecran de pause sensoriel.

## Installation

1. Telecharger ``$apkName``.
2. Autoriser l'installation depuis cette source si demande.
3. Ouvrir l'app puis accepter le role Redirection d'appels.

> Si une version de debug est deja installee, la desinstaller d'abord
> (signature differente : INSTALL_FAILED_UPDATE_INCOMPATIBLE).

## Details

| | |
|---|---|
| Package | com.instantpause.app |
| Version | $versionName (versionCode $versionCode) |
| Taille | $sizeMb Mo |
| minSdk | $minSdk |
| SHA-256 | ``$sha256`` |
"@
}

$token   = Get-GitHubToken
$repoUrl = (git remote get-url origin) -replace '\.git$', ''
$repo    = ($repoUrl -replace '^https://github\.com/', '')
if ($repo -notmatch '/') { Fail "Depot distant non reconnu : $repoUrl" }

$headers = @{
    Authorization = "Bearer $token"
    'User-Agent'  = 'cooldown-release'
    Accept        = 'application/vnd.github+json'
}

# Brouillon d'abord : permet l'upload meme si les releases immuables sont activees.
$payload = @{
    tag_name         = $Tag
    name             = "Instant Pause $versionName"
    body             = $body
    draft            = $true
    prerelease       = $false
} | ConvertTo-Json -Depth 3

try {
    $release = Invoke-RestMethod -Method Post `
        -Uri "https://api.github.com/repos/$repo/releases" `
        -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($payload)) `
        -ContentType 'application/json; charset=utf-8'
} catch {
    Fail "Creation de la release impossible : $($_.ErrorDetails.Message)"
}
Write-Ok "brouillon #$($release.id)"

$uploadUrl = "https://uploads.github.com/repos/$repo/releases/$($release.id)/assets?name=$apkName"
$response = & curl.exe -s -X POST `
    -H "Authorization: Bearer $token" `
    -H "User-Agent: cooldown-release" `
    -H "Content-Type: application/vnd.android.package-archive" `
    --data-binary "@$outApk" `
    --max-time 600 $uploadUrl 2>&1

$asset = $response | ConvertFrom-Json
if (-not $asset.id -or $asset.state -ne 'uploaded') {
    Fail "Upload de l'APK echoue : $response"
}
Write-Ok "$($asset.name) ($([math]::Round($asset.size / 1MB, 2)) Mo)"

$publishPayload = @{ draft = $false; make_latest = 'true' } | ConvertTo-Json
try {
    $published = Invoke-RestMethod -Method Patch `
        -Uri "https://api.github.com/repos/$repo/releases/$($release.id)" `
        -Headers $headers -Body $publishPayload -ContentType 'application/json'
} catch {
    Fail "Publication impossible : $($_.ErrorDetails.Message)"
}

Write-Host ""
Write-Host "Release publiee : $($published.html_url)" -ForegroundColor Green
Write-Host "Telechargement  : https://github.com/$repo/releases/download/$Tag/$apkName"
Write-Host "SHA-256         : $sha256"
