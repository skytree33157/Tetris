package blocks;

import blocks.blocks.BlockPattern;

import java.awt.Color;

public class BlockStyle {

    private final Color color;
    private final BlockPattern pattern;

    public BlockStyle(Color color, BlockPattern pattern) {
        this.color = color;
        this.pattern = pattern;
    }

    public Color getColor() {
        return color;
    }

    public BlockPattern getPattern() {
        return pattern;
    }
}