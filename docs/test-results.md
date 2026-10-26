# 테스트와 검증 기록

[README](../README.md) · [요구사항과 근거](requirements-and-evidence.md) · [촬영 안내](screenshots.md)

[전체 실행 결과](#확인된-실행-결과) · [확률](#블록-생성-확률-검증) · [속도](#하강-속도와-레벨) · [아이템·NEXT](#아이템과-next) · [점수](#점수-계산) · [색상·표시](#색상-모드와-아이템-표시) · [설정·기록](#설정과-스코어보드) · [커버리지](#커버리지-기록)

검사한 버전·환경·명령·결과를 남깁니다. 테스트 계획, 코드의 존재, 실제 실행 결과를 구분합니다. 전체 PR·커밋·이슈 이력은 복사하지 않고 필요한 근거만 연결합니다.

## 확인된 실행 결과

2026-10-26에 기존 GitHub Actions 로그를 확인했습니다. 이번 문서 작업에서 게임이나 테스트를 새로 실행한 결과는 아닙니다.

| 실행일 / 기준 버전 | 환경 | 검사 | 확인된 결과 | 근거 |
|---|---|---|---|---|
| 2026-10-26 17:39~17:40 KST / `631f843` | GitHub Actions Ubuntu x64 / Temurin JDK 21.0.12 / Gradle 9.2.0 | `./gradlew test` | 124개 실행, 124개 성공, 실패 0, 건너뜀 0. 테스트와 JaCoCo 보고서 생성 작업 성공 | [CI 실행](https://github.com/skytree33157/Tetris/actions/runs/40293831562), [build 작업 로그](https://github.com/skytree33157/Tetris/actions/runs/40293831562/job/121474960355) |
| 같은 실행·버전 | 같은 환경 | `./gradlew jacocoTestCoverageVerification` | 설정된 LINE 50% 기준 통과. 정확한 커버리지 비율은 공개 로그만으로 확인하지 못함 | 위 CI의 커버리지 검사 단계, [당시 빌드 설정](https://github.com/skytree33157/Tetris/blob/631f843fdb41fe141fbf3634e2f567351cfdd373/build.gradle) |

전체 기준 SHA는 `631f843fdb41fe141fbf3634e2f567351cfdd373`입니다. 현재 문서 브랜치의 기준 `992a8e4`는 README 수정만 추가된 버전이며, 위 실행을 `992a8e4`에서 새로 돌린 것으로 표시하지 않습니다.

이 CI는 Ubuntu에서 실행했습니다. Windows 11의 실제 입력·표시·패키지 실행·설정 및 점수의 완전 종료 후 복원까지 검증한 결과는 아닙니다. 테스트 실패 0은 테스트가 다루지 않은 모든 동작의 정상 여부를 뜻하지 않습니다.

## 다시 실행하는 방법

JDK 21을 준비하고 프로젝트 루트의 PowerShell에서 실행합니다. 아래 명령은 사용자가 다시 실행할 때 사용하는 안내입니다.

```powershell
git rev-parse HEAD
java -version
.\gradlew.bat --version
.\gradlew.bat test jacocoTestReport
.\gradlew.bat jacocoTestCoverageVerification
```

과거 결과를 재사용하지 않고 다시 실행해야 할 때는 다음 명령을 사용합니다.

```powershell
.\gradlew.bat test jacocoTestReport jacocoTestCoverageVerification --rerun-tasks
```

| 산출물 | 생성 위치 |
|---|---|
| JUnit HTML | `build/reports/tests/test/index.html` |
| JUnit XML·표준 출력 | `build/test-results/test/TEST-*.xml` |
| JaCoCo HTML | `build/reports/jacoco/test/html/index.html` |
| JaCoCo XML | `build/reports/jacoco/test/jacocoTestReport.xml` |

로컬의 `build/...` 경로는 GitHub에서 바로 열리는 보고서 링크가 아닙니다. 결과를 공유할 때는 검사 버전과 함께 보고서 ZIP·보관 위치 또는 CI 아티팩트 링크를 기록합니다. 현재 CI는 보고서를 생성하지만 업로드 단계가 없어, 확인한 실행의 보관 아티팩트 목록은 비어 있었습니다.

## 블록 생성 확률 검증

원문: `TeamProject_Req2.pdf` 4~5쪽. 실제 게임의 생성 코드를 사용하고 1,000회 이상 반복해야 합니다.

기존 [BlockFactoryTest](../src/test/java/blocks/core/BlockFactoryTest.java)의 검사 설정:

| 항목 | 현재 테스트 기준 |
|---|---|
| 호출 대상 | 실제 [BlockFactory](../src/main/java/blocks/core/BlockFactory.java)의 생성 API |
| 반복 횟수 | 분포 검사 1회당 100,000회 |
| 고정 난수 검사 | `generatedBlocksFollowDifficultyProbabilities`, seed `20261007`, 세 난이도 |
| 공개 API 검사 | `publicFactoryUsesRequestedDifficulty`, 세 난이도. `noArgumentFactoryPreservesNormalDistribution` |
| 오차 판정 | `abs(관측 확률 - 기대 확률) / 기대 확률 <= 0.05` |
| 출력 | I/O/T/S/Z/J/L 개수와 최대 상대오차를 각 검사 표준 출력에 기록 |

| 난이도 | I 기대 확률 | 다른 블록 각각의 기대 확률 |
|---|---:|---:|
| EASY | 12/72 ≈ 16.6667% | 10/72 ≈ 13.8889% |
| NORMAL | 1/7 ≈ 14.2857% | 1/7 ≈ 14.2857% |
| HARD | 8/68 ≈ 11.7647% | 10/68 ≈ 14.7059% |

```powershell
.\gradlew.bat test --tests "blocks.core.BlockFactoryTest" --rerun-tasks
```

위 CI에서 전체 테스트가 통과한 사실과 분포 테스트 코드의 포함은 확인했습니다. 다만 CI의 요약 로그에는 각 종류의 관측 개수·상대오차가 출력되지 않아 **정확한 수치는 아직 이 기록에 옮기지 않았습니다.** 과거 대화의 수치를 현재 버전의 결과로 재사용하지 않습니다.

아래는 다음 실행의 XML·표준 출력을 확인한 뒤 채우는 표입니다. 미기록은 테스트 실패를 의미하지 않습니다.

| 난이도 | 대상 검사 | 표본 수 | I/O/T/S/Z/J/L 실제 개수 | 최대 상대오차 | 버전·보고서 |
|---|---|---:|---|---|---|
| EASY | 고정 seed | 100,000 | 미기록 | 미기록 | 다음 실행 결과로 기록 |
| NORMAL | 고정 seed | 100,000 | 미기록 | 미기록 | 다음 실행 결과로 기록 |
| HARD | 고정 seed | 100,000 | 미기록 | 미기록 | 다음 실행 결과로 기록 |
| EASY / NORMAL / HARD | 공개 API, 난이도별 | 각 100,000 | 미기록 | 미기록 | 다음 실행 결과로 기록 |
| NORMAL | 인자 없는 API | 100,000 | 미기록 | 미기록 | 다음 실행 결과로 기록 |

## 커버리지 기록

원문: 1차 17쪽 / 2차 12쪽. 중간 50%, 기말 70% 기준입니다.

| 항목 | 확인 상태 |
|---|---|
| 현재 CI 최소 기준 | LINE 50%, `631f843`에서 통과 |
| 정확한 covered / missed / 비율 | 해당 CI 보고서가 업로드되지 않아 미확인 |
| 기말 70% 충족 | 이 로그와 50% 검사 통과만으로 판정하지 않음 |
| HTML·XML 보고서 제외 범위 | [build.gradle](../build.gradle)의 `jacocoTestReport`는 `Main.class`, `ui` 패키지를 제외 |
| 기준 검증의 범위 | `jacocoTestCoverageVerification`에는 위 제외 목록을 별도로 적용하지 않아 보고서와 범위가 다를 수 있음 |

최종 기록에는 측정 대상·제외 이유·covered/missed 줄 수·비율을 함께 적고, 직접 작성한 코드에 대한 기준 충족인지 확인합니다. UI를 제외한 보고서 수치를 제품 전체 커버리지로 표시하지 않습니다.

## 하강 속도와 레벨

[README의 속도 설명](../README.md#레벨과-하강-속도) · [관련 PR #49](https://github.com/skytree33157/Tetris/pull/49)

- 테스트: [GameStateManagerTest](../src/test/java/game/GameStateManagerTest.java)의 초기 1초, 블록·줄 기준 레벨 상승, 난이도별 감소량, Slow 상한, 최소 간격 검사.
- 결과 근거: 위 [전체 CI](#확인된-실행-결과)의 테스트 묶음에 포함됩니다. 이 문서에서는 실제 플레이로 시간을 새로 측정하지 않았습니다.
- S는 레벨 감소 대신 하강 간격을 변경합니다. [아이템 규칙](items.md#슬로우-s)과 구분해 확인합니다.

## 아이템과 NEXT

[아이템 설명으로 이동](items.md) · 관련 PR [#36](https://github.com/skytree33157/Tetris/pull/36) · [#38](https://github.com/skytree33157/Tetris/pull/38) · [#45](https://github.com/skytree33157/Tetris/pull/45) · [#47](https://github.com/skytree33157/Tetris/pull/47) · [#52](https://github.com/skytree33157/Tetris/pull/52)

| 확인 범위 | 기존 테스트 |
|---|---|
| 아이템 종류별 생성·부착 위치 | [BlockFactoryItemTest](../src/test/java/blocks/core/BlockFactoryItemTest.java), [ItemRegistryTest](../src/test/java/item/core/ItemRegistryTest.java) |
| Bomb·Bonus·Slow 모양·좌표·회전 | [아이템 타입 테스트](../src/test/java/item/types) |
| 문자·원래 타입 저장·복원 | [ItemAppearanceResolverTest](../src/test/java/ui/ItemAppearanceResolverTest.java) |
| 10줄 경계 뒤 새 NEXT, 일반 모드 제외 | [ActionControllerTest](../src/test/java/game/ActionControllerTest.java)의 아이템 공급 검사 |
| L 행 삭제·점수, W 접촉·하부 제거, Bomb 애니메이션, P/S 발동 | 같은 ActionControllerTest의 효과별 검사 |
| NEXT 패널 표시 | [BlockPreviewPanelTest](../src/test/java/game/BlockPreviewPanelTest.java) |

결과 근거는 [전체 CI](#확인된-실행-결과)입니다. 현재 테스트가 존재하는 범위를 연결한 것으로, 모든 조합의 통합 검증을 완료했다는 뜻은 아닙니다. 특히 같은 행의 복수 P/S, 효과 조합과 실제 화면은 [수동 확인 기록](#수동-확인-기록)에 별도로 남깁니다.

## 점수 계산

[점수 규칙으로 돌아가기](scoring.md) · 관련 PR [#37](https://github.com/skytree33157/Tetris/pull/37) · [#44](https://github.com/skytree33157/Tetris/pull/44)

[ScoreManagerTest](../src/test/java/score/ScoreManagerTest.java)에 하강 거리·방식·레벨·난이도, 배율 정규화 후 반올림, 콤보·퍼펙트 클리어 배율, P 개수별 고정 보너스, 초기화·잘못된 입력 검사가 있습니다. 결과 근거는 [전체 CI](#확인된-실행-결과)입니다.

점수 계산기 테스트와 보드가 발동 조건을 판단하는 통합 테스트는 구분합니다. [현재 퍼펙트 클리어 판정의 확인 사항](scoring.md#현재-구현-확인-사항)을 점수 계산 테스트의 통과만으로 해결됐다고 표시하지 않습니다.

## 색상 모드와 아이템 표시

[README의 접근성 설명](../README.md#설정과-접근성) · 관련 PR [#36](https://github.com/skytree33157/Tetris/pull/36) · [#44](https://github.com/skytree33157/Tetris/pull/44)

[BlockStyleTest](../src/test/java/blocks/style/BlockStyleTest.java), [BlockRendererTest](../src/test/java/ui/BlockRendererTest.java)는 색상·패턴과 일반 칸/아이템 칸·저장 칸의 렌더링을 검사합니다. 결과 근거는 [전체 CI](#확인된-실행-결과)입니다.

렌더러 검사는 화면 없는 이미지로 수행합니다. 실제 사람의 문자·색상 구분과 창 전체의 잘림 여부는 [화면 촬영](screenshots.md#설정과-색상-모드)·수동 확인이 필요합니다.

## 설정과 스코어보드

[설정 안내](../README.md#설정과-접근성) · [기록 안내](../README.md#점수와-스코어보드)

- 설정: [AppSettingsTest](../src/test/java/menu/settings/AppSettingsTest.java), [SettingsScreenTest](../src/test/java/menu/settings/SettingsScreenTest.java). 관련 PR [#75](https://github.com/skytree33157/Tetris/pull/75).
- 저장·종료 화면: [ScoreStorageTest](../src/test/java/storage/ScoreStorageTest.java), [GameOverScreenTest](../src/test/java/menu/GameOverScreenTest.java). 관련 PR [#64](https://github.com/skytree33157/Tetris/pull/64), [#65](https://github.com/skytree33157/Tetris/pull/65).
- 결과 근거: [전체 CI](#확인된-실행-결과). 파일·상태 검증과 별개로, 앱 완전 종료 후 설정·기록 복원은 [수동 확인 기록](#수동-확인-기록)에 남깁니다.

## 보드와 기본 조작

[게임 규칙으로 돌아가기](../README.md#기본-게임-규칙) · 관련 PR [#54](https://github.com/skytree33157/Tetris/pull/54) · [#56](https://github.com/skytree33157/Tetris/pull/56) · [#71](https://github.com/skytree33157/Tetris/pull/71)

[BoardTest](../src/test/java/board/BoardTest.java), [ActionControllerTest](../src/test/java/game/ActionControllerTest.java), [GameControllerTest](../src/test/java/game/GameControllerTest.java), [GameLoopTest](../src/test/java/game/GameLoopTest.java)에서 보드·고정·줄 삭제·조작·루프 경로를 검사합니다. 결과 근거는 [전체 CI](#확인된-실행-결과)이며, 실제 키 반복 입력·포커스·체감 반응은 별도 수동 확인 대상입니다.

## 비기능 검증 기록

| 확인 대상 | 기존 자료 | 현재 구분 |
|---|---|---|
| Java·RAM·여유 디스크 환경 검사 | [PR #76](https://github.com/skytree33157/Tetris/pull/76), 해당 PR의 CI | 확인 시점에 미병합. main의 위 124개 결과와 별도 |
| CPU 1.2GHz·최소 사양에서의 플레이 | 실제 실행 환경과 플레이 기록 필요 | 미확인 |
| 입력·하강·화면 갱신 성능 | 시간·반복 횟수·측정 환경을 정한 성능 검증 필요 | 별도 결과 미기록 |

환경 수치 확인과 플레이 성능 검증은 서로 다른 확인입니다. #76에 Java·RAM·디스크 검사 코드가 있다는 이유만으로 최소 사양에서의 성능이나 Windows 실행까지 완료로 표시하지 않습니다.

## 수동 확인 기록

아래 항목은 촬영·플레이 후 담당자가 결과를 적는 양식입니다. [촬영 안내](screenshots.md)에 파일명과 필요한 화면을 적었습니다. 사진만으로 입력 반응·지속성·시간 변화를 모두 증명할 수는 없습니다.

| 확인 항목 | 절차·기대 결과 | 결과·실행 환경·버전·근거 |
|---|---|---|
| 메뉴·일반 게임 | 방향키·Enter로 시작. 보드·NEXT·실시간 점수 확인 | 미확인 |
| 기본 조작·정지·종료 | 이동·회전·하드드롭·정지/재개·게임 중 종료 | 미확인 |
| 설정 지속성 | 크기·키·색상·난이도 변경 → 완전 종료 → 재실행 | 미확인 |
| 색상·문자 식별 | 네 색상 모드의 일반 블록·L/W/B/P/S와 NEXT 확인 | 미확인 |
| 10줄 공급·아이템 | 일반 모드 제외, 10줄 경계 뒤 새 NEXT 공급·실제 효과 확인 | 미확인 |
| P·S 개수 처리 | 같은 삭제 행의 P/S가 각각 여러 개일 때 개수별 효과 확인 | 미확인 |
| 기록 지속성 | 모드·난이도별 기록 저장·새 기록 강조 → 종료·재실행 → 재조회 | 미확인 |

## 배포 확인 기록

[PR #77](https://github.com/skytree33157/Tetris/pull/77)은 확인 시점에 열려 있고 공개 Release는 없습니다. 아래는 확정 배포물을 확인한 뒤 채우는 양식입니다.

| 항목 | 기록 |
|---|---|
| 배포 버전·소스 SHA·다운로드 링크 | 미확정 |
| 대상 OS·CPU·RAM·디스크·런타임 | 미기록 |
| ZIP 이름·SHA-256 | 미기록 |
| 아이콘·동봉 파일 확인 | 미확인 |
| 압축 해제 후 더블클릭 실행 | 미확인 |
| 명령어·추가 설정 없이 실행 | 미확인 |
| 설정·점수 저장과 재실행 | 미확인 |
| 결과 이미지·실행 기록 | 미첨부 |

## 다음 결과를 추가할 때

| 실행일 | 브랜치·전체 SHA | 환경·명령 | 테스트 성공/실패/건너뜀 | LINE covered/missed·비율·범위 | 결과 링크·오류와 조치 |
|---|---|---|---|---|---|
| 다음 실행 후 작성 | 미기록 | 미기록 | 미기록 | 미기록 | 미기록 |

구현·테스트·빌드 설정이 바뀌면 해당 버전에서 다시 검사합니다. 과거 CI의 결과는 해당 SHA의 기록으로 보존합니다.

