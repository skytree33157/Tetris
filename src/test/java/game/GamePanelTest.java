package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blocks.tetromino.OBlock;
import board.Board;
import blocks.style.ColorMode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import menu.settings.AppSettings;
import menu.settings.ScreenSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GamePanelTest {
    private final AppSettings settings = AppSettings.getInstance();
    private ScreenSize originalScreenSize;
    private ColorMode originalColorMode;

    @BeforeEach
    void setUp() {
        originalScreenSize = settings.getScreenSize();
        originalColorMode = settings.getColorMode();
        settings.setScreenSize(ScreenSize.MEDIUM);
        settings.setColorMode(ColorMode.NORMAL);
    }

    @AfterEach
    void tearDown() {
        settings.setScreenSize(originalScreenSize);
        settings.setColorMode(originalColorMode);
    }

    @Test // 보드의 크기와 검은색 배경을 사용하는지 테스트
    void constructorUsesBoardDimensionsAndBlackBackground() {
        Board board = new Board();
        GamePanel panel = new GamePanel(board);

        assertEquals(new Dimension(300, 690), panel.getPreferredSize());
        assertEquals(Color.BLACK, panel.getBackground());
    }

    @Test // 고정된 셀, 현재 블록, 격자선이 올바르게 그려지는지 테스트
    void paintDrawsFixedCellsCurrentBlockAndGridLines() {
        Board board = new Board();
        board.getBoard()[3][2] = 1;
        GamePanel panel = new GamePanel(board);
        OBlock currentBlock = new OBlock();
        currentBlock.setX(5);
        currentBlock.setY(7);
        panel.setCurrentBlock(currentBlock);

        BufferedImage image = paint(panel);

        assertTrue(hasNonBlackPixel(image, 60, 90, 30, 30));
        assertTrue(hasNonBlackPixel(image, 150, 210, 60, 60));
        assertEquals(Color.DARK_GRAY.getRGB(), image.getRGB(30, 105));
        assertEquals(Color.DARK_GRAY.getRGB(), image.getRGB(15, 90));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(5, 5));
    }

    @Test // 행을 지우는 효과와 정상 렌더링으로 돌아가는지 테스트
    void clearingRowsFlashAndReturnToNormalRendering() throws InterruptedException {
        GamePanel panel = new GamePanel(new Board());
        panel.setClearingRows(List.of(4));
        int sampleX = 2 * ScreenSize.MEDIUM.getCellSize() + 5;
        int sampleY = 4 * ScreenSize.MEDIUM.getCellSize() + 15;
        boolean sawFlash = false;
        boolean sawNormalFrame = false;
        long deadline = System.nanoTime() + 2_000_000_000L;

        while ((!sawFlash || !sawNormalFrame) && System.nanoTime() < deadline) {
            int pixel = paint(panel).getRGB(sampleX, sampleY);
            sawFlash |= pixel != Color.BLACK.getRGB();
            sawNormalFrame |= pixel == Color.BLACK.getRGB();
            if (!sawFlash || !sawNormalFrame) {
                Thread.sleep(10);
            }
        }

        assertTrue(sawFlash, "clearing row should be visible during the flash phase");
        assertTrue(sawNormalFrame, "clearing row should be hidden during the other phase");
    }

    @Test // 화면 크기 변경 시 크기가 업데이트되는지 테스트
    void refreshCellSizeUpdatesPreferredSize() {
        GamePanel panel = new GamePanel(new Board());
        settings.setScreenSize(ScreenSize.LARGE);

        panel.refreshCellSize();

        assertEquals(new Dimension(400, 920), panel.getPreferredSize());
    }

    private static BufferedImage paint(GamePanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static boolean hasNonBlackPixel(BufferedImage image, int x, int y, int width, int height) {
        for (int row = y; row < y + height; row++) {
            for (int col = x; col < x + width; col++) {
                if (image.getRGB(col, row) != Color.BLACK.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }
}