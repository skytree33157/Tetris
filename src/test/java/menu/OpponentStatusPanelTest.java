package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import network.GameStatus;

class OpponentStatusPanelTest {

    @Test
    void activeOpponentShowsSlotNumberAndProgress() {
        List<String> lines = OpponentStatusPanel.formatLines(1, new GameStatus(1200, 3, 12, false));

        assertEquals(List.of("P2", "점수 1200 / Lv 3 / 12줄"), lines);
    }

    @Test
    void eliminatedOpponentIsMarkedInHeader() {
        List<String> lines = OpponentStatusPanel.formatLines(2, new GameStatus(300, 1, 2, true));

        assertEquals("P3 (탈락)", lines.get(0));
    }
}
