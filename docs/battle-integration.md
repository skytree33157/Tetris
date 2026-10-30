# 대전 공격·승패 처리 (FR-41~43)

## 구현 계획

- [x] Board: 공격 행 삽입·밀려남 판정, 삭제되는 폭탄 행 조회, 퍼펙트 클리어 판정 검증.
- [x] ActionController: 삭제 전 공격 복사, 삭제 완료 후 전달, 수신 대기열 10줄 제한, NEXT 전 삽입.
- [x] game.BattleController: 로컬 플레이어 등록, 공격 전달/원격 전송 자료, 승패·종료 관리.
- [x] 공격 칸 표시와 기존 입력·자동 하강의 종료 후 점수 변경 방지.
- [x] 단위·통합·동시 실행 테스트, 전체 회귀 테스트 및 독립 코드 리뷰.

## 범위

기존 Board/ActionController/아이템/점수 규칙을 재사용한다. 새 운영 클래스는
`game.BattleController` 하나이며, 화면·소켓·시간제한 타이머·홀드·고스트는 후속 작업이다.
일반·아이템을 포함하여 한 번의 처리에서 서로 다른 행이 2개 이상 실제 삭제되면 공격한다.
서로 다른 블록에서 1줄씩 지운 것은 합산하지 않는다.

## 작업 제약

요청된 브랜치는 `feature/add-battlemode`이고 커밋·푸시·PR은 사용자 담당이다.
현재 실행 환경에서 `.git/index.lock` 쓰기가 거부되어 브랜치 생성과 스테이징이 차단되었다.
프로젝트 소스와 테스트만 작업하며 기존 사용자 파일은 건드리지 않는다.

## 공격 규칙

- 한 번의 블록 처리에서 삭제되는 서로 다른 행을 합친다. 1줄씩 여러 번 삭제한 것은 합산하지 않는다.
- L의 강제 삭제와 일반 완성 행은 중복 없이 합친다. 폭탄은 일부 칸만 지운 행은 제외하고,
  실제 비어 압축되는 행과 폭발 직후 완성 행 삭제로 없어지는 행을 합친다.
- 삭제 전 행을 위→아래 순서로 복사하고 마지막 블록의 실제 점유 칸만 빈칸으로 만든다.
  나머지 점유 칸은 ATTACK(11)로 바꿔 아이템 문자·효과를 상대에게 복제하지 않는다.
- 삭제 애니메이션이 완료된 뒤 전달한다. 경기 종료로 취소된 애니메이션에서는 공격하지 않는다.
- 새 공격은 대기 행 아래에 붙인다. 전체의 위쪽 10행만 남기므로 기존 8행+신규 4행이면
  신규 위쪽 2행만 받고 나머지 2행은 버린다. 대기 행은 조회 시에도 깊은 복사한다.
- 현재 블록의 고정·행 삭제·아이템·점수 처리가 끝난 뒤, 다음 블록 배치 전에 대기 공격을 삽입한다.
  자신의 고정 결과가 이미 게임 오버라면 다음 블록과 공격 반영을 진행하지 않는다.
- 수신 행 등으로 5행 이상이 한꺼번에 삭제되어도 실제 삭제·공격 행 수는 모두 보존한다.
  줄 삭제 점수의 기본값은 기존 최대인 4줄(Tetris) 점수를 상한으로 사용한다.
- 공격 삽입으로 위쪽 블록이 배열 밖으로 밀리거나 숨은 행에 블록이 남거나 새 블록을 놓지 못하면 패배한다.

## 로컬 화면 담당자 연결

기존 Board/GameStateManager/ScoreManager/ActionController를 플레이어마다 따로 생성한다.
기존 GameScreen 창 두 개를 복제하지 말고, 대전 화면이 두 플레이어의 객체를 보관한다.

```java
BattleController battle = new BattleController();
battle.registerPlayer(BattleController.Player.ONE, firstAction);
battle.registerPlayer(BattleController.Player.TWO, secondAction);
battle.start();
// 이후 플레이어별 입력/자동 하강을 시작한다.
```

등록은 **한 번도 조작하지 않은 새 ActionController**로, 게임 실행 스레드/입력을 시작하기 전에 한다.
등록된 두 게임은 경기 관리자 잠금 하나로 상태를 변경한다. 실행 중 다른 경기에 옮겨 등록할 수 없다.

| 기능 | API |
| --- | --- |
| 이동·회전 | 기존 moveLeftAction / moveRightAction / rotateAction |
| 점수 포함 하강 | softDropAction / automaticDropAction / hardDropWithScoreAction |
| 기존 저수준 API | moveDownAction / hardDropAction은 이전과 같이 하강 점수를 별도로 부여하지 않음 |
| 공격 미리보기 | getPendingAttackRows / getPendingAttackCount |
| 고정 보드 복사 | getBoardSnapshot |
| 현재 블록/NEXT | 기존 getCurrentBlock / getNextBlock (가변 참조이므로 통신에서는 직접 전송하지 말고 복사) |
| 경기 결과 | battle.getOutcome / getEndReason |
| 시간 만료 | battle.finishByTime(oneScore, twoScore) |
| 화면 이탈·연결 종료 | battle.close() |

기존 GameController/GameLoop는 점수 포함 진입점을 사용한다. 종료 후에는 조작·점수·예약된
삭제가 진행되지 않으며 GameLoop는 멈춘 컨트롤러를 확인하고 종료한다.
화면 담당자는 자신의 렌더/입력 타이머도 정리해야 한다.
2인 키 배정은 별도 구현 대상이다. 기존 GameController 두 개는 전역 AppSettings의 동일 키를
읽으므로 그대로 동일 창에 설치하면 안 된다.

대전 종료 화면은 Outcome을 표시하고 **싱글 GameOverScreen의 기록 저장 경로를 호출하지 않는다.**
시간제한 타이머는 후속 담당자가 구현한다. 점수 비교 결과가 같으면 DRAW다.
최종 점수 비교와 진행 정지를 같은 순간에 처리하려면 `synchronized (battle)` 안에서 두
컨트롤러의 getScore()를 읽고 finishByTime()을 호출한다. 대전 모드 선택/타이머의 기준 시간도
경기 단위로 하나를 사용한다. 블록 생성·아이템 규칙은 기존 GameMode.NORMAL/ITEM을 재사용한다.

## P2P 담당자 연결

서버는 `new BattleController()`, 클라이언트는 `new BattleController(false)`를 사용한다.
각 PC에는 자신의 ActionController만 같은 Player ID(서버 ONE, 클라이언트 TWO)로 등록한다.
서버 모드 선택·양쪽 준비 확인을 끝낸 뒤 각 PC의 start()를 호출한다.

1. drainOutgoingAttacks()를 꺼내 반환 후 소켓으로 전송한다.
2. 받은 공격은 상대 PC의 battle.receiveAttack(localPlayerId, attack.rows())로 전달한다.
3. 클라이언트의 drainOutgoingGameOvers()에서 탈락을 꺼내 서버에 보고한다.
4. 서버는 reportGameOver(loser) 또는 finishByTime()으로 결과를 한 번 확정한다.
5. 클라이언트는 서버 결과를 acceptRemoteResult(outcome, reason)로 적용한다.

클라이언트가 자기 탈락을 감지하면 로컬 진행을 멈추되 최종 결과는 서버에게서 받는다.
서버가 동시에 들어온 사건을 처리한 순서로 먼저 확정된 종료가 우선한다.
두 플레이어가 시작 시점부터 배치 불가능한 로컬 경기는 DRAW다.
재경기는 새 BattleController와 새 게임 객체로 시작하며 연결 자체는 통신 담당자가 유지한다.

이 API는 소켓 전송 완료를 보장하지 않는다. 전송 실패 처리, 경기 ID·메시지 순번·중복 제거,
상태/NEXT/현재 블록/점수 동기화, 시작 동기화, 지연·단절 감지, 최근 IP 저장, 200ms 성능 검증은
통신·UI 담당 범위다. 네트워크/화면 처리는 경기 잠금 밖에서 수행해야 한다.
완전한 P2P나 대전 메뉴가 이 PR에서 완성된 것으로 표시하면 안 된다.

## 테스트와 리뷰 기록

새 테스트: board.BoardAttackTest, game.BattleAttackTest, game.BattleControllerTest.
기존 BlockStyleTest/TetrominoTest도 ATTACK 타입에 맞춰 확장한다.
기능이 없을 때 삽입 API/경기 관리자 테스트 실패를 확인한 후 구현했다.
독립 리뷰에서 폭탄 후속 삭제 누락과 5행 이상 삭제의 점수 예외를 재현 테스트로 확인하고 수정했다.
P2P 결과 권한은 서버가 소유하도록 설계를 구체화했다.

2026-10-30 검증 결과:

- JDK 21로 운영 코드와 전체 테스트를 새로 컴파일하고 JUnit Platform 1.11.4로 실행:
  **163개 통과, 실패/건너뜀 0** (기존 132개 대비 31개 추가).
- JaCoCo 0.8.12: Main/ui를 포함한 전체 라인 **1,594/2,556 = 62.36%**.
  BattleController 87/89, ActionController 279/295, Board 155/172.
  전체 기말 70% 달성으로 간주하지 않는다. Gradle의 기존 제외 범위/도구 버전과 측정 범위가 다르다.
- 보고서: `build/reports/battle-tests/jacoco/index.html`, `build/reports/battle-tests/junit/`.
- Gradle 원래 명령은 사용자 홈 캐시 잠금 접근 제한에 막혔다. 쓰기 가능한 임시 캐시로도
  Gradle 데몬 연결이 실패하여 이 환경에서는 Gradle 태스크/커버리지 게이트 완료를 확인하지 못했다.
  위 JDK/JUnit 직접 실행으로 기능 회귀를 검증했다.
- 실제 대전 화면/P2P 소켓 테스트는 해당 후속 기능 구현 후 별도로 필요하다.
- `git diff --check` 통과. 브랜치 생성과 git add는 모두 `.git/index.lock: Permission denied`로 실패했다.

빌드·스테이징은 다음 명령을 프로젝트 루트의 IntelliJ 터미널에서 실행한다.

```powershell
git switch -c feature/add-battlemode main
.\gradlew.bat test jacocoTestCoverageVerification
git add -- docs/battle-integration.md src/main/java/blocks/core/BlockType.java src/main/java/blocks/style/BlockStyle.java src/main/java/board/Board.java src/main/java/game/ActionController.java src/main/java/game/BattleController.java src/main/java/game/GameController.java src/main/java/game/GameLoop.java src/test/java/blocks/core/TetrominoTest.java src/test/java/blocks/style/BlockStyleTest.java src/test/java/board/BoardAttackTest.java src/test/java/game/BattleAttackTest.java src/test/java/game/BattleControllerTest.java
git diff --cached --stat
```

기존 사용자 파일을 포함하지 않도록 `git add .`는 사용하지 않는다. 커밋·푸시·PR은 수행하지 않았다.
