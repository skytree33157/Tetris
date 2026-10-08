package game;

import blocks.core.Block;
import blocks.core.BlockFactory;
import item.types.LineClearItem;
import item.types.WeightItem;
import item.types.BombItem;
import item.types.BonusItem;
import board.Board;
import score.ScoreManager;
import ui.ItemAppearanceResolver;

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

        // 블록이 아래로 이동 가능한지 확인
        boolean canMoveDown = board.isValidPosition(block.getShape(), x, nextY);

        // 폭탄 아이템 처리
        // 블록이 BombItem이고, 더 이상 아래로 이동 불가 시
        if (block instanceof BombItem bombItem && !canMoveDown) {

            // 폭탄 발동
            bombItem.explode(board);
            // 폭탄 발동 후 line clear
            int clearedLines = board.clearLines();
            int bonusLinesCleared = board.getLastClearedBonusLines();
            int previousTotalLines = gameStateManager.getTotalLinesCleared();
            gameStateManager.updateLevelUp(clearedLines);
            // 10줄 단위로 아이템 생성
            boolean shouldSpawnItem = gameStateManager.shouldSpawnItem(previousTotalLines);
            // 점수 계산
            int currentLevel = gameStateManager.getCurrentLevel();
            scoreManager.addLineClearScore(clearedLines, currentLevel, board.isPerfectClear());
            scoreManager.addBonusScore(bonusLinesCleared);
            spawnNextBlock(shouldSpawnItem);
            return;
        }

        // 무게추 아이템 처리
        // 블록이 WeightItem이고, 더 이상 아래로 이동 불가, 기존 블록과 충돌
        if (block instanceof WeightItem weightItem
                && !canMoveDown
                && board.hasBlockCollision(block.getShape(), x, nextY)) {
            // 고정 블록에 닿은 순간부터 좌우 이동 금지, 바로 아래 한 행 제거
            weightItem.markLanded();
            weightItem.clearBlocksBelow(board);
            canMoveDown = board.isValidPosition(block.getShape(), x, nextY);
        }

        if (canMoveDown) {
            block.moveDown();
        } else { // 블록이 더 이상 내려갈 수 없으면 현재 위치에 블록을 고정 후 새로운 블록 생성
            // rawShape : 블록 모양
            int[][] rawShape = block.getShape();
            // colorShape : 블록 모양에 색상 정보를 추가한 배열
            int[][] colorShape = new int[rawShape.length][rawShape[0].length];
            int blockValue = block.getType().getValue();
            // 블록이 LineClearItem인지 확인
            LineClearItem lineClearItem = block instanceof LineClearItem item ? item : null;
            BonusItem bonusItem = block instanceof BonusItem item ? item : null;
            
            // 블록 모양대로 색상 주입
            for (int i = 0; i < rawShape.length; i++) {
                for (int j = 0; j < rawShape[i].length; j++) {
                    if(rawShape[i][j] != 0) {
                        // LineClearItem이면 L 셀인지 확인 후 색상 주입
                        if (bonusItem != null) {
                            colorShape[i][j] = ItemAppearanceResolver.toBoardCell(block, i, j);
                        } else if (lineClearItem == null) {
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
            int bonusLinesCleared = 0;
            if (lineClearItem != null) {
                lineClearItem.activate(board);
                clearedLines++;
                bonusLinesCleared += board.getLastClearedBonusLines();
            }

            // 무게추 - 바닥에 닿은 경우, 블록과 충돌 후 아랫줄 삭제 뒤 처리
            if (block instanceof WeightItem weightItem) {
                weightItem.markLanded();
            }

            // 꽉 찬 줄 제거 후 제거된 line 수 반환
            clearedLines += board.clearLines();
            bonusLinesCleared += board.getLastClearedBonusLines();
            int previousTotalLines = gameStateManager.getTotalLinesCleared();
            gameStateManager.updateLevelUp(clearedLines);
            boolean shouldSpawnItem = gameStateManager.shouldSpawnItem(previousTotalLines);

            // 점수 계산
            int currentLevel = gameStateManager.getCurrentLevel();
            boolean perfectClear = board.isPerfectClear();
            scoreManager.addLineClearScore(clearedLines, currentLevel, perfectClear);
            scoreManager.addBonusScore(bonusLinesCleared);

            // 누적 줄 수가 10줄 단위를 넘으면 다음 블록 대신 아이템을 생성
            spawnNextBlock(shouldSpawnItem);
        }
    }

    // 다음 블록 또는 아이템을 생성하는 메서드
    private void spawnNextBlock(boolean shouldSpawnItem) {
        if (shouldSpawnItem) {
// Todo : 아이템 생성 로직 추가 후 변경 예정
            block = BlockFactory.createRandomItem();
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
