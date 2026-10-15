package menu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;

import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameOverScreenTest {

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
}