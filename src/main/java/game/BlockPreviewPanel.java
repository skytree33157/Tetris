package game;

import blocks.core.Block;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import menu.settings.AppSettings;
import ui.BlockRenderer;
import ui.theme.Theme;

public class BlockPreviewPanel extends JPanel {

    private static final int CELL_SIZE = 20;
    private static final int PREVIEW_WIDTH = 80;
    private static final int PREVIEW_HEIGHT = 80;

    private volatile ActionController.BlockSnapshot block;

    public BlockPreviewPanel() {
        setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        // NEXT·HOLD 카드와 같은 색이라 블록만 보임
        setBackground(Theme.CARD);
    }

    public void setBlock(Block block) {
        setSnapshot(ActionController.snapshotOf(block));
    }

    /** EDT에서 호출한다. NEXT가 현재 블록으로 바뀌어도 기존 프레임은 변하지 않는다. */
    public void setSnapshot(ActionController.BlockSnapshot block) {
        this.block = block;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        ActionController.BlockSnapshot previewBlock = block;
        if (previewBlock == null) {
            return;
        }

        int[][] shape = previewBlock.cells();
        int shapeWidth = shape[0].length * CELL_SIZE;
        int shapeHeight = shape.length * CELL_SIZE;
        int offsetX = (getWidth() - shapeWidth) / 2;
        int offsetY = (getHeight() - shapeHeight) / 2;

        BlockRenderer.drawCells((Graphics2D) graphics, shape, offsetX, offsetY,
                CELL_SIZE, AppSettings.getInstance().getColorMode());
    }
}
