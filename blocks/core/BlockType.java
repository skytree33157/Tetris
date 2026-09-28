package blocks.core;

public enum BlockType {
    I(1),
    O(2),
    T(3),
    S(4),
    Z(5),
    J(6),
    L(7);

    private final int value;

    BlockType(int value) {
        this.value = value;
    }

    /*
    How to use:
    Block block = new IBlock();
    block.getType().getValue() == 1; // will return true

    This getValue() method was simply added for compatibility and extensibility with Daewoon(skytree33157)'s Board implementation.
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
