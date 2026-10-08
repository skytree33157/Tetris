package item;

import blocks.core.Block;
import blocks.core.BlockType;
import board.Board;
import game.GameStateManager;
import score.ScoreManager;

public class LineClearItem extends Block {
	// L이 표시된 셀의 보드 값. 일반 블록 셀 값과 구분하기 위해 -1 임시 사용
	public static final int L_CELL_VALUE = -1;
	private final BlockType sourceType;

	public LineClearItem(Block sourceBlock, int markerRow, int markerCol) {
		int[][] sourceShape = sourceBlock.getShape();
        
		shape = new int[sourceShape.length][];
		for (int row = 0; row < sourceShape.length; row++) {
			shape[row] = sourceShape[row].clone();
		}
		shape[markerRow][markerCol] = L_CELL_VALUE;
//-----Todo : 임시 타입
		type = BlockType.LINE_CLEAR;
		sourceType = sourceBlock.getType();
	}

	public BlockType getSourceType() {
		return sourceType;
	}

	// 현재 위치와 회전 상태를 반영해 L이 있는 보드 행을 반환
	public int getLRow() {
		for (int row = 0; row < shape.length; row++) {
			for (int col = 0; col < shape[row].length; col++) {
				if (isMarkerCell(row, col)) {
					return getY() + row;
				}
			}
		}
		throw new IllegalStateException("L 셀이 없음");
	}

	// 아이템 효과가 적용될 대상 행을 반환한다.
	public int getTargetRow() {
		return getLRow();
	}

	// 지정한 모양의 셀이 L 셀인지 확인한다.
	public boolean isMarkerCell(int row, int col) {
		return row >= 0 && row < shape.length
				&& col >= 0 && col < shape[row].length
				&& shape[row][col] == L_CELL_VALUE;
	}

	// 블록이 고정될 때 L이 있는 행을 삭제한다.
	public boolean activate(Board board) {
		return board.eraseLine(getLRow());
	}

	// 외부 게임 흐름에서 지정한 행에 아이템 효과를 적용한다.
	public void use(Board board, ScoreManager scoreManager, GameStateManager gameStateManager, int targetRow) {
		board.eraseLine(targetRow);
	}
}

