package menu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import network.GameStatus;
import org.junit.jupiter.api.Test;

// FR-43: 대전 승패 판정과 결과 화면 문구
class BattleResultTest {
    private static final int[][] BOARD = {{0, 1}, {1, 1}};

    @Test // 내 블록만 천장에 닿으면 패배
    void iLoseWhenOnlyIToppedOut() {
        BattleResult result = BattleResult.fromP2P(
                new GameStatus(300, 2, 5, true, BOARD), new GameStatus(800, 3, 9, false), "일반 모드", 61_000);

        assertEquals(BattleResult.Outcome.RIGHT_WINS, result.outcome());
        assertTrue(result.rightWon());
        assertFalse(result.leftWon());
        assertEquals("패배", result.headline());
        assertEquals("블록이 먼저 천장에 닿았어요", result.reason());
    }

    @Test // 상대 블록만 천장에 닿으면 승리, 양쪽 기록이 그대로 담김
    void iWinWhenOnlyOpponentToppedOut() {
        BattleResult result = BattleResult.fromP2P(
                new GameStatus(1200, 4, 12, false, BOARD), new GameStatus(450, 2, 6, true), "아이템 모드", 0);

        assertEquals("승리", result.headline());
        assertEquals("상대 블록이 먼저 천장에 닿았어요", result.reason());
        assertEquals("나", result.left().name());
        assertEquals(1200, result.left().score());
        assertEquals(4, result.left().level());
        assertEquals(12, result.left().lines());
        assertEquals("상대", result.right().name());
        assertEquals(450, result.right().score());
        assertEquals("아이템 모드", result.modeLabel());
    }

    @Test // 두 사람이 동시에 끝나면 무승부
    void bothToppedOutIsDraw() {
        BattleResult result = BattleResult.fromP2P(
                new GameStatus(10, 1, 0, true), new GameStatus(20, 1, 0, true), "일반 모드", 0);

        assertEquals(BattleResult.Outcome.DRAW, result.outcome());
        assertEquals("무승부", result.headline());
        assertEquals("두 사람이 동시에 끝났어요", result.reason());
        assertFalse(result.leftWon());
        assertFalse(result.rightWon());
    }

    @Test // 상대 상태를 한 번도 받지 못했으면 상대 기록은 0이고 보드는 비어 있음
    void missingRemoteStatusCountsAsEmptyOpponent() {
        BattleResult result = BattleResult.fromP2P(new GameStatus(50, 1, 1, true), null, "일반 모드", 0);

        assertEquals(0, result.right().score());
        assertEquals(0, result.right().cells().length);
        assertFalse(result.leftWon());
    }

    @Test // 로컬 대전처럼 personal이 아니면 "1P 승리" 형식
    void localBattleHeadlineUsesPlayerNames() {
        BattleResult.Side one = new BattleResult.Side("1P", 900, 3, 10, false, BOARD);
        BattleResult.Side two = new BattleResult.Side("2P", 400, 2, 4, true, BOARD);
        BattleResult result = new BattleResult(one, two, BattleResult.judge(false, true), false, "일반 모드", 0);

        assertEquals("1P 승리", result.headline());
        assertEquals("2P의 블록이 먼저 천장에 닿았어요", result.reason());
    }

    @Test // FR-40: 시간 제한 P2P 대전은 호스트가 확정한 점수로 승패를 정한다
    void timeUpUsesConfirmedScores() {
        // 화면의 마지막 상태(300)보다 호스트가 확정한 점수(1500)를 쓴다
        BattleResult win = BattleResult.fromP2PTimeUp(
                new GameStatus(300, 2, 5, false, BOARD), new GameStatus(800, 3, 9, false),
                1500, 900, "시간 제한 모드", 180_000);
        assertTrue(win.timeUp());
        assertEquals(BattleResult.Outcome.LEFT_WINS, win.outcome());
        assertEquals(1500, win.left().score());
        assertEquals(900, win.right().score());
        assertEquals("승리", win.headline());
        assertEquals("시간 종료 · 내 점수가 더 높아요", win.reason());

        BattleResult lose = BattleResult.fromP2PTimeUp(null, null, 100, 200, "시간 제한 모드", 0);
        assertEquals("패배", lose.headline());
        assertEquals("시간 종료 · 상대 점수가 더 높아요", lose.reason());

        BattleResult draw = BattleResult.fromP2PTimeUp(null, null, 500, 500, "시간 제한 모드", 0);
        assertEquals("무승부", draw.headline());
        assertEquals("시간 종료 · 점수가 같아요", draw.reason());
    }

    @Test // 로컬 시간 제한 대전 결과 문구
    void localTimeUpNamesTheWinner() {
        BattleResult.Side one = new BattleResult.Side("1P", 200, 1, 2, false, BOARD);
        BattleResult.Side two = new BattleResult.Side("2P", 700, 2, 6, false, BOARD);
        BattleResult result = BattleResult.fromLocal(one, two, BattleResult.Outcome.RIGHT_WINS, true,
                "시간 제한 모드", 180_000);

        assertFalse(result.personal());
        assertEquals("2P 승리", result.headline());
        assertEquals("시간 종료 · 2P 점수가 더 높아요", result.reason());
    }

    @Test // 결과 보드 칸은 복사본이라 바깥에서 바꿔도 영향 없음, 보드 패널용 상태로 바꿀 수 있음
    void sideCellsAreCopiedAndConvertibleToStatus() {
        int[][] cells = {{1, 0}};
        BattleResult.Side side = new BattleResult.Side("나", 10, 1, 0, true, cells);
        cells[0][0] = 9;
        side.cells()[0][1] = 9;

        assertArrayEquals(new int[][] {{1, 0}}, side.cells());
        GameStatus status = side.toStatus();
        assertTrue(status.isGameOver());
        assertArrayEquals(new int[][] {{1, 0}}, status.getCells());
    }
}
