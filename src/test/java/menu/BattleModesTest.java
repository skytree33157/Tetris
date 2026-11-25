package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.BattleMode;
import game.BattleSettings;
import game.GameMode;
import org.junit.jupiter.api.Test;

// 대전 방식 선택지: 일반 / 아이템 / 시간 제한
class BattleModesTest {
    @Test
    void choicesMapToBattleSettings() {
        assertEquals(3, BattleModes.count());
        assertEquals("시간 제한", BattleModes.labels()[BattleModes.TIME_LIMIT_INDEX]);

        BattleSettings normal = BattleModes.settings(BattleModes.NORMAL_INDEX);
        assertEquals(BattleMode.NORMAL, normal.getMode());
        assertFalse(normal.isTimeLimited());

        BattleSettings item = BattleModes.settings(BattleModes.ITEM_INDEX);
        assertEquals(GameMode.ITEM, item.getGameMode());
        assertEquals(0, item.getTimeLimitSeconds());

        // 시간 제한은 일반 블록 규칙 + 제한 시간
        BattleSettings timed = BattleModes.settings(BattleModes.TIME_LIMIT_INDEX);
        assertTrue(timed.isTimeLimited());
        assertEquals(GameMode.NORMAL, timed.getGameMode());
        assertEquals(BattleModes.TIME_LIMIT_SECONDS, timed.getTimeLimitSeconds());
    }

    @Test
    void labelsForResultScreen() {
        assertEquals("일반 모드", BattleModes.modeLabel(BattleModes.settings(BattleModes.NORMAL_INDEX)));
        assertEquals("아이템 모드", BattleModes.modeLabel(BattleModes.settings(BattleModes.ITEM_INDEX)));
        assertEquals("시간 제한 모드", BattleModes.modeLabel(BattleModes.settings(BattleModes.TIME_LIMIT_INDEX)));
        assertTrue(BattleModes.description(BattleModes.TIME_LIMIT_INDEX).startsWith("03:00"));
    }
}
