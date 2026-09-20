package game;

import blocks.Block;
import blocks.BlockFactory;
import board.Board;

public class GameLoop implements Runnable {

    private int dropSpeed = 1000; // 1000ms = 1s
    private Board board;
    private Block block;

    private int startX = 3;
    private int startY = 0;


    public GameLoop(Board board, Block block) {
        this.board = board;
        this.block = block;
        this.block.setX(startX);
        this.block.setY(startY);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(dropSpeed);
                moveDownBlock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // 블록을 아래로 이동시키는 메서드
    private void moveDownBlock() {
        int x = block.getX();
        int y = block.getY();
        int nextY = y + 1;
        
        // 다음 위치가 유효한지 확인
        if (isValidPosition(x, nextY)) {
            block.setY(nextY);
        } else { // 블록이 더 이상 내려갈 수 없으면 현재 위치에 블록을 고정 후 새로운 블록 생성
            board.addBlock(x, y, block.getShape());
            block = BlockFactory.createRandomBlock();
            block.setX(startX);
            block.setY(startY);
        }
    }

    // 블록이 특정 위치에 놓일 수 있는지 확인하는 메서드
    private boolean isValidPosition(int targetX, int targetY) {
        int[][] shape = block.getShape();
        
        // 블록의 각 칸을 검사하여 충돌 여부 확인
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                // 블록이 실제로 존재하는 칸만 검사
                if (shape[row][col] != 0) {
                    int boardX = targetX + col;
                    int boardY = targetY + row;

                    // 블록이 보드 내부에 있는지 검사
                    if (boardX < 0 || boardX >= board.getWidth() || boardY >= board.getHeight()) {
                        return false;
                    }

                    // 다른 고정된 블록과 충돌하는지 검사 
                    if (boardY >= 0) {
                        int[][] curBoard = board.getBoard();
                        if (curBoard[boardY][boardX] != 0) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }    
}
