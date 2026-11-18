package sound;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import menu.settings.AppSettings;

// 재생 관리: 오디오 장치 유무와 관계없이 안전, BGM 상태 전환
class SoundManagerTest {

    private final SoundManager sound = SoundManager.getInstance();
    private final AppSettings settings = AppSettings.getInstance();

    @BeforeEach // 소리는 켜되 볼륨 0으로 테스트 중 실제 소리는 나지 않게
    void setUp() {
        settings.setSoundEnabled(true);
        settings.setBgmVolume(0);
        settings.setSfxVolume(0);
        sound.stopBgm();
    }

    @AfterEach
    void tearDown() {
        sound.stopBgm();
        settings.resetToDefaults();
        settings.load();
    }

    @Test // 장치가 없어도(CI 등) 예외 없이 무음으로 동작
    void neverThrowsWithOrWithoutAudioDevice() {
        assertDoesNotThrow(() -> {
            for (SoundEvent event : SoundEvent.values()) {
                sound.play(event);
            }
            sound.startBgm();
            sound.setBgmPaused(true);
            sound.setBgmPaused(false);
            sound.ensureBgm();
            sound.stopBgm();
        });
    }

    @Test // 시작 → 정지 상태 전환
    void startAndStopBgm() {
        assumeTrue(sound.isAvailable(), "오디오 장치 없음");
        sound.startBgm();
        assertTrue(sound.isBgmPlaying());
        sound.stopBgm();
        assertFalse(sound.isBgmPlaying());
    }

    @Test // 메뉴용: 정지 상태에서만 시작, 재생 중이면 그대로 유지
    void ensureBgmStartsOnlyWhenStopped() {
        assumeTrue(sound.isAvailable(), "오디오 장치 없음");
        sound.ensureBgm();
        assertTrue(sound.isBgmPlaying());
        sound.ensureBgm();
        assertTrue(sound.isBgmPlaying());
    }

    @Test // 소리 꺼짐 설정이면 BGM을 시작하지 않음
    void disabledSoundDoesNotStartBgm() {
        settings.setSoundEnabled(false);
        sound.startBgm();
        sound.ensureBgm();
        assertFalse(sound.isBgmPlaying());
    }

    @Test // 재생 중이 아닐 때 일시정지 해제해도 BGM이 켜지지 않음
    void resumeWithoutStartDoesNothing() {
        sound.setBgmPaused(false);
        assertFalse(sound.isBgmPlaying());
    }

    @Test // 볼륨 0~100 → dB 변환, 장치 범위로 제한
    void volumeToDecibel() {
        assertEquals(-80f, SoundManager.toDecibel(0, -80f, 6f));
        assertEquals(0f, SoundManager.toDecibel(100, -80f, 6f), 1e-4);
        assertEquals(-6.02f, SoundManager.toDecibel(50, -80f, 6f), 0.01);
        assertEquals(-10f, SoundManager.toDecibel(1, -10f, 6f)); // -40dB → 최소값으로 제한
    }

    @Test // 설정 화면에서 켜고 끄면 BGM 상태가 따라간다 (장치 없으면 계속 정지)
    void applySettingsFollowsSoundToggle() {
        settings.setSoundEnabled(false);
        assertDoesNotThrow(sound::applySettings);
        assertFalse(sound.isBgmPlaying());

        settings.setSoundEnabled(true);
        assertDoesNotThrow(sound::applySettings);
        assertEquals(sound.isAvailable(), sound.isBgmPlaying());
    }

}
