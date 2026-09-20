package blocks;

public class TBlock extends Block {

    public TBlock() {
        shape = new int[][] {
                {0, 1, 0},
                {1, 1, 1},
                {0, 0, 0}
        };

        type = BlockType.T;
    }
}