package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import blocks.core.Block;
import blocks.style.ColorMode;
import board.Board;
import menu.settings.AppSettings;
import ui.BlockRenderer;

// 보드 상태 + 현재 낙하 중인 블록을 그리는 패널
public class GamePanel extends JPanel{
    
    private static final long serialVersionUID =1L;

    private final Board board;
    private int cellSize;
    private volatile Block currentBlock;

    public GamePanel(Board board) {
        this.board = board;
        this.cellSize = AppSettings.getInstance().getScreenSize().getCellSize();
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(board.getWidth() * cellSize, board.getHeight() * cellSize));
    }

    public void setCurrentBlock(Block currentBlock) {
        this.currentBlock = currentBlock;
    }

    @Override 
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        ColorMode colormode = AppSettings.getInstance().getColorMode();
        
        drawFixedCells(g2, colormode);
        drawCurrentBlock(g2, colormode);
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
        int height = board.getHeight() * cellSize;

        for (int col = 0; col <= board.getWidth(); col++) {
            g2.drawLine(col * cellSize, 0, col * cellSize, height);
        }
        for (int row = 0; row <= board.getHeight(); row++) {
            g2.drawLine(0, row * cellSize, width, row * cellSize);
        }
    }

    public void refreshCellSize() {
    this.cellSize = AppSettings.getInstance().getScreenSize().getCellSize();
    setPreferredSize(new Dimension(board.getWidth() * cellSize, board.getHeight() * cellSize));
    revalidate();
    repaint();
    }
}
