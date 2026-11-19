package menu.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// 대전 메뉴 이동: 준비 중인 로컬 대전 항목은 건너뛴다
class BattleMenuScreenTest {
    @Test
    void localBattleIsDisabledUntilScreenExists() {
        assertFalse(BattleMenuScreen.isEnabledItem(BattleMenuScreen.LOCAL_INDEX));
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.HOST_INDEX));
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.JOIN_INDEX));
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.BACK_INDEX));
    }

    @Test
    void navigationSkipsDisabledItemAndWraps() {
        assertEquals(BattleMenuScreen.HOST_INDEX, BattleMenuScreen.nextEnabled(-1, 1));
        assertEquals(BattleMenuScreen.BACK_INDEX, BattleMenuScreen.nextEnabled(BattleMenuScreen.HOST_INDEX, -1));
        assertEquals(BattleMenuScreen.HOST_INDEX, BattleMenuScreen.nextEnabled(BattleMenuScreen.BACK_INDEX, 1));
        assertEquals(BattleMenuScreen.JOIN_INDEX, BattleMenuScreen.nextEnabled(BattleMenuScreen.HOST_INDEX, 1));
    }
}
