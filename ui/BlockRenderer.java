package ui;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.style.BlockPattern;
import blocks.style.BlockStyle;
import blocks.style.ColorMode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class BlockRenderer {

    private static final int HORIZONTAL_SPACING = 4;
    private static final int VERTICAL_SPACING = 4;
    private static final int DOT_SPACING = 5;
    private static final int DOT_SIZE = 2;
    private static final int CROSS_SPACING = 7;
    private static final int DIAGONAL_RIGHT_SPACING = 6;
    private static final int DIAGONAL_LEFT_SPACING = 6;
    private static final int GRID_SPACING = 6;

    private BlockRenderer() {}

    public static void drawCell(Graphics2D g, int x, int y, int size, BlockType type, ColorMode mode) {
        if (g == null || type == null || mode == null || size <= 0) return;

        BlockStyle style = BlockStyle.of(type, mode);
        Color fillColor = style.getColor();

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setColor(fillColor);
            g2d.fillRect(x, y, size, size);

            Graphics2D patternGraphics = (Graphics2D) g2d.create();
            try {
                patternGraphics.clipRect(x + 1, y + 1, Math.max(1, size - 2), Math.max(1, size - 2));
                patternGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                patternGraphics.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
                patternGraphics.setColor(getPatternColor(fillColor));
                drawPattern(patternGraphics, x, y, size, style.getPattern());
            } finally {
                patternGraphics.dispose();
            }

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(size >= 30 ? 2.0f : 1.0f));
            g2d.drawRect(x, y, size - 1, size - 1);
        } finally {
            g2d.dispose();
        }
    }

    public static void drawBlock(Graphics2D g, Block block, int boardX, int boardY, int cellSize, ColorMode mode) {
        if (g == null || block == null || mode == null || cellSize <= 0) return;

        int[][] shape = block.getShape();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 0) continue;

                int boardRow = block.getY() + row;
                int boardCol = block.getX() + col;

                if (boardRow < 0) continue;

                int pixelX = boardX + boardCol * cellSize;
                int pixelY = boardY + boardRow * cellSize;

                drawCell(g, pixelX, pixelY, cellSize, block.getType(), mode);
            }
        }
    }

    public static void drawBoardCell(Graphics2D g, int x, int y, int size, int cellValue, ColorMode mode) {
        if (cellValue == 0) return;

        BlockType type = BlockType.fromValue(cellValue);

        drawCell(g, x, y, size, type, mode);
    }

    private static void drawPattern(Graphics2D g, int x, int y, int size, BlockPattern pattern) {
        switch (pattern) {
            case HORIZONTAL -> drawHorizontal(g, x, y, size);
            case DOT -> drawDot(g, x, y, size);
            case CROSS -> drawCross(g, x, y, size);
            case DIAGONAL_RIGHT -> drawDiagonalRight(g, x, y, size);
            case DIAGONAL_LEFT -> drawDiagonalLeft(g, x, y, size);
            case GRID -> drawGrid(g, x, y, size);
            case VERTICAL -> drawVertical(g, x, y, size);
        }
    }

    private static void drawHorizontal(Graphics2D g, int x, int y, int size) {
        for (int offset = HORIZONTAL_SPACING; offset < size; offset += HORIZONTAL_SPACING) {
            g.drawLine(x, y + offset, x + size, y + offset);
        }
    }

    private static void drawVertical(Graphics2D g, int x, int y, int size) {
        for (int offset = VERTICAL_SPACING; offset < size; offset += VERTICAL_SPACING) {
            g.drawLine(x + offset, y, x + offset, y + size);
        }
    }

    private static void drawDot(Graphics2D g, int x, int y, int size) {
        for (int py = y + DOT_SPACING / 2; py < y + size; py += DOT_SPACING) {
            for (int px = x + DOT_SPACING / 2; px < x + size; px += DOT_SPACING) {
                g.fillOval(px - DOT_SIZE / 2, py - DOT_SIZE / 2, DOT_SIZE, DOT_SIZE);
            }
        }
    }

    private static void drawDiagonalRight(Graphics2D g, int x, int y, int size) {
        drawDiagonalRightWithSpacing(g, x, y, size, DIAGONAL_RIGHT_SPACING);
    }

    private static void drawDiagonalLeft(Graphics2D g, int x, int y, int size) {
        drawDiagonalLeftWithSpacing(g, x, y, size, DIAGONAL_LEFT_SPACING);
    }

    private static void drawGrid(Graphics2D g, int x, int y, int size) {
        for (int offset = GRID_SPACING; offset < size; offset += GRID_SPACING) {
            g.drawLine(x, y + offset, x + size, y + offset);
            g.drawLine(x + offset, y, x + offset, y + size);
        }
    }

    private static void drawCross(Graphics2D g, int x, int y, int size) {
        drawDiagonalRightWithSpacing(g, x, y, size, CROSS_SPACING);
        drawDiagonalLeftWithSpacing(g, x, y, size, CROSS_SPACING);
    }

    private static void drawDiagonalRightWithSpacing(Graphics2D g, int x, int y, int size, int spacing) {
        for (int offset = -size; offset <= size; offset += spacing) {
            g.drawLine(x + offset, y + size, x + offset + size, y);
        }
    }

    private static void drawDiagonalLeftWithSpacing(Graphics2D g, int x, int y, int size, int spacing) {
        for (int offset = -size; offset <= size; offset += spacing) {
            g.drawLine(x + offset, y, x + offset + size, y + size);
        }
    }

    private static Color getPatternColor(Color background) {
        double luminance = 0.2126 * background.getRed()
                + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue();

        return luminance >= 140 ? Color.BLACK : Color.WHITE;
    }
}