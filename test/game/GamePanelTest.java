package game;

import blocks.tetromino.OBlock;
import board.Board;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import menu.settings.AppSettings;
import menu.settings.ScreenSize;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GamePanelTest {

    @Test // 게임 패널 크기 확인
    void preferredSizeIncludesVisibleAndHiddenRows() {
        AppSettings settings = AppSettings.getInstance();
        ScreenSize originalSize = settings.getScreenSize();
        try {
            settings.setScreenSize(ScreenSize.MEDIUM);
            Board board = new Board();
            GamePanel panel = new GamePanel(board);
            // 패널의 초기 크기 확인
            assertEquals(new Dimension(300, 690), panel.getPreferredSize());
        } finally {
            settings.setScreenSize(originalSize);
        }
    }

    @Test // 현재 블록과 보드에 고정된 셀들이 올바르게 나오는지 테스트
    void paintComponentDrawsCurrentBlockAndFixedCells() {
        Board board = new Board();
        board.getBoard()[10][0] = 1;
        GamePanel panel = new GamePanel(board);
        OBlock currentBlock = new OBlock();
        currentBlock.setX(3);
        currentBlock.setY(5);
        panel.setCurrentBlock(currentBlock);

        BufferedImage image = new BufferedImage(300, 690, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.paintComponent(graphics);
            // 현재 위치가 검은색이 아닌지 확인
            assertNotEquals(java.awt.Color.BLACK.getRGB(), image.getRGB(3 * 30 + 10, 5 * 30 + 10));
            assertNotEquals(java.awt.Color.BLACK.getRGB(), image.getRGB(10, 10 * 30 + 10));
        } finally {
            graphics.dispose();
        }
    }

    @Test // 스크린 사이즈 변경 후 패널의 크기가 변경되는지 테스트
    void refreshCellSize() {
        AppSettings settings = AppSettings.getInstance();
        ScreenSize originalSize = settings.getScreenSize();
        try {
            settings.setScreenSize(ScreenSize.MEDIUM);
            Board board = new Board();
            GamePanel panel = new GamePanel(board);

            settings.setScreenSize(ScreenSize.SMALL);
            panel.refreshCellSize();

            assertEquals(new Dimension(200, 460), panel.getPreferredSize());
        } finally {
            settings.setScreenSize(originalSize);
        }
    }
}