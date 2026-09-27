package game;

import blocks.Block;
import blocks.BlockFactory;
import board.Board;

// 블록 이동 클래스

public class ActionController {

    private Board board;
    private Block block;
    private GameStateManager gameStateManager;

    private int startX = 3;
    private int startY = 0;

    public ActionController(Board board, Block block, GameStateManager gameStateManager) {
        this.board = board;
        this.block = block;
        this.gameStateManager = gameStateManager;
        this.block.setX(startX);
        this.block.setY(startY);
    }

    // 블록을 아래로 이동시키는 메서드
    private synchronized void moveDownBlock() {
        int x = block.getX();
        int y = block.getY();
        int nextY = y + 1;
        
        // 다음 위치가 유효한지 확인
        if (board.isValidPosition(block.getShape(),x, nextY)) {
            block.moveDown();
        } else { // 블록이 더 이상 내려갈 수 없으면 현재 위치에 블록을 고정 후 새로운 블록 생성
            board.addBlock(x, y, block.getShape());
            
            // 줄 삭제 및 하강 속도 증가
            gameStateManager.onBlockPlaced();
            int clearedLines = board.clearLines();
            gameStateManager.onLinesCleared(clearedLines);

            // 새 블록 생성
            block = BlockFactory.createRandomBlock();
            block.setX(startX);
            block.setY(startY);
        }
    }

    public synchronized void moveLeftAction() {
        int targetX = block.getX() - 1;
        if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
            block.moveLeft();
        }
    }

    public synchronized void moveRightAction() {
        int targetX = block.getX() + 1;
        if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
            block.moveRight();
        }
    }

    public synchronized void moveDownAction() {
        moveDownBlock();
    }

    public synchronized void rotateAction(){
        int[][] rotatedShape = block.getRotate();
        if (board.isValidPosition(rotatedShape, block.getX(), block.getY())) {
            block.rotate();
        }
    }

    // 하드드롭
    public synchronized void hardDropAction() {
        while (board.isValidPosition(block.getShape(), block.getX(), block.getY() + 1)) {
            block.moveDown();
        }
        moveDownBlock(); // 블록을 고정하고 새로운 블록 생성 -> moveDownBlock()의 else 실행
    }
}
