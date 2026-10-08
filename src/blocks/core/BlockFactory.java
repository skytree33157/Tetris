package blocks.core;

import blocks.tetromino.*;

import java.util.Random;
import java.util.Objects;
import java.util.random.RandomGenerator;
import difficulty.Difficulty;
import item.core.ItemRegistry;
import item.core.ItemType;
import item.types.LineClearItem;
import item.types.BombItem;
import item.types.BonusItem;

public class BlockFactory {

    private static final Random random = new Random();
    private static final int OTHER_BLOCK_WEIGHT = 10;
    private static final BlockType[] TETROMINO_TYPES = {
            BlockType.I, BlockType.O, BlockType.T, BlockType.S,
            BlockType.Z, BlockType.J, BlockType.L
    };

    // 사용 예시:
    // Block random = BlockFactory.createRandomBlock();
    // random.getType() == BlockType.I; // I형 블록인지 확인
    // random.getWidth() == 4;          // 블록의 너비가 4칸인지 확인
    public static Block createRandomBlock() {
        return createRandomBlock(Difficulty.NORMAL);
    }

    /** 난이도별 상대 가중치에 따라 일반블럭 생성. */
    public static Block createRandomBlock(Difficulty difficulty) {
        return createRandomBlock(difficulty, random);
    }

    static Block createRandomBlock(Difficulty difficulty, RandomGenerator generator) {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(generator, "generator");
        return createBlock(selectBlockType(difficulty, generator));
    }

    // 지정한 아이템 생성. 기본 난이도 NORMAL.
    public static Block createItem(ItemType type) {
        return createItem(type, Difficulty.NORMAL);
    }

    // 종류는 지정하고, 부착형의 원본 블록에는 난이도 적용.
    public static Block createItem(ItemType type, Difficulty difficulty) {
        return ItemRegistry.create(type, difficulty);
    }

    /** 기본 난이도 NORMAL로 등록된 아이템 중 하나를 생성. */
    public static Block createRandomItem() {
        return createRandomItem(Difficulty.NORMAL);
    }

    /** 아이템 종류는 균등 선택하고, 부착형의 원본 블록에 전달한 난이도를 적용. */
    public static Block createRandomItem(Difficulty difficulty) {
        return ItemRegistry.createRandom(difficulty, random);
    }

    // 원본 블록의 채워진 칸을 골라 생성자에 L 위치를 전달한다.
    public static LineClearItem createRandomLineClearItem(Difficulty difficulty) {
        Block source = createRandomBlock(difficulty);
        int[] marker = selectRandomOccupiedCell(source);
        return new LineClearItem(source, marker[0], marker[1]);
    }

    /** 채워진 칸 하나에 P 부착. 원본 블록은 난이도별 확률로 생성. */
    public static BonusItem createRandomBonusItem(Difficulty difficulty) {
        Block source = createRandomBlock(difficulty);
        int[] marker = selectRandomOccupiedCell(source);
        return new BonusItem(source, marker[0], marker[1]);
    }

    /** 난이도와 무관하게 1×1 폭탄 생성. */
    public static BombItem createBombItem() {
        return new BombItem();
    }

    /** 표시 칸을 포함한 모든 채워진 칸 중 하나를 균등 선택해 {행, 열}로 반환. */
    public static int[] selectRandomOccupiedCell(Block block) {
        Objects.requireNonNull(block, "block");
        int[][] shape = block.getShape();
        int occupied = 0;
        for (int[] row : shape) {
            for (int cell : row) if (cell != 0) occupied++;
        }
        if (occupied == 0) throw new IllegalArgumentException("Block has no occupied cells");
        int selected = random.nextInt(occupied);
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0 && selected-- == 0) return new int[]{row, col};
            }
        }
        throw new IllegalStateException("Block shape changed during marker selection");
    }

    private static BlockType selectBlockType(Difficulty difficulty, RandomGenerator generator) {
        int iWeight = difficulty.getIBlockWeight();
        int maxWeight = Math.max(iWeight, OTHER_BLOCK_WEIGHT);

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
            default -> throw new IllegalArgumentException("Not a tetromino: " + type);
        };
    }
}
