package blocks.core;

public enum BlockType {
    I(1),
    O(2),
    T(3),
    S(4),
    Z(5),
    J(6),
    L(7),
    LINE_CLEAR(8), // 줄 삭제 아이템의 객체 타입. 원본 스타일은 아이템에서 별도로 조회한다.
    WEIGHT(9),     // 무게추 셀의 보드 저장 값과 전용 스타일 타입
    BOMB(10);      // 1×1 폭탄의 전용 스타일 타입

    private final int value;

    BlockType(int value) {
        this.value = value;
    }

    /*
    사용 예시:
    Block block = new IBlock();
    block.getType().getValue() == 1; // true를 반환한다

    getValue()는 대운이의 Board 구현과 호환되도록 블록 타입을 보드 저장 값으로 변환한다.
    ㄴ public void setBlock(int row, int col, int value) {...}
    ㄴㄴ ex) board.setBlock(row, col, block.getType().getValue());
    */
    public int getValue() {
        return value;
    }

    public static BlockType fromValue(int value) {
        for (BlockType type : BlockType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Invalid BlockType value: " + value
        );
    }
}
