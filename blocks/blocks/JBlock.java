package blocks.blocks;

import blocks.Block;
import blocks.BlockType;

public class JBlock extends Block {

    public JBlock() {
        shape = new int[][] {
                {1, 1, 1},
                {0, 0, 1},
                {0, 0, 0}
        };

        type = BlockType.J;
    }
}