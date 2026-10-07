package game;

import blocks.core.Block;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import menu.settings.AppSettings;
import ui.BlockRenderer;

public class BlockPreviewPanel extends JPanel {

    private static final int CELL_SIZE = 20;
    private static final int PREVIEW_WIDTH = 80;
    private static final int PREVIEW_HEIGHT = 80;

    private volatile Block block;

    public BlockPreviewPanel() {
        setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        setBackground(Color.BLACK);
    }

    public void setBlock(Block block) {
        this.block = block;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        Block previewBlock = block;
        if (previewBlock == null) {
            return;
        }

        int[][] shape = previewBlock.getShape();
        int shapeWidth = shape[0].length * CELL_SIZE;
        int shapeHeight = shape.length * CELL_SIZE;
        int offsetX = (getWidth() - shapeWidth) / 2;
        int offsetY = (getHeight() - shapeHeight) / 2;

        Graphics2D graphics2D = (Graphics2D) graphics;
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int x = offsetX + col * CELL_SIZE;
                    int y = offsetY + row * CELL_SIZE;
                    BlockRenderer.drawCell(
                            graphics2D,
                            x,
                            y,
                            CELL_SIZE,
                            previewBlock.getType(),
                            AppSettings.getInstance().getColorMode()
                    );
                }
            }
        }
    }
}