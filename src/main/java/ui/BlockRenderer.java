package ui;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.style.BlockPattern;
import blocks.style.BlockStyle;
import blocks.style.ColorMode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.RenderingHints;

public final class BlockRenderer {

    // 간격 및 크기 상수 정의
    private static final int HORIZONTAL_SPACING = 4;
    private static final int VERTICAL_SPACING = 4;
    private static final int DOT_SPACING = 5;
    private static final int DOT_SIZE = 2;
    private static final int CROSS_SPACING = 7;
    private static final int DIAGONAL_RIGHT_SPACING = 6;
    private static final int DIAGONAL_LEFT_SPACING = 6;
    private static final int GRID_SPACING = 6;

    private BlockRenderer() {}

    // 셀을 그리는 메서드
    public static void drawCell(
            Graphics2D g,
            int x,
            int y,
            int size,
            BlockType type,
            ColorMode mode
    ) {
        if (g == null || type == null || mode == null || size <= 0) return;

        BlockStyle style = BlockStyle.of(type, mode);
        Color fillColor = style.getColor();

        Graphics2D g2d = (Graphics2D) g.create();

        try {
            g2d.setColor(fillColor);
            g2d.fillRect(x, y, size, size);

            Graphics2D patternGraphics = (Graphics2D) g2d.create();

            try {
                // 패턴 영역 클리핑
                patternGraphics.clipRect(
                        x + 1,
                        y + 1,
                        Math.max(1, size - 2),
                        Math.max(1, size - 2)
                );

                patternGraphics.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF
                );

                patternGraphics.setStroke(
                        new BasicStroke(
                                1.0f,
                                BasicStroke.CAP_BUTT,
                                BasicStroke.JOIN_MITER
                        )
                );

                patternGraphics.setColor(getPatternColor(fillColor));

                // 패턴 그리기
                drawPattern(
                        patternGraphics,
                        x,
                        y,
                        size,
                        style.getPattern()
                );

            } finally {
                patternGraphics.dispose();
            }

            g2d.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_OFF
            );

            // 셀 테두리 그리기
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(size >= 30 ? 2.0f : 1.0f));
            g2d.drawRect(x, y, size - 1, size - 1);

        } finally {
            g2d.dispose();
        }
    }

    // 블록을 그리는 메서드
    public static void drawBlock(
            Graphics2D g,
            Block block,
            int boardX,
            int boardY,
            int cellSize,
            ColorMode mode
    ) {
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

                drawBlockCell(g, block, row, col, pixelX, pixelY, cellSize, mode);
            }
        }
    }

    // 보드 셀을 그리는 메서드
    public static void drawBoardCell(
            Graphics2D g,
            int x,
            int y,
            int size,
            int cellValue,
            ColorMode mode
    ) {
        if (cellValue == 0) return;

        if (g == null || mode == null || size <= 0) return;
        ItemAppearanceResolver.CellAppearance cell = ItemAppearanceResolver.fromBoardCell(cellValue);
        drawAppearance(g, x, y, size, cell, mode);
    }

    /** 낙하 화면과 NEXT가 공유한다. 블록의 보드 좌표 대신 전달받은 화면 좌표에 한 칸을 그린다. */
    public static void drawBlockCell(Graphics2D g, Block block, int row, int col,
                                     int x, int y, int size, ColorMode mode) {
        if (g == null || block == null || mode == null || size <= 0) return;
        int[][] shape = block.getShape();
        if (row < 0 || row >= shape.length || col < 0 || col >= shape[row].length
                || shape[row][col] == 0) return;
        drawAppearance(g, x, y, size, ItemAppearanceResolver.resolve(block, row, col), mode);
    }

    // 원본 배경·패턴을 먼저 그린 뒤 필요한 칸에만 아이템 문자를 덧그린다.
    private static void drawAppearance(Graphics2D g, int x, int y, int size,
                                        ItemAppearanceResolver.CellAppearance cell, ColorMode mode) {
        drawCell(g, x, y, size, cell.type(), mode);
        if (cell.type() == BlockType.BOMB) {
            drawBomb(g, x, y, size);
            return;
        }
        if (cell.symbol() != '\0') {
            drawItemSymbol(g, x, y, size, cell.symbol(), BlockStyle.of(cell.type(), mode).getColor());
        }
    }

    // 한 칸 안에 폭탄 몸체·심지·B 표시. NEXT도 같은 메서드 사용.
    private static void drawBomb(Graphics2D g, int x, int y, int size) {
        if (size < 8) return;
        Graphics2D bombGraphics = (Graphics2D) g.create();
        try {
            bombGraphics.clipRect(x + 1, y + 1, size - 2, size - 2);
            bombGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int diameter = Math.max(4, size * 2 / 3);
            int bodyX = x + (size - diameter) / 2;
            int bodyY = y + size / 4;
            int neckX = x + size / 2;
            int neckY = bodyY - Math.max(1, size / 10);
            bombGraphics.setStroke(new BasicStroke(Math.max(1.0f, size / 20.0f)));

            // 검은 몸체·밝은 테두리로 형태 구분. 색상에만 의존 X.
            bombGraphics.setColor(Color.BLACK);
            bombGraphics.fillOval(bodyX, bodyY, diameter, diameter);
            bombGraphics.setColor(new Color(0xE8EDF2));
            bombGraphics.drawOval(bodyX, bodyY, diameter, diameter);
            bombGraphics.drawLine(neckX, neckY, neckX, bodyY);

            int sparkX = x + size * 4 / 5;
            int sparkY = y + size / 8;
            bombGraphics.setColor(new Color(0xFFD166));
            bombGraphics.drawLine(neckX, neckY, sparkX, sparkY);
            int sparkRadius = Math.max(1, size / 12);
            bombGraphics.drawLine(sparkX - sparkRadius, sparkY, sparkX + sparkRadius, sparkY);
            bombGraphics.drawLine(sparkX, sparkY - sparkRadius, sparkX, sparkY + sparkRadius);

            drawItemSymbol(bombGraphics, bodyX, bodyY, diameter, 'B', Color.BLACK);
        } finally {
            bombGraphics.dispose();
        }
    }

    // 작은 NEXT 셀에서도 문자가 패턴에 묻히지 않도록 대비되는 외곽선을 사용한다.
    private static void drawItemSymbol(Graphics2D g, int x, int y, int size,
                                       char symbol, Color background) {
        if (size < 8) return;
        Graphics2D textGraphics = (Graphics2D) g.create();
        try {
            textGraphics.clipRect(x + 1, y + 1, size - 2, size - 2);
            textGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String text = String.valueOf(symbol);
            int fontSize = Math.max(1, size * 2 / 3);
            FontMetrics metrics;
            do {
                textGraphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize));
                metrics = textGraphics.getFontMetrics();
                if (metrics.stringWidth(text) <= size - 4 && metrics.getHeight() <= size - 2) break;
                fontSize--;
            } while (fontSize > 1);
            int textX = x + (size - metrics.stringWidth(text)) / 2;
            int textY = y + (size - metrics.getHeight()) / 2 + metrics.getAscent();
            Color ink = getPatternColor(background);
            Color outline = ink.equals(Color.BLACK) ? Color.WHITE : Color.BLACK;
            textGraphics.setColor(outline);
            textGraphics.drawString(text, textX - 1, textY);
            textGraphics.drawString(text, textX + 1, textY);
            textGraphics.drawString(text, textX, textY - 1);
            textGraphics.drawString(text, textX, textY + 1);
            textGraphics.setColor(ink);
            textGraphics.drawString(text, textX, textY);
        } finally {
            textGraphics.dispose();
        }
    }

    // 패턴을 그리는 메서드
    private static void drawPattern(
            Graphics2D g,
            int x,
            int y,
            int size,
            BlockPattern pattern
    ) {
        switch (pattern) {
            case HORIZONTAL -> drawHorizontal(g, x, y, size);
            case DOT -> drawDot(g, x, y, size);
            case CROSS -> drawCross(g, x, y, size);
            case DIAGONAL_RIGHT -> drawDiagonalRight(g, x, y, size);
            case DIAGONAL_LEFT -> drawDiagonalLeft(g, x, y, size);
            case GRID -> drawGrid(g, x, y, size);
            case VERTICAL -> drawVertical(g, x, y, size);
            case BRICK -> drawBrick(g, x, y, size);
        }
    }

    // 가로선 패턴
    private static void drawHorizontal(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        for (int offset = HORIZONTAL_SPACING; offset < size; offset += HORIZONTAL_SPACING) {
            g.drawLine(x, y + offset, x + size, y + offset);
        }
    }

    // 세로선 패턴
    private static void drawVertical(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        for (int offset = VERTICAL_SPACING; offset < size; offset += VERTICAL_SPACING) {
            g.drawLine(x + offset, y, x + offset, y + size);
        }
    }

    // 점 패턴
    private static void drawDot(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        for (int py = y + DOT_SPACING / 2; py < y + size; py += DOT_SPACING) {
            for (int px = x + DOT_SPACING / 2; px < x + size; px += DOT_SPACING) {
                g.fillOval(
                        px - DOT_SIZE / 2,
                        py - DOT_SIZE / 2,
                        DOT_SIZE,
                        DOT_SIZE
                );
            }
        }
    }

    // 오른쪽 대각선 패턴
    private static void drawDiagonalRight(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        drawDiagonalRightWithSpacing(
                g,
                x,
                y,
                size,
                DIAGONAL_RIGHT_SPACING
        );
    }

    // 왼쪽 대각선 패턴
    private static void drawDiagonalLeft(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        drawDiagonalLeftWithSpacing(
                g,
                x,
                y,
                size,
                DIAGONAL_LEFT_SPACING
        );
    }

    // 격자 패턴
    private static void drawGrid(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        for (int offset = GRID_SPACING; offset < size; offset += GRID_SPACING) {
            g.drawLine(x, y + offset, x + size, y + offset);
            g.drawLine(x + offset, y, x + offset, y + size);
        }
    }

    // 십자 패턴
    private static void drawCross(
            Graphics2D g,
            int x,
            int y,
            int size
    ) {
        drawDiagonalRightWithSpacing(
                g,
                x,
                y,
                size,
                CROSS_SPACING
        );

        drawDiagonalLeftWithSpacing(
                g,
                x,
                y,
                size,
                CROSS_SPACING
        );
    }

    // 오른쪽 대각선 패턴 그리기 (간격 포함)
    private static void drawDiagonalRightWithSpacing(
            Graphics2D g,
            int x,
            int y,
            int size,
            int spacing
    ) {
        for (int offset = -size; offset <= size; offset += spacing) {
            g.drawLine(
                    x + offset,
                    y + size,
                    x + offset + size,
                    y
            );
        }
    }

    // 왼쪽 대각선 패턴 그리기 (간격 포함)
    private static void drawDiagonalLeftWithSpacing(
            Graphics2D g,
            int x,
            int y,
            int size,
            int spacing
    ) {
        for (int offset = -size; offset <= size; offset += spacing) {
            g.drawLine(
                    x + offset,
                    y,
                    x + offset + size,
                    y + size
            );
        }
    }

    // Weight 전용 패턴: 행마다 세로 이음매를 반 칸 엇갈리게 배치한다.
    private static void drawBrick(Graphics2D g, int x, int y, int size) {
        int brickHeight = 5;
        int brickWidth = 10;
        for (int offsetY = 0; offsetY < size; offsetY += brickHeight) {
            if (offsetY > 0) g.drawLine(x, y + offsetY, x + size, y + offsetY);
            int shift = (offsetY / brickHeight) % 2 == 0 ? brickWidth / 2 : 0;
            for (int offsetX = shift; offsetX < size; offsetX += brickWidth) {
                g.drawLine(x + offsetX, y + offsetY, x + offsetX,
                        y + Math.min(size, offsetY + brickHeight));
            }
        }
    }

    // 패턴 색상 결정
    private static Color getPatternColor(Color background) {
        double luminance =
                0.2126 * background.getRed()
                        + 0.7152 * background.getGreen()
                        + 0.0722 * background.getBlue();

        return luminance >= 140 ? Color.BLACK : Color.WHITE;
    }
}
