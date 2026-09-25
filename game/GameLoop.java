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

    private int countBlock = 0;
    private int countClearLine = 0;

    private static final int SPEED_DECREASE_AMOUNT = 100; // 한 레벨 당 감소할 속도
    private static final int MIN_DROP_SPEED = 100; // 최소 드롭 속도
    private static final int BLOCKS_FOR_LEVEL_UP = 10; // 블록 임계값
    private static final int LINES_FOR_LEVEL_UP = 10; // 삭제된 줄 임계값

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
                Thread.currentThread().interrupt();
                return;
            }
        }
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

            countBlock++;

            countClearLine += board.clearLines();

            increaseSpeed();

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
        int targetY = block.getY() + 1;
        if (board.isValidPosition(block.getShape(), block.getX(), targetY)) {
            block.moveDown();
        }
    }

    public synchronized void rotateAction(){
        int[][] rotatedShape = block.getRotate();
        if (board.isValidPosition(rotatedShape, block.getX(), block.getY())) {
            block.rotate();
        }
    }

    // 하강 속도 증가시키는 메서드
    private void increaseSpeed() {
        boolean levelUp = false;

        // 블록이 임계값만큼 생성되었을 때
        if (countBlock >= BLOCKS_FOR_LEVEL_UP) {
            levelUp = true;
            countBlock = 0;
        }

        // 줄이 임계값만큼 삭제되었을 때
        if (countClearLine >= LINES_FOR_LEVEL_UP) {
            levelUp = true;
            countClearLine = 0;
        }

        // 위 조건 중 한 개 이상의 조건을 만족하고, 제한 속도보다 느릴 때만 속도 증가
        if (levelUp && dropSpeed > MIN_DROP_SPEED) {
            dropSpeed -= SPEED_DECREASE_AMOUNT;
        }
    }
}