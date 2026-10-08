package score;
import difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// FR-17·18·38: 점수 정책 검증. 삭제·이동 판정은 테스트 범위 X.
class ScoreManagerTest {
    @Test
    void dropPointsFollowDistanceTypeLevelAndDifficulty() {
        int[] auto = {1,2,2,3,3,4,4,5,5,5};
        for (Difficulty difficulty : Difficulty.values()) {
            double factor = factor(difficulty);
            for (int level = 1; level <= 10; level++) for (DropType type : DropType.values()) {
                ScoreManager score = new ScoreManager(difficulty);
                int unit = switch(type) { case AUTO -> auto[level-1]; case SOFT -> 1; case HARD -> 2; };
                int expected = (int)Math.round(7 * unit * factor);
                assertEquals(expected, score.addDropScore(7, type, level));
                assertEquals(expected, score.getScore());
            }
        }
    }

    @Test
    void firstClearUsesBasePointsAndLevelNormalization() {
        int[] base = {100,300,500,800};
        // NORMAL, 콤보 1: 레벨 1·3·5·7·9의 정책상 결과.
        int[][] expected = {{100,300,500,800}, {147,442,737,1179},
                {195,584,974,1558}, {242,726,1211,1937}, {289,868,1447,2316}};
        for (int band = 0; band < 5; band++) for (int lines = 1; lines <= 4; lines++) {
            ScoreManager score = new ScoreManager();
            assertEquals(expected[band][lines-1], score.addLineClearScore(lines, band*2+1, false),
                    "레벨 " + (band*2+1) + " / 삭제 " + lines + " / 기본점수 " + base[lines-1]);
        }
    }

    @Test
    void difficultyIsAppliedAfterNormalizationAndBeforeRounding() {
        assertEquals(354, new ScoreManager(Difficulty.EASY).addLineClearScore(2, 3, false));
        assertEquals(442, new ScoreManager(Difficulty.NORMAL).addLineClearScore(2, 3, false));
        assertEquals(531, new ScoreManager(Difficulty.HARD).addLineClearScore(2, 3, false));
    }

    @Test
    void comboPerfectClearAndDifficultyReachDocumentedMaximum() {
        for (Difficulty difficulty : Difficulty.values()) {
            ScoreManager score = new ScoreManager(difficulty);
            int last = 0;
            for (int i = 0; i < 11; i++) last = score.addLineClearScore(4, 10, true);
            assertEquals((int)Math.round(8000 * factor(difficulty)), last);
            assertEquals(last, score.addLineClearScore(4, 10, true));
            assertEquals(12, score.getComboCount());
        }
    }

    @Test
    void emptyClearResetsComboButBonusAndDropsDoNot() {
        ScoreManager score = new ScoreManager();
        assertEquals(100, score.addLineClearScore(1, 1, false));
        assertEquals(105, score.addLineClearScore(1, 1, false));
        assertEquals(2, score.getComboCount());
        score.addBonusScore(1); score.addDropScore(1, DropType.AUTO, 1);
        assertEquals(2, score.getComboCount());
        int before = score.getScore();
        assertEquals(0, score.addLineClearScore(0, 1, false));
        assertEquals(before, score.getScore()); assertEquals(0, score.getComboCount());
        assertEquals(100, score.addLineClearScore(1, 1, false));
    }

    @Test
    void bonusIsFixedPerItemAndUsesDifficultyOnly() {
        for (Difficulty difficulty : Difficulty.values()) {
            ScoreManager score = new ScoreManager(difficulty);
            assertEquals(0, score.addBonusScore(0));
            assertEquals((int)(3000 * factor(difficulty)), score.addBonusScore(3));
            assertEquals(0, score.getComboCount());
        }
    }

    @Test
    void resetClearsScoreAndComboWithoutLosingDifficulty() {
        ScoreManager score = new ScoreManager(Difficulty.HARD);
        score.addLineClearScore(4, 10, true); score.addBonusScore(2);
        score.reset(); assertEquals(0, score.getScore()); assertEquals(0, score.getComboCount());
        assertEquals(1200, score.addBonusScore(1));
    }

    @Test
    void defaultConstructorKeepsNormalPolicyAcrossMixedEvents() {
        ScoreManager legacy = new ScoreManager(), normal = new ScoreManager(Difficulty.NORMAL);
        for (int level = 1; level <= 10; level++) {
            assertEquals(normal.addDropScore(3, DropType.HARD, level), legacy.addDropScore(3, DropType.HARD, level));
            assertEquals(normal.addLineClearScore(2, level, false), legacy.addLineClearScore(2, level, false));
            assertEquals(normal.addBonusScore(1), legacy.addBonusScore(1));
        }
        assertEquals(normal.getScore(), legacy.getScore());
    }

    @Test
    void invalidCountsAndLevelsDoNotChangeScoreOrCombo() {
        ScoreManager score = new ScoreManager(); score.addLineClearScore(1,1,false);
        for (int level : new int[]{0,11}) {
            assertThrows(IllegalArgumentException.class, () -> score.addDropScore(1, DropType.AUTO, level));
            assertThrows(IllegalArgumentException.class, () -> score.addLineClearScore(1, level, false));
        }
        for (int count : new int[]{-1,5}) assertThrows(IllegalArgumentException.class,
                () -> score.addLineClearScore(count,1,false));
        assertThrows(IllegalArgumentException.class, () -> score.addBonusScore(-1));
        assertThrows(NullPointerException.class, () -> new ScoreManager(null));
        assertEquals(100, score.getScore()); assertEquals(1, score.getComboCount());
        assertEquals(0, score.addDropScore(0, DropType.AUTO,1));
        assertEquals(0, score.addDropScore(-1, DropType.AUTO,1));
    }
    private static double factor(Difficulty difficulty) {
        return switch(difficulty) { case EASY -> 0.8; case NORMAL -> 1.0; case HARD -> 1.2; };
    }
}
