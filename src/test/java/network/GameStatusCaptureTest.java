package network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import game.ActionController;
import game.GameMode;
import game.GameStateManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

// 실제 게임(ActionController)에서 상대에게 보낼 상태를 뽑아내는지 검증
class GameStatusCaptureTest {
    private Board board;
    private GameStateManager manager;
    private ScoreManager scoreManager;
    private ActionController controller;

    @BeforeEach
    void setUp() {
        board = new Board();
        manager = new GameStateManager();
        scoreManager = new ScoreManager();
        controller = new ActionController(board, new OBlock(), manager, scoreManager,
                Difficulty.NORMAL, GameMode.NORMAL, 0);
    }

    @Test // 보이는 줄 크기(20×10)로, 점수·레벨·줄·게임 오버까지 그대로
    void captureUsesVisibleBoardSizeAndCurrentStatistics() {
        GameStatus status = GameStatus.capture(controller, manager, scoreManager);

        assertEquals(board.getHeight(), status.getCells().length);
        assertEquals(board.getWidth(), status.getCells()[0].length);
        assertEquals(scoreManager.getScore(), status.getScore());
        assertEquals(manager.getCurrentLevel(), status.getLevel());
        assertEquals(manager.getTotalLinesCleared(), status.getLines());
        assertFalse(status.isGameOver());
    }

    @Test // 바닥에 쌓인 블록이 상대 보드 맨 아래 줄에 나타남
    void captureIncludesFixedCells() {
        int bottomRow = board.getHiddenRows() + board.getHeight() - 1;
        board.addBlock(0, bottomRow, new int[][] {{4, 4}});

        int[][] cells = GameStatus.capture(controller, manager, scoreManager).getCells();

        int last = cells.length - 1;
        assertEquals(4, cells[last][0]);
        assertEquals(4, cells[last][1]);
        assertEquals(0, cells[last][2]);
    }

    @Test // 떨어지는 블록도 같은 화면 스냅샷 기준으로 겹쳐서 보냄
    void captureOverlaysFallingBlockLikeTheLocalScreen() {
        // 블록이 숨은 줄을 벗어나 보이는 영역에 들어올 때까지 내린다
        for (int i = 0; i < 5; i++) {
            controller.moveDownAction();
        }
        ActionController.ViewSnapshot frame = controller.getViewSnapshot();

        int[][] cells = GameStatus.capture(controller, manager, scoreManager).getCells();

        assertArrayEquals(GameStatus.compose(frame.board(), frame.hiddenRows(), frame.currentBlock()), cells);
        // 실제로 블록 칸이 보드에 들어갔는지 (빈 보드와 다름)
        assertNotEquals(0, countFilled(cells));
    }

    private static int countFilled(int[][] cells) {
        int count = 0;
        for (int[] row : cells) {
            for (int value : row) {
                if (value != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
