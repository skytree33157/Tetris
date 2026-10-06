package blocks.tetromino;

import blocks.core.Block;
import blocks.core.BlockType;

public class ZBlock extends Block {

    public ZBlock() {
        shape = new int[][] {
                {1, 1, 0},
                {0, 1, 1},
                {0, 0, 0}
        };

        type = BlockType.Z;
    }
}