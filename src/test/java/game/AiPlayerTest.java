package game;

import static org.junit.jupiter.api.Assertions.*;

import blocks.core.Block;
import blocks.tetromino.IBlock;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import item.types.BombItem;
import item.types.LineClearItem;
import item.types.WeightItem;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

class AiPlayerTest {
    @Test
    void retrievesTheHeldIBlockForAnOpenWell() {
        Board board = wellForIBlock();
        ActionController action = action(board, new OBlock());
        try {
            var view = withHold(action.getViewSnapshot(), new OBlock(), new IBlock());
            assertEquals(AiPlayer.Move.HOLD, AiPlayer.chooseMove(view, 0, true));
            assertNotEquals(AiPlayer.Move.HOLD, AiPlayer.chooseMove(view, 0, false));
        } finally {
            action.shutdown();
        }
    }

    @Test
    void advancesToThePreviewWhenHoldingWouldClearTheWell() {
        ActionController action = action(wellForIBlock(), new OBlock());
        try {
            var view = withHold(action.getViewSnapshot(), new IBlock(), null);
            assertEquals(AiPlayer.Move.HOLD, AiPlayer.chooseMove(view, 0, true));
        } finally {
            action.shutdown();
        }
    }

    @Test
    void keepsAnIBlockForLaterOnlyWhenItIsNotNeededNow() {
        ActionController open = action(new Board(), new IBlock());
        ActionController well = action(wellForIBlock(), new IBlock());
        try {
            assertEquals(AiPlayer.Move.HOLD,
                    AiPlayer.chooseMove(withHold(open.getViewSnapshot(), new OBlock(), null), 0, true));
            assertNotEquals(AiPlayer.Move.HOLD,
                    AiPlayer.chooseMove(withHold(well.getViewSnapshot(), new OBlock(), null), 0, true));
        } finally {
            open.shutdown();
            well.shutdown();
        }
    }

    @Test
    void usesTheEnginesHoldLimitUntilTheNextLock() {
        ActionController action = action(new Board(), new OBlock());
        try {
            assertTrue(action.canHold());
            assertTrue(action.holdAction());
            assertFalse(action.canHold());
            assertFalse(action.holdAction());
            action.hardDropWithScoreAction();
            assertTrue(action.canHold());
            action.setPaused(true);
            assertFalse(action.canHold());
        } finally {
            action.shutdown();
        }
    }

    @Test
    void executesTheHoldAndDoesNotSwapBackBeforeLocking() {
        Board board = new Board();
        IBlock reserved = new IBlock();
        ActionController action = action(board, reserved);
        ActionController opponent = action(new Board(), new OBlock());
        try (BattleController battle = new BattleController()) {
            battle.registerPlayer(BattleController.Player.ONE, opponent);
            battle.registerPlayer(BattleController.Player.TWO, action);
            battle.start();
            assertTrue(action.holdAction());
            action.hardDropWithScoreAction();

            int[][] well = wellForIBlock().getBoard();
            for (int row = 0; row < well.length; row++) {
                System.arraycopy(well[row], 0, board.getBoard()[row], 0, board.getWidth());
            }
            Block outgoing = action.getCurrentBlock();
            var landing = GhostProjection.project(well, ActionController.snapshotOf(outgoing));
            assertNotNull(landing);
            outgoing.setY(landing.y());

            try (AiPlayer ai = new AiPlayer(battle, BattleController.Player.TWO, action)) {
                assertEquals(AiPlayer.Move.HOLD, ai.step());
                assertSame(reserved, action.getCurrentBlock());
                assertSame(outgoing, action.getHeldBlock());
                assertEquals(AiPlayer.Move.HOLD, ai.getLastMove());
                assertNotEquals(AiPlayer.Move.HOLD, ai.step());
            }
        }
    }

    @Test
    void leavesAnOpeningForTheNextBlockInsteadOfCoveringIt() {
        Board board = new Board();
        int[] heights = {3, 3, 2, 0, 1, 2, 3, 2, 2, 2};
        for (int column = 0; column < heights.length; column++) {
            for (int row = board.getBoard().length - heights[column]; row < board.getBoard().length; row++) {
                board.getBoard()[row][column] = 1;
            }
        }
        ActionController action = action(board, new OBlock());
        try {
            var view = action.getViewSnapshot();
            assertEquals(AiPlayer.Move.RIGHT, AiPlayer.chooseMove(withNext(view, new IBlock())));
            assertEquals(AiPlayer.Move.HARD_DROP, AiPlayer.chooseMove(withNext(view, new OBlock())));
        } finally {
            action.shutdown();
        }
    }

    @Test
    void choosesTheFirstMoveTowardADoubleClear() {
        Board board = new Board();
        fillDoubleWithGap(board, 6);
        ActionController action = action(board, new OBlock());
        try {
            assertEquals(AiPlayer.Move.RIGHT, AiPlayer.chooseMove(withNext(action.getViewSnapshot(), null)));
        } finally {
            action.shutdown();
        }
    }

    @Test
    void dropsNowWhenGravityWouldLockBeforeReachingTheDoubleClear() {
        Board board = new Board();
        fillDoubleWithGap(board, 6);
        ActionController action = action(board, new OBlock());
        try {
            action.getCurrentBlock().setY(18);
            var view = withNext(action.getViewSnapshot(), null);
            assertEquals(AiPlayer.Move.RIGHT, AiPlayer.chooseMove(view));
            assertEquals(AiPlayer.Move.HARD_DROP, AiPlayer.chooseMove(view, 4));
        } finally {
            action.shutdown();
        }
    }

    @Test
    void leavesAnUnreachableLandingOutOfTheSearch() {
        Board board = new Board();
        fillDoubleWithGap(board, 6);
        ActionController action = action(board, new OBlock());
        try {
            int x = action.getCurrentBlock().getX();
            int y = action.getCurrentBlock().getY();
            for (int row = y; row < board.getBoard().length - 2; row++) {
                board.getBoard()[row][x + 2] = 1;
            }
            assertNotEquals(AiPlayer.Move.RIGHT, AiPlayer.chooseMove(withNext(action.getViewSnapshot(), null)));
        } finally {
            action.shutdown();
        }
    }

    @Test
    void bombChoosesAColumnWhereTheBlastCanRemoveBlocks() {
        Board board = new Board();
        int bottom = board.getBoard().length - 1;
        board.getBoard()[bottom][8] = 1;
        board.getBoard()[bottom][9] = 1;
        board.getBoard()[bottom - 1][9] = 1;
        ActionController action = action(board, new BombItem());
        try {
            assertEquals(AiPlayer.Move.RIGHT, AiPlayer.chooseMove(withNext(action.getViewSnapshot(), null)));
            assertEquals(1, board.getBoard()[bottom][9], "AI prediction must leave the real board untouched");
        } finally {
            action.shutdown();
        }
    }

    @Test
    void lineClearMarkerMakesAPlacementWorthTaking() {
        Board board = new Board();
        int bottom = board.getBoard().length - 1;
        int[] heights = {1, 1, 2, 1, 1, 2, 0, 1, 0, 0};
        for (int column = 0; column < heights.length; column++) {
            for (int offset = 0; offset < heights[column]; offset++) {
                board.getBoard()[bottom - offset][column] = 1;
            }
        }
        ActionController action = action(board, new LineClearItem(new OBlock(), 0, 0));
        ActionController normal = action(board, new OBlock());
        try {
            assertEquals(AiPlayer.Move.RIGHT, AiPlayer.chooseMove(withNext(action.getViewSnapshot(), null)));
            assertEquals(AiPlayer.Move.HARD_DROP, AiPlayer.chooseMove(withNext(normal.getViewSnapshot(), null)));
        } finally {
            action.shutdown();
            normal.shutdown();
        }
    }

    @Test
    void aiUsesTheRealAttackAndWaitsWhilePausedOrFinished() {
        Board aiBoard = new Board();
        fillDoubleWithGap(aiBoard, 6);
        ActionController aiAction = action(aiBoard, new OBlock());
        ActionController opponent = action(new Board(), new OBlock());
        try (BattleController battle = new BattleController()) {
            battle.registerPlayer(BattleController.Player.ONE, opponent);
            battle.registerPlayer(BattleController.Player.TWO, aiAction);
            battle.start();
            try (AiPlayer ai = new AiPlayer(battle, BattleController.Player.TWO, aiAction)) {
                battle.setPaused(true);
                assertEquals(AiPlayer.Move.WAIT, ai.step());
                battle.setPaused(false);
                for (int i = 0; i < 8 && opponent.getPendingAttackCount() == 0; i++) {
                    ai.step();
                }
                assertEquals(2, opponent.getPendingAttackCount());
                assertTrue(aiAction.getScore() > 0);
                battle.reportGameOver(BattleController.Player.ONE);
                assertEquals(AiPlayer.Move.WAIT, ai.step());
            }
        }
    }

    @Test
    void sessionConstructorAccessorsAndCloseSupportUiLifecycle() {
        try (LocalBattleSession session = new LocalBattleSession(
                new BattleSettings(BattleMode.NORMAL, 0), Difficulty.NORMAL);
             AiPlayer ai = new AiPlayer(session, BattleController.Player.TWO)) {
            assertEquals(BattleController.Player.TWO, ai.getPlayer());
            assertEquals(500, ai.getInputIntervalMillis());
            assertFalse(ai.isRunning());
            session.start();
            ai.start();
            ai.start();
            assertTrue(ai.isRunning());
            ai.close();
            assertFalse(ai.isRunning());
            assertEquals(AiPlayer.Move.WAIT, ai.step());
        }
    }

    @Test
    void landedWeightKeepsItsColumn() {
        WeightItem weight = new WeightItem();
        ActionController aiAction = action(new Board(), weight);
        ActionController opponent = action(new Board(), new OBlock());
        try (BattleController battle = new BattleController()) {
            battle.registerPlayer(BattleController.Player.ONE, opponent);
            battle.registerPlayer(BattleController.Player.TWO, aiAction);
            battle.start();
            weight.markLanded();
            try (AiPlayer ai = new AiPlayer(battle, BattleController.Player.TWO, aiAction)) {
                assertEquals(AiPlayer.Move.HARD_DROP, ai.step());
            }
        }
    }

    private static ActionController action(Board board, Block block) {
        return new ActionController(board, block, new GameStateManager(), new ScoreManager(),
                Difficulty.NORMAL, GameMode.ITEM, 0);
    }

    private static ActionController.ViewSnapshot withNext(ActionController.ViewSnapshot view, Block next) {
        return new ActionController.ViewSnapshot(view.board(), view.hiddenRows(), view.currentBlock(),
                ActionController.snapshotOf(next), view.heldBlock(), view.clearingRows(), view.clearingCells(),
                view.animationElapsedMs(), view.score(), view.level(), view.lines(), view.pendingAttackRows(),
                view.paused(), view.stopped(), view.gameOver());
    }

    private static ActionController.ViewSnapshot withHold(ActionController.ViewSnapshot view, Block next, Block held) {
        return new ActionController.ViewSnapshot(view.board(), view.hiddenRows(), view.currentBlock(),
                ActionController.snapshotOf(next), ActionController.snapshotOf(held),
                view.clearingRows(), view.clearingCells(), view.animationElapsedMs(), view.score(), view.level(),
                view.lines(), view.pendingAttackRows(), view.paused(), view.stopped(), view.gameOver());
    }

    private static Board wellForIBlock() {
        Board board = new Board();
        for (int row = board.getBoard().length - 4; row < board.getBoard().length; row++) {
            for (int column = 1; column < board.getWidth(); column++) board.getBoard()[row][column] = 1;
        }
        return board;
    }

    private static void fillDoubleWithGap(Board board, int gap) {
        for (int row = board.getBoard().length - 2; row < board.getBoard().length; row++) {
            for (int column = 0; column < board.getWidth(); column++) {
                if (column != gap && column != gap + 1) board.getBoard()[row][column] = 1;
            }
        }
    }
}
