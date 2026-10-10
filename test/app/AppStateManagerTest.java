package app;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

// FR-01 시작 메뉴, FR-02 메뉴 간 상태 관리
class AppStateManagerTest {

    @Test // 시작 상태는 생성 시 전달한 상태
    void startsWithGivenState() {
        AppStateManager manager = new AppStateManager(AppState.START_MENU);
        assertEquals(AppState.START_MENU, manager.getCurrentState());
    }

    @Test // 메뉴 선택에 따른 상태 전이
    void transitionsBetweenMenuStates() {
        AppStateManager manager = new AppStateManager(AppState.START_MENU);

        manager.transitionTo(AppState.PLAYING);
        assertEquals(AppState.PLAYING, manager.getCurrentState());

        manager.transitionTo(AppState.SETTINGS);
        assertEquals(AppState.SETTINGS, manager.getCurrentState());

        manager.transitionTo(AppState.SCOREBOARD);
        assertEquals(AppState.SCOREBOARD, manager.getCurrentState());

        manager.transitionTo(AppState.EXIT);
        assertEquals(AppState.EXIT, manager.getCurrentState());
    }
}
