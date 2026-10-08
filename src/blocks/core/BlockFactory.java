package blocks.core;

import blocks.tetromino.*;

import java.util.Random;
import java.util.Objects;
import java.util.random.RandomGenerator;
import difficulty.Difficulty;

public class BlockFactory {

    private static final Random random = new Random();
    private static final int OTHER_BLOCK_WEIGHT = 10;
    private static final BlockType[] TETROMINO_TYPES = {
            BlockType.I, BlockType.O, BlockType.T, BlockType.S,
            BlockType.Z, BlockType.J, BlockType.L
    };

    // How to use:
    // Block random = BlockFactory.createRandomBlock();
    // random.getType() == BlockType.I; // true if the block is of type I
    // random.getWidth() == 4;          // true if the block width is 4
    public static Block createRandomBlock() {
        return createRandomBlock(Difficulty.NORMAL);
    }

    /** Creates a tetromino with the difficulty's relative selection weights. */
    public static Block createRandomBlock(Difficulty difficulty) {
        return createRandomBlock(difficulty, random);
    }

    static Block createRandomBlock(Difficulty difficulty, RandomGenerator generator) {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(generator, "generator");
        return createBlock(selectBlockType(difficulty, generator));
    }

    private static BlockType selectBlockType(Difficulty difficulty, RandomGenerator generator) {
        int iWeight = difficulty.getIBlockWeight();
        int maxWeight = Math.max(iWeight, OTHER_BLOCK_WEIGHT);

        // Lipowski & Lipowska, arXiv:1109.3627, section II:
        // choose a uniform candidate, accept with w_i / w_max, reselect on rejection.
        while (true) {
            BlockType candidate = TETROMINO_TYPES[generator.nextInt(TETROMINO_TYPES.length)];
            int weight = candidate == BlockType.I ? iWeight : OTHER_BLOCK_WEIGHT;
            if (generator.nextDouble() < weight / (double) maxWeight) {
                return candidate;
            }
        }
    }

    private static Block createBlock(BlockType type) {
        return switch (type) {
            case I -> new IBlock();
            case O -> new OBlock();
            case T -> new TBlock();
            case S -> new SBlock();
            case Z -> new ZBlock();
            case J -> new JBlock();
            case L -> new LBlock();
        };
    }
}
