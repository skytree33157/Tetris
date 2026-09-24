package blocks.blocks;

import blocks.Block;
import blocks.BlockType;

public class LBlock extends Block {

    public LBlock() {
        shape = new int[][] {
                {1, 1, 1},
                {1, 0, 0},
                {0, 0, 0}
        };

        type = BlockType.L;
    }
}