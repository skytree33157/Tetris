# 요구사항과 검증 자료

[README](../README.md) · [테스트 기록](test-results.md) · [팀 노션](https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7)

교수 자료에서 명시한 조건과 평가자가 확인할 자료의 위치를 정리합니다. 요구사항을 설명하는 문서와 실제 검증 결과는 구분합니다.

## 기준 자료와 읽는 방법

- `TeamProject_Req1.pdf`: 1차 요구사항, 총 19쪽.
- `TeamProject_Req2.pdf`: 2차 요구사항, 총 12쪽.
- 쪽수는 표지를 포함한 PDF 페이지 기준입니다. 원문은 팀이 공유한 PDF와 [노션의 원문 자료](https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7)를 확인합니다.
- FR/NFR 번호는 교수 PDF의 원래 번호가 아니라 팀 노션에서 부여한 관리 ID입니다.
- PDF는 아래 자료를 모두 README 본문에 넣으라고 요구하지 않습니다. README에서 관련 문서·노션·테스트·PR로 찾아갈 수 있게 연결합니다.
- 아래 링크는 구현과 기존 테스트를 찾는 출발점입니다. 링크가 있거나 PR이 병합됐다는 사실만으로 해당 요구사항의 모든 조건이 검증됐다고 판단하지 않습니다.

## PDF가 명시적으로 요구한 것

| 구분 | 원문 근거 | 요구 내용 | 확인할 자료 |
|---|---|---|---|
| 기본 게임 기능 | 1차 5~14쪽 | 메뉴, 20×10 보드, 7종 블록, NEXT·점수, 조작·종료, 색맹 모드, 설정 지속성, 상위 10개 이상 기록 | [게임 안내](../README.md#기본-게임-규칙), 기능별 테스트, 직접 확인 결과 |
| 기능·성능 검증 | 1차 4·17쪽 | 충분한 단위 테스트, 기능·성능 충족 여부 검증 | [테스트 코드](../src/test/java), [실행 방법과 결과](test-results.md) |
| 라인 커버리지 | 1차 17쪽 / 2차 12쪽 | 직접 작성한 코드 기준 중간 평가 50%, 기말 평가 70% | 실행 버전·측정 범위·실제 커버리지 보고서 |
| 비기능 테스트 | 1차 15·17쪽 | 비기능 요구사항 충족 여부를 검증하는 테스트 1개 이상 | 환경·성능 테스트 코드, 측정 환경과 결과 |
| 반복 실행 체계 | 1차 17쪽 | 테스트도 관리하고 변경 시 손쉽게 실행하여 검증 | [Gradle 설정](../build.gradle), [CI 설정](../.github/workflows/ci.yml), [실행 명령](test-results.md#다시-실행하는-방법) |
| 난이도별 생성 확률 | 2차 3~5쪽 | I 상대 가중치 EASY 12 / NORMAL 10 / HARD 8, 나머지 각 10. 실제 게임 생성 코드로 1,000회 이상 선택하여 설정 확률과 오차 ±5% 이내인지 검증 | [확률 검증 기준](test-results.md#블록-생성-확률-검증), 테스트·횟수·기대값·실제 결과 |
| 난이도·모드 확장 | 2차 3·6~7쪽 | 난이도별 속도 증가량, 기록 구분, 삭제 애니메이션, 일반/아이템 모드, 10줄마다 새 NEXT에 아이템 공급 | [모드·난이도 설명](../README.md#모드와-난이도), 상태·고정·표시·저장 경로 테스트 |
| L·무게추 규칙 | 2차 8~9쪽 | L 무작위 위치·행 삭제·기존 점수, 너비 4칸 무게추·접촉 후 좌우 제한·하부 제거 | [아이템 명세](items.md), 효과·이동·점수 테스트 |
| 자체 아이템 명세 | 2차 7·10쪽 | 추가 아이템 최소 3종. 형태·문자·기능을 정리해 평가하는 팀에 제공 | [Bomb·Bonus·Slow 명세](items.md), 확인 절차·결과 |
| 확장성과 1차 기능 유지 | 1차 2·5쪽 / 2차 10쪽 | 메뉴·아이템 추가를 고려한 구조. 색맹 모드·설정 저장·실행 환경 등 기존 조건 유지 | 설계 설명·관련 코드·리뷰, 일반/아이템 모드 회귀 결과 |
| 실행 환경·성능 | 1차 15쪽 | Java 21 이상, Windows 11 또는 macOS 26 이상 중 선택. 1.2GHz 이상 CPU·1GB RAM·500MB 여유 공간에서 플레이 가능한 성능 | 지원 환경, 실제 실행 환경·성능·확인하지 못한 조건 |
| 개발·업무 기록 | 1차 16쪽 / 2차 11쪽 | Git/GitHub로 코드·리소스 버전 관리, 브랜치·PR 정책, Task 배정·진척과 팀원 업무 확인 | [개발 규칙](../rule.md), [노션](https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7), 관련 작업·PR |
| 이슈 추적 | 1차 16쪽 / 2차 11쪽 | 발생한 이슈를 GitHub에 등록하고 해결 내역을 추적 | [GitHub Issues](https://github.com/skytree33157/Tetris/issues), 수정 PR·재확인 결과 |
| 패키지 제작·실행 | 1차 18쪽 | 배포 패키지 제작 체계, 아이콘 있는 실행 파일, 추가 명령·설정 없이 OS 동작으로 실행. JRE 포함 또는 사전 Java·JAVA_HOME 가정 가능 | 배포 파일·빌드 방법·실행 확인, [packaging PR #77](https://github.com/skytree33157/Tetris/pull/77) |
| 팀이 정한 세부 규칙 | 1차 19쪽 / 2차 10·12쪽 | 원문과 충돌하지 않는 세부사항 결정, 모호한 사항은 문의 | 아이템·점수 명세, 결정 이유·문의 결과 |

±5%의 해석은 재현 가능한 기준으로 명시해야 합니다. 현재 팀 테스트는 기대 확률 대비 **상대오차 5% 이내**를 검사합니다. 퍼센트포인트 ±5와 혼용하지 않습니다.

## 요구사항·코드·테스트·PR 연결

아래는 주요 경로를 묶은 연결표입니다. 기존 코드·테스트의 존재와 관련 PR 상태를 2026-10-26에 확인했습니다. 2026-10-26 로컬 실행 결과는 [테스트 결과](test-results.md)에 정리했습니다.

| 팀 요구사항 / 확인 범위 | 구현 | 기존 테스트 | 관련 PR |
|---|---|---|---|
| FR-04·05·27 / NFR-11: 7종 생성·난이도별 분포 | [BlockFactory](../src/main/java/blocks/core/BlockFactory.java), [Difficulty](../src/main/java/difficulty/Difficulty.java) | [BlockFactoryTest](../src/test/java/blocks/core/BlockFactoryTest.java), [TetrominoTest](../src/test/java/blocks/core/TetrominoTest.java) | [#35](https://github.com/skytree33157/Tetris/pull/35), [#44](https://github.com/skytree33157/Tetris/pull/44) |
| FR-06·16: 색·패턴과 아이템 문자 | [BlockStyle](../src/main/java/blocks/style/BlockStyle.java), [BlockRenderer](../src/main/java/ui/BlockRenderer.java) | [BlockStyleTest](../src/test/java/blocks/style/BlockStyleTest.java), [BlockRendererTest](../src/test/java/ui/BlockRendererTest.java) | [#36](https://github.com/skytree33157/Tetris/pull/36), [#44](https://github.com/skytree33157/Tetris/pull/44) |
| FR-17·18·38: 점수 정책 | [ScoreManager](../src/main/java/score/ScoreManager.java) | [ScoreManagerTest](../src/test/java/score/ScoreManagerTest.java) | [#37](https://github.com/skytree33157/Tetris/pull/37), [#44](https://github.com/skytree33157/Tetris/pull/44) |
| FR-03·14·21: 보드·삭제·게임 오버 | [Board](../src/main/java/board/Board.java), [ActionController](../src/main/java/game/ActionController.java) | [BoardTest](../src/test/java/board/BoardTest.java), [ActionControllerTest](../src/test/java/game/ActionControllerTest.java) | [#56](https://github.com/skytree33157/Tetris/pull/56), [#71](https://github.com/skytree33157/Tetris/pull/71) |
| FR-07~12·28: 하강·조작·정지·속도 | [GameController](../src/main/java/game/GameController.java), [GameStateManager](../src/main/java/game/GameStateManager.java) | [GameControllerTest](../src/test/java/game/GameControllerTest.java), [GameLoopTest](../src/test/java/game/GameLoopTest.java), [GameStateManagerTest](../src/test/java/game/GameStateManagerTest.java) | [#49](https://github.com/skytree33157/Tetris/pull/49), [#54](https://github.com/skytree33157/Tetris/pull/54), [#71](https://github.com/skytree33157/Tetris/pull/71) |
| FR-15·30·32: NEXT·애니메이션·공급 | [ActionController](../src/main/java/game/ActionController.java), [BlockPreviewPanel](../src/main/java/game/BlockPreviewPanel.java) | [ActionControllerTest](../src/test/java/game/ActionControllerTest.java), [BlockPreviewPanelTest](../src/test/java/game/BlockPreviewPanelTest.java), [GamePanelTest](../src/test/java/game/GamePanelTest.java) | [#52](https://github.com/skytree33157/Tetris/pull/52), [#62](https://github.com/skytree33157/Tetris/pull/62) |
| FR-33~36-3: 아이템 생성·표시·효과 | [item 패키지](../src/main/java/item), [ActionController](../src/main/java/game/ActionController.java) | [BlockFactoryItemTest](../src/test/java/blocks/core/BlockFactoryItemTest.java), [아이템 타입 테스트](../src/test/java/item/types), [ItemAppearanceResolverTest](../src/test/java/ui/ItemAppearanceResolverTest.java), [ActionControllerTest](../src/test/java/game/ActionControllerTest.java) | [#38](https://github.com/skytree33157/Tetris/pull/38), [#45](https://github.com/skytree33157/Tetris/pull/45), [#47](https://github.com/skytree33157/Tetris/pull/47) |
| FR-19·20·26: 설정·저장 | [설정 패키지](../src/main/java/menu/settings) | [AppSettingsTest](../src/test/java/menu/settings/AppSettingsTest.java), [SettingsScreenTest](../src/test/java/menu/settings/SettingsScreenTest.java) | [#50](https://github.com/skytree33157/Tetris/pull/50), [#75](https://github.com/skytree33157/Tetris/pull/75) |
| FR-22~25·29: 기록·표시 | [GameOverScreen](../src/main/java/menu/GameOverScreen.java), [ScoreStorage](../src/main/java/storage/ScoreStorage.java) | [GameOverScreenTest](../src/test/java/menu/GameOverScreenTest.java), [ScoreStorageTest](../src/test/java/storage/ScoreStorageTest.java) | [#42](https://github.com/skytree33157/Tetris/pull/42), [#64](https://github.com/skytree33157/Tetris/pull/64), [#65](https://github.com/skytree33157/Tetris/pull/65) |
| NFR-01·02·07: 환경 확인 | 현재 main에 환경 전용 테스트 미반영 | [EnvironmentTest 추가 PR #76](https://github.com/skytree33157/Tetris/pull/76/files) | [#76](https://github.com/skytree33157/Tetris/pull/76) 확인 시점 미병합 |
| NFR-06·08: 테스트·커버리지 | [build.gradle](../build.gradle), [CI](../.github/workflows/ci.yml) | [전체 테스트](../src/test/java), [확인된 CI 결과](test-results.md#확인된-실행-결과) | [테스트 추가 #71](https://github.com/skytree33157/Tetris/pull/71) |
| NFR-09: 배포 | main 반영·공개 Release 대기 | [남은 배포 검증](test-results.md#남은-검증) | [#77](https://github.com/skytree33157/Tetris/pull/77) 확인 시점 미병합 |

FR-38의 난이도 배율, P당 고정 보너스, S당 500ms 증가는 팀이 정한 세부 규칙입니다. PDF에서 해당 수치를 직접 지정한 것은 아닙니다.

## 기록의 갱신

- 구현·테스트를 바꾸면 대상 커밋과 결과를 갱신합니다. 문서만 수정한 경우에도 과거 실행과 현재 문서 버전을 구분합니다.
- 오류는 GitHub Issue에 기록하고 수정 PR·재확인 결과를 연결합니다. 모든 이력을 이 문서에 복사하지 않습니다.
- 실행하거나 직접 확인하지 않은 항목은 미확인으로 남깁니다.

