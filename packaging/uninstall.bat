@echo off
chcp 65001 >nul
set "APPDIR=%~dp0"

tasklist /FI "IMAGENAME eq TFSE.exe" | find /I "TFSE.exe" >nul
if not errorlevel 1 (
    echo 게임이 실행 중입니다. 먼저 게임을 종료하세요.
    pause
    exit /b
)

choice /M "TFSE와 저장된 설정/점수를 모두 삭제할까요"
if errorlevel 2 exit /b

rem 이 창이 폴더를 붙잡지 않도록 작업 위치를 옮김
cd /d "%TEMP%"

rem 이 폴더를 열어 둔 탐색기 창은 상위 폴더로 이동
powershell -NoProfile -Command "$t='%APPDIR:~0,-1%'; try { (New-Object -ComObject Shell.Application).Windows() | Where-Object { try { $_.Document.Folder.Self.Path -eq $t } catch { $false } } | ForEach-Object { $_.Navigate((Split-Path $t)) } } catch {}"

rem 이 bat 파일이 끝난 뒤 폴더 전체 삭제 (잠겨 있으면 1초 간격으로 최대 10번 재시도)
start "" /min cmd /c "for /l %%i in (1,1,10) do (timeout /t 1 /nobreak >nul & rmdir /s /q "%APPDIR%" 2>nul & if not exist "%APPDIR%" exit)"
