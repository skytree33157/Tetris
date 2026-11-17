package sound;

import java.util.EnumMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import menu.settings.AppSettings;

// 효과음/BGM 재생. 오디오 장치가 없으면 조용히 무음으로 동작한다.
public final class SoundManager {

    private static final int POOL_SIZE = 3; // 연타해도 소리가 끊기지 않게
    private static final AudioFormat FORMAT =
            new AudioFormat(SoundSynth.SAMPLE_RATE, 16, 1, true, false);
    private static final SoundManager INSTANCE = new SoundManager();


    private final Map<SoundEvent, Clip[]> effects = new EnumMap<>(SoundEvent.class);
    private final Map<SoundEvent, Integer> nextIndex = new EnumMap<>(SoundEvent.class);
    private Clip bgm;
    private boolean available;
    private boolean bgmPlaying;

    private SoundManager() {
        try {
            for (SoundEvent event : SoundEvent.values()) {
                byte[] pcm = SoundSynth.effect(event);
                Clip[] pool = new Clip[POOL_SIZE];
                for (int i = 0; i < POOL_SIZE; i++) {
                    pool[i] = open(pcm);
                }
                effects.put(event, pool);
                nextIndex.put(event, 0);
            }
            bgm = open(SoundSynth.bgmLoop());
            available = true;
        } catch (Exception | LinkageError e) {
            // 헤드리스/사운드카드 없음 → 무음
            closeAll();
            available = false;
        }
    }

    public static SoundManager getInstance() {
        return INSTANCE;
    }

    public boolean isAvailable() {
        return available;
    }

    private static Clip open(byte[] pcm) throws Exception {
        Clip clip = AudioSystem.getClip();
        clip.open(FORMAT, pcm, 0, pcm.length);
        return clip;
    }

    public synchronized void play(SoundEvent event) {
        AppSettings settings = AppSettings.getInstance();
        if (!available || !settings.isSoundEnabled()) return;
        Clip[] pool = effects.get(event);
        int index = nextIndex.get(event);
        nextIndex.put(event, (index + 1) % pool.length);
        Clip clip = pool[index];
        clip.stop();
        clip.setFramePosition(0);
        applyVolume(clip, settings.getSfxVolume());
        clip.start();
    }

    public synchronized void startBgm() {
        AppSettings settings = AppSettings.getInstance();
        if (!available || !settings.isSoundEnabled()) return;
        bgm.stop();
        bgm.setFramePosition(0);
        applyVolume(bgm, settings.getBgmVolume());
        bgm.loop(Clip.LOOP_CONTINUOUSLY);
        bgmPlaying = true;
    }

    // 이미 재생 중이면 그대로 둠 (메뉴 ↔ 설정/스코어보드 이동 시 BGM이 처음부터 다시 시작되지 않게)
    public synchronized void ensureBgm() {
        if (!bgmPlaying) startBgm();
    }

    public synchronized boolean isBgmPlaying() {
        return bgmPlaying;
    }

    // 여러 번 호출돼도 안전 (매 프레임 호출 가능)
    public synchronized void setBgmPaused(boolean paused) {
        if (!available || !bgmPlaying) return;
        if (paused && bgm.isRunning()) {
            bgm.stop(); // 위치는 유지됨
        } else if (!paused && !bgm.isRunning()) {
            bgm.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public synchronized void stopBgm() {
        bgmPlaying = false;
        if (!available) return;
        bgm.stop();
        bgm.setFramePosition(0);
    }

    // 0~100 → dB
    static float toDecibel(int volume, float min, float max) {
        if (volume <= 0) return min;
        float db = (float) (20 * Math.log10(volume / 100.0));
        return Math.max(min, Math.min(max, db));
    }

    private static void applyVolume(Clip clip, int volume) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gain.setValue(toDecibel(volume, gain.getMinimum(), gain.getMaximum()));
    }

    private void closeAll() {
        for (Clip[] pool : effects.values()) {
            for (Clip clip : pool) if (clip != null) clip.close();
        }
        effects.clear();
        if (bgm != null) bgm.close();
        bgm = null;
    }
}