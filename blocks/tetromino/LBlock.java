package blocks.tetromino;

import blocks.core.Block;
import blocks.core.BlockType;

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