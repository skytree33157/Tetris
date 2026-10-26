# 테스트 결과

[README](../README.md) · [요구사항·코드·테스트·PR 연결](requirements-and-evidence.md)

[생성 확률](#블록-생성-확률-검증) · [커버리지](#커버리지-기록) · [속도](#하강-속도와-레벨) · [아이템·NEXT](#아이템과-next) · [점수](#점수-계산) · [색상·표시](#색상-모드와-아이템-표시) · [설정·기록](#설정과-스코어보드)


---

| 항목 | 실제 결과 | 판정                    |
|---|---|-----------------------|
| 테스트 클래스 | 23개 | 전체 실행                 |
| 테스트 케이스 | 124개 성공 / 실패 0 / 오류 0 / 건너뜀 0 | 통과                    |
| 블록 생성 분포 | 7개 분포 검사, 각 100,000회. 최대 상대오차 1.864% | 상대오차 5% 이내로 통과        |
| 라인 커버리지 | 1,156 / 2,103줄, **54.97%** | 보고서 범위에서 중간 50% 이상 통과 |


## 블록 생성 확률 검증

모든 분포 검사 통과. [BlockFactory](../src/main/java/blocks/core/BlockFactory.java)를 검사별 100,000회 호출했습니다. 
- 테스트 코드: [BlockFactoryTest](../src/test/java/blocks/core/BlockFactoryTest.java)
- 고정 seed : 20261007의 검사 결과
- 관련 PR: [#35](https://github.com/skytree33157/Tetris/pull/35), [#44](https://github.com/skytree33157/Tetris/pull/44)

| 난이도 | I 기대 확률 | 고정 seed 검사 최대 상대오차 | 공개 API 검사 최대 상대오차 | 판정 |
|---|---:|---:|---:|---|
| EASY | 12/72 ≈ 16.67% | 1.864% | 1.657% | 통과 |
| NORMAL | 1/7 ≈ 14.29% | 1.160% | 1.423% | 통과 |
| HARD | 8/68 ≈ 11.76% | 1.135% | 1.332% | 통과 |

인자 없는 기본 생성 API는 NORMAL 분포를 검사하며, 최대 상대오차 **1.223%**로 통과했습니다. 다른 6종의 기대 확률은 EASY 각 10/72, NORMAL 각 1/7, HARD 각 10/68입니다.

<details>
<summary>실제 생성 개수 — 각 행 100,000회, I/O/T/S/Z/J/L 순서</summary>

| 검사 | I | O | T | S | Z | J | L |
|---|---:|---:|---:|---:|---:|---:|---:|
| default NORMAL | 14,335 | 14,347 | 14,119 | 14,349 | 14,333 | 14,406 | 14,111 |
| seeded EASY | 16,810 | 13,815 | 13,848 | 14,007 | 14,076 | 13,814 | 13,630 |
| seeded NORMAL | 14,384 | 14,240 | 14,276 | 14,398 | 14,352 | 14,230 | 14,120 |
| seeded HARD | 11,872 | 14,656 | 14,691 | 14,791 | 14,818 | 14,633 | 14,539 |
| public EASY | 16,553 | 14,046 | 13,665 | 13,819 | 14,119 | 13,975 | 13,823 |
| public NORMAL | 14,165 | 14,209 | 14,253 | 14,417 | 14,237 | 14,489 | 14,230 |
| public HARD | 11,608 | 14,623 | 14,769 | 14,727 | 14,771 | 14,874 | 14,628 |

원본: `TEST-blocks.core.BlockFactoryTest.xml`의 `system-out`.

</details>

## 커버리지 기록

| 구분 | 줄 수 |
|---|---:|
| 테스트에서 실행된 줄 | 1,156 |
| 실행되지 않은 줄 | 947 |
| 보고서 전체 | 2,103 |
| 실행 비율 | **54.97%** |

[build.gradle](../build.gradle)의 HTML·XML 보고서는 `Main.class`와 `ui` 패키지를 제외합니다. 
## 하강 속도와 레벨

초기 1초, 블록·줄 기준 상승, 난이도별 하강 간격 감소량, Slow 상한, 최소 간격을 검사합니다.

[테스트 코드](../src/test/java/game/GameStateManagerTest.java) · [속도 설명](../README.md#레벨과-하강-속도) · [PR #49](https://github.com/skytree33157/Tetris/pull/49)

## 아이템과 NEXT

| 확인 범위 | 테스트 코드 | 실제 결과 |
|---|---|---|
| 아이템 생성·부착 위치 | [BlockFactoryItemTest](../src/test/java/blocks/core/BlockFactoryItemTest.java) | 5개 통과 |
| Registry 등록·표시 정보 | [ItemRegistryTest](../src/test/java/item/core/ItemRegistryTest.java) | 5개 통과 |
| Bomb·Bonus·Slow 타입 | [아이템 타입 테스트](../src/test/java/item/types) | 각각 1·2·2개 통과 |
| 고정·아이템 공급·효과·애니메이션 등 | [ActionControllerTest](../src/test/java/game/ActionControllerTest.java) | 16개 통과 |
| NEXT 패널 | [BlockPreviewPanelTest](../src/test/java/game/BlockPreviewPanelTest.java) | 3개 통과 |

ActionControllerTest에는 10줄 경계 뒤 새 NEXT 공급·일반 모드 제외, L 행 삭제·점수, W 접촉·하부 제거, Bomb 애니메이션, P/S 발동 검사가 포함됩니다. 

[아이템 규칙](items.md) · 관련 PR [#38](https://github.com/skytree33157/Tetris/pull/38), [#45](https://github.com/skytree33157/Tetris/pull/45), [#47](https://github.com/skytree33157/Tetris/pull/47), [#52](https://github.com/skytree33157/Tetris/pull/52)

## 점수 계산

**ScoreManagerTest 9개 통과.** 하강 거리·방식·레벨·난이도, 배율 정규화·반올림, 콤보·퍼펙트 클리어 배율, P 개수별 고정 보너스, 초기화·잘못된 입력을 검사합니다.

[테스트 코드](../src/test/java/score/ScoreManagerTest.java) · [점수 규칙](scoring.md) · [PR #37](https://github.com/skytree33157/Tetris/pull/37)


## 색상 모드와 아이템 표시

**BlockStyleTest 2개, BlockRendererTest 6개, ItemAppearanceResolverTest 4개 통과.** 색·패턴, 일반 칸과 아이템 칸, 저장 전후의 렌더링·문자 복원을 검사합니다. 

[스타일 테스트](../src/test/java/blocks/style/BlockStyleTest.java) · [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java) · [저장·복원 테스트](../src/test/java/ui/ItemAppearanceResolverTest.java)

## 설정과 스코어보드

| 확인 범위 | 테스트 코드 | 실제 결과 |
|---|---|---|
| 설정·저장 | [AppSettingsTest](../src/test/java/menu/settings/AppSettingsTest.java), [SettingsScreenTest](../src/test/java/menu/settings/SettingsScreenTest.java) | 각각 5·1개 통과 |
| 기록 파일·종료 화면 | [ScoreStorageTest](../src/test/java/storage/ScoreStorageTest.java), [GameOverScreenTest](../src/test/java/menu/GameOverScreenTest.java) | 각각 6개 통과 |

[설정 안내](../README.md#설정과-접근성) · [기록 안내](../README.md#점수와-스코어보드) · 관련 PR [#65](https://github.com/skytree33157/Tetris/pull/65), [#75](https://github.com/skytree33157/Tetris/pull/75)

## 보드와 기본 조작

**BoardTest 8개, GameControllerTest 9개, GameLoopTest 4개, GamePanelTest 4개 통과.** 보드·줄 삭제·조작·루프·패널 경로를 검사합니다. 고정·충돌 경로는 위 ActionControllerTest에서도 검사합니다.

[보드 테스트](../src/test/java/board/BoardTest.java) · [입력 테스트](../src/test/java/game/GameControllerTest.java) · [루프 테스트](../src/test/java/game/GameLoopTest.java) · [패널 테스트](../src/test/java/game/GamePanelTest.java)

이외 [TetrominoTest](../src/test/java/blocks/core/TetrominoTest.java) 3개, [AppStateManagerTest](../src/test/java/app/AppStateManagerTest.java) 2개도 통과했습니다. 

## 직접 실행하는 방법

JDK 21을 준비하고 프로젝트 루트의 PowerShell에서 실행합니다.

```powershell
.\gradlew.bat test jacocoTestReport jacocoTestCoverageVerification --rerun-tasks
```

- 개별 결과: `build/reports/tests/test/index.html`
- 커버리지: `build/reports/jacoco/test/html/index.html`
- 원본 XML·확률 표준 출력: `build/test-results/test/TEST-*.xml`

전체 보고서는 위 로컬 경로에 생성되며, 코드·테스트를 바꾼 뒤에는 다시 실행한 버전과 결과로 갱신합니다.

