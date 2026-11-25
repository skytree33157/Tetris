package game;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import menu.settings.AppSettings;
import network.GameStatus;
import ui.BlockRenderer;
import ui.theme.Theme;

// P2P 대전에서 상대 보드를 작게 그리는 패널. 받은 GameStatus의 칸 값을 그대로 그린다.
public class RemoteBoardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final int CELL_SIZE = 14;
    private static final int COLS = 10;
    private static final int ROWS = 20;

    private volatile int[][] cells = new int[0][0];
    private volatile boolean gameOver;

    public RemoteBoardPanel() {
        setBackground(Theme.BOARD_BG);
        setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));
    }

    /** EDT에서 호출. 보드가 없는 상태(예: 이전 버전 메시지)는 마지막 보드를 유지한다. */
    public void setStatus(GameStatus status) {
        if (status == null) {
            return;
        }
        if (status.hasBoard()) {
            cells = status.getCells();
        }
        gameOver = status.isGameOver();
        repaint();
    }

    boolean hasBoard() {
        return cells.length > 0;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            int[][] frame = cells;
            if (frame.length == 0) {
                drawCenteredText(g2, "상대를 기다리는 중", Theme.TEXT_MUTED);
                return;
            }
            // 받은 보드 크기가 기본(10×20)과 달라도 패널 안에 들어가도록 칸 크기를 맞춘다
            int size = Math.max(1, Math.min(getWidth() / frame[0].length, getHeight() / frame.length));
            int offsetX = (getWidth() - size * frame[0].length) / 2;
            int offsetY = (getHeight() - size * frame.length) / 2;

            drawGrid(g2, frame.length, frame[0].length, size, offsetX, offsetY);
            BlockRenderer.drawCells(g2, frame, offsetX, offsetY, size,
                    AppSettings.getInstance().getColorMode());

            if (gameOver) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                g2.setColor(Theme.BOARD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setComposite(AlphaComposite.SrcOver);
                drawCenteredText(g2, "GAME OVER", Theme.TEXT);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawGrid(Graphics2D g2, int rows, int cols, int size, int x0, int y0) {
        g2.setColor(Theme.GRID_LINE);
        for (int c = 0; c <= cols; c++) {
            g2.drawLine(x0 + c * size, y0, x0 + c * size, y0 + rows * size);
        }
        for (int r = 0; r <= rows; r++) {
            g2.drawLine(x0, y0 + r * size, x0 + cols * size, y0 + r * size);
        }
    }

    private void drawCenteredText(Graphics2D g2, String text, java.awt.Color color) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.body(12, Font.BOLD));
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
    }
}
