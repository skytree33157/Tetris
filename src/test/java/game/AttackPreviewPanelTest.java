package game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import blocks.core.BlockType;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

// 공격 대기 줄 미리보기: 받은 줄 수를 그대로 보여주고, 비우면 0줄
class AttackPreviewPanelTest {
    private static final int A = BlockType.ATTACK.getValue();

    @Test
    void showsPendingRowsAndClearsWhenApplied() {
        AttackPreviewPanel panel = new AttackPreviewPanel();
        assertEquals(0, panel.getRowCount());

        panel.setRows(List.of(row(3), row(7)));
        assertEquals(2, panel.getRowCount());

        panel.setRows(List.of());
        assertEquals(0, panel.getRowCount());

        panel.setRows(null);
        assertEquals(0, panel.getRowCount());
    }

    @Test
    void paintsEmptyPartialAndFullQueueWithoutError() {
        AttackPreviewPanel panel = new AttackPreviewPanel();
        panel.setSize(panel.getPreferredSize());
        paint(panel);

        panel.setRows(List.of(row(0)));
        paint(panel);

        List<int[]> full = new ArrayList<>();
        for (int i = 0; i < ActionController.MAX_PENDING_ATTACK_ROWS; i++) {
            full.add(row(i));
        }
        panel.setRows(full);
        assertEquals(ActionController.MAX_PENDING_ATTACK_ROWS, panel.getRowCount());
        paint(panel);
    }

    private static int[] row(int hole) {
        int[] row = new int[10];
        for (int c = 0; c < row.length; c++) {
            row[c] = c == hole ? 0 : A;
        }
        return row;
    }

    private static void paint(AttackPreviewPanel panel) {
        BufferedImage image = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        try {
            panel.paint(g);
        } finally {
            g.dispose();
        }
    }
}
