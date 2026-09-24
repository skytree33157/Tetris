package blocks;

import java.awt.Color;

public enum BlockType {
    I(1, Color.CYAN, BlockPattern.HORIZONTAL),
    O(2, Color.YELLOW, BlockPattern.DOT),
    T(3, Color.MAGENTA, BlockPattern.CROSS),
    S(4, Color.GREEN, BlockPattern.DIAGONAL_RIGHT),
    Z(5, Color.RED, BlockPattern.DIAGONAL_LEFT),
    J(6, Color.BLUE, BlockPattern.GRID),
    L(7, Color.GRAY, BlockPattern.VERTICAL);

    private final int value;
    private final Color baseColor;
    private final BlockPattern pattern;

    BlockType(int value, Color baseColor, BlockPattern pattern) {
        this.value = value;
        this.baseColor = baseColor;
        this.pattern = pattern;
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

    public Color getBaseColor() {
        return baseColor;
    }

    public BlockPattern getPattern() {
        return pattern;
    }

    public BlockStyle getStyle(ColorMode mode) {
        Color color = convertColor(mode);

        return new BlockStyle(
                color,
                pattern
        );
    }

    private Color convertColor(ColorMode mode) {
        return switch (mode) {
            case NORMAL -> baseColor;

            case PROTANOPIA ->
                    convertForProtanopia(baseColor);

            case DEUTERANOPIA ->
                    convertForDeuteranopia(baseColor);

            case TRITANOPIA ->
                    convertForTritanopia(baseColor);
        };
    }

    private Color convertForProtanopia(Color color) {
        // TODO:
        // 적색맹(Protanopia)에 대한 색 변환 공식 구현
        throw new UnsupportedOperationException(
                "Protanopia color conversion is not implemented yet."
        );
    }

    private Color convertForDeuteranopia(Color color) {
        // TODO:
        // 녹색맹(Deuteranopia)에 대한 색 변환 공식 구현
        throw new UnsupportedOperationException(
                "Deuteranopia color conversion is not implemented yet."
        );
    }

    private Color convertForTritanopia(Color color) {
        // TODO:
        // 청황색맹(Tritanopia)에 대한 색 변환 공식 구현
        throw new UnsupportedOperationException(
                "Tritanopia color conversion is not implemented yet."
        );
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