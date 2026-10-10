# Tetris 클린 빌드 스크립트
# out 폴더를 매번 지우고 새로 컴파일해서, 패키지 이동/삭제로 생기는 오래된 .class 파일이 안 남게 함
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (Test-Path out) {
    Remove-Item -Recurse -Force out
}

# test 폴더의 JUnit 테스트도 함께 컴파일하도록 lib의 JUnit jar를 classpath에 추가
$junitJar = "lib\junit-platform-console-standalone-6.1.3.jar"

javac -encoding UTF-8 -cp $junitJar -d out (Get-ChildItem -Path src,test -Recurse -Filter *.java).FullName
if ($LASTEXITCODE -ne 0) {
    Write-Host "BUILD FAILED"
    exit 1
}

Write-Host "PATH: out\"
