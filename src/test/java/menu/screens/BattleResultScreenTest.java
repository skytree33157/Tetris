package menu.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

// 대전 결과 화면 버튼 이동: 대전 메뉴 ↔ 시작 메뉴, 양 끝에서 순환
class BattleResultScreenTest {
    @Test
    void selectionWrapsBetweenTwoButtons() {
        assertEquals(BattleResultScreen.START_MENU_INDEX,
                BattleResultScreen.next(BattleResultScreen.BATTLE_MENU_INDEX, 1));
        assertEquals(BattleResultScreen.BATTLE_MENU_INDEX,
                BattleResultScreen.next(BattleResultScreen.START_MENU_INDEX, 1));
        assertEquals(BattleResultScreen.START_MENU_INDEX,
                BattleResultScreen.next(BattleResultScreen.BATTLE_MENU_INDEX, -1));
    }
}
