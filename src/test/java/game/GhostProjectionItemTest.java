package game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import item.types.BombItem;
import item.types.BonusItem;
import item.types.LineClearItem;
import item.types.SlowItem;
import item.types.WeightItem;
import score.ScoreManager;

class GhostProjectionItemTest {

    static Stream<Arguments> items() {
        return Stream.of(
                Arguments.of(
                        "L",
                        new LineClearItem(new OBlock(), 1, 0)
                ),
                Arguments.of("무게추", new WeightItem()),
                Arguments.of("폭탄", new BombItem()),
                Arguments.of(
                        "보너스",
                        new BonusItem(new OBlock(), 1, 0)
                ),
                Arguments.of(
                        "슬로우",
                        new SlowItem(new OBlock(), 1, 0)
                )
        );
    }

    @ParameterizedTest(name = "{0} 아이템 착지 위치")
    @MethodSource("items")
    void projectionMatchesItemLandingWithoutChangingGame(
            String name,
            Block item) {

        Board board = new Board();

        board.getBoard()[15][3] = BlockType.I.getValue();
        board.getBoard()[18][4] = BlockType.ATTACK.getValue();
        board.getBoard()[22][6] = BlockType.I.getValue();

        GameStateManager state = new GameStateManager();
        ScoreManager score = new ScoreManager();

        ActionController controller = new ActionController(
                board,
                item,
                state,
                score,
                Difficulty.NORMAL,
                GameMode.ITEM,
                0
        );

        try {
            ActionController.ViewSnapshot frame =
                    controller.getViewSnapshot();

            int[][] inputBoard = frame.board();
            int[][] expectedBoard = frame.board();

            ActionController.BlockSnapshot ghost =
                    GhostProjection.project(
                            inputBoard,
                            frame.currentBlock()
                    );

            assertNotNull(ghost);

            assertArrayEquals(expectedBoard, inputBoard);
            assertArrayEquals(expectedBoard, board.getBoard());
            assertEquals(frame.currentBlock().x(), item.getX());
            assertEquals(frame.currentBlock().y(), item.getY());
            assertEquals(frame.score(), score.getScore());

            assertArrayEquals(
                    frame.currentBlock().cells(),
                    ghost.cells()
            );
            assertArrayEquals(
                    frame.currentBlock().cells(),
                    ActionController.snapshotOf(item).cells()
            );

            if (item instanceof WeightItem weight) {
                assertFalse(weight.isLanded());
            }

            controller.hardDropAction();

            int maxSteps = board.getHiddenRows()
                    + board.getHeight()
                    + 1;

            for (int step = 0;
                    step < maxSteps
                            && controller.getCurrentBlock() == item
                            && !state.isGameOver();
                    step++) {
                controller.moveDownAction();
            }

            assertNotSame(
                    item,
                    controller.getCurrentBlock(),
                    name + " 아이템 처리가 끝나야 합니다."
            );
            assertEquals(item.getX(), ghost.x(), name);
            assertEquals(item.getY(), ghost.y(), name);
        } finally {
            controller.shutdown();
        }
    }
}