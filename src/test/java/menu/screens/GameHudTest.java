package menu.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.util.List;

import org.junit.jupiter.api.Test;

import difficulty.Difficulty;
import game.GameMode;
import menu.ScoreRecord;

// 게임 화면 정보 패널 값 계산 (창 없이 검증)
class GameHudTest {

    @Test
    void formatsElapsedTimeAsMinutesAndSeconds() {
        assertEquals("00:00", GameHud.formatTime(0));
        assertEquals("00:00", GameHud.formatTime(999));
        assertEquals("00:01", GameHud.formatTime(1000));
        assertEquals("04:32", GameHud.formatTime(272_000));
        assertEquals("61:05", GameHud.formatTime(3_665_000));
        assertEquals("00:00", GameHud.formatTime(-500));
    }

    @Test
    void levelProgressFollowsWhicheverCounterIsCloserToLevelUp() {
        assertEquals(0.0, GameHud.levelProgress(1, 0, 10, 0, 10));
        assertEquals(0.4, GameHud.levelProgress(1, 4, 10, 2, 10), 1e-9);
        assertEquals(0.7, GameHud.levelProgress(1, 3, 10, 7, 10), 1e-9);
        assertEquals(1.0, GameHud.levelProgress(1, 12, 10, 0, 10));
    }

    @Test
    void maxLevelShowsFullProgressAndMaxLabel() {
        assertEquals(1.0, GameHud.levelProgress(GameHud.MAX_LEVEL, 0, 10, 0, 10));
        assertEquals("MAX", GameHud.nextLevelText(GameHud.MAX_LEVEL, 3, 10, 2, 10));
    }

    @Test
    void nextLevelTextShowsRemainingBlocksAndLines() {
        assertEquals("블록 10 · 라인 10", GameHud.nextLevelText(1, 0, 10, 0, 10));
        assertEquals("블록 6 · 라인 3", GameHud.nextLevelText(4, 4, 10, 7, 10));
    }

    @Test
    void comboIsShownFromTwoInARow() {
        assertEquals("-", GameHud.comboText(0));
        assertEquals("-", GameHud.comboText(1));
        assertEquals("x2", GameHud.comboText(2));
        assertEquals("x7", GameHud.comboText(7));
    }

    @Test
    void arrowKeysUseSymbolsAndOtherKeysUseTheirNames() {
        assertEquals("←", GameHud.keyName(KeyEvent.VK_LEFT));
        assertEquals("→", GameHud.keyName(KeyEvent.VK_RIGHT));
        assertEquals("↑", GameHud.keyName(KeyEvent.VK_UP));
        assertEquals("↓", GameHud.keyName(KeyEvent.VK_DOWN));
        assertEquals(KeyEvent.getKeyText(KeyEvent.VK_A), GameHud.keyName(KeyEvent.VK_A));
    }

    @Test
    void difficultyLabelsAreKorean() {
        assertEquals("쉬움", GameHud.difficultyLabel(Difficulty.EASY));
        assertEquals("보통", GameHud.difficultyLabel(Difficulty.NORMAL));
        assertEquals("어려움", GameHud.difficultyLabel(Difficulty.HARD));
    }

    @Test
    void bestScoreOnlyCountsTheSameDifficultyAndMode() {
        List<ScoreRecord> records = List.of(
                new ScoreRecord("A", 500, "NORMAL", "NORMAL"),
                new ScoreRecord("B", 900, "NORMAL", "NORMAL"),
                new ScoreRecord("C", 5000, "HARD", "NORMAL"),
                new ScoreRecord("D", 7000, "NORMAL", "ITEM"));

        assertEquals(900, GameHud.bestScore(records, Difficulty.NORMAL, GameMode.NORMAL));
        assertEquals(7000, GameHud.bestScore(records, Difficulty.NORMAL, GameMode.ITEM));
        assertEquals(0, GameHud.bestScore(records, Difficulty.EASY, GameMode.NORMAL));
        assertEquals(0, GameHud.bestScore(List.of(), Difficulty.HARD, GameMode.ITEM));
    }
}
