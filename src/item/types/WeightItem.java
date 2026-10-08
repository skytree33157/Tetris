package item.types;

import blocks.core.Block;
import blocks.core.BlockType;
import board.Board;

public class WeightItem extends Block {
    // 보드의 블록과 충돌 시 좌우 이동 고정
    private boolean landed;

// Todo : 임시 무게추 모양
    public WeightItem() {
        shape = new int[][]{
                {0, 1, 1, 0},
                {1, 1, 1, 1}
        };
//------ 임시 타입
        type = BlockType.WEIGHT;
    }

    // 무게추 아랫줄 삭제
    public void clearBlocksBelow(Board board) {
        // getY + getHeight - 1 = 무게추 가장 아랫줄
        board.clearOneRowBelow(getX(), getY() + getHeight() - 1, getWidth());
    }

    // 무게추가 고정 블록에 닿았는지
    public boolean isLanded() {
        return landed;
    }

    // 충돌 직후 좌우 이동 잠금
    public void markLanded() {
        landed = true;
    }

    @Override // 충돌 후 좌로 이동 금지
    public void moveLeft() {
        if (!landed) {
            super.moveLeft();
        }
    }

    @Override // 충돌 후 우로 이동 금지
    public void moveRight() {
        if (!landed) {
            super.moveRight();
        }
    }

    @Override // 회전 불가하므로 원래 모양 반환
    public int[][] getRotate() {
        return copyShape();
    }

    @Override // 회전 불가
    public void rotate() {
    }

    // 무게추 원래 모양 복사
    private int[][] copyShape() {
        int[][] copy = new int[shape.length][];
        for (int row = 0; row < shape.length; row++) {
            copy[row] = shape[row].clone();
        }
        return copy;
    }
}