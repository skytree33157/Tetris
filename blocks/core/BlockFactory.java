package blocks.core;

import blocks.tetromino.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockFactory {

    private static final List<BlockType> bag = new ArrayList<>();

    public static Block createRandomBlock() {
        if (bag.isEmpty()) refillBag();

        BlockType type = bag.remove(bag.size() - 1);

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

    private static void refillBag() {
        bag.add(BlockType.I);
        bag.add(BlockType.O);
        bag.add(BlockType.T);
        bag.add(BlockType.S);
        bag.add(BlockType.Z);
        bag.add(BlockType.J);
        bag.add(BlockType.L);

        Collections.shuffle(bag);
    }
}