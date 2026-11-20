package game;

import blocks.core.Block;
import blocks.core.BlockType;
import board.Board;
import item.types.WeightItem;
import ui.ItemAppearanceResolver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

public final class AiPlayer implements AutoCloseable {
    public enum Move { LEFT, RIGHT, ROTATE, HARD_DROP, HOLD, WAIT }

    public static final int DEFAULT_INPUT_INTERVAL_MILLIS = 500;
    private static final int[] ROTATION_KICKS = {0, -1, 1, -2, 2};
    private static final int LOOKAHEAD_CANDIDATES = 10;
    private static final int TOP_OUT_COST = 100_000;
    private static final int MIN_HOLD_GAIN = 45;
    private static final int I_BLOCK_RESERVE_COST = 20;
    //다음 블록 생성 위치.
    private static final int SPAWN_X = 3;
    private static final int SPAWN_Y = 0;

    private final BattleController battle;
    private final BattleController.Player player;
    private final ActionController action;
    private final int inputIntervalMillis;
    private final IntSupplier dropIntervalMillis;
    private ScheduledExecutorService worker;
    private ScheduledFuture<?> scheduledInput;
    private boolean closed;
    private boolean stepping;
    private Move lastMove = Move.WAIT;

    public AiPlayer(LocalBattleSession session, BattleController.Player player) {
        this(session, player, DEFAULT_INPUT_INTERVAL_MILLIS);
    }

    public AiPlayer(LocalBattleSession session, BattleController.Player player, int inputIntervalMillis) {
        this(session.getBattleController(), player, session.getPlayer(player).action(), inputIntervalMillis,
                session.getPlayer(player).state()::getDropSpeed);
    }

    AiPlayer(BattleController battle, BattleController.Player player, ActionController action) {
        this(battle, player, action, DEFAULT_INPUT_INTERVAL_MILLIS, () -> 1_000);
    }

    private AiPlayer(BattleController battle, BattleController.Player player, ActionController action,
                     int inputIntervalMillis, IntSupplier dropIntervalMillis) {
        if (inputIntervalMillis < 500 || inputIntervalMillis > 1000) {
            throw new IllegalArgumentException("AI input interval must be between 500 and 1000 ms");
        }
        this.battle = Objects.requireNonNull(battle);
        this.player = Objects.requireNonNull(player);
        this.action = Objects.requireNonNull(action);
        this.inputIntervalMillis = inputIntervalMillis;
        this.dropIntervalMillis = dropIntervalMillis;
    }

    public BattleController.Player getPlayer() {
        return player;
    }

    public int getInputIntervalMillis() {
        return inputIntervalMillis;
    }

    //마지막 조작을 반환. 시작 전과 종료 후에는 대기 상태.
    public Move getLastMove() {
        synchronized (battle) {
            return lastMove;
        }
    }

    //자동 입력 등록 여부 확인. 일시정지 중에도 등록 유지.
    public boolean isRunning() {
        synchronized (battle) {
            return !closed && battle.isRunning() && scheduledInput != null && !scheduledInput.isDone();
        }
    }

    //경기 시작 후 자동 입력을 한 번만 등록.
    public void start() {
        synchronized (battle) {
            if (closed || !battle.isRunning()) {
                throw new IllegalStateException("Start the match before starting an open AI player");
            }
            if (worker != null) return;
            worker = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "battle-ai-" + player.name());
                thread.setDaemon(true);
                return thread;
            });
            scheduledInput = worker.scheduleWithFixedDelay(this::step, inputIntervalMillis,
                    inputIntervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    //현재 보드를 보고 조작을 최대 한 번 실행.
    public Move step() {
        ActionController.ViewSnapshot view;
        Block block;
        int[][] originalShape;
        long pauseRevision;
        int gravitySteps;
        boolean movementLocked;
        boolean holdAvailable;
        synchronized (battle) {
            if (!battle.isRunning()) {
                if (worker != null) close();
                return Move.WAIT;
            }
            if (closed || stepping || action.isPaused() || action.isStopped()
                    || action.isGameOver() || action.isClearing()) {
                return Move.WAIT;
            }
            view = action.getViewSnapshot();
            block = action.getCurrentBlock();
            originalShape = block.getShape();
            pauseRevision = action.pauseRevision();
            movementLocked = block instanceof WeightItem weight && weight.isLanded();
            holdAvailable = action.canHold();
            gravitySteps = Math.ceilDiv(inputIntervalMillis, dropIntervalMillis.getAsInt());
            stepping = true;
        }

        try {
            Move move = chooseMove(view, movementLocked, gravitySteps, holdAvailable);
            synchronized (battle) {
                //판단 도중 경기 상태가 바뀌면 입력 생략.
                if (closed || !battle.isRunning() || action.isPaused() || action.isStopped()
                        || action.isGameOver() || action.isClearing()
                        || action.pauseRevision() != pauseRevision || action.getCurrentBlock() != block
                        || block.getX() != view.currentBlock().x() || block.getY() != view.currentBlock().y()
                        || block.getShape() != originalShape
                        || action.getPendingAttackCount() != view.pendingAttackRows().size()) {
                    return Move.WAIT;
                }
                switch (move) {
                    case LEFT -> action.moveLeftAction();
                    case RIGHT -> action.moveRightAction();
                    case ROTATE -> action.rotateAction();
                    case HARD_DROP -> action.hardDropWithScoreAction();
                    case HOLD -> {
                        if (!action.holdAction()) return Move.WAIT;
                    }
                    case WAIT -> { }
                }
                lastMove = move;
                return move;
            }
        } finally {
            synchronized (battle) {
                stepping = false;
            }
        }
    }

    @Override
    public void close() {
        synchronized (battle) {
            if (closed) return;
            closed = true;
            lastMove = Move.WAIT;
            if (scheduledInput != null) scheduledInput.cancel(false);
            if (worker != null) worker.shutdown();
        }
    }

    //보드 복사본에서 다음 조작을 선택.
    static Move chooseMove(ActionController.ViewSnapshot view) {
        return chooseMove(view, 0);
    }

    static Move chooseMove(ActionController.ViewSnapshot view, int gravitySteps) {
        return chooseMove(view, gravitySteps, false);
    }

    static Move chooseMove(ActionController.ViewSnapshot view, int gravitySteps, boolean holdAvailable) {
        return chooseMove(view, false, gravitySteps, holdAvailable);
    }

    private static Move chooseMove(ActionController.ViewSnapshot view, boolean movementLocked, int gravitySteps,
                                   boolean holdAvailable) {
        ActionController.BlockSnapshot block = view.currentBlock();
        if (view.paused() || view.stopped() || view.gameOver() || block == null
                || !view.clearingRows().isEmpty() || !view.clearingCells().isEmpty()) {
            return Move.WAIT;
        }

        Board board = copyBoard(view.board());
        List<int[]> attackRows = view.pendingAttackRows();
        List<Candidate> candidates = findPlacements(board, block, movementLocked, attackRows, gravitySteps);
        if (candidates.isEmpty()) return Move.WAIT;
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());

        ActionController.BlockSnapshot preview = view.nextBlock();
        Candidate bestImmediate = candidates.getFirst();
        ActionController.BlockSnapshot replacement = view.heldBlock() == null ? preview : view.heldBlock();
        if (holdAvailable && !movementLocked && replacement != null
                && shouldHold(board, block, replacement, bestImmediate, attackRows, gravitySteps)) {
            return Move.HOLD;
        }
        if (preview == null || movementLocked) return bestImmediate.move();
        ActionController.BlockSnapshot next = new ActionController.BlockSnapshot(SPAWN_X, SPAWN_Y, preview.cells());
        int bestScore = Integer.MIN_VALUE;
        Move bestMove = bestImmediate.move();
        for (Candidate candidate : candidates.subList(0, Math.min(candidates.size(), LOOKAHEAD_CANDIDATES))) {
            if (candidate.topOut()) break;
            List<Candidate> continuations = findPlacements(candidate.board(), next, false, List.of(), gravitySteps);
            int futureScore = continuations.stream().mapToInt(Candidate::score).max().orElse(-TOP_OUT_COST);
            //다음 블록의 효과는 낮은 비중으로 반영.
            int score = 4 * candidate.score() + 3 * (futureScore + candidate.surfaceCost());
            if (score > bestScore) {
                bestScore = score;
                bestMove = candidate.move();
            }
        }
        return bestMove;
    }

    private static boolean shouldHold(Board board, ActionController.BlockSnapshot current,
                                      ActionController.BlockSnapshot replacement, Candidate keep,
                                      List<int[]> attackRows, int gravitySteps) {
        //빈 홀드는 다음 블록을 당겨 오므로 양쪽 모두 한 블록만 비교.
        int[][] shape = replacement.cells();
        if (!board.isValidPosition(shape, SPAWN_X, SPAWN_Y)) return false;
        int fallen = 0;
        while (fallen < gravitySteps && board.isValidPosition(shape, SPAWN_X, SPAWN_Y + fallen + 1)) fallen++;
        ActionController.BlockSnapshot incoming = new ActionController.BlockSnapshot(SPAWN_X, SPAWN_Y + fallen,
                shape);
        Candidate swap = findPlacements(board, incoming, fallen < gravitySteps, attackRows, gravitySteps)
                .stream().max(Comparator.comparingInt(Candidate::score)).orElse(null);
        if (swap == null || swap.topOut()) return false;
        if (keep.topOut() || swap.score() >= keep.score() + MIN_HOLD_GAIN) return true;

        //지금 줄을 못 지우고 교체 블록이 안전하면 일자 블록을 보관.
        return keep.clearedLines() == 0 && isPlainI(current.cells()) && !isPlainI(shape)
                && swap.score() >= keep.score() - I_BLOCK_RESERVE_COST;
    }

    private static boolean isPlainI(int[][] shape) {
        return Arrays.stream(shape).flatMapToInt(Arrays::stream).filter(cell -> cell != 0)
                .allMatch(cell -> cell == BlockType.I.getValue());
    }

    private static List<Candidate> findPlacements(Board board, ActionController.BlockSnapshot block,
                                                  boolean movementLocked, List<int[]> attackRows, int gravitySteps) {
        int[][] shape = block.cells();
        int y = block.y();
        boolean weight = containsSymbol(shape, 'W');
        boolean bomb = containsSymbol(shape, 'B');
        if (!board.isValidPosition(shape, block.x(), y)) return List.of();

        List<int[][]> orientations = new ArrayList<>();
        orientations.add(shape);
        if (!weight && !bomb) {
            for (int[][] rotated = rotate(shape); !Arrays.deepEquals(rotated, shape); rotated = rotate(rotated)) {
                orientations.add(rotated);
            }
        }

        //다음 입력 전까지의 자동 낙하를 고려.
        ArrayDeque<Placement> pending = new ArrayDeque<>();
        Set<Position> seen = new HashSet<>();
        Position current = new Position(block.x(), 0);
        pending.add(new Placement(current, y, Move.HARD_DROP, 0, false));
        seen.add(current);
        List<Candidate> candidates = new ArrayList<>();

        while (!pending.isEmpty()) {
            Placement placement = pending.remove();
            Position position = placement.position();
            int[][] oriented = orientations.get(position.rotation());
            candidates.add(evaluate(board.getBoard(), oriented, placement, weight, bomb, attackRows));
            if (movementLocked || placement.gravityLocked()) continue;

            for (Move move : List.of(Move.LEFT, Move.RIGHT)) {
                int x = position.x() + (move == Move.LEFT ? -1 : 1);
                if (board.isValidPosition(oriented, x, placement.y())) {
                    enqueue(board, pending, seen, placement, new Position(x, position.rotation()),
                            oriented, move, gravitySteps);
                }
            }

            if (orientations.size() > 1) {
                int rotation = (position.rotation() + 1) % orientations.size();
                for (int kick : ROTATION_KICKS) {
                    int x = position.x() + kick;
                    int[][] rotated = orientations.get(rotation);
                    if (board.isValidPosition(rotated, x, placement.y())) {
                        enqueue(board, pending, seen, placement, new Position(x, rotation),
                                rotated, Move.ROTATE, gravitySteps);
                        break;
                    }
                }
            }
        }
        return candidates;
    }

    private static void enqueue(Board board, ArrayDeque<Placement> pending, Set<Position> seen, Placement from,
                                Position target, int[][] shape, Move move, int gravitySteps) {
        if (!seen.add(target)) return;
        int fallen = 0;
        while (fallen < gravitySteps && board.isValidPosition(shape, target.x(), from.y() + fallen + 1)) fallen++;
        Move first = from.steps() == 0 ? move : from.firstMove();
        pending.add(new Placement(target, from.y() + fallen, first, from.steps() + 1, fallen < gravitySteps));
    }

    private static Candidate evaluate(int[][] cells, int[][] shape, Placement placement,
                                      boolean weight, boolean bomb, List<int[]> attackRows) {
        int x = placement.position().x();
        int y = placement.y();
        Board simulated = copyBoard(cells);
        while (true) {
            while (simulated.isValidPosition(shape, x, y + 1)) y++;
            if (!weight || !simulated.hasBlockCollision(shape, x, y + 1)) break;
            simulated.clearOneRowBelow(x, y + shape.length - 1, shape[0].length);
            if (!simulated.isValidPosition(shape, x, y + 1)) break;
            y++;
        }
        List<Integer> clearedRows;
        if (bomb) {
            simulated.clearArea(x, y, 3, 3);
            clearedRows = simulated.findFullRows();
        } else {
            simulated.addBlock(x, y, shape);
            clearedRows = new ArrayList<>(simulated.findFullRows());
            for (int row = 0; row < shape.length; row++) {
                int targetRow = y + row;
                if (targetRow >= 0 && containsSymbol(new int[][]{shape[row]}, 'L')
                        && !clearedRows.contains(targetRow)) {
                    clearedRows.add(targetRow);
                }
            }
        }
        int cleared = simulated.clearRows(clearedRows);
        boolean topOut = simulated.hasBlocksInHiddenRows();
        if (!attackRows.isEmpty()) {
            topOut |= simulated.insertAttackRows(attackRows) || simulated.hasBlocksInHiddenRows();
        }
        int reward = 100 * cleared + (!bomb && cleared >= 2 ? 28 * cleared : 0);
        int surfaceCost = surfaceCost(simulated.getBoard());
        int score = reward - surfaceCost - 4 * placement.steps() - (topOut ? TOP_OUT_COST : 0);
        return new Candidate(placement.firstMove(), simulated, surfaceCost, score, topOut, cleared);
    }

    private static int surfaceCost(int[][] cells) {
        int cost = 0;
        int previousHeight = 0;
        int maxHeight = 0;
        for (int column = 0; column < cells[0].length; column++) {
            int height = 0;
            int coveringCells = 0;
            for (int row = 0; row < cells.length; row++) {
                if (cells[row][column] != 0) {
                    if (height == 0) height = cells.length - row;
                    coveringCells++;
                } else if (coveringCells > 0) {
                    //위를 덮은 블록이 많을수록 감점 증가.
                    cost += 65 + 6 * coveringCells;
                }
            }
            cost += 4 * height;
            if (column > 0) cost += 5 * Math.abs(height - previousHeight);
            maxHeight = Math.max(maxHeight, height);
            previousHeight = height;
        }
        return cost + 5 * maxHeight;
    }

    private static boolean containsSymbol(int[][] shape, char symbol) {
        for (int[] row : shape) {
            for (int cell : row) {
                if (cell != 0 && ItemAppearanceResolver.fromBoardCell(cell).symbol() == symbol) return true;
            }
        }
        return false;
    }

    private static Board copyBoard(int[][] cells) {
        Board copy = new Board();
        for (int row = 0; row < cells.length; row++) {
            System.arraycopy(cells[row], 0, copy.getBoard()[row], 0, cells[row].length);
        }
        return copy;
    }

    private static int[][] rotate(int[][] shape) {
        int[][] rotated = new int[shape[0].length][shape.length];
        for (int row = 0; row < shape.length; row++) {
            for (int column = 0; column < shape[row].length; column++) {
                rotated[column][shape.length - 1 - row] = shape[row][column];
            }
        }
        return rotated;
    }

    private record Position(int x, int rotation) { }
    private record Placement(Position position, int y, Move firstMove, int steps, boolean gravityLocked) { }
    private record Candidate(Move move, Board board, int surfaceCost, int score, boolean topOut, int clearedLines) { }
}
