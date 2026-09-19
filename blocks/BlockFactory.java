package blocks;

import java.util.Random;

public class BlockFactory {

    private static final Random random = new Random();

    public static Block createRandomBlock() {
        int number = random.nextInt(7);

        return switch (number) {
            case 0 -> new IBlock();
            case 1 -> new OBlock();
            case 2 -> new TBlock();
            case 3 -> new SBlock();
            case 4 -> new ZBlock();
            case 5 -> new JBlock();
            case 6 -> new LBlock();
            default -> throw new IllegalStateException(
                    "Unexpected value: " + number
            );
        };
    }
}