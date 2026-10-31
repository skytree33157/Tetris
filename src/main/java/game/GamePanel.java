package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.JPanel;

import blocks.core.Block;
import blocks.style.ColorMode;
import board.Board;
import menu.settings.AppSettings;
import ui.BlockRenderer;

// 보드 상태 + 현재 낙하 중인 블록을 그리는 패널
public class GamePanel extends JPanel{
    
    private static final long serialVersionUID =1L;
    private static final int FLASH_INTERVAL_MS = 75;
    private static final Color FLASH_COLOR = new Color(255, 255, 255, 220);
    private volatile List<Integer> clearingRows = List.of();
    private volatile List<ActionController.ClearingCell> clearingCells = List.of();

    private final Board board;
    private int cellSize;
    private volatile Block currentBlock;
    private volatile ActionController.ViewSnapshot snapshot;

    /**
     * EDT에서 호출한다. 대전에서는 각 패널에 해당 플레이어의 복사본을 전달한다.
     * 이후 렌더링은 복사본만 사용한다. 기존 setCurrentBlock/setClearingRows/Cells는 이전 방식의 호출자용이다.
     */
    public void setSnapshot(ActionController.ViewSnapshot snapshot) {
        this.snapshot = java.util.Objects.requireNonNull(snapshot);
        repaint();
    }

    public GamePanel(Board board) {
        this.board = board;
        this.cellSize = AppSettings.getInstance().getScreenSize().getCellSize();
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(board.getWidth() * cellSize, (board.getHeight() + board.getHiddenRows()) * cellSize));
    }

    public void setCurrentBlock(Block currentBlock) {
        this.currentBlock = currentBlock;
    }

    public void setClearingRows(List<Integer> clearingRows) {
        this.clearingRows = clearingRows;
    }

    public void setClearingCells(List<ActionController.ClearingCell> clearingCells) {
        this.clearingCells = clearingCells;
    }

    @Override 
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        ColorMode colormode = AppSettings.getInstance().getColorMode();
        ActionController.ViewSnapshot frame = snapshot;
        if (frame != null) {
            BlockRenderer.drawCells(g2, frame.board(), 0, 0, cellSize, colormode);
            ActionController.BlockSnapshot falling = frame.currentBlock();
            if (falling != null) {
                BlockRenderer.drawCells(g2, falling.cells(), falling.x() * cellSize,
                        falling.y() * cellSize, cellSize, colormode);
            }
            drawClearingTargets(g2, frame.clearingRows(), frame.clearingCells(), frame.animationElapsedMs());
            drawGridLines(g2);
            return;
        }
        
        drawFixedCells(g2, colormode);
        drawCurrentBlock(g2, colormode);
        drawClearingTargets(g2);
        drawGridLines(g2);
    }
    
    // 이미 고정된(쌓인) 블록들을 그림
    private void drawFixedCells(Graphics2D g2, ColorMode colorMode) {
        int[][] cells = board.getBoard();
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[row].length; col++) {
                if (cells[row][col] != 0) {
                    BlockRenderer.drawBoardCell(g2, col * cellSize, row * cellSize, cellSize, cells[row][col], colorMode);
                }
            }
        }
    }
    
    // 현재 낙하 중인 블록을 그림 (색상 + 무늬 반영)
    private void drawCurrentBlock(Graphics2D g2, ColorMode colorMode) {
        Block block = currentBlock;
        if (block == null) {
            return;
        }

        BlockRenderer.drawBlock(g2, block, 0, 0, cellSize, colorMode);
    }

    private void drawGridLines(Graphics2D g2) {
        g2.setColor(Color.DARK_GRAY);
        int width = board.getWidth() * cellSize;
        int top = board.getHiddenRows() * cellSize;
        int bottom = (board.getHiddenRows() + board.getHeight()) * cellSize;

        for (int col = 0; col <= board.getWidth(); col++) {
            g2.drawLine(col * cellSize, top, col * cellSize, bottom);
        }
        for (int row = 0; row <= board.getHeight(); row++) {
            int y = top + row * cellSize;
            g2.drawLine(0, y, width, y);
        }
    }

    // 삭제 예정인 행과 칸을 흰색으로 깜빡이게 그림
    private void drawClearingTargets(Graphics2D g2) {
        List<Integer> rows = clearingRows;
        List<ActionController.ClearingCell> cells = clearingCells;
        drawClearingTargets(g2, rows, cells, System.currentTimeMillis());
    }

    private void drawClearingTargets(Graphics2D g2, List<Integer> rows,
            List<ActionController.ClearingCell> cells, long elapsedMs) {
        if (rows.isEmpty() && cells.isEmpty()) {
            return;
        }
        if ((elapsedMs / FLASH_INTERVAL_MS) % 2 != 0) {
            return;
        }
        g2.setColor(FLASH_COLOR);
        int width = board.getWidth() * cellSize;
        for (int row : rows) {
            g2.fillRect(0, row * cellSize, width, cellSize);
        }
        for (ActionController.ClearingCell cell : cells) {
            g2.fillRect(cell.column() * cellSize, cell.row() * cellSize, cellSize, cellSize);
        }
    }

    public void refreshCellSize() {
    this.cellSize = AppSettings.getInstance().getScreenSize().getCellSize();
        setPreferredSize(new Dimension(board.getWidth() * cellSize, (board.getHeight() + board.getHiddenRows()) * cellSize));
    revalidate();
    repaint();
    }
}
