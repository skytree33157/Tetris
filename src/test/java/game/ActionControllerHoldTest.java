package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import blocks.core.Block;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import item.types.LineClearItem;
import score.ScoreManager;
import item.types.BombItem;
import item.types.BonusItem;
import item.types.SlowItem;
import item.types.WeightItem;

class ActionControllerHoldTest {

    private ActionController controller;

    @BeforeEach
    void setUp() {
        controller = new ActionController(
                new Board(),
                new OBlock(),
                new GameStateManager(),
                new ScoreManager(),
                Difficulty.NORMAL,
                GameMode.NORMAL,
                0
        );
    }

    @AfterEach
    void tearDown() {
        controller.shutdown();
    }

    @Test
    void firstHoldStoresCurrentAndUsesNextBlock() {
        Block current = controller.getCurrentBlock();
        Block next = controller.getNextBlock();

        assertTrue(controller.holdAction());

        assertSame(current, controller.getHeldBlock());
        assertSame(next, controller.getCurrentBlock());
        assertNotSame(next, controller.getNextBlock());
    }

    @Test
    void cannotHoldTwiceBeforeLocking() {
        assertTrue(controller.holdAction());

        Block current = controller.getCurrentBlock();
        Block held = controller.getHeldBlock();
        Block next = controller.getNextBlock();

        assertFalse(controller.holdAction());

        assertSame(current, controller.getCurrentBlock());
        assertSame(held, controller.getHeldBlock());
        assertSame(next, controller.getNextBlock());
    }

    @Test
    void canSwapHeldBlockAfterLocking() {
        Block first = controller.getCurrentBlock();

        assertTrue(controller.holdAction());
        controller.hardDropAction();

        Block current = controller.getCurrentBlock();
        Block next = controller.getNextBlock();

        assertTrue(controller.holdAction());

        assertSame(first, controller.getCurrentBlock());
        assertSame(current, controller.getHeldBlock());
        assertSame(next, controller.getNextBlock());
    }

    static Stream<Arguments> holdableItems() {
        return Stream.of(
                Arguments.of("L", new LineClearItem(new OBlock(), 1, 0)),
                Arguments.of("무게추", new WeightItem()),
                Arguments.of("폭탄", new BombItem()),
                Arguments.of("보너스", new BonusItem(new OBlock(), 1, 0)),
                Arguments.of("슬로우", new SlowItem(new OBlock(), 1, 0))
        );
    }

    @ParameterizedTest(name = "{0} 아이템 홀드 및 효과")
    @MethodSource("holdableItems")
    void heldItemKeepsInformationAndEffect(String name, Block item) {
        controller.shutdown();

        Board board = new Board();
        GameStateManager manager = new GameStateManager();
        ScoreManager scoreManager = new ScoreManager();

        int[][] expectedShape = new int[item.getShape().length][];
        for (int row = 0; row < expectedShape.length; row++) {
            expectedShape[row] = item.getShape()[row].clone();
        }

        controller = new ActionController(
                board,
                item,
                manager,
                scoreManager,
                Difficulty.NORMAL,
                GameMode.ITEM,
                0
        );

        Block firstNext = controller.getNextBlock();

        assertTrue(controller.holdAction());
        assertSame(item, controller.getHeldBlock());
        assertSame(firstNext, controller.getCurrentBlock());

        controller.hardDropAction();

        Block outgoing = controller.getCurrentBlock();
        Block nextBeforeSwap = controller.getNextBlock();

        assertTrue(controller.holdAction());
        assertSame(item, controller.getCurrentBlock());
        assertSame(outgoing, controller.getHeldBlock());
        assertSame(nextBeforeSwap, controller.getNextBlock());

        assertEquals(3, item.getX());
        assertEquals(0, item.getY());
        assertEquals(expectedShape.length, item.getShape().length);

        for (int row = 0; row < expectedShape.length; row++) {
            assertArrayEquals(expectedShape[row], item.getShape()[row]);
        }

        int[][] cells = board.getBoard();
        for (int[] row : cells) {
            Arrays.fill(row, 0);
        }

        int bottom = cells.length - 1;
        Arrays.fill(cells[bottom], 1);
        cells[bottom][3] = 0;
        cells[bottom][4] = 0;

        if (item instanceof LineClearItem) {
            cells[bottom][0] = 0;
        }

        if (item instanceof SlowItem) {
            for (int i = 0; i < 9; i++) {
                manager.updateLevelUp(0);
            }
            assertTrue(manager.getDropSpeed() < 1000);
        }

        scoreManager.reset();
        int previousLines = manager.getTotalLinesCleared();

        controller.hardDropAction();

        if (item instanceof WeightItem
                && controller.getCurrentBlock() == item) {
            controller.moveDownAction();
        }

        if (item instanceof LineClearItem) {
            assertEquals(previousLines + 1, manager.getTotalLinesCleared());
        } else if (item instanceof BonusItem) {
            assertEquals(1, board.getLastClearedBonusLines());
            assertTrue(scoreManager.getScore() >= 1000);
        } else if (item instanceof SlowItem) {
            assertEquals(1, board.getLastClearedSlowLines());
            assertEquals(1000, manager.getDropSpeed());
        } else if (item instanceof BombItem) {
            // 폭발 범위 안의 칸은 삭제되고 범위 밖의 칸은 유지
            assertEquals(0, board.getBoard()[bottom][2]);
            assertEquals(1, board.getBoard()[bottom][0]);
        } else if (item instanceof WeightItem weight) {
            assertTrue(weight.isLanded());
            assertEquals(previousLines + 1, manager.getTotalLinesCleared());

            assertEquals(weight.getType().getValue(),
                    board.getBoard()[bottom][5]);
            assertEquals(0, board.getBoard()[bottom][6]);
        }
    }
    
}