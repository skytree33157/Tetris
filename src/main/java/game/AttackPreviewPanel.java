package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import blocks.core.BlockType;
import ui.theme.Theme;

/**
 * 대전에서 받은 공격 중 아직 보드에 들어가지 않은 줄을 미리 보여주는 패널.
 * 현재 블록이 고정되면 이 줄들이 보드 맨 아래에 추가된다. 빈 칸(구멍) 위치도 그대로 보여준다.
 * 아래쪽부터 쌓아서 그린다. 줄이 들어가 대기열이 비면 안내 문구만 남는다.
 */
public class AttackPreviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final int CELL_SIZE = 12;
    private static final int COLS = 10;
    private static final int ROWS = ActionController.MAX_PENDING_ATTACK_ROWS;
    private static final Color ATTACK_FILL = new Color(0x8A8F98);
    private static final Color ATTACK_EDGE = new Color(0x5C6068);
    private static final Color WARNING = new Color(0xF26B6B);

    private volatile List<int[]> rows = List.of();

    public AttackPreviewPanel() {
        setBackground(Theme.BOARD_BG);
        setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));
    }

    /** EDT에서 호출. ViewSnapshot.pendingAttackRows()의 복사본을 넘긴다. */
    public void setRows(List<int[]> pending) {
        List<int[]> next = pending == null ? List.of() : List.copyOf(pending);
        if (sameRows(rows, next)) {
            return;
        }
        rows = next;
        repaint();
    }

    public int getRowCount() {
        return rows.size();
    }

    private static boolean sameRows(List<int[]> a, List<int[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            int size = Math.max(1, Math.min(getWidth() / COLS, getHeight() / ROWS));
            int x0 = (getWidth() - size * COLS) / 2;
            int y0 = (getHeight() - size * ROWS) / 2;

            g2.setColor(Theme.GRID_LINE);
            for (int c = 0; c <= COLS; c++) {
                g2.drawLine(x0 + c * size, y0, x0 + c * size, y0 + ROWS * size);
            }
            for (int r = 0; r <= ROWS; r++) {
                g2.drawLine(x0, y0 + r * size, x0 + COLS * size, y0 + r * size);
            }

            List<int[]> frame = rows;
            if (frame.isEmpty()) {
                drawCenteredText(g2, "공격 없음", Theme.TEXT_MUTED);
                return;
            }

            int shown = Math.min(frame.size(), ROWS);
            for (int i = 0; i < shown; i++) {
                int[] row = frame.get(i);
                // 첫 줄이 맨 아래
                int y = y0 + (ROWS - 1 - i) * size;
                for (int c = 0; c < Math.min(COLS, row.length); c++) {
                    if (row[c] != BlockType.ATTACK.getValue()) {
                        continue;
                    }
                    int x = x0 + c * size;
                    g2.setColor(ATTACK_FILL);
                    g2.fillRect(x + 1, y + 1, size - 1, size - 1);
                    g2.setColor(ATTACK_EDGE);
                    g2.drawRect(x + 1, y + 1, size - 2, size - 2);
                }
            }

            // 대기열이 가득 차면 위쪽 테두리를 빨갛게 표시
            if (frame.size() >= ROWS) {
                g2.setColor(WARNING);
                g2.fillRect(x0, y0, COLS * size, 2);
            }
        } finally {
            g2.dispose();
        }
    }

    private void drawCenteredText(Graphics2D g2, String text, Color color) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.body(11, Font.PLAIN));
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
    }
}
