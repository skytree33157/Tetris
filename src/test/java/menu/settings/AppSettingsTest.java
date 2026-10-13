package menu.settings;

import static org.junit.jupiter.api.Assertions.*;

import blocks.style.ColorMode;
import difficulty.Difficulty;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// 설정 저장/불러오기, FR-26 난이도 설정
class AppSettingsTest {
    private static final Path SETTINGS_PATH = Paths.get("src/menu/settings/settings.properties");

    private final AppSettings settings = AppSettings.getInstance();
    private byte[] backup;

    @BeforeEach // 실제 설정 파일 백업 후 기본값에서 시작
    void setUp() throws IOException {
        backup = Files.exists(SETTINGS_PATH) ? Files.readAllBytes(SETTINGS_PATH) : null;
        settings.resetToDefaults();
    }

    @AfterEach // 테스트가 사용자 설정을 덮어쓰지 않도록 복원
    void tearDown() throws IOException {
        if (backup != null) {
            Files.write(SETTINGS_PATH, backup);
        } else {
            Files.deleteIfExists(SETTINGS_PATH);
        }
        settings.resetToDefaults();
        settings.load();
    }

    @Test // 기본값 복원
    void resetToDefaultsRestoresDefaultValues() {
        settings.setDifficulty(Difficulty.HARD);
        settings.setScreenSize(ScreenSize.LARGE);

        settings.resetToDefaults();

        assertAll(
                () -> assertEquals(ScreenSize.MEDIUM, settings.getScreenSize()),
                () -> assertEquals(ColorMode.NORMAL, settings.getColorMode()),
                () -> assertEquals(Difficulty.NORMAL, settings.getDifficulty()),
                () -> assertEquals(KeyEvent.VK_LEFT, settings.getKey(KeyAction.MOVE_LEFT)),
                () -> assertEquals(KeyEvent.VK_RIGHT, settings.getKey(KeyAction.MOVE_RIGHT)),
                () -> assertEquals(KeyEvent.VK_DOWN, settings.getKey(KeyAction.MOVE_DOWN)),
                () -> assertEquals(KeyEvent.VK_UP, settings.getKey(KeyAction.ROTATE))
        );
    }

    @Test // 저장 후 다시 불러와도 값 유지
    void saveAndLoadKeepsAllSettings() {
        settings.setScreenSize(ScreenSize.LARGE);
        settings.setColorMode(ColorMode.PROTANOPIA);
        settings.setDifficulty(Difficulty.HARD);
        settings.setKey(KeyAction.MOVE_LEFT, KeyEvent.VK_A);
        settings.save();

        settings.resetToDefaults();
        settings.load();

        assertAll(
                () -> assertEquals(ScreenSize.LARGE, settings.getScreenSize()),
                () -> assertEquals(ColorMode.PROTANOPIA, settings.getColorMode()),
                () -> assertEquals(Difficulty.HARD, settings.getDifficulty()),
                () -> assertEquals(KeyEvent.VK_A, settings.getKey(KeyAction.MOVE_LEFT))
        );
    }

    @Test // FR-26: 세 난이도 모두 저장·불러오기 가능
    void everyDifficultyCanBeSavedAndLoaded() {
        for (Difficulty difficulty : Difficulty.values()) {
            settings.setDifficulty(difficulty);
            settings.save();
            settings.resetToDefaults();
            settings.load();
            assertEquals(difficulty, settings.getDifficulty());
        }
    }

    @Test // FR-26: 이전 버전 파일처럼 difficulty 키가 없으면 기본 난이도 유지
    void missingDifficultyKeyKeepsDefault() throws IOException {
        Files.writeString(SETTINGS_PATH, "screenSize=LARGE\n");

        settings.load();

        assertEquals(ScreenSize.LARGE, settings.getScreenSize());
        assertEquals(Difficulty.NORMAL, settings.getDifficulty());
    }

    @Test // 저장된 값이 깨져 있으면 전체 기본값으로 복구
    void brokenValueFallsBackToDefaults() throws IOException {
        Files.writeString(SETTINGS_PATH, "screenSize=LARGE\ndifficulty=SUPER_HARD\n");

        settings.load();

        assertEquals(ScreenSize.MEDIUM, settings.getScreenSize());
        assertEquals(Difficulty.NORMAL, settings.getDifficulty());
    }
}
