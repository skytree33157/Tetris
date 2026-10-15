package storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.Files;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import menu.ScoreRecord;

class ScoreStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsKoreanRecord() {
        // 준비: 실제 점수 파일 대신 임시 파일 사용
        ScoreStorage storage =
                new ScoreStorage(tempDir.resolve("scores.txt").toString());

        ArrayList<ScoreRecord> records = new ArrayList<>();
        records.add(new ScoreRecord("지훈", 60000, "HARD", "ITEM"));

        // 실행: 저장하고 다시 읽기
        storage.save(records);
        ArrayList<ScoreRecord> loaded = storage.load();

        // 검증: 기록 개수와 모든 값이 같은지 확인
        assertEquals(1, loaded.size());

        ScoreRecord actual = loaded.get(0);
        assertEquals("지훈", actual.getName());
        assertEquals(60000, actual.getScore());
        assertEquals("HARD", actual.getDifficulty());
        assertEquals("ITEM", actual.getMode());
    }

    @Test
    void savesAndLoadsMultipleRecords() {
        ScoreStorage storage =
                new ScoreStorage(tempDir.resolve("scores.txt").toString());

        ArrayList<ScoreRecord> records = new ArrayList<>();
        records.add(new ScoreRecord("지훈", 60000, "HARD", "ITEM"));
        records.add(new ScoreRecord("태준", 30000, "EASY", "NORMAL"));
        records.add(new ScoreRecord("정민", 45000, "NORMAL", "ITEM"));

        storage.save(records);
        ArrayList<ScoreRecord> loaded = storage.load();

        assertEquals(records.size(), loaded.size());

        for (int i = 0; i < records.size(); i++) {
            ScoreRecord expected = records.get(i);
            ScoreRecord actual = loaded.get(i);

            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getScore(), actual.getScore());
            assertEquals(expected.getDifficulty(), actual.getDifficulty());
            assertEquals(expected.getMode(), actual.getMode());
        }
    }

    @Test
    void clearsSavedRecords() {
        ScoreStorage storage =
                new ScoreStorage(tempDir.resolve("scores.txt").toString());

        ArrayList<ScoreRecord> records = new ArrayList<>();
        records.add(new ScoreRecord("지훈", 60000, "HARD", "ITEM"));
        records.add(new ScoreRecord("태준", 30000, "EASY", "NORMAL"));
        storage.save(records);

        // 초기화 전에 기록이 저장돼 있는지 확인
        assertEquals(2, storage.load().size());

        // 초기화 성공 여부와 기록이 비워졌는지 확인
        assertEquals(true, storage.clear());
        assertEquals(0, storage.load().size());
    }

    @Test
    void addsRecordWithoutLosingExistingRecords() {
        ScoreStorage storage =
                new ScoreStorage(tempDir.resolve("scores.txt").toString());

        ArrayList<ScoreRecord> records = new ArrayList<>();
        records.add(new ScoreRecord("지훈", 60000, "HARD", "ITEM"));
        storage.save(records);

        // 기존 기록이 있는 상태에서 새 기록 추가
        storage.addRecord(
                new ScoreRecord("태준", 30000, "EASY", "NORMAL")
        );

        ArrayList<ScoreRecord> loaded = storage.load();

        assertEquals(2, loaded.size());

        // 기존 기록이 유지되는지 확인
        ScoreRecord existing = loaded.get(0);
        assertEquals("지훈", existing.getName());
        assertEquals(60000, existing.getScore());
        assertEquals("HARD", existing.getDifficulty());
        assertEquals("ITEM", existing.getMode());

        // 새 기록이 추가됐는지 확인
        ScoreRecord added = loaded.get(1);
        assertEquals("태준", added.getName());
        assertEquals(30000, added.getScore());
        assertEquals("EASY", added.getDifficulty());
        assertEquals("NORMAL", added.getMode());
    }

    @Test
    void loadsEmptyListWhenFileDoesNotExist() {
        Path scorePath = tempDir.resolve("scores.txt");

        // 파일이 없는 최초 실행 상황인지 확인
        assertFalse(Files.exists(scorePath));

        ScoreStorage storage = new ScoreStorage(scorePath.toString());

        ArrayList<ScoreRecord> loaded = storage.load();

        // 저장된 기록이 없으므로 빈 목록이어야 함
        assertEquals(0, loaded.size());
    }
    @Test
    void skipsInvalidRecordsAndLoadsValidRecords() throws IOException {
        Path scorePath = tempDir.resolve("scores.txt");

        String content =
                "지훈,60000,HARD,ITEM\n"
                + "항목이 부족한 기록,100\n"
                + "잘못된점수,abc,EASY,NORMAL\n"
                + "태준,30000,EASY,NORMAL\n";

        Files.writeString(scorePath, content, StandardCharsets.UTF_8);

        ScoreStorage storage = new ScoreStorage(scorePath.toString());
        ArrayList<ScoreRecord> loaded = storage.load();

        assertEquals(2, loaded.size());

        ScoreRecord first = loaded.get(0);
        assertEquals("지훈", first.getName());
        assertEquals(60000, first.getScore());
        assertEquals("HARD", first.getDifficulty());
        assertEquals("ITEM", first.getMode());

        ScoreRecord second = loaded.get(1);
        assertEquals("태준", second.getName());
        assertEquals(30000, second.getScore());
        assertEquals("EASY", second.getDifficulty());
        assertEquals("NORMAL", second.getMode());
    }
}