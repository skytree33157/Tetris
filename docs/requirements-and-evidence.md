# 요구사항과 검증 자료

[README](../README.md) · [테스트 기록](test-results.md) · [팀 노션](https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7)

- FR/NFR 번호는 팀 노션에서 부여한 관리 ID입니다. 노션을 기반으로 확인해주세요.

## 요구사항 확인 및 VCS

| 구분 | 원문 근거 | 요구 내용 | 자료 |
|---|---|---|---|
| 기본 게임 기능 | 1차 5~14쪽 | 메뉴, 20×10 보드, 7종 블록, NEXT·점수, 조작·종료, 색맹 모드, 설정 지속성, 상위 10개 이상 기록 | [게임 안내](../README.md#기본-게임-규칙), 기능별 테스트, 직접 확인 결과 |
| 기능·성능 검증 | 1차 4·17쪽 | 충분한 단위 테스트, 기능·성능 충족 여부 검증 | [테스트 코드](../src/test/java), [실행 방법과 결과](test-results.md) |
| 라인 커버리지 | 1차 17쪽 / 2차 12쪽 | 직접 작성한 코드 기준 중간 평가 50%, 기말 평가 70% | [실제 커버리지 54.97%와 측정 범위](test-results.md#커버리지-기록) |
| 비기능 테스트 | 1차 15·17쪽 | 비기능 요구사항 충족 여부를 검증하는 테스트 1개 이상 | [입력 응답 시간 검사](../src/test/java/game/GameControllerTest.java), [환경 테스트 PR #76](https://github.com/skytree33157/Tetris/pull/76) |
| 반복 실행 체계 | 1차 17쪽 | 테스트도 관리하고 변경 시 손쉽게 실행하여 검증 | [Gradle 설정](../build.gradle), [CI 설정](../.github/workflows/ci.yml), [실행 명령](test-results.md#직접-실행하는-방법) |
| 난이도별 생성 확률 | 2차 3~5쪽 | I 상대 가중치 EASY 12 / NORMAL 10 / HARD 8, 나머지 각 10. 실제 게임 생성 코드로 1,000회 이상 선택하여 설정 확률과 오차 ±5% 이내인지 검증 | [확률 검증 기준](test-results.md#블록-생성-확률-검증), 테스트·횟수·기대값·실제 결과 |
| 난이도·모드 확장 | 2차 3·6~7쪽 | 난이도별 속도 증가량, 기록 구분, 삭제 애니메이션, 일반/아이템 모드, 10줄마다 새 NEXT에 아이템 공급 | [모드·난이도 설명](../README.md#모드와-난이도), [상태·고정·표시·저장 테스트 결과](test-results.md) |
| L·무게추 규칙 | 2차 8~9쪽 | L 무작위 위치·행 삭제·기존 점수, 너비 4칸 무게추·접촉 후 좌우 제한·하부 제거 | [아이템 명세](items.md), 효과·이동·점수 테스트 |
| 자체 아이템 명세 | 2차 7·10쪽 | 추가 아이템 최소 3종. 형태·문자·기능을 정리해 평가하는 팀에 제공 | [Bomb·Bonus·Slow 명세](items.md), 확인 절차·결과 |
| 확장성과 1차 기능 유지 | 1차 2·5쪽 / 2차 10쪽 | 메뉴·아이템 추가를 고려한 구조. 색맹 모드·설정 저장·실행 환경 등 기존 조건 유지 | [아이템 등록 구조](../src/main/java/item/core/ItemRegistry.java), [관련 리뷰](#리뷰에서-정리한-연동설계-내용) |
| 실행 환경·성능 | 1차 15쪽 | Java 21 이상, Windows 11 또는 macOS 26 이상 중 선택. 1.2GHz 이상 CPU·1GB RAM·500MB 여유 공간에서 플레이 가능한 성능 | 지원 환경·실행 환경·성능 측정 결과 |
| 개발·업무 기록 | 1차 16쪽 / 2차 11쪽 | Git/GitHub로 코드·리소스 버전 관리, 브랜치·PR 정책, Task 배정·진척과 팀원 업무 확인 | [개발 규칙](rule.md), [노션](https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7), 관련 작업·PR |
| 이슈 추적 | 1차 16쪽 / 2차 11쪽 | 발생한 이슈를 GitHub에 등록하고 해결 내역을 추적 | [GitHub Issues](https://github.com/skytree33157/Tetris/issues), 수정 PR·재확인 결과 |
| 패키지 제작·실행 | 1차 18쪽 | 배포 패키지 제작 체계, 아이콘 있는 실행 파일, 추가 명령·설정 없이 OS 동작으로 실행. JRE 포함 또는 사전 Java·JAVA_HOME 가정 가능 | 배포 파일·빌드 방법·실행 확인, [packaging PR #77](https://github.com/skytree33157/Tetris/pull/77) |
| 팀이 정한 세부 규칙 | 1차 19쪽 / 2차 10·12쪽 | 원문과 충돌하지 않는 세부사항 결정, 모호한 사항은 문의 | 아이템·점수 명세, 결정 이유·문의 결과 |


## 요구사항·코드·테스트·PR 연결

기능 설명에서 구현과 개발 기록으로 이동할 수 있도록 노션의 FR/NFR 번호로 정리했습니다. 전체 테스트 결과는 2026-10-26 로컬 실행 기준 **124개 통과**이며, 난이도별 실제 분포와 커버리지는 [테스트 결과](test-results.md)에 정리했습니다.

### 1차 기능 요구사항

| ID·요구사항 | 구현·규칙 | 테스트·결과 | 개발 기록(Pull Request List) |
|---|---|---|---|
| FR-01 · 시작 메뉴 | 게임 이름과 시작·설정·스코어보드·종료 항목, 방향키·Enter 조작. [StartMenu](../src/main/java/menu/StartMenu.java) |  | [#5](https://github.com/skytree33157/Tetris/pull/5) |
| FR-02 · 메뉴 확장 구조 | 메뉴 목록을 관리하고 화면 상태를 AppStateManager로 분리. [상태 관리](../src/main/java/app/AppStateManager.java) | [상태 테스트](../src/test/java/app/AppStateManagerTest.java) | [#5](https://github.com/skytree33157/Tetris/pull/5) |
| FR-03 · 20×10 보드 | 화면에 표시하는 보드는 20행×10열. 생성·충돌용 상단 여유 공간은 별도 관리. | [보드 테스트](../src/test/java/board/BoardTest.java) | [#1](https://github.com/skytree33157/Tetris/pull/1), [#56](https://github.com/skytree33157/Tetris/pull/56) / [#55](https://github.com/skytree33157/Tetris/issues/55) |
| FR-04 · 7종 테트로미노 | I·O·T·S·Z·J·L 모양과 타입별 블록 생성. [블록 7종](../src/main/java/blocks/tetromino) | [모양·회전 테스트](../src/test/java/blocks/core/TetrominoTest.java) | [#2](https://github.com/skytree33157/Tetris/pull/2) |
| FR-05 · 동일 확률 생성 | NORMAL에서 각 타입의 기대 확률 1/7. 인자 없는 생성 API도 NORMAL 적용. | [생성 확률 테스트](../src/test/java/blocks/core/BlockFactoryTest.java) / [실제 분포 결과](test-results.md#블록-생성-확률-검증) | [#2](https://github.com/skytree33157/Tetris/pull/2), [#35](https://github.com/skytree33157/Tetris/pull/35) |
| FR-06 · 블록 색상·구분 | 블록별 색상과 패턴으로 구분. [BlockStyle](../src/main/java/blocks/style/BlockStyle.java) | [스타일 테스트](../src/test/java/blocks/style/BlockStyleTest.java), [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java) | [#8](https://github.com/skytree33157/Tetris/pull/8), [#21](https://github.com/skytree33157/Tetris/pull/21) |
| FR-07 · 자동 하강 | 초기 하강 간격 1,000ms. 자동으로 한 칸씩 이동. | [루프 테스트](../src/test/java/game/GameLoopTest.java) | [#3](https://github.com/skytree33157/Tetris/pull/3), [#14](https://github.com/skytree33157/Tetris/pull/14), [#71](https://github.com/skytree33157/Tetris/pull/71) |
| FR-08 · 하강 속도 증가 | 블록 10개 고정 또는 10줄 삭제 기준으로 하강 간격 감소. [속도 규칙](../README.md#레벨과-하강-속도) | [속도 테스트](../src/test/java/game/GameStateManagerTest.java) | [#3](https://github.com/skytree33157/Tetris/pull/3), [#14](https://github.com/skytree33157/Tetris/pull/14) |
| FR-09 · 이동·회전 | 좌·우·아래 이동과 시계방향 90도 회전. 보드 끝에서는 회전 위치 보정. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java), [입력 테스트](../src/test/java/game/GameControllerTest.java) | [#6](https://github.com/skytree33157/Tetris/pull/6), [#58](https://github.com/skytree33157/Tetris/pull/58) / [#57](https://github.com/skytree33157/Tetris/issues/57) |
| FR-10 · 하드드롭 | Space로 최하단 유효 위치까지 이동·고정하고 실제 이동 거리만큼 점수 계산. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java), [입력 테스트](../src/test/java/game/GameControllerTest.java) | [#14](https://github.com/skytree33157/Tetris/pull/14), [#19](https://github.com/skytree33157/Tetris/pull/19) |
| FR-11 · 입력 반응성 | 첫 입력 즉시 이동, 길게 누르면 반복 이동. 좌우 동시 입력은 마지막 방향 반영. | [입력 테스트](../src/test/java/game/GameControllerTest.java) | [#17](https://github.com/skytree33157/Tetris/pull/17), [#54](https://github.com/skytree33157/Tetris/pull/54) / [#53](https://github.com/skytree33157/Tetris/issues/53) |
| FR-12 · 일시정지·재개 | P로 정지·재개하고 정지 중 자동 하강과 블록 이동 중단. | [루프 테스트](../src/test/java/game/GameLoopTest.java), [입력 테스트](../src/test/java/game/GameControllerTest.java) | [#23](https://github.com/skytree33157/Tetris/pull/23), [#26](https://github.com/skytree33157/Tetris/pull/26) |
| FR-13 · 게임 중 종료 | 일시정지 메뉴의 종료와 ESC 길게 누르기. 설정에서 시작 메뉴로 복귀. | [입력 테스트](../src/test/java/game/GameControllerTest.java) | [#27](https://github.com/skytree33157/Tetris/pull/27), [#69](https://github.com/skytree33157/Tetris/pull/69), [#70](https://github.com/skytree33157/Tetris/pull/70), [#73](https://github.com/skytree33157/Tetris/pull/73) |
| FR-14 · 줄 삭제 | 완성 행 삭제 후 위쪽 행 하강. 여러 행 삭제 처리. | [보드 테스트](../src/test/java/board/BoardTest.java), [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#4](https://github.com/skytree33157/Tetris/pull/4) |
| FR-15 · 화면 구성 | 보드·현재 블록·NEXT·점수·레벨·삭제 줄 수 표시. | [게임 패널 테스트](../src/test/java/game/GamePanelTest.java), [NEXT 테스트](../src/test/java/game/BlockPreviewPanelTest.java) | [#16](https://github.com/skytree33157/Tetris/pull/16), [#24](https://github.com/skytree33157/Tetris/pull/24), [#26](https://github.com/skytree33157/Tetris/pull/26) |
| FR-16 · 색맹 모드 | 일반·적색맹·녹색맹·청황색맹 모드의 색상·패턴과 아이템 문자. [접근성 설명](../README.md#설정과-접근성) | [스타일 테스트](../src/test/java/blocks/style/BlockStyleTest.java), [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java) | [#8](https://github.com/skytree33157/Tetris/pull/8), [#21](https://github.com/skytree33157/Tetris/pull/21), [#36](https://github.com/skytree33157/Tetris/pull/36) |
| FR-17 · 기본 점수 | 하강 거리·방식별 점수, 레벨별 자동 하강 단가와 실시간 표시. [점수 명세](scoring.md#하강-점수) | [점수 테스트](../src/test/java/score/ScoreManagerTest.java) / [계산 결과](test-results.md#점수-계산) | [#9](https://github.com/skytree33157/Tetris/pull/9), [#19](https://github.com/skytree33157/Tetris/pull/19) |
| FR-18 · 추가 점수 | 줄 삭제·콤보·퍼펙트 클리어 배율과 하드드롭 점수. [계산 예시](scoring.md#계산-예시) | [점수 테스트](../src/test/java/score/ScoreManagerTest.java) | [#9](https://github.com/skytree33157/Tetris/pull/9), [#19](https://github.com/skytree33157/Tetris/pull/19) |
| FR-19 · 설정 화면 | 화면 크기 3종·이동/회전 키·색상 모드·스코어보드 초기화·기본값 복원. | [설정 저장 테스트](../src/test/java/menu/settings/AppSettingsTest.java), [입력 테스트](../src/test/java/game/GameControllerTest.java) | [#15](https://github.com/skytree33157/Tetris/pull/15), [#22](https://github.com/skytree33157/Tetris/pull/22), [#29](https://github.com/skytree33157/Tetris/pull/29), [#75](https://github.com/skytree33157/Tetris/pull/75) |
| FR-20 · 설정 지속성 | 설정을 파일로 저장하고 다시 불러옴. 난이도도 저장 대상에 포함. | [설정 저장 테스트](../src/test/java/menu/settings/AppSettingsTest.java) | [#15](https://github.com/skytree33157/Tetris/pull/15), [#50](https://github.com/skytree33157/Tetris/pull/50) |
| FR-21 · 게임 오버 | 새 블록 배치 불가 또는 상단 여유 공간에 고정되면 게임 오버. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#13](https://github.com/skytree33157/Tetris/pull/13), [#56](https://github.com/skytree33157/Tetris/pull/56) / [#55](https://github.com/skytree33157/Tetris/issues/55) |
| FR-22 · 게임 종료 화면 | 최종 점수와 해당 난이도·모드의 상위 10위 표시. | [게임 종료·순위 테스트](../src/test/java/menu/GameOverScreenTest.java) | [#7](https://github.com/skytree33157/Tetris/pull/7), [#64](https://github.com/skytree33157/Tetris/pull/64) / [#63](https://github.com/skytree33157/Tetris/issues/63) |
| FR-23 · 스코어보드 갱신 | 기록 조건에 따라 이름 입력·저장 후 새 기록 강조. | [게임 종료·순위 테스트](../src/test/java/menu/GameOverScreenTest.java), [기록 저장 테스트](../src/test/java/storage/ScoreStorageTest.java) | [#7](https://github.com/skytree33157/Tetris/pull/7), [#64](https://github.com/skytree33157/Tetris/pull/64), [#65](https://github.com/skytree33157/Tetris/pull/65) |
| FR-24 · 기록 유지 | 카테고리별 상위 10위 조회와 기록 파일 저장·불러오기. | [기록 저장 테스트](../src/test/java/storage/ScoreStorageTest.java), [게임 종료·순위 테스트](../src/test/java/menu/GameOverScreenTest.java) | [#18](https://github.com/skytree33157/Tetris/pull/18), [#28](https://github.com/skytree33157/Tetris/pull/28), [#65](https://github.com/skytree33157/Tetris/pull/65) |
| FR-25 · 종료 후 이동 | 게임 종료 화면에서 시작 메뉴 또는 프로그램 종료 선택. [GameOverScreen](../src/main/java/menu/GameOverScreen.java) |  | [#20](https://github.com/skytree33157/Tetris/pull/20), [#64](https://github.com/skytree33157/Tetris/pull/64) |

### 2차 기능 요구사항

| ID·요구사항 | 구현·규칙 | 테스트·결과 | 개발 기록 |
|---|---|---|---|
| FR-26 · 난이도 설정 | 설정에서 EASY·NORMAL·HARD 선택·저장, 새 게임에 적용. | [설정 저장 테스트](../src/test/java/menu/settings/AppSettingsTest.java), [난이도 선택 테스트](../src/test/java/menu/settings/SettingsScreenTest.java) | [#50](https://github.com/skytree33157/Tetris/pull/50) |
| FR-27 · 난이도별 생성 확률 | I 가중치 EASY 12·NORMAL 10·HARD 8, 나머지 각 10. Stochastic Acceptance로 선택. | [생성 확률 테스트](../src/test/java/blocks/core/BlockFactoryTest.java) / [100,000회·오차 결과](test-results.md#블록-생성-확률-검증) | [#35](https://github.com/skytree33157/Tetris/pull/35), [#44](https://github.com/skytree33157/Tetris/pull/44) |
| FR-28 · 난이도별 하강 속도 | 10줄 삭제 시 하강 간격 감소량 EASY 80ms·NORMAL 100ms·HARD 120ms. | [속도 테스트](../src/test/java/game/GameStateManagerTest.java) | [#49](https://github.com/skytree33157/Tetris/pull/49) |
| FR-29 · 난이도·모드별 기록 | 기록에 난이도·모드 저장, 조합별 순위 조회와 종료 화면 연동. | [기록 저장 테스트](../src/test/java/storage/ScoreStorageTest.java), [게임 종료·순위 테스트](../src/test/java/menu/GameOverScreenTest.java) | [#42](https://github.com/skytree33157/Tetris/pull/42), [#48](https://github.com/skytree33157/Tetris/pull/48), [#64](https://github.com/skytree33157/Tetris/pull/64) |
| FR-30 · 줄 삭제 애니메이션 | 삭제 대상 행을 표시한 뒤 실제 삭제. 표시 중 입력 처리도 검사. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#50](https://github.com/skytree33157/Tetris/pull/50), [#71](https://github.com/skytree33157/Tetris/pull/71) |
| FR-31 · 아이템 모드 시작 | 시작 메뉴에서 일반/아이템 모드를 선택하고 GameMode 전달. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#50](https://github.com/skytree33157/Tetris/pull/50), [#48](https://github.com/skytree33157/Tetris/pull/48) |
| FR-32 · 아이템 등장 조건 | 누적 10줄 경계 뒤 기존 NEXT를 현재 블록으로 승계하고 새 NEXT에 아이템 생성. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#33](https://github.com/skytree33157/Tetris/pull/33), [#41](https://github.com/skytree33157/Tetris/pull/41), [#52](https://github.com/skytree33157/Tetris/pull/52) / [#51](https://github.com/skytree33157/Tetris/issues/51) |
| FR-33 · 아이템 식별 | L·W·B·P·S 고유 문자. 게임 화면·NEXT의 표시 경로 공유. [외형 명세](items.md) | [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java), [NEXT 테스트](../src/test/java/game/BlockPreviewPanelTest.java), [저장·복원 테스트](../src/test/java/ui/ItemAppearanceResolverTest.java) | [#36](https://github.com/skytree33157/Tetris/pull/36), [#38](https://github.com/skytree33157/Tetris/pull/38), [#45](https://github.com/skytree33157/Tetris/pull/45), [#68](https://github.com/skytree33157/Tetris/pull/68) |
| FR-34 · 줄 삭제 L | 일반 블록의 채워진 칸 중 무작위 한 칸에 L. 고정 시 해당 행 강제 삭제·기존 줄 점수 적용. | [아이템 생성 테스트](../src/test/java/blocks/core/BlockFactoryItemTest.java), [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#32](https://github.com/skytree33157/Tetris/pull/32), [#36](https://github.com/skytree33157/Tetris/pull/36) |
| FR-35 · 무게추 W | 너비 4칸. 접촉 전 좌우 이동, 접촉 후 좌우 제한·아래 블록 제거. 제거 효과 자체 0점. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#33](https://github.com/skytree33157/Tetris/pull/33), [#36](https://github.com/skytree33157/Tetris/pull/36) |
| FR-36 · 추가 아이템 3종 | Bonus·Bomb·Slow의 형태·문자·발동 조건·효과를 명세. [아이템 문서](items.md) | [아이템 생성 테스트](../src/test/java/blocks/core/BlockFactoryItemTest.java), [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java), [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#38](https://github.com/skytree33157/Tetris/pull/38), [#45](https://github.com/skytree33157/Tetris/pull/45), [#47](https://github.com/skytree33157/Tetris/pull/47) |
| FR-36-1 · Bonus P | P를 포함한 행 삭제 시 P 한 칸당 기본 1,000점. 난이도 배율 적용. | [Bonus 타입 테스트](../src/test/java/item/types/BonusItemTest.java), [점수 테스트](../src/test/java/score/ScoreManagerTest.java), [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#38](https://github.com/skytree33157/Tetris/pull/38), [#37](https://github.com/skytree33157/Tetris/pull/37), [#47](https://github.com/skytree33157/Tetris/pull/47) |
| FR-36-2 · Bomb B | 1×1 폭탄 고정 시 중심 3×3 제거·폭발 표시. [범위·점수 명세](items.md#폭탄-b) | [Bomb 타입 테스트](../src/test/java/item/types/BombItemTest.java), [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java), [보드 테스트](../src/test/java/board/BoardTest.java) | [#38](https://github.com/skytree33157/Tetris/pull/38), [#41](https://github.com/skytree33157/Tetris/pull/41), [#68](https://github.com/skytree33157/Tetris/pull/68) / [#66](https://github.com/skytree33157/Tetris/issues/66) |
| FR-36-3 · Slow S | S를 포함한 행 삭제 시 S 한 칸당 하강 간격 500ms 증가, 최대 1,000ms. | [Slow 타입 테스트](../src/test/java/item/types/SlowItemTest.java), [속도 테스트](../src/test/java/game/GameStateManagerTest.java), [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java) | [#45](https://github.com/skytree33157/Tetris/pull/45), [#47](https://github.com/skytree33157/Tetris/pull/47) |
| FR-37 · 아이템 모드 연동 | 모드별 공급·색상·점수·정지·게임 오버·기록 메타데이터를 각 모듈에 연결. | [고정·효과 테스트](../src/test/java/game/ActionControllerTest.java), [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java), [입력 테스트](../src/test/java/game/GameControllerTest.java), [게임 종료·순위 테스트](../src/test/java/menu/GameOverScreenTest.java) | [#48](https://github.com/skytree33157/Tetris/pull/48), [#50](https://github.com/skytree33157/Tetris/pull/50), [#64](https://github.com/skytree33157/Tetris/pull/64) |
| FR-38 · 난이도·아이템 점수 | EASY ×0.8·NORMAL ×1.0·HARD ×1.2, P 개수별 고정 보너스. [점수 명세](scoring.md) | [점수 테스트](../src/test/java/score/ScoreManagerTest.java) / [점수 검증](test-results.md#점수-계산) | [#37](https://github.com/skytree33157/Tetris/pull/37), [#44](https://github.com/skytree33157/Tetris/pull/44), [#47](https://github.com/skytree33157/Tetris/pull/47) |

### 비기능 요구사항

| ID·요구사항 | 구현·규칙 | 테스트·결과 | 개발 기록 |
|---|---|---|---|
| NFR-01 · 실행 환경 | Java 21 이상·Windows 11 대상. [실행 안내](../README.md#다운로드와-실행) |  | [#76](https://github.com/skytree33157/Tetris/pull/76) |
| NFR-02 · 최소 하드웨어 | CPU 1.2GHz 이상·RAM 1GB·여유 디스크 500MB 기준. |  | [#76](https://github.com/skytree33157/Tetris/pull/76) |
| NFR-03 · 성능 | 이동 키 처리 250ms 제한과 실제 위치 변경을 자동 검사. | [입력 테스트](../src/test/java/game/GameControllerTest.java)의 `movementKeyTakesEffectImmediatelyWithinTheInputResponseBudget` 통과 | [#71](https://github.com/skytree33157/Tetris/pull/71) |
| NFR-04 · 산출물 추적성 | 요구사항별 설명·코드·테스트·PR·이슈·리뷰 댓글 연결. | 이 문서 / [팀 노션](https://app.notion.com/p/3dd5a0723f0d8027bed9e58150ca3dc7) | [이슈](https://github.com/skytree33157/Tetris/issues) · [PR](https://github.com/skytree33157/Tetris/pulls) |
| NFR-05 · 버전 관리 | Git/GitHub 사용, main·feature·fix·test·docs 브랜치와 커밋·PR 규칙. | [개발 규칙](rule.md) | [변경 이력](https://github.com/skytree33157/Tetris/commits/main/) |
| NFR-06 · 단위 테스트·커버리지 | 23개 클래스·124개 테스트 통과. 보고서 기준 LINE 54.97% — Main·ui 제외. | [실행 결과](test-results.md#테스트-결과) / [커버리지](test-results.md#커버리지-기록) | [#44](https://github.com/skytree33157/Tetris/pull/44), [#62](https://github.com/skytree33157/Tetris/pull/62), [#65](https://github.com/skytree33157/Tetris/pull/65), [#71](https://github.com/skytree33157/Tetris/pull/71) |
| NFR-07 · 비기능 검증 | 키 입력 반응 시간의 상한을 검사하는 자동 테스트. | [입력 테스트](../src/test/java/game/GameControllerTest.java)의 250ms 제한 검사 통과 | [#71](https://github.com/skytree33157/Tetris/pull/71), [#76](https://github.com/skytree33157/Tetris/pull/76) |
| NFR-08 · 테스트 실행 체계 | Gradle Wrapper·JUnit·JaCoCo·GitHub Actions로 테스트와 커버리지 검사. | [실행 명령](test-results.md#직접-실행하는-방법) · [CI 설정](../.github/workflows/ci.yml) | [실행된 CI](https://github.com/skytree33157/Tetris/actions/runs/40293831562) |
| NFR-09 · 배포 가능성 | 아이콘·런타임을 포함하는 TFSE ZIP과 EXE 패키징. |  | [#77](https://github.com/skytree33157/Tetris/pull/77) |
| NFR-10 · 확장성 | Factory의 생성 API, Registry의 생성·표시 정보 등록, 아이템별 클래스 분리. | [ItemRegistry](../src/main/java/item/core/ItemRegistry.java) · [아이템 사용 문서](../src/main/java/item/item_ReadMe_for_dev.MD) | [#36](https://github.com/skytree33157/Tetris/pull/36), [#38](https://github.com/skytree33157/Tetris/pull/38), [#45](https://github.com/skytree33157/Tetris/pull/45) |
| NFR-11 · 확률 검증 | 실제 생성기로 분포 검사 7개, 각 100,000회. 최대 상대오차 1.864%. | [생성 확률 테스트](../src/test/java/blocks/core/BlockFactoryTest.java) / [난이도별 결과](test-results.md#블록-생성-확률-검증) | [#35](https://github.com/skytree33157/Tetris/pull/35), [#44](https://github.com/skytree33157/Tetris/pull/44) |

## 개발 중 발생한 이슈와 해결

| 관련 요구사항 | 변경 내용 | 이슈 | 수정 PR | 리뷰 댓글 |
|---|---|---|---|---|
| FR-32 · NEXT 아이템 | 기존 NEXT 승계와 새 NEXT 아이템 생성 순서 조정. | [#51](https://github.com/skytree33157/Tetris/issues/51) | [#52](https://github.com/skytree33157/Tetris/pull/52) | [수정 확인](https://github.com/skytree33157/Tetris/pull/52#pullrequestreview-5469611757) |
| FR-11 · 조작감 | 첫 입력 즉시 반영·반복 이동·마지막 좌우 입력 우선 처리. | [#53](https://github.com/skytree33157/Tetris/issues/53) | [#54](https://github.com/skytree33157/Tetris/pull/54) | [플레이 후 리뷰](https://github.com/skytree33157/Tetris/pull/54#pullrequestreview-5470874140) |
| FR-03·21 · 보드 상단 | 보이는 보드 20행을 유지하며 상단 여유 공간과 고정 시 게임 오버 판정 추가. | [#55](https://github.com/skytree33157/Tetris/issues/55) | [#56](https://github.com/skytree33157/Tetris/pull/56) | [보드 크기 확인](https://github.com/skytree33157/Tetris/pull/56#pullrequestreview-5471353367) |
| FR-09 · 벽 회전 | 보드 양 끝에서 회전할 수 있도록 위치 보정. | [#57](https://github.com/skytree33157/Tetris/issues/57) | [#58](https://github.com/skytree33157/Tetris/pull/58) | [리뷰](https://github.com/skytree33157/Tetris/pull/58#pullrequestreview-5487810849) |
| FR-22·23·29 · 종료·기록 화면 | 종료 화면에 순위와 새 기록 강조, 기록 인코딩·화면 이동 정리. | [#63](https://github.com/skytree33157/Tetris/issues/63) | [#64](https://github.com/skytree33157/Tetris/pull/64) |  |
| FR-33·36-2 · 아이템 표시·폭탄 | 부착 아이템 표시 대비, 폭발 표시와 비어 버린 행 하강 처리. | [#66](https://github.com/skytree33157/Tetris/issues/66) | [#68](https://github.com/skytree33157/Tetris/pull/68) |  |
| FR-13·19·26 · 게임 중 메뉴 | 게임 중 난이도 변경 제한, 메뉴 이름과 종료 동작 정리. | [#67](https://github.com/skytree33157/Tetris/issues/67) | [#69](https://github.com/skytree33157/Tetris/pull/69) |  |
| FR-13 · 시작 메뉴 복귀 | 창을 닫은 뒤 후속 갱신·게임 재개가 실행되지 않도록 closed·stopped 처리. | [#72](https://github.com/skytree33157/Tetris/issues/72) | [#73](https://github.com/skytree33157/Tetris/pull/73) |  |
| FR-19 · 키 재설정 | ESC 취소 시 기존 키 유지, 중복 키와 전용 키 지정 제한. | [#74](https://github.com/skytree33157/Tetris/issues/74) | [#75](https://github.com/skytree33157/Tetris/pull/75) |  |

## 리뷰에서 정리한 연동·설계 내용

| 주제 | 논의 내용 | 리뷰 댓글 | 반영·후속 기록 |
|---|---|---|---|
| 아이템 생성·외형과 효과 연결의 역할 | 생성·외형·저장 변환 API와 게임 로직의 연결 범위를 구분했습니다. | [표시·저장 API 범위](https://github.com/skytree33157/Tetris/pull/36#discussion_r4214357118) | [#36](https://github.com/skytree33157/Tetris/pull/36) → [#47](https://github.com/skytree33157/Tetris/pull/47) |
| P/S 판별과 보너스 개수 | 공통 저장 표시 대신 복원한 문자로 구분하고, 행 수가 아닌 P 칸 수를 보너스로 전달하도록 리뷰했습니다. | [판별·개수 집계 요청](https://github.com/skytree33157/Tetris/pull/43#pullrequestreview-5458810833) | [#43 변경 통합 안내](https://github.com/skytree33157/Tetris/pull/43#issuecomment-6073276899) → [#47](https://github.com/skytree33157/Tetris/pull/47) |
| Slow 레벨·속도 경계와 마커 복원 | 레벨 상한·최소 하강 간격·P 개수 집계·공통 문자 복원 API 사용을 리뷰하고 반영을 확인했습니다. | [수정 요청](https://github.com/skytree33157/Tetris/pull/47#pullrequestreview-5459599776) | [수정 후 확인](https://github.com/skytree33157/Tetris/pull/47#pullrequestreview-5465668235) |
| 아이템 클래스와 테스트의 API 호환 | Bomb 효과 구현 중 좌표 getter 변경에 맞춰 테스트를 정리하는 내용을 논의했습니다. | [호환성 논의](https://github.com/skytree33157/Tetris/pull/47#pullrequestreview-5465668235) | [현재 BombItemTest](../src/test/java/item/types/BombItemTest.java) |

FR-38의 난이도 배율·P당 고정 보너스와 Slow의 S당 500ms 증가는 팀이 정한 세부 규칙입니다. 설명과 계산 예시는 [점수 문서](scoring.md), 아이템별 발동 조건은 [아이템 문서](items.md)에 정리했습니다.
