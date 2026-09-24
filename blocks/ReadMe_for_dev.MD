# blocks 패키지 사용법

다른 패키지에서는 `blocks` 폴더에 있는 공개 클래스와 enum을 사용합니다.

## BlockFactory.java

- `BlockFactory.createRandomBlock()`
  - 무작위 블록 하나를 생성합니다.

```java
Block block = BlockFactory.createRandomBlock();
```

## Block.java

- `getType()` : 블록 종류를 반환합니다.
- `getShape()` : 현재 블록 모양을 2차원 배열로 반환합니다.
- `getWidth()`, `getHeight()` : 현재 블록의 너비와 높이를 반환합니다.
- `getX()`, `getY()` : 현재 위치를 반환합니다.
- `setX(int)`, `setY(int)` : 현재 위치를 변경합니다.

```java
BlockType type = block.getType();
int[][] shape = block.getShape();

block.setX(4);
block.setY(0);
```

## BlockType.java

- `getValue()` : 블록 종류를 보드 저장용 숫자 `1~7`로 변환합니다.
- `BlockType.fromValue(int)` : 보드의 숫자 `1~7`을 `BlockType`으로 변환합니다.

```java
int boardValue = block.getType().getValue();
BlockType type = BlockType.fromValue(boardValue);
```

## ColorMode.java

현재 선택된 색상 모드를 나타냅니다.

```java
ColorMode.NORMAL
ColorMode.PROTANOPIA
ColorMode.DEUTERANOPIA
ColorMode.TRITANOPIA
```

## BlockStyle.java

- `BlockStyle.of(BlockType, ColorMode)` : 블록 종류와 색상 모드에 맞는 스타일을 반환합니다.
- `getColor()` : 렌더링에 사용할 색상을 반환합니다.
- `getPattern()` : 렌더링에 사용할 패턴을 반환합니다.

```java
BlockType type = BlockType.fromValue(cellValue);
BlockStyle style = BlockStyle.of(type, currentColorMode);

Color color = style.getColor();
BlockPattern pattern = style.getPattern();
```

떨어지는 블록은 보드 값으로 변환하지 않고 바로 스타일을 가져올 수 있습니다.

```java
BlockStyle style = BlockStyle.of(block.getType(), currentColorMode);
```

`color`와 `pattern`을 실제로 그리는 작업은 렌더링 코드에서 처리합니다.
