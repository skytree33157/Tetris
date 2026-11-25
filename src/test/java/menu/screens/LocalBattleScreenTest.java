package menu.screens;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.ActionController;
import game.BattleController;
import java.util.List;
import menu.BattleModes;
import menu.BattleResult;
import org.junit.jupiter.api.Test;

// 로컬 대전 화면의 결과 변환: 경기 결과 → 결과 화면 기록, 방식 선택 메뉴
class LocalBattleScreenTest {
    @Test
    void matchOutcomeMapsToLeftRight() {
        assertEquals(BattleResult.Outcome.LEFT_WINS,
                LocalBattleScreen.toResultOutcome(BattleController.Outcome.ONE_WINS));
        assertEquals(BattleResult.Outcome.RIGHT_WINS,
                LocalBattleScreen.toResultOutcome(BattleController.Outcome.TWO_WINS));
        assertEquals(BattleResult.Outcome.DRAW,
                LocalBattleScreen.toResultOutcome(BattleController.Outcome.DRAW));
    }

    @Test // 결과 화면에는 숨은 줄을 뺀 보드와 최종 기록을 넘긴다
    void sideDropsHiddenRows() {
        int[][] board = {{1, 1}, {0, 2}, {3, 0}};
        ActionController.ViewSnapshot frame = new ActionController.ViewSnapshot(
                board, 1, null, null, null, List.of(), List.of(), 0,
                1200, 3, 8, List.of(), false, true, true);

        BattleResult.Side side = LocalBattleScreen.side("2P", frame);

        assertEquals("2P", side.name());
        assertEquals(1200, side.score());
        assertEquals(3, side.level());
        assertEquals(8, side.lines());
        assertTrue(side.toppedOut());
        assertArrayEquals(new int[][] {{0, 2}, {3, 0}}, side.cells());

        BattleResult.Side empty = LocalBattleScreen.side("1P", null);
        assertFalse(empty.toppedOut());
        assertEquals(0, empty.cells().length);
    }

    @Test // 보드 두 개가 들어가도록 칸 크기를 줄인다: 1366×728 화면이면 설정의 30px보다 작게
    void cellSizeFitsTwoBoardsOnScreen() {
        int small = LocalBattleScreen.fitCellSize(1366, 728, 10, 23);
        assertTrue(small < 30, "1366x768 laptop: " + small);
        assertTrue(small * 23 + 96 <= 728);

        int fullHd = LocalBattleScreen.fitCellSize(1920, 1032, 10, 23);
        assertTrue(fullHd >= 30, "full HD keeps medium cells: " + fullHd);
        assertEquals(14, LocalBattleScreen.fitCellSize(100, 100, 10, 23));
    }

    @Test
    void setupMenuListsEveryModeThenBack() {
        String[] items = LocalBattleSetupScreen.menuItems();
        assertEquals(BattleModes.count() + 1, items.length);
        assertEquals("일반 모드", items[BattleModes.NORMAL_INDEX]);
        assertEquals("아이템 모드", items[BattleModes.ITEM_INDEX]);
        assertEquals("시간 제한 모드 (03:00)", items[BattleModes.TIME_LIMIT_INDEX]);
        assertEquals("뒤로", items[LocalBattleSetupScreen.BACK_INDEX]);
    }
}
