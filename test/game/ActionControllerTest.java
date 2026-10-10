package game;

import static org.junit.jupiter.api.Assertions.*;

import blocks.core.Block;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import item.core.ItemRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
