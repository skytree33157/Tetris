package game;

import blocks.core.Block;
import blocks.core.BlockFactory;
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

        if (!board.isValidPosition(block.getShape(), startX, startY)) {
            gameStateManager.setGameOver(true);
        }
    }

    // 블록을 아래로 이동시키고 보드에 고정시키는 메서드
    private synchronized void moveDownBlock() {
        int x = block.getX();
        int y = block.getY();
        int nextY = y + 1;
        
        // 다음 위치가 유효한지 확인
        if (board.isValidPosition(block.getShape(),x, nextY)) {
            block.moveDown();
        } else { // 블록이 더 이상 내려갈 수 없으면 현재 위치에 블록을 고정 후 새로운 블록 생성
            // rawShape : 블록 모양
            int[][] rawShape = block.getShape();
            // colorShape : 블록 모양에 색상 정보를 추가한 배열
            int[][] colorShape = new int[rawShape.length][rawShape[0].length];
            int blockValue = block.getType().getValue();
            
            // 블록 모양대로 색상 주입
            for (int i = 0; i < rawShape.length; i++) {
                for (int j = 0; j < rawShape[i].length; j++) {
                    if(rawShape[i][j] != 0) {
                        colorShape[i][j] = blockValue;
                    }
                }
            }
            
            // 블록을 색상 정보와 함께 보드에 고정
            board.addBlock(x, y, colorShape);
            
            // 줄 삭제 및 레벨 업(하강 속도 증가)
            int clearedLines = board.clearLines();
            gameStateManager.updateLevelUp(clearedLines);

            // 새 블록 생성
            block = BlockFactory.createRandomBlock();
            block.setX(startX);
            block.setY(startY);

            // 새 블록을 시작 위치에 배치 못하면? -> gameover
            if (!board.isValidPosition(block.getShape(), startX, startY)) {
                gameStateManager.setGameOver(true);
            }
        }
    }

    public synchronized void moveLeftAction() {
        if(gameStateManager.isGameOver()) {
            return;
        }

        int targetX = block.getX() - 1;
        if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
            block.moveLeft();
        }
    }

    public synchronized void moveRightAction() {
        if(gameStateManager.isGameOver()) {
            return;
        }
        
        int targetX = block.getX() + 1;
        if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
            block.moveRight();
        }
    }

    public synchronized void moveDownAction() {
        if(gameStateManager.isGameOver()) {
            return;
        }

        moveDownBlock();
    }

    public synchronized void rotateAction(){
        if(gameStateManager.isGameOver()) {
            return;
        }

        int[][] rotatedShape = block.getRotate();
        if (board.isValidPosition(rotatedShape, block.getX(), block.getY())) {
            block.rotate();
        }
    }

    // 하드드롭
    public synchronized int hardDropAction() {
        if (gameStateManager.isGameOver()) {
            return 0;
        }
        int dropDistance = 0;
        while (board.isValidPosition(block.getShape(), block.getX(), block.getY() + 1)) {
            block.moveDown();
            dropDistance++;
        }
        moveDownBlock(); // 블록을 고정하고 새로운 블록 생성 -> moveDownBlock()의 else 실행
        return dropDistance;
    }
}
