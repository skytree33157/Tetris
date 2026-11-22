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
