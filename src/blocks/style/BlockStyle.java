package blocks.style;

import blocks.core.BlockType;

import java.awt.Color;

public class BlockStyle {

    private static final Color NORMAL_I = new Color(0x00BCD4);
    private static final Color NORMAL_O = new Color(0xFFD600);
    private static final Color NORMAL_T = new Color(0xAB47BC);
    private static final Color NORMAL_S = new Color(0x00C853);
    private static final Color NORMAL_Z = new Color(0xFF1744);
    private static final Color NORMAL_J = new Color(0x2962FF);
    private static final Color NORMAL_L = new Color(0xFF9100);

    private static final Color PROTANOPIA_I = new Color(0x00F5F5);
    private static final Color PROTANOPIA_O = new Color(0xA3F500);
    private static final Color PROTANOPIA_T = new Color(0xC71EC7);
    private static final Color PROTANOPIA_S = new Color(0x00F5A3);
    private static final Color PROTANOPIA_Z = new Color(0x994217);
    private static final Color PROTANOPIA_J = new Color(0x469CC7);
    private static final Color PROTANOPIA_L = new Color(0x999900);

    private static final Color DEUTERANOPIA_I = new Color(0x56F58B);
    private static final Color DEUTERANOPIA_O = new Color(0xA3F500);
    private static final Color DEUTERANOPIA_T = new Color(0x8F1EC7);
    private static final Color DEUTERANOPIA_S = new Color(0x339900);
    private static final Color DEUTERANOPIA_Z = new Color(0x993657);
    private static final Color DEUTERANOPIA_J = new Color(0x0052F5);
    private static final Color DEUTERANOPIA_L = new Color(0xF525AF);

    private static final Color TRITANOPIA_I = new Color(0x00F5A3);
    private static final Color TRITANOPIA_O = new Color(0xC0F556);
    private static final Color TRITANOPIA_T = new Color(0xA300F5);
    private static final Color TRITANOPIA_S = new Color(0x429917);
    private static final Color TRITANOPIA_Z = new Color(0xC70042);
    private static final Color TRITANOPIA_J = new Color(0x995736);
    private static final Color TRITANOPIA_L = new Color(0xF5C056);

    private final Color color;
    private final BlockPattern pattern;

    private BlockStyle(Color color, BlockPattern pattern) {
        this.color = color;
        this.pattern = pattern;
    }

    public static BlockStyle of(BlockType type, ColorMode mode) {
        return new BlockStyle(getColor(type, mode), getPattern(type));
    }

    public Color getColor() {
        return color;
    }

    public BlockPattern getPattern() {
        return pattern;
    }

    private static Color getColor(BlockType type, ColorMode mode) {
        return switch (mode) {
            case NORMAL -> getNormalColor(type);
            case PROTANOPIA -> getProtanopiaColor(type);
            case DEUTERANOPIA -> getDeuteranopiaColor(type);
            case TRITANOPIA -> getTritanopiaColor(type);
        };
    }

    private static Color getNormalColor(BlockType type) {
        return switch (type) {
            case I -> NORMAL_I;
            case O -> NORMAL_O;
            case T -> NORMAL_T;
            case S -> NORMAL_S;
            case Z -> NORMAL_Z;
            case J -> NORMAL_J;
            case L -> NORMAL_L;
        };
    }

    private static Color getProtanopiaColor(BlockType type) {
        return switch (type) {
            case I -> PROTANOPIA_I;
            case O -> PROTANOPIA_O;
            case T -> PROTANOPIA_T;
            case S -> PROTANOPIA_S;
            case Z -> PROTANOPIA_Z;
            case J -> PROTANOPIA_J;
            case L -> PROTANOPIA_L;
        };
    }

    private static Color getDeuteranopiaColor(BlockType type) {
        return switch (type) {
            case I -> DEUTERANOPIA_I;
            case O -> DEUTERANOPIA_O;
            case T -> DEUTERANOPIA_T;
            case S -> DEUTERANOPIA_S;
            case Z -> DEUTERANOPIA_Z;
            case J -> DEUTERANOPIA_J;
            case L -> DEUTERANOPIA_L;
        };
    }

    private static Color getTritanopiaColor(BlockType type) {
        return switch (type) {
            case I -> TRITANOPIA_I;
            case O -> TRITANOPIA_O;
            case T -> TRITANOPIA_T;
            case S -> TRITANOPIA_S;
            case Z -> TRITANOPIA_Z;
            case J -> TRITANOPIA_J;
            case L -> TRITANOPIA_L;
        };
    }

    private static BlockPattern getPattern(BlockType type) {
        return switch (type) {
            case I -> BlockPattern.HORIZONTAL;
            case O -> BlockPattern.DOT;
            case T -> BlockPattern.CROSS;
            case S -> BlockPattern.DIAGONAL_RIGHT;
            case Z -> BlockPattern.DIAGONAL_LEFT;
            case J -> BlockPattern.GRID;
            case L -> BlockPattern.VERTICAL;
        };
    }
}
