package blocks;

public class IBlock extends Block {

    public IBlock() {
        shape = new int[][] {
                {0, 0, 0, 0},
                {1, 1, 1, 1},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        };

        type = BlockType.I;
    }
}