package blocks.blocks;

import blocks.Block;
import blocks.BlockType;

public class OBlock extends Block {

    public OBlock() {
        shape = new int[][] {
                {1, 1},
                {1, 1}
        };

        type = BlockType.O;
    }
}