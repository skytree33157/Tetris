package item.types;

import blocks.core.Block;
import blocks.core.BlockType;
import board.Board;

public class BombItem extends Block {
    public BombItem() {
        shape = new int[][] { { 1 } };
        type = BlockType.BOMB;
    }

    // 폭탄 아이템 발동
    public void explode(Board board) {
        // 3*3 영역 삭제
        board.clearArea(getX(), getY(), 3, 3);
    }
}