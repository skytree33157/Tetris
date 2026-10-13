package item.types;
import blocks.core.Block;
import blocks.tetromino.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// FR-36: P 부착·복사·회전 좌표 검증. 줄 삭제 판정과 점수 호출 검증 X.
class BonusItemTest {
    @Test
    void markerPreservesSourceShapeAndTypeThroughFourRotations() {
        for (Block source : new Block[]{new IBlock(), new OBlock(), new TBlock(), new SBlock(),
                new ZBlock(), new JBlock(), new LBlock()}) {
            for (int r = 0; r < source.getHeight(); r++) for (int c = 0; c < source.getWidth(); c++) {
                if (source.getShape()[r][c] == 0) continue;
                BonusItem bonus = new BonusItem(source, r, c);
                assertEquals(1, source.getShape()[r][c]);
                assertSame(source.getType(), bonus.getType());
                assertSame(source.getType(), bonus.getSourceType());
                bonus.setX(3); bonus.setY(-2);
                int row = r, col = c;
                for (int rotation = 0; rotation < 4; rotation++) {
                    assertEquals(row, bonus.getMarkerRow()); assertEquals(col, bonus.getMarkerCol());
                    assertEquals(row - 2, bonus.getTargetRow()); assertEquals(col + 3, bonus.getTargetCol());
                    assertTrue(bonus.isMarkerCell(row, col));
                    assertEquals(BonusItem.P_CELL_VALUE, bonus.getShape()[row][col]);
                    int nextRow = col, nextCol = bonus.getHeight() - 1 - row;
                    bonus.rotate(); row = nextRow; col = nextCol;
                }
                assertEquals(r, bonus.getMarkerRow()); assertEquals(c, bonus.getMarkerCol());
            }
        }
    }

    @Test
    void rejectsEmptyAndOutOfBoundsMarkers() {
        TBlock source = new TBlock();
        for (int[] cell : new int[][]{{0,0},{-1,1},{3,1},{0,-1},{0,3}}) {
            assertThrows(IllegalArgumentException.class, () -> new BonusItem(source, cell[0], cell[1]));
        }
        assertThrows(NullPointerException.class, () -> new BonusItem(null, 0, 0));
        BonusItem bonus = new BonusItem(source, 0, 1);
        assertFalse(bonus.isMarkerCell(-1, 0)); assertFalse(bonus.isMarkerCell(3, 0));
        assertFalse(bonus.isMarkerCell(0, -1)); assertFalse(bonus.isMarkerCell(0, 3));
        assertFalse(bonus.isMarkerCell(0, 0));
    }
}
