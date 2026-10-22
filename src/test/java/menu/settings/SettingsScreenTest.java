package menu.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import difficulty.Difficulty;
import org.junit.jupiter.api.Test;

class SettingsScreenTest {
    @Test
    void difficultySelectionCyclesForwardAndBackwardAndWraps() {
        assertEquals(Difficulty.NORMAL, SettingsScreen.cycleDifficulty(Difficulty.EASY, 1));
        assertEquals(Difficulty.HARD, SettingsScreen.cycleDifficulty(Difficulty.NORMAL, 1));
        assertEquals(Difficulty.EASY, SettingsScreen.cycleDifficulty(Difficulty.HARD, 1));
        assertEquals(Difficulty.HARD, SettingsScreen.cycleDifficulty(Difficulty.EASY, -1));
        assertEquals(Difficulty.EASY, SettingsScreen.cycleDifficulty(Difficulty.NORMAL, -1));
        assertEquals(Difficulty.NORMAL, SettingsScreen.cycleDifficulty(Difficulty.HARD, -1));
    }
}
