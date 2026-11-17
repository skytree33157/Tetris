package game;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;
import sound.SoundEvent;

// 게임 동작마다 알맞은 소리 이벤트가 발생하는지
class ActionControllerSoundTest {

    private Board board;
    private ActionController controller;
    private final List<SoundEvent> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        board = new Board();
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    private ActionController create() {
        controller = new ActionController(board, new OBlock(), new GameStateManager(),
                new ScoreManager(), Difficulty.NORMAL, GameMode.NORMAL, 0);
        controller.setSoundListener(events::add);
        return controller;
    }

    // 맨 아래 lines줄을 왼쪽 2칸만 비우고 채움 → O블록을 왼쪽 끝에 떨어뜨리면 완성
    private void fillBottomRowsExceptLeftTwo(int lines) {
        int bottomRow = board.getHiddenRows() + board.getHeight() - 1;
        int[][] partial = new int[lines][board.getWidth()];
        for (int row = 0; row < lines; row++) {
            for (int col = 2; col < board.getWidth(); col++) {
                partial[row][col] = 1;
            }
        }
        board.addBlock(0, bottomRow - lines + 1, partial);
    }

    private void dropToLeftEdge() {
        for (int i = 0; i < 5; i++) {
            controller.moveLeftAction();
        }
        events.clear(); // 이동 소리는 제외하고 낙하 결과만 확인
        controller.hardDropWithScoreAction();
    }

    @Test // 이동 성공 시 MOVE
    void moveEmitsMove() {
        create().moveLeftAction();
        assertEquals(List.of(SoundEvent.MOVE), events);
    }

    @Test // 벽에 막혀 이동 실패하면 소리 없음
    void blockedMoveIsSilent() {
        create();
        for (int i = 0; i < 10; i++) {
            controller.moveLeftAction();
        }
        events.clear();
        controller.moveLeftAction();
        assertTrue(events.isEmpty());
    }

    @Test // 회전 성공 시 ROTATE
    void rotateEmitsRotate() {
        create().rotateAction();
        assertEquals(List.of(SoundEvent.ROTATE), events);
    }

    @Test // 홀드 성공 시 HOLD, 같은 블록에서 두 번째 홀드는 실패 → 소리 없음
    void holdEmitsOnlyOnSuccess() {
        create().holdAction();
        assertEquals(List.of(SoundEvent.HOLD), events);
        events.clear();
        controller.holdAction();
        assertTrue(events.isEmpty());
    }

    @Test // 줄 삭제 없는 하드드롭 → HARD_DROP 후 LOCK
    void hardDropEmitsDropThenLock() {
        create().hardDropWithScoreAction();
        assertEquals(List.of(SoundEvent.HARD_DROP, SoundEvent.LOCK), events);
    }

    @Test // 1~3줄 삭제 → LINE_CLEAR
    void lineClearEmitsLineClear() {
        fillBottomRowsExceptLeftTwo(2);
        create();
        dropToLeftEdge();
        assertEquals(List.of(SoundEvent.HARD_DROP, SoundEvent.LINE_CLEAR), events);
    }

    @Test // 소프트 드롭은 너무 잦아서 소리 없음
    void softDropIsSilent() {
        create().softDropAction();
        assertTrue(events.isEmpty());
    }

    @Test // 일시정지 중에는 입력이 무시되므로 소리도 없음
    void pausedControllerIsSilent() {
        create().setPaused(true);
        controller.moveLeftAction();
        controller.rotateAction();
        controller.holdAction();
        assertTrue(events.isEmpty());
    }

    @Test // 새 블록을 놓을 자리가 없으면 GAME_OVER 한 번
    void gameOverEmitsOnce() {
        create();
        // 시작 위치 아래만 남기고 보드를 거의 채워 다음 블록이 나올 자리를 없앰
        for (int row = board.getHiddenRows() + 2; row < board.getHiddenRows() + board.getHeight(); row++) {
            for (int col = 0; col < board.getWidth(); col++) {
                if (col != 0) board.getBoard()[row][col] = 1;
            }
        }
        for (int i = 0; i < 5 && !controller.isGameOver(); i++) {
            controller.hardDropWithScoreAction();
        }
        assertTrue(controller.isGameOver());
        assertEquals(1, events.stream().filter(e -> e == SoundEvent.GAME_OVER).count());
    }

    @Test // 리스너에서 예외가 나도 게임은 계속 진행
    void listenerExceptionDoesNotBreakGame() {
        create().setSoundListener(e -> { throw new RuntimeException("boom"); });
        assertDoesNotThrow(() -> controller.hardDropWithScoreAction());
    }

    @Test // null 리스너는 무시(no-op)
    void nullListenerIsIgnored() {
        create().setSoundListener(null);
        assertDoesNotThrow(() -> controller.moveLeftAction());
    }
}
