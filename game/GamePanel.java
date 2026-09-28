package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import blocks.core.Block;
import blocks.style.BlockStyle;
import board.Board;
import menu.settings.AppSettings;;

// 보드 상태 + 현재 낙하 중인 블록을 그리는 패널
// Board.java / GameLoop.java는 수정하지 않고, 기존 public API만 사용
public class GamePanel extends JPanel{
    
    private static final long serialVersionUID =1L;

    private static final Color FIXED_CELL_COLOR = Color.LIGHT_GRAY;

    private final Board board;
    private final int cellSize;
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
        
        drawFixedCells(g2);
        drawCurrentBlock(g2);
        drawGridLines(g2);
    }
    
    // 이미 고정된(쌓인) 블록들을 그림
    // Board.addBlock이 블록 종류가 아닌 1만 저장하므로, 고정된 칸은 전부 동일한 색으로 표시됨.
    // (Board.java가 타입 값을 저장하도록 바뀌면 colorForValue로 교체 가능)
    private void drawFixedCells(Graphics2D g2) {
        int[][] cells = board.getBoard();
        for (int row = 0; row < cells.length; row++) {
            for (int col = 0; col < cells[row].length; col++) {
                if (cells[row][col] != 0) {
                    drawCell(g2, col, row, FIXED_CELL_COLOR);
                }
            }
        }
    }
    // 현재 낙하 중인 블록을 그림
    private void drawCurrentBlock(Graphics2D g2) {
        Block block = currentBlock;
        if (block == null) {
            return;
        }

        int[][] shape = block.getShape();
        Color color = BlockStyle.of(block.getType(), AppSettings.getInstance().getColorMode()).getColor();

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    drawCell(g2, block.getX() + col, block.getY() + row, color);
                }
            }
        }
    }

    private void drawCell(Graphics2D g2, int col, int row, Color color) {
        if (row < 0) {
            return; // 스폰 위치가 보드 위쪽 경계 밖일 때는 그리지 않음
        }
        int px = col * cellSize;
        int py = row * cellSize;

        g2.setColor(color);
        g2.fillRect(px, py, cellSize, cellSize);
        g2.setColor(Color.BLACK);
        g2.drawRect(px, py, cellSize, cellSize);
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
}
