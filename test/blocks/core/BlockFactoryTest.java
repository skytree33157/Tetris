package blocks.core;

import blocks.tetromino.*;
import difficulty.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

// FR-04·05·27 / NFR-11: 실제 생성 결과와 설정 확률 검증.
class BlockFactoryTest {
    // 요구사항의 1,000회 이상 충족. 상대오차 5% 기준으로 검증.
    private static final int SAMPLES = 100_000;
    private static final double RELATIVE_TOLERANCE = 0.05;
    private static final BlockType[] TYPES = {
            BlockType.I, BlockType.O, BlockType.T, BlockType.S,
            BlockType.Z, BlockType.J, BlockType.L
    };


    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void generatedBlocksFollowDifficultyProbabilities(Difficulty difficulty) {
        RandomGenerator generator = new Random(20261007L);
        int[] counts = new int[TYPES.length];
        for (int sample = 0; sample < SAMPLES; sample++) {
            Block block = BlockFactory.createRandomBlock(difficulty, generator);
            counts[Arrays.asList(TYPES).indexOf(block.getType())]++;
        }
        assertDifficultyDistribution("seeded " + difficulty, difficulty, counts);
    }


    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void publicFactoryUsesRequestedDifficulty(Difficulty difficulty) {
        int[] counts = new int[TYPES.length];
        for (int sample = 0; sample < SAMPLES; sample++) {
            counts[Arrays.asList(TYPES).indexOf(
                    BlockFactory.createRandomBlock(difficulty).getType())]++;
        }
        assertDifficultyDistribution("public " + difficulty, difficulty, counts);
    }

    @Test
    void noArgumentFactoryPreservesNormalDistribution() {
        int[] counts = new int[TYPES.length];
        for (int sample = 0; sample < SAMPLES; sample++) {
            counts[Arrays.asList(TYPES).indexOf(BlockFactory.createRandomBlock().getType())]++;
        }
        double[] expected = new double[TYPES.length];
        Arrays.fill(expected, 1.0 / 7);
        assertDistribution("default NORMAL", counts, expected);
    }

    @Test
    void easyReselectsCandidateAfterRejection() {

        Block result = BlockFactory.createRandomBlock(Difficulty.EASY,
                new ScriptedRandom(new int[]{1, 0}, new double[]{0.9, 0.99}));
        assertInstanceOf(IBlock.class, result);
    }

    @Test
    void easyRejectsAtAcceptanceBoundary() {
        Block result = BlockFactory.createRandomBlock(Difficulty.EASY,
                new ScriptedRandom(new int[]{1, 0}, new double[]{10.0 / 12, 0.99}));
        assertInstanceOf(IBlock.class, result);
    }

    @Test
    void hardReselectsCandidateAfterRejectingIAtBoundary() {

        Block result = BlockFactory.createRandomBlock(Difficulty.HARD,
                new ScriptedRandom(new int[]{0, 1}, new double[]{0.8, 0.99}));
        assertInstanceOf(OBlock.class, result);
    }

    @Test
    void selectedTypesProduceMatchingTetrominoClasses() {
        Class<?>[] expectedClasses = {IBlock.class, OBlock.class, TBlock.class,
                SBlock.class, ZBlock.class, JBlock.class, LBlock.class};
        for (int i = 0; i < TYPES.length; i++) {
            Block result = BlockFactory.createRandomBlock(Difficulty.NORMAL,
                    new ScriptedRandom(new int[]{i}, new double[]{0.0}));
            assertEquals(expectedClasses[i], result.getClass());
            assertEquals(TYPES[i], result.getType());
        }
    }

    @Test
    void eachGenerationProducesIndependentBlockState() {
        Block first = BlockFactory.createRandomBlock(Difficulty.NORMAL,
                new ScriptedRandom(new int[]{2}, new double[]{0.0}));
        first.moveRight();
        first.getShape()[0][1] = 0;
        Block second = BlockFactory.createRandomBlock(Difficulty.NORMAL,
                new ScriptedRandom(new int[]{2}, new double[]{0.0}));
        assertNotSame(first, second);
        assertEquals(0, second.getX());
        assertEquals(1, second.getShape()[0][1]);
    }

    @Test
    void rejectsMissingDifficulty() {
        assertThrows(NullPointerException.class, () -> BlockFactory.createRandomBlock(null));
    }

    @Test
    void rejectsMissingRandomGenerator() {
        assertThrows(NullPointerException.class,
                () -> BlockFactory.createRandomBlock(Difficulty.NORMAL, null));
    }

    private static void assertDifficultyDistribution(String label, Difficulty difficulty, int[] counts) {

        double[] expected = switch (difficulty) {
            case EASY -> new double[]{12.0 / 72, 10.0 / 72, 10.0 / 72, 10.0 / 72,
                    10.0 / 72, 10.0 / 72, 10.0 / 72};
            case NORMAL -> new double[]{1.0 / 7, 1.0 / 7, 1.0 / 7, 1.0 / 7,
                    1.0 / 7, 1.0 / 7, 1.0 / 7};
            case HARD -> new double[]{8.0 / 68, 10.0 / 68, 10.0 / 68, 10.0 / 68,
                    10.0 / 68, 10.0 / 68, 10.0 / 68};
        };
        assertDistribution(label, counts, expected);
    }

    private static void assertDistribution(String label, int[] counts, double[] expected) {
        System.out.printf("%s: samples=%d, counts(I/O/T/S/Z/J/L)=%s%n",
                label, SAMPLES, Arrays.toString(counts));
        double maxRelativeError = 0;
        for (int i = 0; i < TYPES.length; i++) {
            double observed = counts[i] / (double) SAMPLES;
            maxRelativeError = Math.max(maxRelativeError, Math.abs(observed - expected[i]) / expected[i]);
            assertEquals(expected[i], observed, expected[i] * RELATIVE_TOLERANCE,
                    label + " " + TYPES[i] + ": relative probability error exceeds 5%");
        }
        System.out.printf("%s: max relative error=%.3f%%%n", label, maxRelativeError * 100);
    }

    private static final class ScriptedRandom implements RandomGenerator {
        private final int[] candidates;
        private final double[] thresholds;
        private int candidateIndex;
        private int thresholdIndex;

        private ScriptedRandom(int[] candidates, double[] thresholds) {
            this.candidates = candidates;
            this.thresholds = thresholds;
        }

        @Override
        public int nextInt(int bound) {
            if (bound != 7 || candidateIndex >= candidates.length) {
                throw new AssertionError("Unexpected candidate selection");
            }
            return candidates[candidateIndex++];
        }

        @Override
        public double nextDouble() {
            if (thresholdIndex >= thresholds.length) {
                throw new AssertionError("Unexpected acceptance attempt");
            }
            return thresholds[thresholdIndex++];
        }

        @Override
        public long nextLong() {
            throw new AssertionError("Unexpected random operation");
        }
    }
}
