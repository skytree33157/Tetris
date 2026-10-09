# Tetris 클린 빌드 스크립트
# out 폴더를 매번 지우고 새로 컴파일해서, 패키지 이동/삭제로 생기는 오래된 .class 파일이 안 남게 함
$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

if (Test-Path out) {
    Remove-Item -Recurse -Force out
}

javac -d out (Get-ChildItem -Recurse -Filter *.java).FullName

Write-Host "PATH: out\"
