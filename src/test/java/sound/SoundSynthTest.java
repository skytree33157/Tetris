package sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

// 소리 합성: 길이·진폭·무음·믹스 클리핑
class SoundSynthTest {

    @Test // 재생 시간만큼의 16bit 샘플 생성
    void toneLengthMatchesDuration() {
        byte[] pcm = SoundSynth.tone(440, 100, SoundSynth.Wave.SQUARE, 0.5);
        assertEquals(SoundSynth.sampleCount(100) * 2, pcm.length);
    }

    @ParameterizedTest // 어떤 파형이든 지정한 볼륨을 넘지 않음
    @EnumSource(SoundSynth.Wave.class)
    void toneStaysWithinVolume(SoundSynth.Wave wave) {
        byte[] pcm = SoundSynth.tone(440, 100, wave, 0.5);
        for (int i = 0; i < pcm.length / 2; i++) {
            assertTrue(Math.abs(SoundSynth.readSample(pcm, i)) <= 0.5 + 1e-3);
        }
    }

    @Test // 쉼표(주파수 0)는 무음
    void restIsSilent() {
        byte[] pcm = SoundSynth.tone(0, 50, SoundSynth.Wave.SINE, 1.0);
        for (byte b : pcm) {
            assertEquals(0, b);
        }
    }

    @Test // 소리 시작·끝은 엔벨로프로 0에서 시작해 클릭 노이즈 방지
    void toneStartsFromSilence() {
        byte[] pcm = SoundSynth.tone(440, 100, SoundSynth.Wave.SQUARE, 1.0);
        assertEquals(0.0, SoundSynth.readSample(pcm, 0), 1e-6);
    }

    @Test // 여러 음을 이어 붙이면 길이가 합쳐짐
    void sequenceConcatenatesNotes() {
        byte[] pcm = SoundSynth.sequence(SoundSynth.Wave.SQUARE, 0.3,
                new SoundSynth.Note(440, 50), new SoundSynth.Note(0, 30));
        assertEquals((SoundSynth.sampleCount(50) + SoundSynth.sampleCount(30)) * 2, pcm.length);
    }

    @Test // 큰 소리 두 개를 섞어도 최대 진폭을 넘지 않고, 긴 쪽 길이를 따름
    void mixClipsAndUsesLongerLength() {
        byte[] loud = SoundSynth.tone(100, 50, SoundSynth.Wave.SQUARE, 1.0);
        byte[] shorter = SoundSynth.tone(100, 20, SoundSynth.Wave.SQUARE, 1.0);
        byte[] mixed = SoundSynth.mix(loud, shorter);
        assertEquals(loud.length, mixed.length);
        for (int i = 0; i < mixed.length / 2; i++) {
            assertTrue(Math.abs(SoundSynth.readSample(mixed, i)) <= 1.0);
        }
    }

    @Test // MIDI 69 = A4 = 440Hz, 한 옥타브 위는 두 배
    void midiToFrequency() {
        assertEquals(440.0, SoundSynth.midi(69), 1e-9);
        assertEquals(880.0, SoundSynth.midi(81), 1e-9);
        assertEquals(0.0, SoundSynth.midi(0));
    }

    @ParameterizedTest // 모든 이벤트에 소리가 있음
    @EnumSource(SoundEvent.class)
    void everyEventHasSound(SoundEvent event) {
        assertTrue(SoundSynth.effect(event).length > 0);
    }

    @Test // BGM은 8분음표 32개(4마디) 길이
    void bgmLoopIsFourBars() {
        assertEquals(SoundSynth.sampleCount(SoundSynth.BGM_EIGHTH_MS) * 32 * 2,
                SoundSynth.bgmLoop().length);
    }
}
