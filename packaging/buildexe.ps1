# 실행 폴더(TFSE.exe + Java 런타임) 빌드 후 GitHub Release용 zip 생성
# 필요: JDK 21 이상
# 아이콘: exe는 packaging\TFSE.ico, 게임 창은 src\main\resources\icon\TFSE_*.png (jar에 포함)
$ErrorActionPreference = "Stop"
# 스크립트가 packaging\ 안에 있으므로 프로젝트 루트 기준으로 실행
Set-Location (Split-Path $PSScriptRoot -Parent)

$appName = "TFSE"
$appVersion = "0.2.2"
$iconIco = "packaging\TFSE.ico"

# JDK bin 폴더 찾기 (JAVA_HOME 우선, 없으면 Program Files\Java에서 최신 버전)
if ($env:JAVA_HOME) {
    $jdkBin = Join-Path $env:JAVA_HOME "bin"
} else {
    $jdk = Get-ChildItem "C:\Program Files\Java" -Directory -Filter "jdk-*" |
        Sort-Object Name -Descending | Select-Object -First 1
    $jdkBin = Join-Path $jdk.FullName "bin"
}

$out = "build\exe"
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\classes", "$out\input" | Out-Null

# 1. 컴파일 (요구사항 Java 21 기준)
& "$jdkBin\javac.exe" --release 21 -encoding UTF-8 -d "$out\classes" `
    (Get-ChildItem -Path src\main\java -Recurse -Filter *.java).FullName
if ($LASTEXITCODE -ne 0) { Write-Host "COMPILE FAILED"; exit 1 }

# 2. 실행 가능한 jar (resources 폴더가 있으면 아이콘 등도 함께 포함)
$jarArgs = @("--create", "--file", "$out\input\tetris.jar", "--main-class", "app.Main",
    "-C", "$out\classes", ".")
if (Test-Path "src\main\resources") {
    $jarArgs += @("-C", "src\main\resources", ".")
}
& "$jdkBin\jar.exe" @jarArgs
if ($LASTEXITCODE -ne 0) { Write-Host "JAR FAILED"; exit 1 }

# 3. jpackage 옵션 (ico가 있으면 exe 아이콘으로 사용)
$jpackageArgs = @(
    "--type", "app-image",
    "--name", $appName,
    "--app-version", $appVersion,
    "--vendor", "team4",
    "--input", "$out\input",
    "--main-jar", "tetris.jar",
    "--main-class", "app.Main",
    "--dest", "$out\dist"
)
if (Test-Path $iconIco) {
    $jpackageArgs += @("--icon", (Resolve-Path $iconIco).Path)
} else {
    Write-Host "NO ICON: $iconIco not found, using default icon"
}

# 4. 실행 폴더 생성 (Java 런타임 포함, 설치 불필요)
& "$jdkBin\jpackage.exe" @jpackageArgs
if ($LASTEXITCODE -ne 0) { Write-Host "JPACKAGE FAILED"; exit 1 }

# 5. 삭제 스크립트 추가 후 zip 생성
Copy-Item "packaging\uninstall.bat" "$out\dist\$appName\"
# Compress-Archive는 백신 검사 중인 파일에서 실패할 수 있어 Windows 기본 tar 사용
$zipName = "$appName-$appVersion.zip"
& tar.exe -a -c -f "$out\$zipName" -C "$out\dist" $appName
if ($LASTEXITCODE -ne 0) { Write-Host "ZIP FAILED"; exit 1 }

Write-Host "PATH: $out\$zipName"
