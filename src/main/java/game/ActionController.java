package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import blocks.core.Block;
import blocks.core.BlockFactory;
import item.types.BombItem;
import item.types.LineClearItem;
import item.types.WeightItem;
import item.types.BonusItem;
import item.types.SlowItem;
import board.Board;
import score.ScoreManager;
import difficulty.Difficulty;
import ui.ItemAppearanceResolver;

// 블록 이동 클래스

public class ActionController {

    private Board board;
    private Block block;
    private Block nextBlock;
    private GameStateManager gameStateManager;
    private ScoreManager scoreManager;
    private final Difficulty difficulty;
    private final GameMode mode;

    private int startX = 3;
    private int startY = 0;

     // 줄 삭제 애니메이션
    public static final int CLEAR_ANIMATION_MS = 300;
    private final int animationMs;
    private volatile List<Integer> clearingRows = List.of(); // 깜빡이는 중인 줄
    private boolean clearing = false;
    private final Timer clearTimer = new Timer("line-clear", true);
    private TimerTask pendingClear;
    //FR-37
    private boolean paused;
    private boolean stopped;
    private long clearDeadlineNanos;
    private long remainingClearMs;

    public ActionController(Board board, Block block, GameStateManager gameStateManager,
                            ScoreManager scoreManager, Difficulty difficulty, GameMode mode) {
        this(board, block, gameStateManager, scoreManager, difficulty, mode, CLEAR_ANIMATION_MS);
    }

    public ActionController(Board board, Block block, GameStateManager gameStateManager,
                            ScoreManager scoreManager, Difficulty difficulty, GameMode mode,
                            int animationMs) {
        this.board = board;
        this.block = block;
        this.mode = mode;
        this.difficulty = difficulty;
        this.animationMs = animationMs;
        this.nextBlock = BlockFactory.createRandomBlock(difficulty);
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

    public GameMode getMode() {
        return mode;
    }

    public synchronized boolean isClearing() {
        return clearing;
    }

    public List<Integer> getClearingRows() {
        return clearingRows;
    }

    // 게임 종료 시 예약된 줄 삭제 타이머 정리 + //FR-37
    public synchronized void shutdown() {
        stopped = true;
        clearTimer.cancel();
    }

    public synchronized void setPaused(boolean paused) {
        if (stopped || this.paused == paused) {
            return;
        }

        this.paused = paused;

        if (!clearing) {
            return;
        }

        if (paused) {
            remainingClearMs = Math.max(
                1L,
                (clearDeadlineNanos - System.nanoTime() + 999_999L)
                    / 1_000_000L
            );

            if (pendingClear != null) {
                pendingClear.cancel();
                pendingClear = null;
            }
        } else {
            scheduleLineClear(remainingClearMs);
        }
    }

    private void scheduleLineClear(long delayMs) {
        remainingClearMs = delayMs;
        clearDeadlineNanos =
            System.nanoTime() + delayMs * 1_000_000L;

        pendingClear = new TimerTask() {
            @Override
            public void run() {
                synchronized (ActionController.this) {
                    // 취소된 이전 예약이 실행되는 경우 무시
                    if (pendingClear == this) {
                        completeLineClear();
                    }
                }
            }
        };

        clearTimer.schedule(pendingClear, delayMs);
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
            finishLock(clearedLines);
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
            SlowItem slowItem = block instanceof SlowItem item ? item : null;

            // 블록 모양대로 색상 주입
            for (int i = 0; i < rawShape.length; i++) {
                for (int j = 0; j < rawShape[i].length; j++) {
                    if(rawShape[i][j] != 0) {
                        // LineClearItem이면 L 셀인지 확인 후 색상 주입
                        if (bonusItem != null || slowItem != null) {
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

            // 무게추 - 바닥에 닿은 경우, 블록과 충돌 후 아랫줄 삭제 뒤 처리
            if(block instanceof WeightItem weightItem) {
                weightItem.markLanded();
            }

            // 삭제 대상 줄 = 꽉 찬 줄 + L 아이템이 있는 줄 (L 줄이 꽉 차 있어도 중복 집계 안 함)
            List<Integer> rowsToClear = new ArrayList<>(board.findFullRows());
            if (lineClearItem != null && !rowsToClear.contains(lineClearItem.getLRow())){
                    rowsToClear.add(lineClearItem.getLRow());
            }

            if (rowsToClear.isEmpty()) {
                finishLock(0);
            } else if (animationMs <= 0) {
                finishLock(board.clearRows(rowsToClear));
            } else {
                // 삭제 대상 줄을 먼저 보여주고 animationMs 뒤에 실제로 삭제
                clearingRows = List.copyOf(rowsToClear);
                clearing = true;
                scheduleLineClear(animationMs);
            }
        }
    }

    // 애니메이션이 끝난 뒤 실제 줄 삭제 + 후처리
    synchronized void completeLineClear() {
        if (!clearing || paused || stopped) {
            return;
        }
        if (pendingClear != null) {
            pendingClear.cancel();
            pendingClear = null;
        }
        int clearedLines = board.clearRows(clearingRows);
        clearingRows = List.of();
        clearing = false;
        finishLock(clearedLines);
    }

    // 줄 삭제 이후 레벨, 점수, 다음 블록 처리
    private void finishLock(int clearedLines){
        // 줄 삭제가 없으면 보너스/슬로우 마커가 지워진 줄도 없음
        int bonusLinesCleared = clearedLines > 0 ? board.getLastClearedBonusLines() : 0;
        int slowLinesCleared = clearedLines > 0 ? board.getLastClearedSlowLines() : 0;

        int previousTotalLines = gameStateManager.getTotalLinesCleared();
        gameStateManager.updateLevelUp(clearedLines);
        boolean shouldSpawnItem = mode == GameMode.ITEM
                && gameStateManager.shouldSpawnItem(previousTotalLines);

        // 점수 계산
        int currentLevel = gameStateManager.getCurrentLevel();
        boolean perfectClear = board.isPerfectClear();
        scoreManager.addLineClearScore(clearedLines, currentLevel, perfectClear);
        scoreManager.addBonusScore(bonusLinesCleared);
        gameStateManager.applySlowItem(slowLinesCleared);

        // 여유 공간에 블록이 고정되면 게임 오버
        if (board.hasBlocksInHiddenRows()) {
            gameStateManager.setGameOver(true);
        } else {
            // 누적 줄 수가 10줄 단위를 넘으면 다음 블록으로 아이템을 생성
            spawnNextBlock(shouldSpawnItem);
        }
    }
    // 다음 블록 또는 아이템을 생성하는 메서드 (아이템은 미리보기에 먼저 표시됨)
    private void spawnNextBlock(boolean shouldSpawnItem) {
        block = nextBlock;
        if (mode == GameMode.ITEM && shouldSpawnItem) {
            nextBlock = BlockFactory.createRandomItem(difficulty);
        } else {
            nextBlock = BlockFactory.createRandomBlock(difficulty);
        }
        block.setX(startX);
        block.setY(startY);
        // 새 블록을 시작 위치에 배치 못하면? -> gameover
        if (!board.isValidPosition(block.getShape(), startX, startY)) {
            gameStateManager.setGameOver(true);
        }
    }


    public synchronized void moveLeftAction() {
        if(gameStateManager.isGameOver() || clearing || paused || stopped) {
            return;
        }

        int targetX = block.getX() - 1;
        if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
            block.moveLeft();
        }
    }

    public synchronized void moveRightAction() {
        if(gameStateManager.isGameOver() || clearing || paused || stopped) {
            return;
        }

        int targetX = block.getX() + 1;
        if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
            block.moveRight();
        }
    }

    public synchronized boolean moveDownAction() {
        if(gameStateManager.isGameOver() || clearing || paused || stopped) {
            return false;
        }
        int currentY= block.getY();
        moveDownBlock();
        // 블록이 아래로 이동하면 true
        return block.getY()>currentY;
    }

    public synchronized void rotateAction(){
        if(gameStateManager.isGameOver() || clearing || paused || stopped) {
            return;
        }

        int[][] rotatedShape = block.getRotate();
        int[] horizontalKicks = {0, -1, 1, -2, 2};
        for (int kick : horizontalKicks) {
            int targetX = block.getX() + kick;
            if (board.isValidPosition(rotatedShape, targetX, block.getY())) {
                block.rotate();
                block.setX(targetX);
                return;
            }
        }
    }

    // 하드드롭
    public synchronized int hardDropAction() {
        if (gameStateManager.isGameOver() || clearing || paused || stopped) {
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
