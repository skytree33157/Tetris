# item 패키지 사용법

## ItemType.java

생성할 아이템 종류를 나타냅니다. 일반 블록 모양인 `BlockType`과 별개입니다.

```
ItemType.LINE_CLEAR
ItemType.WEIGHT
ItemType.BOMB
ItemType.BONUS
```

- `getId()` : Registry에 등록할 문자열 id를 반환합니다.

원하는 종류는 `BlockFactory.createItem(ItemType, Difficulty)`로 생성합니다.
종류까지 무작위로 고르려면 기존 `createRandomItem(Difficulty)`을 사용합니다.

## BombItem.java

1×1 폭탄 블록입니다. `BlockType.BOMB`를 사용합니다.

- `getTargetRow()`, `getTargetCol()` : 현재 폭탄의 보드 행·열을 반환합니다.

```java
BombItem bomb = BlockFactory.createBombItem();
int row = bomb.getTargetRow();
int col = bomb.getTargetCol();
```

폭발 효과는 게임 로직에서 구현합니다.

## BonusItem.java

일반 블록의 채워진 칸 하나에 P를 표시합니다. 원본 블록 타입을 그대로 사용합니다.

- `getSourceType()` : 원본 블록 타입을 반환합니다.
- `isMarkerCell(row, col)` : 현재 모양에서 P가 있는 칸인지 확인합니다.
- `getMarkerRow()`, `getMarkerCol()` : 현재 모양 배열 안의 P 위치를 반환합니다.
- `getTargetRow()`, `getTargetCol()` : 현재 위치·회전을 반영한 P의 보드 행·열을 반환합니다.

```java
BonusItem bonus = BlockFactory.createRandomBonusItem(Difficulty.HARD);
int row = bonus.getTargetRow();
int col = bonus.getTargetCol();
```

`P_CELL_VALUE = -2`는 모양 배열에서 표시 칸을 찾는 값입니다. 보드 저장 값으로 직접 사용 X.
고정 후 P를 유지하는 방법은 [ui 사용법](../ui/ui_ReadMe_for_dev.MD)을 참고합니다.
보너스 발동 판정·점수 호출은 게임 로직에서 구현합니다.

## ItemAppearance.java

아이템의 배경 타입·문자·표시 칸 판별 함수를 보관합니다.

- `baseType()` : 배경 색깔·패턴에 사용할 타입을 반환합니다.
- `symbolAt(row, col)` : 해당 칸의 표시 문자를 반환합니다. 표시 칸이 아니면 문자 없음.

## ItemRegistry.java

아이템 클래스·생성 함수·표시 정보를 등록합니다. 등록 즉시 랜덤 생성 후보에 포함됩니다.
현재 L·Weight·Bomb·Bonus를 같은 확률로 선택합니다.

```java
ItemRegistry.register("bonus", BonusItem.class, BlockFactory::createRandomBonusItem,
        item -> new ItemAppearance(item.getSourceType(), 'P', item::isMarkerCell));
```

위 Bonus 등록은 이미 되어 있습니다. 같은 id나 클래스를 다시 등록하면 오류가 발생합니다.
표시 칸 판별은 현재 모양을 조회해야 회전 후에도 문자가 따라갑니다.
등록은 생성·표시를 연결합니다. 효과를 자동으로 실행하지는 않습니다.


### 종류를 지정해서 생성

`ItemRegistry.create(ItemType, Difficulty)`는 해당 id의 생성 함수를 실행합니다.
기존 문자열 등록 방식은 유지합니다. 랜덤 생성과 지정 생성은 같은 등록 정보를 사용합니다.

새 아이템을 지정 생성에도 쓰려면 `ItemType`에 종류·id를 추가하고 같은 id로 등록합니다.
문자열 등록만 해도 기존처럼 랜덤 생성·표시는 사용할 수 있습니다.
