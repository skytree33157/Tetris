package item;

import blocks.core.Block;
import blocks.core.BlockType;

/** 1×1 폭탄의 모양·위치 제공. 폭발 효과는 아직 미구현 */
public class BombItem extends Block {
    public BombItem() {
        shape = new int[][]{{1}};
        type = BlockType.BOMB;
    }

    // 폭발 중심의 보드 행·열. 화면 픽셀 좌표 X.
    public int getTargetRow() {
        return getY();
    }

    public int getTargetCol() {
        return getX();
    }
}
