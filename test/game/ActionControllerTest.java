package game;

import blocks.core.Block;
import blocks.tetromino.OBlock;
import blocks.tetromino.TBlock;
import board.Board;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

import static org.junit.jupiter.api.Assertions.*;

class ActionControllerTest {

    @Test // 블록의 초기 위치 확인
    void getCurrentBlock() {
        ActionController controller = createController(new Board(), new OBlock(), new GameStateManager());

        assertEquals(3, controller.getCurrentBlock().getX());
        assertEquals(0, controller.getCurrentBlock().getY());
    }

    @Test // 다음 블록이 존재하는지 확인
    void getNextBlock() {
        ActionController controller = createController(new Board(), new OBlock(), new GameStateManager());

        assertNotNull(controller.getNextBlock());
    }

    @Test // 블록이 왼쪽으로 이동하는 지 테스트
    void moveLeftAction() {
        ActionController controller = createController(new Board(), new OBlock(), new GameStateManager());

        for (int i = 0; i < 4; i++) {
            controller.moveLeftAction();
        }

        assertEquals(0, controller.getCurrentBlock().getX());
    }

    @Test // 블록이 오른쪽으로 이동하는 지 테스트
    void moveRightAction() {
        ActionController controller = createController(new Board(), new OBlock(), new GameStateManager());

        for (int i = 0; i < 10; i++) {
            controller.moveRightAction();
        }

        assertEquals(8, controller.getCurrentBlock().getX());
    }

    @Test // 블록이 아래로 이동하는지 테스트
    void moveDownAction() {
        ActionController controller = createController(new Board(), new OBlock(), new GameStateManager());

        assertTrue(controller.moveDownAction());
        assertEquals(1, controller.getCurrentBlock().getY());
    }

    @Test // 블록이 회전하는지 테스트
    void rotateAction() {
        ActionController controller = createController(new Board(), new TBlock(), new GameStateManager());
        int[][] originalShape = controller.getCurrentBlock().getShape();

        controller.rotateAction();

        assertEquals(originalShape.length, controller.getCurrentBlock().getShape()[0].length);
        assertEquals(originalShape[0].length, controller.getCurrentBlock().getShape().length);
        assertEquals(3, controller.getCurrentBlock().getX());
    }

    @Test // 하드드롭 테스트
    void hardDropActionLocksBlockAndSpawnsNextBlock() {
        Board board = new Board();
        ActionController controller = createController(board, new OBlock(), new GameStateManager());
        Block nextBlock = controller.getNextBlock();
        // 하드드롭 이동 거리 확인
        assertEquals(21, controller.hardDropAction());
        // 블록 고정 확인
        assertEquals(2, board.getBoard()[21][3]);
        assertEquals(2, board.getBoard()[21][4]);
        assertEquals(2, board.getBoard()[22][3]);
        assertEquals(2, board.getBoard()[22][4]);
        // 다음 블록 생성 및 위치 확인
        assertSame(nextBlock, controller.getCurrentBlock());
        assertEquals(3, controller.getCurrentBlock().getX());
        assertEquals(0, controller.getCurrentBlock().getY());
    }

    @Test // 게임 오버 시 블록 이동이 정지되는지 테스트
    void gameOverPreventsBlockActions() {
        GameStateManager manager = new GameStateManager();
        ActionController controller = createController(new Board(), new OBlock(), manager);
        manager.setGameOver(true);

        controller.moveLeftAction();
        controller.moveRightAction();
        controller.rotateAction();
        // 게임 오버 상태에서 블록 이동 및 회전 시도
        assertFalse(controller.moveDownAction());
        assertEquals(0, controller.hardDropAction());
        assertEquals(3, controller.getCurrentBlock().getX());
        assertEquals(0, controller.getCurrentBlock().getY());
    }

    private ActionController createController(Board board, Block block, GameStateManager manager) {
        return new ActionController(board, block, manager, new ScoreManager());
    }
}