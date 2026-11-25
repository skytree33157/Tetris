package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import network.GameStatus;
import org.junit.jupiter.api.Test;
import ui.theme.Theme;

// P2P 상대 보드 패널: 받은 칸을 그리고, 보드 없는 상태가 와도 마지막 보드를 유지
class RemoteBoardPanelTest {
    @Test
    void startsEmptyAndDrawsReceivedCells() {
        RemoteBoardPanel panel = new RemoteBoardPanel();
        assertFalse(panel.hasBoard());

        int[][] cells = new int[20][10];
        cells[19][0] = 1;   // 왼쪽 아래 한 칸
        panel.setStatus(new GameStatus(10, 1, 0, false, cells));

        assertTrue(panel.hasBoard());
        BufferedImage image = paint(panel);
        int size = RemoteBoardPanel.CELL_SIZE;
        // 채운 칸의 가운데는 보드 배경색이 아님
        assertNotEquals(Theme.BOARD_BG.getRGB(), image.getRGB(size / 2, 19 * size + size / 2));
        // 빈 칸의 가운데는 보드 배경색
        assertEquals(Theme.BOARD_BG.getRGB(), image.getRGB(5 * size + size / 2, 10 * size + size / 2));
    }

    @Test
    void keepsLastBoardWhenStatusHasNoBoard() {
        RemoteBoardPanel panel = new RemoteBoardPanel();
        panel.setStatus(new GameStatus(10, 1, 0, false, new int[20][10]));

        panel.setStatus(new GameStatus(20, 1, 0, true));   // 보드 없이 게임 오버만

        assertTrue(panel.hasBoard());
        paint(panel);   // 게임 오버 덮개를 그려도 예외 없음
    }

    @Test
    void ignoresNullStatus() {
        RemoteBoardPanel panel = new RemoteBoardPanel();
        panel.setStatus(null);
        assertFalse(panel.hasBoard());
        paint(panel);   // "상대를 기다리는 중" 문구
    }

    private static BufferedImage paint(RemoteBoardPanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            panel.paint(g);
        } finally {
            g.dispose();
        }
        return image;
    }
}
