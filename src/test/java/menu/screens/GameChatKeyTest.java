package menu.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class GameChatKeyTest {

    @Test
    void usesEnterWithDefaultGameplayKeys() {
        int actual = GameScreen.chooseChatOpenKey(
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT,
                KeyEvent.VK_UP,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_SPACE,
                KeyEvent.VK_C
        );

        assertEquals(KeyEvent.VK_ENTER, actual);
    }

    @Test
    void usesF2WhenEnterIsAssignedToGameplay() {
        // 하드드롭 키를 Enter로 설정한 상황
        int actual = GameScreen.chooseChatOpenKey(
                KeyEvent.VK_LEFT,
                KeyEvent.VK_RIGHT,
                KeyEvent.VK_UP,
                KeyEvent.VK_DOWN,
                KeyEvent.VK_ENTER,
                KeyEvent.VK_C
        );

        assertEquals(KeyEvent.VK_F2, actual);
    }

    @Test
    void avoidsConflictsWhenSeveralCandidateKeysAreOccupied() {
        // 조작 키 6개가 모두 채팅 후보 키를 사용 중인 상황
        int[] gameplayKeys = {
            KeyEvent.VK_ENTER,
            KeyEvent.VK_F2,
            KeyEvent.VK_F3,
            KeyEvent.VK_F4,
            KeyEvent.VK_F6,
            KeyEvent.VK_F7
        };

        int actual = GameScreen.chooseChatOpenKey(gameplayKeys);

        // 선택한 키가 어떤 게임 조작 키와도 겹치면 안 된다.
        assertTrue(
                Arrays.stream(gameplayKeys)
                        .noneMatch(key -> key == actual),
                "채팅 열기 키가 게임 조작 키와 겹칩니다."
        );

        assertTrue(
                actual >= KeyEvent.VK_F2
                        && actual <= KeyEvent.VK_F9,
                "사용하지 않는 F키를 선택해야 합니다."
        );
    }

    @Test
    void prefersEnterWhenFunctionKeysAreOccupiedButEnterIsFree() {
        int actual = GameScreen.chooseChatOpenKey(
                KeyEvent.VK_F2,
                KeyEvent.VK_F3,
                KeyEvent.VK_F4,
                KeyEvent.VK_F6,
                KeyEvent.VK_F7,
                KeyEvent.VK_F8
        );

        assertEquals(KeyEvent.VK_ENTER, actual);
    }
}