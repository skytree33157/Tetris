package blocks;

public class OBlock extends Block {

    public OBlock() {
        shape = new int[][] {
                {1, 1},
                {1, 1}
        };

        type = BlockType.O;
    }
}