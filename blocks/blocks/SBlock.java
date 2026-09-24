package blocks.blocks;

import blocks.Block;
import blocks.BlockType;

public class SBlock extends Block {

    public SBlock() {
        shape = new int[][] {
                {0, 1, 1},
                {1, 1, 0},
                {0, 0, 0}
        };

        type = BlockType.S;
    }
}