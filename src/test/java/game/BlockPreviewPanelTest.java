package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blocks.style.ColorMode;
import blocks.tetromino.OBlock;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import menu.settings.AppSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockPreviewPanelTest {
    @Test
    void twoSnapshotPreviewsStayIndependentAndKeepItemSymbols() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            BlockPreviewPanel one = new BlockPreviewPanel(), two = new BlockPreviewPanel();
            OBlock block = new OBlock();
            one.setSnapshot(ActionController.snapshotOf(block));
            two.setSnapshot(ActionController.snapshotOf(new item.types.BombItem()));
            int[] before = paint(one).getRGB(0, 0, 80, 80, null, 0, 80);
            block.getShape()[0][0] = 0;
            assertTrue(java.util.Arrays.equals(before, paint(one).getRGB(0, 0, 80, 80, null, 0, 80)));
            assertTrue(hasNonBlackPixel(paint(two), 20, 20, 40, 40));
            two.setSnapshot(null);
            assertEquals(Color.BLACK.getRGB(), paint(two).getRGB(40, 40));
            assertTrue(hasNonBlackPixel(paint(one), 20, 20, 40, 40));
        });
    }

    private final AppSettings settings = AppSettings.getInstance();
    private ColorMode originalColorMode;

    @BeforeEach
    void setUp() {
        originalColorMode = settings.getColorMode();
        settings.setColorMode(ColorMode.NORMAL);
    }

    @AfterEach
    void tearDown() {
        settings.setColorMode(originalColorMode);
    }

    @Test // 미리보기 패널 크기와 검은색 배경을 사용하는지 테스트
    void constructorUsesFixedPreviewSizeAndBlackBackground() {
        BlockPreviewPanel panel = new BlockPreviewPanel();

        assertEquals(new Dimension(80, 80), panel.getPreferredSize());
        assertEquals(Color.BLACK, panel.getBackground());
    }

    @Test // 빈 미리보기 패널이 배경만 그리는지 테스트
    void emptyPreviewPaintsOnlyTheBackground() {
        BufferedImage image = paint(new BlockPreviewPanel());

        assertEquals(Color.BLACK.getRGB(), image.getRGB(40, 40));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(5, 5));
    }

    @Test // 블록을 설정하고 렌더링한 후 지우는지 테스트
    void setBlockCentersAndRendersBlockThenClearsIt() {
        BlockPreviewPanel panel = new BlockPreviewPanel();
        panel.setBlock(new OBlock());

        BufferedImage image = paint(panel);

        assertTrue(hasNonBlackPixel(image, 20, 20, 40, 40));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(10, 40));
        assertEquals(Color.BLACK.getRGB(), image.getRGB(70, 40));

        panel.setBlock(null);
        BufferedImage clearedImage = paint(panel);

        assertEquals(Color.BLACK.getRGB(), clearedImage.getRGB(40, 40));
    }

    private static BufferedImage paint(BlockPreviewPanel panel) {
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
