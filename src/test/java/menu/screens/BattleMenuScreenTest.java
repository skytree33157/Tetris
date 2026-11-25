package menu.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// 대전 메뉴 이동: 로컬 대전 / 방 만들기 / 참여하기 / 뒤로 모두 선택할 수 있다
class BattleMenuScreenTest {
    @Test
    void everyItemIsEnabled() {
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.LOCAL_INDEX));
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.HOST_INDEX));
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.JOIN_INDEX));
        assertTrue(BattleMenuScreen.isEnabledItem(BattleMenuScreen.BACK_INDEX));
    }

    @Test
    void navigationStartsAtLocalBattleAndWraps() {
        assertEquals(BattleMenuScreen.LOCAL_INDEX, BattleMenuScreen.nextEnabled(-1, 1));
        assertEquals(BattleMenuScreen.BACK_INDEX, BattleMenuScreen.nextEnabled(BattleMenuScreen.LOCAL_INDEX, -1));
        assertEquals(BattleMenuScreen.LOCAL_INDEX, BattleMenuScreen.nextEnabled(BattleMenuScreen.BACK_INDEX, 1));
        assertEquals(BattleMenuScreen.HOST_INDEX, BattleMenuScreen.nextEnabled(BattleMenuScreen.LOCAL_INDEX, 1));
    }
}
