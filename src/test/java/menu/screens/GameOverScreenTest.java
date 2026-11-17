package menu.screens;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import storage.ScoreStorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import menu.ScoreRecord;

class GameOverScreenTest {
    @TempDir
    Path tempDir;

    @Test
    void acceptsRecordWhenFewerThanTenRecordsExist() {
        ArrayList<ScoreRecord> records = new ArrayList<>();

        for (int i = 0; i < 9; i++) {
            records.add(
                    new ScoreRecord("PLAYER", 1000 - i, "HARD", "ITEM")
            );
        }

        // 자리가 남아 있으므로 기존 기록보다 낮은 점수도 등록 가능
        assertTrue(GameOverScreen.canRegisterRecord(records, 1));
    }

    @ParameterizedTest
    @CsvSource({
        "101, true",
        "100, false",
        "99, false"
    })
    void checksScoreAgainstTenthPlace(int score, boolean expected) {
        ArrayList<ScoreRecord> records = new ArrayList<>();

        // 1000, 900, ... 100점: 총 10개이며 10위는 100점
        for (int i = 0; i < 10; i++) {
            records.add(
                    new ScoreRecord("PLAYER", 1000 - i * 100, "HARD", "ITEM")
            );
        }

        org.junit.jupiter.api.Assertions.assertEquals(
                expected,
                GameOverScreen.canRegisterRecord(records, score)
        );
    }

    @Test
    void selectsMatchingCategoryAndSortsByScoreDescending() {
        ArrayList<ScoreRecord> records = new ArrayList<>();

        ScoreRecord lower =
                new ScoreRecord("지훈", 1000, "HARD", "ITEM");
        ScoreRecord higher =
                new ScoreRecord("태준", 3000, "HARD", "ITEM");

        // 같은 조합의 기록을 일부러 낮은 점수부터 넣음
        records.add(lower);
        records.add(new ScoreRecord("다른난이도", 9000, "EASY", "ITEM"));
        records.add(new ScoreRecord("다른모드", 8000, "HARD", "NORMAL"));
        records.add(higher);

        ArrayList<ScoreRecord> selected =
                GameOverScreen.selectCategoryRecords(records, "HARD", "ITEM");

        assertEquals(2, selected.size());
        assertEquals(higher, selected.get(0));
        assertEquals(lower, selected.get(1));
    }

    @Test
    void keepsOnlyTopTenInTheSelectedCategoryAndPreservesOtherCategories() {
        ArrayList<ScoreRecord> allRecords = new ArrayList<>();
        for (int score = 1; score <= 12; score++) {
            allRecords.add(new ScoreRecord("ITEM-" + score, score, "NORMAL", "ITEM"));
        }
        ScoreRecord otherMode = new ScoreRecord("NORMAL", 50_000, "NORMAL", "NORMAL");
        allRecords.add(otherMode);

        ArrayList<ScoreRecord> itemRecords =
                GameOverScreen.selectCategoryRecords(allRecords, "NORMAL", "ITEM");
        itemRecords.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed());
        GameOverScreen.retainTopTen(allRecords, itemRecords);

        assertEquals(10, itemRecords.size());
        assertEquals(10, GameOverScreen.selectCategoryRecords(allRecords, "NORMAL", "ITEM").size());
        assertEquals(11, allRecords.size());
        assertTrue(allRecords.contains(otherMode));
        assertEquals(12, itemRecords.get(0).getScore());
        assertEquals(3, itemRecords.get(9).getScore());

        ScoreStorage storage = new ScoreStorage(tempDir.resolve("scores.txt").toString());
        storage.save(allRecords);
        assertEquals(10, GameOverScreen.selectCategoryRecords(
                storage.load(), "NORMAL", "ITEM").size());
    }

    @Test
    void normalizesPlayerName() {
        assertEquals("PLAYER", GameOverScreen.normalizeName(null));
        assertEquals("PLAYER", GameOverScreen.normalizeName("   "));
        assertEquals("PLAYER", GameOverScreen.normalizeName("a,b"));
        assertEquals("TESTER", GameOverScreen.normalizeName("  TESTER "));
        assertEquals("1234567890", GameOverScreen.normalizeName("12345678901"));
    }

    @Test // 게임오버·스코어보드 화면의 ←→ 난이도 순환과 Tab 모드 전환
    void scoreViewDifficultyCyclesAndModeToggles() {
        assertEquals("NORMAL", GameOverScreen.nextDifficulty("EASY", 1));
        assertEquals("HARD", GameOverScreen.nextDifficulty("NORMAL", 1));
        assertEquals("EASY", GameOverScreen.nextDifficulty("HARD", 1));
        assertEquals("HARD", GameOverScreen.nextDifficulty("EASY", -1));
        assertEquals("EASY", GameOverScreen.nextDifficulty("NORMAL", -1));
        assertEquals("NORMAL", GameOverScreen.nextDifficulty("HARD", -1));
        // 알 수 없는 값은 NORMAL 기준으로 이동
        assertEquals("HARD", GameOverScreen.nextDifficulty("?", 1));

        assertEquals("ITEM", GameOverScreen.nextMode("NORMAL"));
        assertEquals("NORMAL", GameOverScreen.nextMode("ITEM"));
    }

    @Test
    void newBestRequiresPositiveScoreAboveFirstPlace() {
        ArrayList<ScoreRecord> records = new ArrayList<>();
        assertTrue(GameOverScreen.isNewBest(records, 1));
        assertFalse(GameOverScreen.isNewBest(records, 0));

        records.add(new ScoreRecord("PLAYER", 1000, "HARD", "ITEM"));
        assertTrue(GameOverScreen.isNewBest(records, 1001));
        assertFalse(GameOverScreen.isNewBest(records, 1000));
    }
}