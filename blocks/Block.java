package blocks;

public abstract class Block {

    protected int[][] shape;
    protected BlockType type;

    // How to use :
    // Block block = new IBlock();
    // block.getType() == BlockType.I; //will return true
    // so, it is basically return the enum of the block type, so you can use it to check the type of the block
    public BlockType getType() {
        return type;
    }

    public int[][] getShape() {
        return shape;
    }

    public int getHeight() {
        return shape.length;
    }

    public int getWidth() {
        return shape[0].length;
    }




}