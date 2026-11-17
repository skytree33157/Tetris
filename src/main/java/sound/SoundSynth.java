package sound;

import java.io.ByteArrayOutputStream;
import java.util.Random;

// 16bit mono PCM 소리 합성 (순수 함수 → 테스트 가능)
public final class SoundSynth {

    public static final float SAMPLE_RATE = 44100f;
    private static final int ATTACK_MS = 4;
    private static final int RELEASE_MS = 20;

    public enum Wave { SQUARE, TRIANGLE, SINE, NOISE }

    // freq <= 0 이면 쉼표
    public record Note(double freq, int ms) {}

    private SoundSynth() {}

    public static int sampleCount(int ms) {
        return (int) (SAMPLE_RATE * ms / 1000);
    }

    public static double midi(int note) {
        return note <= 0 ? 0 : 440.0 * Math.pow(2, (note - 69) / 12.0);
    }

    public static byte[] tone(double freq, int ms, Wave wave, double volume) {
        int n = sampleCount(ms);
        byte[] out = new byte[n * 2];
        if (freq <= 0) return out; // 쉼표 = 무음
        int attack = Math.min(sampleCount(ATTACK_MS), n / 2);
        int release = Math.min(sampleCount(RELEASE_MS), n / 2);
        Random noise = new Random(1234); // 결정적인 노이즈
        for (int i = 0; i < n; i++) {
            double phase = (i * freq / SAMPLE_RATE) % 1.0;
            double v = switch (wave) {
                case SQUARE -> phase < 0.5 ? 1 : -1;
                case TRIANGLE -> 4 * Math.abs(phase - 0.5) - 1;
                case SINE -> Math.sin(2 * Math.PI * phase);
                case NOISE -> noise.nextDouble() * 2 - 1;
            };
            double env = 1.0;
            if (i < attack) env = (double) i / attack;
            else if (i >= n - release) env = (double) (n - i) / release;
            writeSample(out, i, v * env * volume);
        }
        return out;
    }

    public static byte[] sequence(Wave wave, double volume, Note... notes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Note note : notes) {
            out.writeBytes(tone(note.freq(), note.ms(), wave, volume));
        }
        return out.toByteArray();
    }

    // 두 PCM을 더함 (길이는 긴 쪽, 클리핑 처리)
    public static byte[] mix(byte[] a, byte[] b) {
        int n = Math.max(a.length, b.length) / 2;
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double sum = readSample(a, i) + readSample(b, i);
            writeSample(out, i, Math.max(-1, Math.min(1, sum)));
        }
        return out;
    }

    public static double readSample(byte[] pcm, int index) {
        int pos = index * 2;
        if (pos + 1 >= pcm.length) return 0;
        short s = (short) ((pcm[pos] & 0xFF) | (pcm[pos + 1] << 8));
        return s / 32767.0;
    }

    private static void writeSample(byte[] out, int index, double value) {
        short s = (short) Math.round(value * 32767);
        out[index * 2] = (byte) s;            // little endian
        out[index * 2 + 1] = (byte) (s >> 8);
    }

    public static byte[] effect(SoundEvent event) {
        return switch (event) {
            case MOVE -> tone(220, 25, Wave.SQUARE, 0.20);
            case ROTATE -> sequence(Wave.SQUARE, 0.20, new Note(523, 25), new Note(784, 25));
            case HARD_DROP -> mix(tone(1, 60, Wave.NOISE, 0.30), tone(110, 90, Wave.TRIANGLE, 0.50));
            case LOCK -> tone(150, 50, Wave.TRIANGLE, 0.40);
            case HOLD -> sequence(Wave.SINE, 0.30, new Note(660, 40), new Note(880, 40));
            case LINE_CLEAR -> sequence(Wave.SQUARE, 0.25,
                    new Note(midi(72), 60), new Note(midi(76), 60), new Note(midi(79), 90));
            case TETRIS -> sequence(Wave.SQUARE, 0.30,
                    new Note(midi(72), 70), new Note(midi(76), 70), new Note(midi(79), 70),
                    new Note(midi(84), 70), new Note(midi(88), 180));
            case LEVEL_UP -> sequence(Wave.TRIANGLE, 0.40,
                    new Note(midi(67), 80), new Note(midi(72), 80), new Note(midi(76), 80),
                    new Note(midi(79), 80), new Note(midi(84), 200));
            case GAME_OVER -> sequence(Wave.SQUARE, 0.30,
                    new Note(midi(67), 180), new Note(midi(64), 180),
                    new Note(midi(60), 180), new Note(midi(55), 450));
        };
    }

    // 직접 만든 4마디 루프 (Am - F - C - G), 8분음표 기준
    private static final int[] MELODY = {
        69, 72, 76, 72, 74, 72, 69, 0,
        65, 69, 72, 69, 71, 69, 65, 0,
        67, 72, 76, 79, 76, 74, 72, 74,
        71, 74, 79, 74, 71, 67, 69, 0
    };
    private static final int[] BASS_ROOTS = { 45, 41, 48, 43 };
    public static final int BGM_EIGHTH_MS = 210;

    public static byte[] bgmLoop() {
        Note[] melody = new Note[MELODY.length];
        Note[] bass = new Note[MELODY.length];
        for (int i = 0; i < MELODY.length; i++) {
            melody[i] = new Note(midi(MELODY[i]), BGM_EIGHTH_MS);
            int root = BASS_ROOTS[i / 8];
            bass[i] = new Note(midi(i % 2 == 0 ? root : root + 12), BGM_EIGHTH_MS);
        }
        return mix(sequence(Wave.SQUARE, 0.15, melody), sequence(Wave.TRIANGLE, 0.25, bass));
    }
}