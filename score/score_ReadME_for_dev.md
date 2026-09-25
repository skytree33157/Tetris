# score_ReadME_for_dev

## DropType.java

블록의 하강 방식을 나타냅니다.

```
DropType.AUTO
DropType.SOFT
DropType.HARD
```

## ScoreManager.java

### 주요 메서드

- `addDropScore(int distance, DropType type, int level)`
    - 블록이 실제로 아래로 이동한 거리만큼 점수를 추가합니다.
    - `AUTO` : 현재 Level에 따라 1칸당 1~5점
    - `SOFT` : 1칸당 1점
    - `HARD` : 1칸당 2점
    - `level` : 1~10

```
scoreManager.addDropScore(1, DropType.AUTO, level);
scoreManager.addDropScore(1, DropType.SOFT, level);
scoreManager.addDropScore(8, DropType.HARD, level);
```

AUTO 하강 점수는 게임 속도가 빨라질수록 증가합니다.

```
Level 1     : x1
Level 2~3   : x2
Level 4~5   : x3
Level 6~7   : x4
Level 8~10  : x5
```

SOFT와 HARD는 Level의 영향을 받지 않습니다.


- `addLineClearScore(int linesCleared, int level, boolean perfectClear)`
    - 줄 삭제 점수와 추가 배율을 계산해서 점수를 추가합니다.
    - `linesCleared` : 0~4
    - `level` : 1~10
    - `perfectClear` : 퍼펙트 클리어 여부

```
scoreManager.addLineClearScore(linesCleared, level, perfectClear);
```

⚠️⚠️ 줄을 삭제하지 않은 경우에도 `linesCleared = 0`으로 호출해야 Combo가 초기화됩니다!! ⚠️⚠️


- `getScore()`
    - 현재 누적 점수를 반환합니다.
    - UI에서는 이 값을 이용해 현재 점수를 표시할 수 있습니다.

```
int score = scoreManager.getScore();
```


- `getComboCount()`
    - 현재 연속 줄 삭제 횟수를 반환합니다.

```
int combo = scoreManager.getComboCount();
```


- `reset()`
    - 현재 점수와 Combo를 초기화합니다.

```
scoreManager.reset();
```

---

---

# 구현

## 하강 점수

AUTO는 게임 속도가 빨라질수록 추가 점수를 받습니다.

- Level 1 : distance × 1
- Level 2~3 : distance × 2
- Level 4~5 : distance × 3
- Level 6~7 : distance × 4
- Level 8~10 : distance × 5

수동 하강은 Level과 관계없이 계산합니다.

- SOFT : distance × 1
- HARD : distance × 2


## 줄 삭제 기본 점수

- 1줄 : 100점
- 2줄 : 300점
- 3줄 : 500점
- 4줄 : 800점


## 줄 삭제 Level 배율

줄 삭제 점수에도 현재 게임 Level에 따른 배율을 적용합니다.

- Level 1~2 : ×1
- Level 3~4 : ×2
- Level 5~6 : ×3
- Level 7~8 : ×4
- Level 9~10 : ×5


## Combo

연속으로 줄 삭제에 성공하면 Combo가 증가합니다.

- 첫 줄 삭제 : ×1.0
- 연속 삭제마다 : +0.1
- 최대 : ×2.0
- 줄 삭제 실패 시 Combo 초기화


## Perfect Clear

줄 삭제 후 보드가 완전히 비어 있으면 Perfect Clear로 처리합니다.

- Perfect Clear : ×2


## 최종 배율 계산

줄 삭제 점수에서는 Level, Combo, Perfect Clear 배율을 먼저 곱해 `rawMultiplier`를 계산합니다.

```
Level ×4
Combo ×1.5
Perfect Clear ×2

rawMultiplier = 4 × 1.5 × 2 = 12
```

현재 최대 `rawMultiplier`는 20입니다.

```
MAX : Level ×5
MAX : Combo ×2
MAX : Perfect Clear ×2

MAX : 5 × 2 × 2 = 20
```

`rawMultiplier`의 1~20 범위를 최종 배율 1~10 범위로 선형 변환해서 사용합니다.

최종 배율은 최대 ×10을 넘지 않습니다.


## 전체 점수 구조

하강 점수

- AUTO : `distance × Level 하강 배율`
- SOFT : `distance × 1`
- HARD : `distance × 2`

줄 삭제 점수

- 줄 수에 따른 기본 점수
- Level 배율
- Combo 배율
- Perfect Clear 배율
- 최종 배율 최대 ×10

`ScoreManager`는 점수 계산만 담당합니다.

블록 이동, 줄 삭제 판정, Perfect Clear 판정, 화면 출력은 다른 클래스에서 처리합니다.