package storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// 설정/점수 파일 저장 위치 결정 검증
class DataPathsTest {

    private static final String DATA_DIR_KEY = "tetris.dataDir";
    private static final String APP_PATH_KEY = "jpackage.app-path";

    @TempDir
    Path tempDir;

    private String originalDataDir;
    private String originalAppPath;

    // 다른 테스트가 쓰는 시스템 프로퍼티를 바꾸므로 원래 값을 저장해 두었다가 복원
    @BeforeEach
    void backupProperties() {
        originalDataDir = System.getProperty(DATA_DIR_KEY);
        originalAppPath = System.getProperty(APP_PATH_KEY);
    }

    @AfterEach
    void restoreProperties() {
        restore(DATA_DIR_KEY, originalDataDir);
        restore(APP_PATH_KEY, originalAppPath);
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void dataDirOverrideIsUsedAndCreated() {
        File expected = tempDir.resolve("custom").toFile();
        System.setProperty(DATA_DIR_KEY, expected.getPath());

        File dir = DataPaths.dataDir();

        assertEquals(expected, dir);
        assertTrue(dir.isDirectory());
    }

    @Test
    void overrideTakesPriorityOverJpackagePath() {
        File expected = tempDir.resolve("override").toFile();
        System.setProperty(DATA_DIR_KEY, expected.getPath());
        System.setProperty(APP_PATH_KEY, tempDir.resolve("app/TFSE.exe").toString());

        assertEquals(expected, DataPaths.dataDir());
    }

    @Test
    void blankOverrideIsIgnored() {
        System.setProperty(DATA_DIR_KEY, "  ");
        File appDir = tempDir.resolve("app").toFile();
        System.setProperty(APP_PATH_KEY, new File(appDir, "TFSE.exe").getPath());

        assertEquals(new File(appDir, "data"), DataPaths.dataDir());
    }

    @Test
    void jpackageAppPathUsesDataFolderNextToExe() {
        System.clearProperty(DATA_DIR_KEY);
        File appDir = tempDir.resolve("app").toFile();
        System.setProperty(APP_PATH_KEY, new File(appDir, "TFSE.exe").getPath());

        File dir = DataPaths.dataDir();

        assertEquals(new File(appDir, "data"), dir);
        assertTrue(dir.isDirectory());
    }

    @Test
    void settingsAndScoresFilesAreInsideDataDir() {
        File dir = tempDir.resolve("data").toFile();
        System.setProperty(DATA_DIR_KEY, dir.getPath());

        assertEquals(new File(dir, "settings.properties"), DataPaths.settingsFile());
        assertEquals(new File(dir, "scores.txt"), DataPaths.scoresFile());
    }
}
