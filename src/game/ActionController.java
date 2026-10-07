package game;

import blocks.core.Block;
import blocks.core.BlockFactory;
import item.LineClearItem;
import board.Board;
import score.ScoreManager;

// 블록 이동 클래스

public class ActionController {

    private Board board;
    private Block block;
    private Block nextBlock;
    private GameStateManager gameStateManager;
    private ScoreManager scoreManager;

    private int startX = 3;
    private int startY = 0;

    public ActionController(Board board, Block block, GameStateManager gameStateManager, ScoreManager scoreManager) {
        this.board = board;
        this.block = block;
        this.nextBlock = BlockFactory.createRandomBlock();
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;
        this.block.setX(startX);
        this.block.setY(startY);

        if (!board.isValidPosition(block.getShape(), startX, startY)) {
            gameStateManager.setGameOver(true);
        }
    }
    
    public synchronized Block getCurrentBlock() {
        return block;
    }

    public synchronized Block getNextBlock() {
        return nextBlock;
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
            // 블록이 LineClearItem인지 확인
            LineClearItem lineClearItem = block instanceof LineClearItem item ? item : null;
            
            // 블록 모양대로 색상 주입
            for (int i = 0; i < rawShape.length; i++) {
                for (int j = 0; j < rawShape[i].length; j++) {
                    if(rawShape[i][j] != 0) {
                        // LineClearItem이면 L 셀인지 확인 후 색상 주입
                        if (lineClearItem == null) {
                            colorShape[i][j] = blockValue;
                        } else if (lineClearItem.isMarkerCell(i, j)) { // L 셀엔 임의 숫자 입력
                            colorShape[i][j] = LineClearItem.L_CELL_VALUE;
                        } else { // 그 외의 셀엔 원래 값 입력
                            colorShape[i][j] = lineClearItem.getSourceType().getValue();
                        }
                    }
                }
            }
            
            // 블록을 색상 정보와 함께 보드에 고정
            board.addBlock(x, y, colorShape);
            
            // 아이템 효과를 먼저 적용한 뒤 일반적인 꽉 찬 줄을 삭제
            int clearedLines = 0;
            if (lineClearItem != null) {
                lineClearItem.activate(board);
                clearedLines++;
            }
            clearedLines += board.clearLines();
            int previousTotalLines = gameStateManager.getTotalLinesCleared();
            gameStateManager.updateLevelUp(clearedLines);
            boolean shouldSpawnItem = gameStateManager.shouldSpawnItem(previousTotalLines);

            int currentLevel = gameStateManager.getCurrentLevel();

            boolean perfectClear = board.isPerfectClear();

            scoreManager.addLineClearScore(clearedLines, currentLevel, perfectClear);

            // 누적 줄 수가 10줄 단위를 넘으면 다음 블록 대신 아이템을 생성
            if (shouldSpawnItem) {
// --------------Todo: 블록 외형 결정 시 수정
                //block = BlockFactory.createRandomLineClearItem();
                nextBlock = BlockFactory.createRandomBlock();
            } else {
                block = nextBlock;
                nextBlock = BlockFactory.createRandomBlock();
            }
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

    public synchronized boolean moveDownAction() {
        if(gameStateManager.isGameOver()) {
            return false;
        }
        int currentY= block.getY();
        moveDownBlock();
        // 블록이 아래로 이동하면 true
        return block.getY()>currentY;
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
