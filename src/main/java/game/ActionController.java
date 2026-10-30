package game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import blocks.core.Block;
import blocks.core.BlockFactory;
import blocks.core.BlockType;
import board.Board;
import difficulty.Difficulty;
import item.types.BombItem;
import item.types.BonusItem;
import item.types.LineClearItem;
import item.types.SlowItem;
import item.types.WeightItem;
import score.ScoreManager;
import score.DropType;
import ui.ItemAppearanceResolver;

// 블록 이동 클래스

public class ActionController {

    // 싱글에서는 자체 잠금, 대전 등록 후에는 양쪽이 경기 잠금을 공유한다.
    private volatile Object stateLock = this;
    private BattleController battle;
    private BattleController.Player player;
    public static final int MAX_PENDING_ATTACK_ROWS = 10;
    // 상대에게 받은 행. 현재 블록의 처리가 끝날 때까지 보드는 바꾸지 않는다.
    private final List<int[]> pendingAttackRows = new ArrayList<>();
    // 이번 블록으로 보낼 행의 삭제 전 모습. 애니메이션 완료 전에는 전달하지 않는다.
    private List<int[]> preparedAttackRows = List.of();

    public record ClearingCell(int row, int column) {}

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
    private volatile List<ClearingCell> clearingCells = List.of();
    private BombItem clearingBomb;
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

    public Block getCurrentBlock() {
        synchronized (stateLock) {
            return block;
        }
    }

    public Block getNextBlock() {
        synchronized (stateLock) {
            return nextBlock;
        }
    }

    public GameMode getMode() {
        return mode;
    }

    public boolean isClearing() {
        synchronized (stateLock) {
            return clearing;
        }
    }

    public List<Integer> getClearingRows() {
        return clearingRows;
    }

    public List<ClearingCell> getClearingCells() {
        return clearingCells;
    }

    // 게임 종료 시 예약된 줄 삭제 타이머 정리 + //FR-37
    public void shutdown() {
        synchronized (stateLock) {
            stopped = true;
            clearTimer.cancel();
            if (pendingClear != null) pendingClear.cancel();
            pendingClear = null;
            pendingAttackRows.clear();
            preparedAttackRows = List.of();
            clearingRows = List.of();
            clearingCells = List.of();
            clearingBomb = null;
            clearing = false;
        }
    }

    void attachBattle(BattleController battle, BattleController.Player player) {
        // 반드시 게임 실행 전에 등록한다. 실행 중 잠금 교체는 지원하지 않는다.
        if (this.battle != null || stopped || clearing || gameStateManager.getCountBlock() != 0
                || block.getX() != startX || block.getY() != startY) {
            throw new IllegalStateException("Register a fresh, unused player");
        }
        this.battle = Objects.requireNonNull(battle);
        this.player = Objects.requireNonNull(player);
        stateLock = battle;
    }

    public boolean isStopped() {
        synchronized (stateLock) {
            return stopped;
        }
    }
    public boolean isGameOver() {
        synchronized (stateLock) {
            return gameStateManager.isGameOver();
        }
    }
    public int getScore() {
        synchronized (stateLock) {
            return scoreManager.getScore();
        }
    }

    public int[][] getBoardSnapshot() {
        synchronized (stateLock) {
            // 외부에서 조회한 배열을 수정해도 실제 보드가 바뀌지 않도록 행까지 복사한다.
            return java.util.Arrays.stream(board.getBoard()).map(int[]::clone).toArray(int[][]::new);
        }
    }

    public int getPendingAttackCount() {
        synchronized (stateLock) {
            return pendingAttackRows.size();
        }
    }

    public List<int[]> getPendingAttackRows() {
        synchronized (stateLock) {
            return BattleController.copyAttackRows(pendingAttackRows);
        }
    }

    /** 공격을 대기열에만 저장하고 실제 수락한 행 수를 반환한다. */
    public int receiveAttackRows(List<int[]> rows) {
        synchronized (stateLock) {
            if (battle == null || !battle.isRunning() || stopped || gameStateManager.isGameOver()) return 0;
            List<int[]> copies = BattleController.copyAttackRows(rows);
            // 기존 대기 행을 먼저 유지한다. 8행 + 새 4행이면 새 행의 위쪽 2행만 받는다.
            int accepted = Math.min(MAX_PENDING_ATTACK_ROWS - pendingAttackRows.size(), copies.size());
            pendingAttackRows.addAll(copies.subList(0, accepted));
            return accepted;
        }
    }

    // 삭제 전 행을 복사하고 마지막 블록이 실제로 차지한 칸만 비운다.
    private void prepareAttackRows(List<Integer> rows) {
        preparedAttackRows = List.of();
        // 같은 블록에서 2행 이상 지운 경우만 공격한다. 이전 블록의 1행 삭제는 합산하지 않는다.
        if (battle == null || rows.size() < 2) return;
        List<int[]> attack = new ArrayList<>();
        int[][] shape = block.getShape();
        // L의 강제 삭제 행과 완성 행이 겹쳐도 한 번만 세고, 원래 위→아래 순서를 보존한다.
        for (int row : rows.stream().distinct().sorted().toList()) {
            int[] cells = new int[board.getWidth()];
            for (int col = 0; col < cells.length; col++) {
                int localRow = row - block.getY(), localCol = col - block.getX();
                // T/S/Z 등의 모양 배열에는 빈칸도 있다. 사각형 전체가 아닌 점유 칸만 비운다.
                boolean lastBlockCell = localRow >= 0 && localRow < shape.length
                        && localCol >= 0 && localCol < shape[localRow].length && shape[localRow][localCol] != 0;
                // 색상·P/S 문자 대신 공격 칸으로 저장해 상대에게 아이템 효과가 복제되지 않게 한다.
                if (board.getBoard()[row][col] != 0 && !lastBlockCell) cells[col] = BlockType.ATTACK.getValue();
            }
            attack.add(cells);
        }
        if (attack.size() >= 2) preparedAttackRows = attack;
    }

    private void markGameOver() {
        gameStateManager.setGameOver(true);
        // 개인의 게임 오버를 보고하면 경기 관리자가 상대 승리와 양쪽 정지를 처리한다.
        if (battle != null) battle.reportGameOver(player);
    }

    private boolean cannotAct() {
        return gameStateManager.isGameOver() || clearing || paused || stopped
                || (battle != null && !battle.isRunning());
    }

    public void setPaused(boolean paused) {
        synchronized (stateLock) {
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
    }

    private void scheduleLineClear(long delayMs) {
        remainingClearMs = delayMs;
        clearDeadlineNanos =
            System.nanoTime() + delayMs * 1_000_000L;

        pendingClear = new TimerTask() {
            @Override
            public void run() {
                synchronized (stateLock) {
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
    private void moveDownBlock() {
        synchronized (stateLock) {
            int x = block.getX();
            int y = block.getY();
            int nextY = y + 1;
            // 블록이 아래로 이동 가능한지 확인
            boolean canMoveDown = board.isValidPosition(block.getShape(), x, nextY);
            // 폭탄 아이템 처리
            // 블록이 BombItem이고, 더 이상 아래로 이동 불가 시
            if (block instanceof BombItem bombItem && !canMoveDown) {
                if (animationMs <= 0) {
                    explodeBomb(bombItem);
                } else {
                    clearingBomb = bombItem;
                    clearingCells = findBombClearingCells(bombItem);
                    clearing = true;
                    scheduleLineClear(animationMs);
                }
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
                // clearRows() 후에는 행이 내려오므로 공격 모양은 반드시 삭제 전에 확보한다.
                prepareAttackRows(rowsToClear);
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
    }

    // 애니메이션이 끝난 뒤 실제 줄 삭제 + 후처리
    void completeLineClear() {
        synchronized (stateLock) {
            if (!clearing || paused || stopped) {
                return;
            }
            if (pendingClear != null) {
                pendingClear.cancel();
                pendingClear = null;
            }
            if (clearingBomb != null) {
                BombItem bombItem = clearingBomb;
                clearingBomb = null;
                explodeBomb(bombItem);
                return;
            }
            int clearedLines = board.clearRows(clearingRows);
            clearingRows = List.of();
            clearing = false;
            finishLock(clearedLines);
        }
    }

    private List<ClearingCell> findBombClearingCells(BombItem bombItem) {
        int[][] cells = board.getBoard();
        int firstRow = Math.max(0, bombItem.getY() - 1);
        int lastRow = Math.min(cells.length - 1, bombItem.getY() + 1);
        int firstColumn = Math.max(0, bombItem.getX() - 1);
        int lastColumn = Math.min(board.getWidth() - 1, bombItem.getX() + 1);
        List<ClearingCell> targets = new ArrayList<>();

        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                if (cells[row][column] != 0
                        || (row == bombItem.getY() && column == bombItem.getX())) {
                    targets.add(new ClearingCell(row, column));
                }
            }
        }
        return List.copyOf(targets);
    }

    private void explodeBomb(BombItem bombItem) {
        // 폭탄은 블록 파괴 효과로만 취급한다. 이 처리에서 삭제되는 행은 공격으로 보내지 않는다.
        preparedAttackRows = List.of();
        bombItem.explode(board);
        // 기존 완성 행 삭제에 따른 점수·레벨·NEXT 처리는 유지한다.
        int clearedLines = board.clearLines();
        clearingCells = List.of();
        clearingRows = List.of();
        clearing = false;
        finishLock(clearedLines);
    }

    // 블록 한 개의 처리 경계: 삭제·점수·아이템 → 공격 전달/수신 반영 → NEXT 배치.
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
        // 대전에서는 수신한 완성 행 등으로 5줄 이상이 함께 삭제될 수 있다.
        // 실제 삭제/공격 줄 수는 보존하고 줄 삭제 기본 점수만 기존 4줄 상한을 사용한다.
        scoreManager.addLineClearScore(Math.min(clearedLines, 4), currentLevel, perfectClear);
        scoreManager.addBonusScore(bonusLinesCleared);
        gameStateManager.applySlowItem(slowLinesCleared);

        // 자신의 고정 결과가 이미 패배라면 공격/다음 블록을 더 처리하지 않는다.
        if (board.hasBlocksInHiddenRows()) {
            markGameOver();
            return;
        }
        if (battle != null) {
            // 실제 삭제가 끝났으므로 이번 공격을 확정한다. 취소된 삭제는 여기까지 오지 않는다.
            battle.publishAttack(player, preparedAttackRows);
            preparedAttackRows = List.of();
            // 방금 고정한 블록을 포함한 기존 보드 전체가 수신 행 수만큼 위로 밀린다.
            // 떨어지는 중에는 삽입하지 않고, NEXT 배치 전에 한 번에 반영한다.
            boolean overflow = board.insertAttackRows(pendingAttackRows);
            pendingAttackRows.clear();
            // 삽입으로 패배했다면 다음 블록을 생성하지 않는다.
            if (overflow || board.hasBlocksInHiddenRows()) {
                markGameOver();
                return;
            }
        }

        // 누적 줄 수가 10줄 단위를 넘으면 다음 블록으로 아이템을 생성
        spawnNextBlock(shouldSpawnItem);
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
            markGameOver();
        }
    }


    public void moveLeftAction() {
        synchronized (stateLock) {
            if(cannotAct()) {
                return;
            }
            int targetX = block.getX() - 1;
            if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
                block.moveLeft();
            }
        }
    }

    public void moveRightAction() {
        synchronized (stateLock) {
            if(cannotAct()) {
                return;
            }
            int targetX = block.getX() + 1;
            if (board.isValidPosition(block.getShape(), targetX, block.getY())) {
                block.moveRight();
            }
        }
    }

    public boolean moveDownAction() {
        synchronized (stateLock) {
            if(cannotAct()) {
                return false;
            }
            int currentY= block.getY();
            moveDownBlock();
            // 블록이 아래로 이동하면 true
            return block.getY()>currentY;
        }
    }

    public void rotateAction(){
        synchronized (stateLock) {
            if(cannotAct()) {
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
    }

    // 기존 호출부 호환용: 이동·고정과 거리 반환만 수행한다. 점수는 WithScore 진입점에서 처리한다.
    public int hardDropAction() {
        synchronized (stateLock) {
            return hardDrop(false);
        }
    }

    /** 입력·AI 등은 점수 처리가 포함된 이 진입점을 사용한다. */
    public int hardDropWithScoreAction() {
        synchronized (stateLock) {
            return hardDrop(true);
        }
    }

    public boolean softDropAction() {
        synchronized (stateLock) {
            return scoredDrop(DropType.SOFT);
        }
    }
    public boolean automaticDropAction() {
        synchronized (stateLock) {
            return scoredDrop(DropType.AUTO);
        }
    }

    private boolean scoredDrop(DropType type) {
        // 이동과 점수를 같은 잠금에서 처리해 경기 종료 뒤 늦게 점수가 붙는 상황을 막는다.
        if (cannotAct()) return false;
        boolean moved = moveDownAction();
        if (moved) scoreManager.addDropScore(1, type, gameStateManager.getCurrentLevel());
        return moved;
    }

    private int hardDrop(boolean awardScore) {
        if (cannotAct()) {
            return 0;
        }
        int dropDistance = 0;
        while (board.isValidPosition(block.getShape(), block.getX(), block.getY() + 1)) {
            block.moveDown();
            dropDistance++;
        }
        // 고정이 경기 종료를 확정하기 전에 마지막 이동 점수까지 반영한다.
        if (awardScore) scoreManager.addDropScore(dropDistance, DropType.HARD, gameStateManager.getCurrentLevel());
        moveDownBlock(); // 블록을 고정하고 새로운 블록 생성
        return dropDistance;
    }
}
