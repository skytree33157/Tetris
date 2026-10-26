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

JUnit 테스트 메서드는 118개이며, 파라미터별 실행을 포함한 테스트 케이스는 124개입니다.


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

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 고정 seed로 난이도별 생성 확률 검사 (EASY·NORMAL·HARD, 3회) | [BlockFactoryTest.generatedBlocksFollowDifficultyProbabilities()](../src/test/java/blocks/core/BlockFactoryTest.java#L28) |
| 공개 생성 API의 난이도별 분포 검사 (EASY·NORMAL·HARD, 3회) | [BlockFactoryTest.publicFactoryUsesRequestedDifficulty()](../src/test/java/blocks/core/BlockFactoryTest.java#L41) |
| 인자 없는 API의 NORMAL 분포 검사 | [BlockFactoryTest.noArgumentFactoryPreservesNormalDistribution()](../src/test/java/blocks/core/BlockFactoryTest.java#L51) |
| EASY에서 후보 거부 후 블록 종류 재추첨 | [BlockFactoryTest.easyReselectsCandidateAfterRejection()](../src/test/java/blocks/core/BlockFactoryTest.java#L62) |
| EASY 수락 확률의 경계값 처리 | [BlockFactoryTest.easyRejectsAtAcceptanceBoundary()](../src/test/java/blocks/core/BlockFactoryTest.java#L70) |
| HARD에서 경계값의 I형 거부 후 후보 재추첨 | [BlockFactoryTest.hardReselectsCandidateAfterRejectingIAtBoundary()](../src/test/java/blocks/core/BlockFactoryTest.java#L77) |
| 선택한 타입에 맞는 테트로미노 클래스 생성 | [BlockFactoryTest.selectedTypesProduceMatchingTetrominoClasses()](../src/test/java/blocks/core/BlockFactoryTest.java#L85) |
| 생성된 블록 간 상태 독립성 | [BlockFactoryTest.eachGenerationProducesIndependentBlockState()](../src/test/java/blocks/core/BlockFactoryTest.java#L97) |
| 난이도 인자 누락 거부 | [BlockFactoryTest.rejectsMissingDifficulty()](../src/test/java/blocks/core/BlockFactoryTest.java#L110) |
| 난수 생성기 누락 거부 | [BlockFactoryTest.rejectsMissingRandomGenerator()](../src/test/java/blocks/core/BlockFactoryTest.java#L115) |

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

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 초기 레벨·하강 간격 | [GameStateManagerTest.startsWithDefaultState()](../src/test/java/game/GameStateManagerTest.java#L20) |
| 고정 블록 수에 따른 레벨 상승 | [GameStateManagerTest.levelsUpBlocks()](../src/test/java/game/GameStateManagerTest.java#L32) |
| 삭제 줄 수에 따른 레벨 상승·누적 줄 수 | [GameStateManagerTest.levelsUpClearedLinesAndKeepsTotal()](../src/test/java/game/GameStateManagerTest.java#L48) |
| 난이도별 하강 간격 감소량 | [GameStateManagerTest.lineBasedSpeedIncreaseUsesTheSelectedDifficulty()](../src/test/java/game/GameStateManagerTest.java#L62) |
| Slow 적용과 초기 하강 간격 상한 | [GameStateManagerTest.slowItemRestoresDropSpeedButNeverAboveTheInitialSpeed()](../src/test/java/game/GameStateManagerTest.java#L78) |
| 최소 하강 간격 제한 | [GameStateManagerTest.dropSpeedDoesNotGoBelowMinimum()](../src/test/java/game/GameStateManagerTest.java#L91) |
| 게임 오버 상태 설정 | [GameStateManagerTest.canSetGameOver()](../src/test/java/game/GameStateManagerTest.java#L104) |

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

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 아이템 종류 지정·기존 생성 API 호환 | [BlockFactoryItemTest.specifiedAndLegacyApisCreateRequestedItems()](../src/test/java/blocks/core/BlockFactoryItemTest.java#L14) |
| 무작위 생성 API가 등록된 아이템 반환 | [BlockFactoryItemTest.randomItemApisReturnRegisteredItems()](../src/test/java/blocks/core/BlockFactoryItemTest.java#L30) |
| 채워진 칸에 표시 마커 한 개 부착 | [BlockFactoryItemTest.attachedItemsHaveExactlyOneMarkerOnOccupiedCells()](../src/test/java/blocks/core/BlockFactoryItemTest.java#L41) |
| 표시 위치 선택 시 빈칸 제외·모든 채워진 칸 선택 가능 | [BlockFactoryItemTest.occupiedCellSelectionNeverPicksHolesAndReachesEveryCell()](../src/test/java/blocks/core/BlockFactoryItemTest.java#L66) |
| 필수 인자 누락·빈 모양 입력 거부 | [BlockFactoryItemTest.rejectsMissingArgumentsAndEmptyShape()](../src/test/java/blocks/core/BlockFactoryItemTest.java#L80) |
| 고유 문자·원본 블록 스타일 유지 | [ItemRegistryTest.builtInSymbolsAreDistinctAndSourceStyleIsPreserved()](../src/test/java/item/core/ItemRegistryTest.java#L31) |
| 고정 seed의 무작위 생성에서 아이템 5종 균등 분포 | [ItemRegistryTest.seededRandomCreationUsesAllFiveRegisteredKindsEqually()](../src/test/java/item/core/ItemRegistryTest.java#L52) |
| 새 아이템 등록 시 난이도 전달·추첨 후보 참여 | [ItemRegistryTest.newRegistrationPassesDifficultyAndImmediatelyJoinsRandomCandidates()](../src/test/java/item/core/ItemRegistryTest.java#L64) |
| 중복·잘못된 등록 거부 후 기존 추첨 후보 유지 | [ItemRegistryTest.rejectsDuplicateAndInvalidRegistrationsWithoutChangingCandidates()](../src/test/java/item/core/ItemRegistryTest.java#L77) |
| 등록된 Slow 아이템 생성·표시 정보 조회 | [ItemRegistryTest.slowIsAvailableAsRegisteredItem()](../src/test/java/item/core/ItemRegistryTest.java#L94) |
| Bomb의 1×1 모양·현재 위치에 따른 폭발 대상 | [BombItemTest.bombIsOneCellAndTargetFollowsPosition()](../src/test/java/item/types/BombItemTest.java#L9) |
| 표시 칸·원본 모양·타입의 네 번 회전 후 유지 | [BonusItemTest.markerPreservesSourceShapeAndTypeThroughFourRotations()](../src/test/java/item/types/BonusItemTest.java#L10) |
| 빈칸·모양 범위 밖 표시 위치 거부 | [BonusItemTest.rejectsEmptyAndOutOfBoundsMarkers()](../src/test/java/item/types/BonusItemTest.java#L35) |
| 표시 칸·원본 모양·타입의 네 번 회전 후 유지 | [SlowItemTest.markerPreservesSourceShapeAndTypeThroughFourRotations()](../src/test/java/item/types/SlowItemTest.java#L10) |
| 빈칸·모양 범위 밖 표시 위치 거부 | [SlowItemTest.rejectsEmptyAndOutOfBoundsMarkers()](../src/test/java/item/types/SlowItemTest.java#L35) |
| 고정 후 기존 NEXT가 현재 블록으로 전환 | [ActionControllerTest.nextBlockBecomesCurrentAfterLock()](../src/test/java/game/ActionControllerTest.java#L80) |
| 애니메이션 미설정 시 즉시 줄 삭제 | [ActionControllerTest.clearsImmediatelyWithoutAnimation()](../src/test/java/game/ActionControllerTest.java#L95) |
| 줄 삭제 애니메이션 중 대상 행 유지 | [ActionControllerTest.keepsRowsDuringAnimation()](../src/test/java/game/ActionControllerTest.java#L106) |
| 애니메이션 중 조작 입력 제한 | [ActionControllerTest.ignoresInputDuringAnimation()](../src/test/java/game/ActionControllerTest.java#L117) |
| 애니메이션 종료 후 줄 삭제 완료 | [ActionControllerTest.completesLineClearAfterAnimation()](../src/test/java/game/ActionControllerTest.java#L126) |
| Bomb의 제거 전 폭발 표시 | [ActionControllerTest.animatesBombClearedCellsBeforeRemovingThem()](../src/test/java/game/ActionControllerTest.java#L141) |
| 아이템 모드에서 10줄 삭제 후 아이템 공급 | [ActionControllerTest.itemModeSpawnsItemAfterTenLines()](../src/test/java/game/ActionControllerTest.java#L164) |
| 일반 모드에서 아이템 공급 제외 | [ActionControllerTest.normalModeNeverSpawnsItem()](../src/test/java/game/ActionControllerTest.java#L176) |
| 누적 삭제 줄 수가 정확히 10일 때 아이템 공급 | [ActionControllerTest.itemModeSpawnsAnItemWhenLineCountReachesTenExactly()](../src/test/java/game/ActionControllerTest.java#L188) |
| 보드 양쪽 벽·고정 블록에서 이동 제한 | [ActionControllerTest.movementStopsAtBothBoardEdgesAndAtFixedBlocks()](../src/test/java/game/ActionControllerTest.java#L207) |
| 아래쪽 충돌 시 장애물 위에 블록 고정 | [ActionControllerTest.downwardCollisionLocksTheBlockOnTopOfTheObstacle()](../src/test/java/game/ActionControllerTest.java#L240) |
| 하드드롭 고정 위치·실제 이동 거리 | [ActionControllerTest.hardDropLocksAtTheLowestValidPositionAndReturnsDistance()](../src/test/java/game/ActionControllerTest.java#L265) |
| 상단 여유 공간에 고정 시 게임 오버 | [ActionControllerTest.lockingIntoHiddenRowsCausesGameOver()](../src/test/java/game/ActionControllerTest.java#L285) |
| L 표시 행 삭제·줄 삭제 점수 | [ActionControllerTest.lineClearItemClearsItsMarkedRowAndAwardsLinePoints()](../src/test/java/game/ActionControllerTest.java#L302) |
| 행 삭제 시 Bonus·Slow 효과의 한 번 적용 | [ActionControllerTest.bonusAndSlowMarkersApplyTheirEffectsOnceWhenTheirLineClears()](../src/test/java/game/ActionControllerTest.java#L327) |
| Weight 접촉 후 아래 칸 제거·좌우 이동 제한 | [ActionControllerTest.weightItemClearsTheRowBelowWhenItLandsAndCannotSlideAfterLock()](../src/test/java/game/ActionControllerTest.java#L359) |
| NEXT 패널의 크기·검정 배경 초기화 | [BlockPreviewPanelTest.constructorUsesFixedPreviewSizeAndBlackBackground()](../src/test/java/game/BlockPreviewPanelTest.java#L33) |
| NEXT 블록이 없을 때 배경만 표시 | [BlockPreviewPanelTest.emptyPreviewPaintsOnlyTheBackground()](../src/test/java/game/BlockPreviewPanelTest.java#L41) |
| NEXT 블록 중앙 표시·비우기 | [BlockPreviewPanelTest.setBlockCentersAndRendersBlockThenClearsIt()](../src/test/java/game/BlockPreviewPanelTest.java#L49) |

## 점수 계산

**ScoreManagerTest 9개 통과.** 하강 거리·방식·레벨·난이도, 배율 정규화·반올림, 콤보·퍼펙트 클리어 배율, P 개수별 고정 보너스, 초기화·잘못된 입력을 검사합니다.

[테스트 코드](../src/test/java/score/ScoreManagerTest.java) · [점수 규칙](scoring.md) · [PR #37](https://github.com/skytree33157/Tetris/pull/37)

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 하강 거리·방식·레벨·난이도별 점수 | [ScoreManagerTest.dropPointsFollowDistanceTypeLevelAndDifficulty()](../src/test/java/score/ScoreManagerTest.java#L9) |
| 줄 삭제 기본 점수·레벨 배율 정규화 | [ScoreManagerTest.firstClearUsesBasePointsAndLevelNormalization()](../src/test/java/score/ScoreManagerTest.java#L24) |
| 난이도 배율 적용 순서·반올림 | [ScoreManagerTest.difficultyIsAppliedAfterNormalizationAndBeforeRounding()](../src/test/java/score/ScoreManagerTest.java#L37) |
| 콤보·퍼펙트 클리어·난이도 배율의 최댓값 | [ScoreManagerTest.comboPerfectClearAndDifficultyReachDocumentedMaximum()](../src/test/java/score/ScoreManagerTest.java#L44) |
| 줄 삭제 실패 시 콤보 초기화·다른 점수와의 독립성 | [ScoreManagerTest.emptyClearResetsComboButBonusAndDropsDoNot()](../src/test/java/score/ScoreManagerTest.java#L56) |
| P 개수별 고정 보너스·난이도 적용 | [ScoreManagerTest.bonusIsFixedPerItemAndUsesDifficultyOnly()](../src/test/java/score/ScoreManagerTest.java#L70) |
| 점수·콤보 초기화 후 난이도 유지 | [ScoreManagerTest.resetClearsScoreAndComboWithoutLosingDifficulty()](../src/test/java/score/ScoreManagerTest.java#L80) |
| 기본 생성자의 NORMAL 정책 호환 | [ScoreManagerTest.defaultConstructorKeepsNormalPolicyAcrossMixedEvents()](../src/test/java/score/ScoreManagerTest.java#L88) |
| 잘못된 개수·레벨 입력 시 점수·콤보 유지 | [ScoreManagerTest.invalidCountsAndLevelsDoNotChangeScoreOrCombo()](../src/test/java/score/ScoreManagerTest.java#L99) |

## 색상 모드와 아이템 표시

**BlockStyleTest 2개, BlockRendererTest 6개, ItemAppearanceResolverTest 4개 통과.** 색·패턴, 일반 칸과 아이템 칸, 저장 전후의 렌더링·문자 복원을 검사합니다. 

[스타일 테스트](../src/test/java/blocks/style/BlockStyleTest.java) · [렌더러 테스트](../src/test/java/ui/BlockRendererTest.java) · [저장·복원 테스트](../src/test/java/ui/ItemAppearanceResolverTest.java)

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 색상 모드별 7종 블록의 색·패턴 구분 | [BlockStyleTest.sevenBlocksHaveDistinctColorsAndPatternsInEveryMode()](../src/test/java/blocks/style/BlockStyleTest.java#L11) |
| 색상 모드별 특수 아이템 전용 스타일 유지 | [BlockStyleTest.specialItemsKeepDedicatedStylesAcrossModes()](../src/test/java/blocks/style/BlockStyleTest.java#L29) |
| 색상 모드·셀 크기별 일반 칸의 기존 렌더링 유지 | [BlockRendererTest.ordinaryCellsKeepLegacyRenderingInEveryModeAndSize()](../src/test/java/ui/BlockRendererTest.java#L46) |
| 표시 칸의 문자와 나머지 칸의 원본 배경 유지 | [BlockRendererTest.attachedMarkersChangeOnlyMarkedCellAndKeepUnmarkedBackground()](../src/test/java/ui/BlockRendererTest.java#L58) |
| 낙하 블록·보드 저장 칸의 외형 일치 | [BlockRendererTest.objectAndStoredCellMatchAndPreserveLegacyBorder()](../src/test/java/ui/BlockRendererTest.java#L71) |
| 잘못된 인자·빈칸·범위 밖 칸은 그리지 않음 | [BlockRendererTest.invalidArgumentsEmptyAndOutOfBoundsCellsDrawNothing()](../src/test/java/ui/BlockRendererTest.java#L90) |
| 보드 좌표에 맞춰 블록 표시·보드 위쪽 칸 제외 | [BlockRendererTest.wholeBlockUsesBoardCoordinatesAndSkipsCellsAboveBoard()](../src/test/java/ui/BlockRendererTest.java#L116) |
| 렌더링 후 호출자의 Graphics 설정 유지 | [BlockRendererTest.renderingPreservesCallerGraphicsState()](../src/test/java/ui/BlockRendererTest.java#L127) |
| 회전한 아이템의 타입·문자 저장과 복원 | [ItemAppearanceResolverTest.occupiedCellsRoundTripWithoutLosingTypeOrSymbolAfterRotation()](../src/test/java/ui/ItemAppearanceResolverTest.java#L13) |
| 일반 칸의 기존 저장 값·부착 마커의 원본 스타일 유지 | [ItemAppearanceResolverTest.normalCellsKeepExistingValuesAndAttachedMarkersKeepSourceStyle()](../src/test/java/ui/ItemAppearanceResolverTest.java#L27) |
| Slow 모양 마커를 보드 저장 값으로 변환·문자 복원 | [ItemAppearanceResolverTest.slowMarkerSurvivesStoredCellConversionWithoutUsingShapeMarkerValue()](../src/test/java/ui/ItemAppearanceResolverTest.java#L41) |
| 기존 마커 저장 값 호환·잘못된 인코딩 값 거부 | [ItemAppearanceResolverTest.legacyMarkerIsReadableAndInvalidPackedValuesAreRejected()](../src/test/java/ui/ItemAppearanceResolverTest.java#L50) |

## 설정과 스코어보드

| 확인 범위 | 테스트 코드 | 실제 결과 |
|---|---|---|
| 설정·저장 | [AppSettingsTest](../src/test/java/menu/settings/AppSettingsTest.java), [SettingsScreenTest](../src/test/java/menu/settings/SettingsScreenTest.java) | 각각 5·1개 통과 |
| 기록 파일·종료 화면 | [ScoreStorageTest](../src/test/java/storage/ScoreStorageTest.java), [GameOverScreenTest](../src/test/java/menu/GameOverScreenTest.java) | 각각 6개 통과 |

[설정 안내](../README.md#설정과-접근성) · [기록 안내](../README.md#점수와-스코어보드) · 관련 PR [#65](https://github.com/skytree33157/Tetris/pull/65), [#75](https://github.com/skytree33157/Tetris/pull/75)

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 기본 설정 복원 | [AppSettingsTest.resetToDefaultsRestoresDefaultValues()](../src/test/java/menu/settings/AppSettingsTest.java#L43) |
| 변경한 설정 저장·불러오기 | [AppSettingsTest.saveAndLoadKeepsAllSettings()](../src/test/java/menu/settings/AppSettingsTest.java#L61) |
| 각 난이도의 저장·복원 | [AppSettingsTest.everyDifficultyCanBeSavedAndLoaded()](../src/test/java/menu/settings/AppSettingsTest.java#L80) |
| 저장 파일에 난이도가 없을 때 기본값 유지 | [AppSettingsTest.missingDifficultyKeyKeepsDefault()](../src/test/java/menu/settings/AppSettingsTest.java#L91) |
| 잘못된 설정값을 읽으면 기본값 복원 | [AppSettingsTest.brokenValueFallsBackToDefaults()](../src/test/java/menu/settings/AppSettingsTest.java#L101) |
| 난이도 선택 순환 | [SettingsScreenTest.difficultySelectionCyclesForwardAndBackwardAndWraps()](../src/test/java/menu/settings/SettingsScreenTest.java#L10) |
| 한글 이름과 기록 정보 저장·복원 | [ScoreStorageTest.savesAndLoadsKoreanRecord()](../src/test/java/storage/ScoreStorageTest.java#L20) |
| 여러 점수 기록 저장·복원 | [ScoreStorageTest.savesAndLoadsMultipleRecords()](../src/test/java/storage/ScoreStorageTest.java#L43) |
| 저장된 점수 기록 초기화 | [ScoreStorageTest.clearsSavedRecords()](../src/test/java/storage/ScoreStorageTest.java#L69) |
| 기존 기록을 유지하며 새 기록 추가 | [ScoreStorageTest.addsRecordWithoutLosingExistingRecords()](../src/test/java/storage/ScoreStorageTest.java#L87) |
| 기록 파일이 없을 때 빈 목록 반환 | [ScoreStorageTest.loadsEmptyListWhenFileDoesNotExist()](../src/test/java/storage/ScoreStorageTest.java#L120) |
| 잘못된 기록은 건너뛰고 유효한 기록 복원 | [ScoreStorageTest.skipsInvalidRecordsAndLoadsValidRecords()](../src/test/java/storage/ScoreStorageTest.java#L134) |
| 기록이 10개 미만일 때 새 기록 진입 허용 | [GameOverScreenTest.acceptsRecordWhenFewerThanTenRecordsExist()](../src/test/java/menu/GameOverScreenTest.java#L24) |
| 10위 점수에 따른 기록 진입 판정 (99·100·101점, 3회) | [GameOverScreenTest.checksScoreAgainstTenthPlace()](../src/test/java/menu/GameOverScreenTest.java#L43) |
| 난이도·모드별 조회와 점수 내림차순 정렬 | [GameOverScreenTest.selectsMatchingCategoryAndSortsByScoreDescending()](../src/test/java/menu/GameOverScreenTest.java#L60) |
| 해당 카테고리 상위 10위 유지·다른 카테고리 보존 | [GameOverScreenTest.keepsOnlyTopTenInTheSelectedCategoryAndPreservesOtherCategories()](../src/test/java/menu/GameOverScreenTest.java#L83) |

## 보드와 기본 조작

**BoardTest 8개, GameControllerTest 9개, GameLoopTest 4개, GamePanelTest 4개 통과.** 보드·줄 삭제·조작·루프·패널 경로를 검사합니다. 고정·충돌 경로는 위 ActionControllerTest에서도 검사합니다.

[보드 테스트](../src/test/java/board/BoardTest.java) · [입력 테스트](../src/test/java/game/GameControllerTest.java) · [루프 테스트](../src/test/java/game/GameLoopTest.java) · [패널 테스트](../src/test/java/game/GamePanelTest.java)

이외 [TetrominoTest](../src/test/java/blocks/core/TetrominoTest.java) 3개, [AppStateManagerTest](../src/test/java/app/AppStateManagerTest.java) 2개도 통과했습니다. 

| 확인할 내용 | 테스트 메서드 |
|---|---|
| 20×10 보드 생성·초기화 | [BoardTest.createsAnEmptyTwentyByTenBoard()](../src/test/java/board/BoardTest.java#L22) |
| 벽·바닥·고정 블록 충돌 | [BoardTest.validatesWallsFloorAndOccupiedCells()](../src/test/java/board/BoardTest.java#L36) |
| 완성 행이 없을 때 빈 목록 반환 | [BoardTest.findFullRowsReturnsEmptyWhenNoFullRow()](../src/test/java/board/BoardTest.java#L59) |
| 보드를 변경하지 않고 완성 행만 탐색 | [BoardTest.findFullRowsFindsOnlyFullRowsWithoutClearing()](../src/test/java/board/BoardTest.java#L65) |
| 지정한 행 삭제·위쪽 행 하강 | [BoardTest.clearRowsRemovesGivenRowsAndShiftsDown()](../src/test/java/board/BoardTest.java#L73) |
| 완성 행 삭제·위쪽 행 하강 | [BoardTest.clearsOneFullLineAndShiftsRowsAboveItDown()](../src/test/java/board/BoardTest.java#L89) |
| 행 삭제마다 아이템 마커 집계 초기화 | [BoardTest.clearRowsResetsItemMarkerCounts()](../src/test/java/board/BoardTest.java#L100) |
| 폭발로 비워진 행 압축·위쪽 행 하강 | [BoardTest.bombAreaClearCollapsesRowsEmptiedByTheBlast()](../src/test/java/board/BoardTest.java#L110) |
| ESC 설정 진입 중 일시정지·설정 종료 후 재개 | [GameControllerTest.escPausesWhileSettingsOpenAndResumesAfter()](../src/test/java/game/GameControllerTest.java#L88) |
| 설정 중 게임 종료 시 루프 재개 방지 | [GameControllerTest.doesNotResumeWhenShutdownDuringSettings()](../src/test/java/game/GameControllerTest.java#L99) |
| 일시정지 중 설정을 닫아도 정지 상태 유지 | [GameControllerTest.escWhilePausedKeepsPausedAfterSettings()](../src/test/java/game/GameControllerTest.java#L113) |
| P 입력으로 일시정지·재개 | [GameControllerTest.pTogglesPause()](../src/test/java/game/GameControllerTest.java#L123) |
| 일시정지 중 이동 입력 무시 | [GameControllerTest.movementIgnoredWhilePaused()](../src/test/java/game/GameControllerTest.java#L137) |
| 키 입력 즉시 반영·250ms 응답 제한 | [GameControllerTest.movementKeyTakesEffectImmediatelyWithinTheInputResponseBudget()](../src/test/java/game/GameControllerTest.java#L147) |
| 하드드롭 키 입력 시 실제 이동 거리만큼 점수 지급 | [GameControllerTest.hardDropKeyAddsPointsForTheActualDropDistance()](../src/test/java/game/GameControllerTest.java#L158) |
| 게임 오버 후 ESC·P 입력 무시 | [GameControllerTest.escAndPIgnoredAfterGameOver()](../src/test/java/game/GameControllerTest.java#L168) |
| ESC 길게 누르기로 종료 동작 호출 | [GameControllerTest.holdingEscapeExitsInsteadOfOpeningSettings()](../src/test/java/game/GameControllerTest.java#L180) |
| 게임 루프 초기 상태·일시정지 전환 | [GameLoopTest.startsUnpausedAndCanTogglePause()](../src/test/java/game/GameLoopTest.java#L29) |
| 스레드 인터럽트 시 게임 루프 종료 | [GameLoopTest.runStopsWhenItsThreadIsInterrupted()](../src/test/java/game/GameLoopTest.java#L41) |
| 설정된 1초 주기 자동 하강 | [GameLoopTest.automaticallyDropsAtTheConfiguredOneSecondInterval()](../src/test/java/game/GameLoopTest.java#L54) |
| 일시정지 시 자동 하강 중지·재개 후 하강 | [GameLoopTest.pauseStopsAutomaticDropAndResumeRestartsIt()](../src/test/java/game/GameLoopTest.java#L74) |
| 게임 패널의 보드 크기·검정 배경 초기화 | [GamePanelTest.constructorUsesBoardDimensionsAndBlackBackground()](../src/test/java/game/GamePanelTest.java#L40) |
| 고정 칸·현재 블록·격자 표시 | [GamePanelTest.paintDrawsFixedCellsCurrentBlockAndGridLines()](../src/test/java/game/GamePanelTest.java#L49) |
| 삭제 대상 행의 점멸·일반 표시 복귀 | [GamePanelTest.clearingRowsFlashAndReturnToNormalRendering()](../src/test/java/game/GamePanelTest.java#L68) |
| 셀 크기 변경 시 패널 크기 갱신 | [GamePanelTest.refreshCellSizeUpdatesPreferredSize()](../src/test/java/game/GamePanelTest.java#L91) |
| 7종 블록의 모양·채워진 칸 확인 | [TetrominoTest.sevenShapesHaveExpectedOccupiedCells()](../src/test/java/blocks/core/TetrominoTest.java#L10) |
| 7종 블록이 네 번 회전한 뒤 원래 모양으로 복귀 | [TetrominoTest.everyTetrominoReturnsToItsOriginalShapeAfterFourRotations()](../src/test/java/blocks/core/TetrominoTest.java#L30) |
| 기존 타입 값 호환·알 수 없는 값 거부 | [TetrominoTest.typeValuesKeepBoardCompatibilityAndRejectUnknownValues()](../src/test/java/blocks/core/TetrominoTest.java#L44) |
| 지정한 초기 화면 상태 유지 | [AppStateManagerTest.startsWithGivenState()](../src/test/java/app/AppStateManagerTest.java#L11) |
| 메뉴 상태 간 전환 | [AppStateManagerTest.transitionsBetweenMenuStates()](../src/test/java/app/AppStateManagerTest.java#L17) |

## 직접 실행하는 방법

JDK 21을 준비하고 프로젝트 루트의 PowerShell에서 실행합니다.

```powershell
.\gradlew.bat test jacocoTestReport jacocoTestCoverageVerification --rerun-tasks
```

- 개별 결과: `build/reports/tests/test/index.html`
- 커버리지: `build/reports/jacoco/test/html/index.html`
- 원본 XML·확률 표준 출력: `build/test-results/test/TEST-*.xml`

전체 보고서는 위 로컬 경로에 생성되며, 코드·테스트를 바꾼 뒤에는 다시 실행한 버전과 결과로 갱신합니다.

