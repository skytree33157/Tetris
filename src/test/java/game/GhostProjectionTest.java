package game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.tetromino.IBlock;
import blocks.tetromino.JBlock;
import blocks.tetromino.LBlock;
import blocks.tetromino.OBlock;
import blocks.tetromino.SBlock;
import blocks.tetromino.TBlock;
import blocks.tetromino.ZBlock;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;

class GhostProjectionTest {

    static Stream<Block> normalBlocks() {
        return Stream.of(
                new IBlock(),
                new OBlock(),
                new TBlock(),
                new SBlock(),
                new ZBlock(),
                new JBlock(),
                new LBlock()
        );
    }

    @ParameterizedTest(name = "{index}: 일반 블록 착지 위치")
    @MethodSource("normalBlocks")
    void projectionMatchesHardDropInEveryRotation(Block block) {
        for (int rotation = 0; rotation < 4; rotation++) {
            if (rotation > 0) {
                block.rotate();
            }

            Board board = new Board();

            board.getBoard()[17][4] = BlockType.I.getValue();
            board.getBoard()[20][3] = -1;
            board.getBoard()[22][6] = BlockType.ATTACK.getValue();

            ActionController controller = new ActionController(
                    board,
                    block,
                    new GameStateManager(),
                    new ScoreManager(),
                    Difficulty.NORMAL,
                    GameMode.NORMAL,
                    0
            );

            try {
                ActionController.ViewSnapshot frame =
                        controller.getViewSnapshot();

                ActionController.BlockSnapshot ghost =
                        GhostProjection.project(
                                frame.board(),
                                frame.currentBlock()
                        );

                assertNotNull(ghost);

                int originalY = block.getY();
                int distance = controller.hardDropAction();

                String message =
                        block.getType() + ", rotation=" + rotation;

                assertEquals(block.getX(), ghost.x(), message);
                assertEquals(block.getY(), ghost.y(), message);
                assertEquals(
                        ghost.y() - originalY,
                        distance,
                        message
                );
            } finally {
                controller.shutdown();
            }
        }
    }

    @Test
    void emptyBoardLandingIgnoresEmptyShapePadding() {
        Board board = new Board();

        for (Block block : new Block[]{new IBlock(), new OBlock()}) {
            block.setX(3);
            block.setY(0);

            ActionController.BlockSnapshot ghost =
                    GhostProjection.project(
                            board.getBoard(),
                            ActionController.snapshotOf(block)
                    );

            assertNotNull(ghost);

            assertEquals(21, ghost.y());
        }
    }

    @Test
    void projectionDoesNotChangeBoardBlockOrScore() {
        Board board = new Board();
        board.getBoard()[18][4] = BlockType.I.getValue();

        OBlock block = new OBlock();
        ScoreManager score = new ScoreManager();
        score.addBonusScore(2);

        ActionController controller = new ActionController(
                board,
                block,
                new GameStateManager(),
                score,
                Difficulty.NORMAL,
                GameMode.NORMAL,
                0
        );

        try {
            ActionController.ViewSnapshot frame =
                    controller.getViewSnapshot();

            int[][] inputBoard = frame.board();
            int[][] boardBefore = Arrays.stream(inputBoard)
                    .map(int[]::clone)
                    .toArray(int[][]::new);

            int[][] shapeBefore = frame.currentBlock().cells();
            int scoreBefore = score.getScore();

            ActionController.BlockSnapshot ghost =
                    GhostProjection.project(
                            inputBoard,
                            frame.currentBlock()
                    );

            assertNotNull(ghost);
            assertArrayEquals(boardBefore, inputBoard);
            assertArrayEquals(boardBefore, board.getBoard());
            assertArrayEquals(
                    shapeBefore,
                    frame.currentBlock().cells()
            );

            assertSame(block, controller.getCurrentBlock());
            assertEquals(frame.currentBlock().x(), block.getX());
            assertEquals(frame.currentBlock().y(), block.getY());
            assertEquals(scoreBefore, score.getScore());
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void returnsNoGhostWithoutOccupiedCells() {
        int[][] board = new Board().getBoard();

        assertNull(GhostProjection.project(board, null));

        ActionController.BlockSnapshot emptyBlock =
                new ActionController.BlockSnapshot(
                        3,
                        0,
                        new int[][]{
                            {0, 0},
                            {0, 0}
                        }
                );

        assertNull(GhostProjection.project(board, emptyBlock));
    }

    @Test
    void returnsNoGhostWhenCurrentPositionAlreadyCollides() {
        Board board = new Board();
        board.getBoard()[0][3] = BlockType.I.getValue();

        OBlock block = new OBlock();
        block.setX(3);
        block.setY(0);

        assertNull(
                GhostProjection.project(
                        board.getBoard(),
                        ActionController.snapshotOf(block)
                )
        );
    }
}