package game;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import item.core.ItemRegistry;
import item.types.BonusItem;
import item.types.LineClearItem;
import item.types.SlowItem;
import item.types.WeightItem;
import score.ScoreManager;

// FR-15 다음 블록, FR-30 줄 삭제 애니메이션, FR-31 모드별 아이템 생성
class ActionControllerTest {
    // 테스트 중 타이머가 먼저 줄을 지우지 않도록 길게 설정 (완료는 completeLineClear()로 직접 호출)
    private static final int LONG_ANIMATION_MS = 10_000;

    private Board board;
    private GameStateManager manager;
    private ScoreManager scoreManager;
    private ActionController controller;
    private int bottomRow; // 숨김 줄을 포함한 보드 배열의 맨 아래 행

    @BeforeEach
    void setUp() {
        board = new Board();
        manager = new GameStateManager();
        scoreManager = new ScoreManager();
        bottomRow = board.getHiddenRows() + board.getHeight() - 1;
        // 맨 아래 두 줄을 왼쪽 2칸만 비우고 채움 -> O블록을 왼쪽 끝에 떨어뜨리면 두 줄 완성
        int[][] partial = new int[2][board.getWidth()];
        for (int row = 0; row < 2; row++) {
            for (int col = 2; col < board.getWidth(); col++) {
                partial[row][col] = 1;
            }
        }
        board.addBlock(0, bottomRow - 1, partial);
    }

    @AfterEach // 줄 삭제 타이머 정리
    void tearDown() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    private ActionController create(GameMode mode, int animationMs) {
        controller = new ActionController(board, new OBlock(), manager, scoreManager,
                Difficulty.NORMAL, mode, animationMs);
        return controller;
    }

    // O블록을 왼쪽 끝으로 옮긴 뒤 하드드롭
    private void dropToLeftEdge(ActionController controller) {
        for (int i = 0; i < 5; i++) {
            controller.moveLeftAction();
        }
        controller.hardDropAction();
    }

    // ---------- FR-15 다음 블록 ----------

    @Test // 블록 고정 후 미리보기에 있던 블록이 현재 블록이 되고 새 다음 블록 생성
    void nextBlockBecomesCurrentAfterLock() {
        ActionController controller = create(GameMode.NORMAL, 0);
        Block next = controller.getNextBlock();
        assertNotNull(next);

        controller.hardDropAction(); // 가운데에 떨어뜨림 -> 줄 삭제 없음

        assertSame(next, controller.getCurrentBlock());
        assertNotNull(controller.getNextBlock());
        assertNotSame(next, controller.getNextBlock());
    }

    // ---------- FR-30 줄 삭제 애니메이션 ----------

    @Test // 애니메이션 없으면 즉시 삭제
    void clearsImmediatelyWithoutAnimation() {
        ActionController controller = create(GameMode.NORMAL, 0);
        dropToLeftEdge(controller);

        assertFalse(controller.isClearing());
        assertEquals(2, manager.getTotalLinesCleared());
        assertTrue(board.findFullRows().isEmpty());
        assertTrue(scoreManager.getScore() > 0);
    }

    @Test // 애니메이션 중에는 줄이 남아 있고 삭제 대상이 표시됨
    void keepsRowsDuringAnimation() {
        ActionController controller = create(GameMode.NORMAL, LONG_ANIMATION_MS);
        dropToLeftEdge(controller);

        assertTrue(controller.isClearing());
        assertEquals(List.of(bottomRow - 1, bottomRow), controller.getClearingRows());
        assertEquals(2, board.findFullRows().size());
        assertEquals(0, manager.getTotalLinesCleared());
    }

    @Test // 애니메이션 중 입력 무시
    void ignoresInputDuringAnimation() {
        ActionController controller = create(GameMode.NORMAL, LONG_ANIMATION_MS);
        dropToLeftEdge(controller);

        assertFalse(controller.moveDownAction());
        assertEquals(0, controller.hardDropAction());
    }

    @Test // 애니메이션 완료 후 실제 삭제 + 다음 블록
    void completesLineClearAfterAnimation() {
        ActionController controller = create(GameMode.NORMAL, LONG_ANIMATION_MS);
        dropToLeftEdge(controller);
        assertTrue(controller.isClearing());

        controller.completeLineClear();

        assertFalse(controller.isClearing());
        assertTrue(controller.getClearingRows().isEmpty());
        assertEquals(2, manager.getTotalLinesCleared());
        assertTrue(board.findFullRows().isEmpty());
        assertEquals(0, controller.getCurrentBlock().getY());
    }

    @Test // 폭탄 제거 대상은 애니메이션이 끝날 때까지 보드에 남아 있다
    void animatesBombClearedCellsBeforeRemovingThem() {
        controller = new ActionController(board, new item.types.BombItem(), manager, scoreManager,
                Difficulty.NORMAL, GameMode.NORMAL, LONG_ANIMATION_MS);
        int targetRow = bottomRow - 1;
        int targetColumn = 3;

        controller.hardDropAction();

        assertTrue(controller.isClearing());
        assertTrue(controller.getClearingCells().contains(
                new ActionController.ClearingCell(targetRow, targetColumn)));
        assertNotEquals(0, board.getBoard()[targetRow][targetColumn]);

        controller.completeLineClear();

        assertFalse(controller.isClearing());
        assertTrue(controller.getClearingCells().isEmpty());
        assertEquals(0, board.getBoard()[targetRow][targetColumn]);
    }

    // ---------- FR-31 모드별 아이템 생성 ----------

    @Test // 아이템 모드: 누적 10줄을 넘기면 다음 블록(미리보기)이 아이템
    void itemModeSpawnsItemAfterTenLines() {
        ActionController controller = create(GameMode.ITEM, 0);
        manager.updateLevelUp(9); // 누적 9줄 상태
        Block next = controller.getNextBlock();

        dropToLeftEdge(controller); // 2줄 삭제 -> 누적 11줄

        assertSame(next, controller.getCurrentBlock());
        assertNotNull(ItemRegistry.appearanceOf(controller.getNextBlock()));
    }

    @Test // 일반 모드: 10줄을 넘겨도 아이템이 생성되지 않음
    void normalModeNeverSpawnsItem() {
        ActionController controller = create(GameMode.NORMAL, 0);
        manager.updateLevelUp(9);
        Block next = controller.getNextBlock();

        dropToLeftEdge(controller);

        assertSame(next, controller.getCurrentBlock());
        assertNull(ItemRegistry.appearanceOf(controller.getNextBlock()));
    }

    @Test
    void itemModeSpawnsAnItemWhenLineCountReachesTenExactly() {
        Board thresholdBoard = new Board();
        int bottom = thresholdBoard.getHiddenRows() + thresholdBoard.getHeight() - 1;
        int[] partialRow = new int[thresholdBoard.getWidth()];
        java.util.Arrays.fill(partialRow, BlockType.I.getValue());
        partialRow[3] = 0;
        partialRow[4] = 0;
        thresholdBoard.addBlock(0, bottom, new int[][] {partialRow});
        manager = new GameStateManager();
        controller = new ActionController(thresholdBoard, new OBlock(), manager, scoreManager,
                Difficulty.NORMAL, GameMode.ITEM, 0);
        manager.updateLevelUp(9);
        controller.hardDropAction();

        assertEquals(10, manager.getTotalLinesCleared());
        assertNotNull(ItemRegistry.appearanceOf(controller.getNextBlock()));
    }

    @Test
    void movementStopsAtBothBoardEdgesAndAtFixedBlocks() {
        ActionController controller = create(GameMode.NORMAL, 0);
        Block current = controller.getCurrentBlock();

        for (int i = 0; i < 10; i++) {
            controller.moveLeftAction();
        }
        assertEquals(0, current.getX());
        controller.moveLeftAction();
        assertEquals(0, current.getX());

        for (int i = 0; i < 10; i++) {
            controller.moveRightAction();
        }
        assertEquals(board.getWidth() - current.getWidth(), current.getX());
        controller.moveRightAction();
        assertEquals(board.getWidth() - current.getWidth(), current.getX());

        Block collisionBlock = new OBlock();
        ActionController collisionController = new ActionController(
                new BoardWithObstacle(5, 0), collisionBlock, new GameStateManager(),
                new ScoreManager(), Difficulty.NORMAL, GameMode.NORMAL, 0);
        try {
            collisionBlock.setX(3);
            collisionBlock.setY(0);
            collisionController.moveRightAction();
            assertEquals(3, collisionBlock.getX());
        } finally {
            collisionController.shutdown();
        }
    }

    @Test
    void downwardCollisionLocksTheBlockOnTopOfTheObstacle() {
        Board collisionBoard = new Board();
        int obstacleRow = 10;
        collisionBoard.addBlock(3, obstacleRow, new int[][] {{BlockType.I.getValue()}});
        GameStateManager collisionState = new GameStateManager();
        ActionController collisionController = new ActionController(collisionBoard, new OBlock(),
                collisionState, new ScoreManager(), Difficulty.NORMAL, GameMode.NORMAL, 0);
        try {
            Block falling = collisionController.getCurrentBlock();
            while (falling.getY() < obstacleRow - falling.getHeight()) {
                assertTrue(collisionController.moveDownAction());
            }

            assertFalse(collisionController.moveDownAction());

            assertNotSame(falling, collisionController.getCurrentBlock());
            assertEquals(BlockType.I.getValue(), collisionBoard.getBoard()[obstacleRow][3]);
            assertEquals(BlockType.O.getValue(), collisionBoard.getBoard()[obstacleRow - 2][3]);
            assertEquals(BlockType.O.getValue(), collisionBoard.getBoard()[obstacleRow - 1][4]);
        } finally {
            collisionController.shutdown();
        }
    }

    @Test
    void hardDropLocksAtTheLowestValidPositionAndReturnsDistance() {
        Board emptyBoard = new Board();
        ActionController controller = new ActionController(emptyBoard, new OBlock(),
                new GameStateManager(), new ScoreManager(), Difficulty.NORMAL, GameMode.NORMAL, 0);
        try {
            Block falling = controller.getCurrentBlock();

            int distance = controller.hardDropAction();

            assertEquals(emptyBoard.getHiddenRows() + emptyBoard.getHeight() - falling.getHeight(),
                    falling.getY());
            assertEquals(falling.getY(), distance);
            assertEquals(BlockType.O.getValue(), emptyBoard.getBoard()[falling.getY()][falling.getX()]);
            assertNotSame(falling, controller.getCurrentBlock());
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void lockingIntoHiddenRowsCausesGameOver() {
        Board stackedBoard = new Board();
        int[][] cells = new int[1][stackedBoard.getWidth()];
        java.util.Arrays.fill(cells[0], BlockType.I.getValue());
        stackedBoard.addBlock(0, 2, cells);
        GameStateManager gameState = new GameStateManager();
        ActionController controller = new ActionController(stackedBoard, new OBlock(), gameState,
                new ScoreManager(), Difficulty.NORMAL, GameMode.NORMAL, 0);
        try {
            controller.hardDropAction();
            assertTrue(gameState.isGameOver());
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void lineClearItemClearsItsMarkedRowAndAwardsLinePoints() {
        Board itemBoard = new Board();
        int itemBottomRow = itemBoard.getHiddenRows() + itemBoard.getHeight() - 1;
        int[] partialRow = new int[itemBoard.getWidth()];
        java.util.Arrays.fill(partialRow, BlockType.I.getValue());
        partialRow[3] = 0;
        partialRow[4] = 0;
        itemBoard.addBlock(0, itemBottomRow, new int[][] {partialRow});
        LineClearItem item = new LineClearItem(new OBlock(), 1, 0);
        GameStateManager itemState = new GameStateManager();
        ScoreManager itemScore = new ScoreManager();
        ActionController itemController = new ActionController(itemBoard, item, itemState,
                itemScore, Difficulty.NORMAL, GameMode.ITEM, 0);
        try {
            itemController.hardDropAction();

            assertEquals(1, itemState.getTotalLinesCleared());
            assertEquals(100, itemScore.getScore());
            assertTrue(itemBoard.findFullRows().isEmpty());
        } finally {
            itemController.shutdown();
        }
    }

    @Test
    void bonusAndSlowMarkersApplyTheirEffectsOnceWhenTheirLineClears() {
        for (boolean bonus : new boolean[] {true, false}) {
            Board itemBoard = new Board();
            int itemBottomRow = itemBoard.getHiddenRows() + itemBoard.getHeight() - 1;
            int[] partialRow = new int[itemBoard.getWidth()];
            java.util.Arrays.fill(partialRow, BlockType.I.getValue());
            partialRow[3] = 0;
            partialRow[4] = 0;
            itemBoard.addBlock(0, itemBottomRow, new int[][] {partialRow});
            Block item = bonus
                    ? new BonusItem(new OBlock(), 1, 0)
                    : new SlowItem(new OBlock(), 1, 0);
            GameStateManager itemState = new GameStateManager();
            if (!bonus) {
                itemState.updateLevelUp(10);
            }
            ScoreManager itemScore = new ScoreManager();
            ActionController itemController = new ActionController(itemBoard, item, itemState,
                    itemScore, Difficulty.NORMAL, GameMode.ITEM, 0);
            try {
                itemController.hardDropAction();
                assertEquals(bonus ? 1 : 11, itemState.getTotalLinesCleared());
                assertEquals(bonus ? 1100 : 100, itemScore.getScore());
                assertEquals(bonus ? 0 : 1, itemBoard.getLastClearedSlowLines());
                assertEquals(1000, itemState.getDropSpeed());
            } finally {
                itemController.shutdown();
            }
        }
    }

    @Test
    void weightItemClearsTheRowBelowWhenItLandsAndCannotSlideAfterLock() {
        Board weightBoard = new Board();
        int rowBelowLanding = weightBoard.getHiddenRows() + weightBoard.getHeight() - 1;
        weightBoard.addBlock(4, rowBelowLanding, new int[][] {{BlockType.I.getValue()}});
        WeightItem weight = new WeightItem();
        ActionController weightController = new ActionController(weightBoard, weight,
                new GameStateManager(), new ScoreManager(), Difficulty.NORMAL, GameMode.ITEM, 0);
        try {
            while (weight.getY() < rowBelowLanding - weight.getHeight()) {
                assertTrue(weightController.moveDownAction());
            }

            assertTrue(weightController.moveDownAction());
            assertEquals(rowBelowLanding - weight.getHeight() + 1, weight.getY());
            assertEquals(0, weightBoard.getBoard()[rowBelowLanding][4]);

            while (weightController.getCurrentBlock() == weight) {
                weightController.moveDownAction();
            }
            assertTrue(weight.isLanded());
            int landedX = weight.getX();
            weight.moveLeft();
            weight.moveRight();
            assertEquals(landedX, weight.getX());
        } finally {
            weightController.shutdown();
        }
    }

    private static final class BoardWithObstacle extends Board {
        private BoardWithObstacle(int x, int y) {
            addBlock(x, y, new int[][] {{BlockType.I.getValue()}});
        }
    }
}
