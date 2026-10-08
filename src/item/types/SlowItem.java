package item.types;

import blocks.core.Block;
import blocks.core.BlockType;
import java.util.Objects;

/** 일반 블록 한 칸에 S 표시. 속도 효과는 게임 로직에서 구현. */
public class SlowItem extends Block {
    // 모양 배열에서 표시 칸을 찾는 값. 보드 저장 값으로 직접 사용 X.
    public static final int S_CELL_VALUE = -3;
    private final BlockType sourceType;

    public SlowItem(Block sourceBlock, int markerRow, int markerCol) {
        Objects.requireNonNull(sourceBlock, "sourceBlock");
        int[][] sourceShape = sourceBlock.getShape();
        if (markerRow < 0 || markerRow >= sourceShape.length
                || markerCol < 0 || markerCol >= sourceShape[markerRow].length
                || sourceShape[markerRow][markerCol] == 0) {
            throw new IllegalArgumentException("Slow marker must be on an occupied cell");
        }

        sourceType = sourceBlock.getType();
        type = sourceType;
        shape = new int[sourceShape.length][];
        for (int row = 0; row < sourceShape.length; row++) {
            shape[row] = sourceShape[row].clone();
        }
        shape[markerRow][markerCol] = S_CELL_VALUE;
    }

    public BlockType getSourceType() {
        return sourceType;
    }

    // 현재 모양에서 S 위치 확인. 회전 후에도 표시 위치 유지.
    public boolean isMarkerCell(int row, int col) {
        return row >= 0 && row < shape.length
                && col >= 0 && col < shape[row].length
                && shape[row][col] == S_CELL_VALUE;
    }

    // 모양 배열 안의 표시 위치. 보드 좌표는 getTargetRow()/getTargetCol() 사용.
    public int getMarkerRow() {
        return findMarker()[0];
    }

    public int getMarkerCol() {
        return findMarker()[1];
    }

    public int getTargetRow() {
        return getY() + getMarkerRow();
    }

    public int getTargetCol() {
        return getX() + getMarkerCol();
    }

    private int[] findMarker() {
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (isMarkerCell(row, col)) return new int[]{row, col};
            }
        }
        throw new IllegalStateException("Slow marker is missing");
    }
}
