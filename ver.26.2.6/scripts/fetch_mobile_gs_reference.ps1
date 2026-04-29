# Mobile-GS 업스트림 참고 파일을 assets 에 다시 받습니다.
# 실행: PowerShell에서 ver.26.2.6 폴더 기준
#   .\scripts\fetch_mobile_gs_reference.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$outDir = Join-Path $root "app\src\main\assets\mobile_gs_ref"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$base = "https://raw.githubusercontent.com/xiaobiaodu/Mobile-GS/main"
$files = @(
    @{ Url = "$base/README.md"; Name = "README_upstream_Mobile-GS.md" },
    @{ Url = "$base/render.py"; Name = "render_upstream_reference.py" }
)

foreach ($f in $files) {
    $dest = Join-Path $outDir $f.Name
    Write-Host "GET $($f.Url) -> $dest"
    Invoke-WebRequest -Uri $f.Url -OutFile $dest -UseBasicParsing -TimeoutSec 60
}
Write-Host "Done."
