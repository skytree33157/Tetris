package blocks.tetromino;

import blocks.core.Block;
import blocks.core.BlockType;

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