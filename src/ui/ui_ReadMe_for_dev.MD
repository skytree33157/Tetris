# ui 패키지 사용법

## BlockRenderer.java

일반 블록과 아이템을 그립니다. 색상 모드에 맞는 색깔·패턴을 사용합니다.

- L·Bonus : 원본 블록 색깔·패턴 유지. 표시 칸 하나에 L 또는 P 표시.
- Weight : 은회색·BRICK 패턴. 채워진 칸마다 W 표시.
- Bomb : 어두운 배경·둥근 몸체·심지·B 표시.

게임 화면과 NEXT는 `drawBlockCell()`을 같이 사용합니다.

## ItemAppearanceResolver.java

블록 객체 또는 보드 값을 한 칸의 배경 타입·문자로 변환합니다.

- `resolve(block, row, col)` : 블록 모양의 해당 칸에서 표시 정보를 가져옵니다.
- `toBoardCell(block, row, col)` : 원본 타입·문자를 보드에 저장할 정수 값으로 변환합니다. 빈칸은 0.
- `fromBoardCell(value)` : 저장된 값에서 원본 타입·문자를 복원합니다.
- `CellAppearance.type()` : 배경 타입을 반환합니다.
- `CellAppearance.symbol()` : 표시 문자를 반환합니다. 문자가 없으면 `\0`.

### 고정 후 표시 유지

기존 타입만 저장하면 Bonus의 P가 사라집니다. 고정 로직에서 각 칸을 변환해 저장합니다.

```java
storedShape[row][col] = ItemAppearanceResolver.toBoardCell(block, row, col);
```

아이템 표시가 저장된 값은 `BlockType.fromValue()`에 직접 전달 X.
일반 셀도 아래 API로 읽을 수 있습니다. 빈칸 0은 먼저 제외합니다.

```java
ItemAppearanceResolver.CellAppearance cell = ItemAppearanceResolver.fromBoardCell(value);
boolean isBonus = cell.symbol() == 'P';
```

저장되는 것은 타입·문자입니다. 발동 조건·효과는 저장하거나 실행하지 않습니다.
이번 작업에는 보드 고정 연결·폭발·보너스 발동·점수 호출을 포함하지 않았습니다.
