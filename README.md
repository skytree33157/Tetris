# Tetris

## 노션
- https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7

## 예제코드
- https://github.com/Jindae/SeoulTech-SE-Tetris-Ref

## test
buildfortest.ps1 이용시 out 컴파일 파일 생성
Tetris루트에서 java -cp out app.Main 로 cmd로 실행

## 실행 파일 빌드
Tetris 루트의 PowerShell에서 `.\packaging\buildexe.ps1` 실행 → `build\exe\TFSE-1.0.0.zip` 생성 (JDK 21 이상 필요)

아이콘: exe 아이콘은 `packaging\TFSE.ico`, 게임 창 아이콘은 `src\main\resources\icon\TFSE_{16,32,48,256}.png`를 사용합니다. 아이콘을 바꾸면 ico와 png를 함께 교체하세요.

## 실행
1. Releases에서 `TFSE-1.0.0.zip`을 받아 압축 해제 (바탕화면, 다운로드 폴더 등. `C:\Program Files`에는 풀지 마세요)
2. `TFSE.exe` 실행 (Java 설치 불필요)
3. "Windows의 PC 보호" 창이 뜨면 "추가 정보" → "실행"

## 삭제
폴더 안의 `uninstall.bat`를 실행하거나 폴더를 통째로 삭제하면 설정과 점수까지 모두 지워집니다.
