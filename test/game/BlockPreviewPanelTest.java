package game;

import blocks.tetromino.OBlock;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockPreviewPanelTest {

    @Test // BlockPreviewPanel의 초기 크기와 배경색 확인
    void startsWithFixedPreviewSizeAndBlackBackground() {
        BlockPreviewPanel panel = new BlockPreviewPanel();

        assertEquals(new Dimension(80, 80), panel.getPreferredSize());
        assertEquals(java.awt.Color.BLACK, panel.getBackground());
    }

    @Test
    void paintComponentDrawsAssignedBlockAndHandlesEmptyPreview() {
        BlockPreviewPanel panel = new BlockPreviewPanel();
        panel.setSize(80, 80);
        BufferedImage image = new BufferedImage(80, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            panel.paintComponent(graphics);
            int[] emptyPixels = image.getRGB(0, 0, 80, 80, null, 0, 80);

            panel.setBlock(new OBlock());
            panel.paintComponent(graphics);
            // 블록이 미리보기 패널에 보여지는지 테스트
            assertFalse(java.util.Arrays.equals(emptyPixels, image.getRGB(0, 0, 80, 80, null, 0, 80)));
        } finally {
            graphics.dispose();
        }
    }
}