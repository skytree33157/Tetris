package blocks.tetromino;

import blocks.core.Block;
import blocks.core.BlockType;

public class OBlock extends Block {

    public OBlock() {
        shape = new int[][] {
                {1, 1},
                {1, 1}
        };

        type = BlockType.O;
    }
}