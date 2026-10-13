package item.types;
import blocks.core.Block;
import blocks.tetromino.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// FR-36-3: S 부착·복사·회전 좌표 검증. 줄 삭제 판정과 속도 효과 검증 X.
class SlowItemTest {
    @Test
    void markerPreservesSourceShapeAndTypeThroughFourRotations() {
        for (Block source : new Block[]{new IBlock(), new OBlock(), new TBlock(), new SBlock(),
                new ZBlock(), new JBlock(), new LBlock()}) {
            for (int r = 0; r < source.getHeight(); r++) for (int c = 0; c < source.getWidth(); c++) {
                if (source.getShape()[r][c] == 0) continue;
                SlowItem slow = new SlowItem(source, r, c);
                assertEquals(1, source.getShape()[r][c]);
                assertSame(source.getType(), slow.getType());
                assertSame(source.getType(), slow.getSourceType());
                slow.setX(3); slow.setY(-2);
                int row = r, col = c;
                for (int rotation = 0; rotation < 4; rotation++) {
                    assertEquals(row, slow.getMarkerRow()); assertEquals(col, slow.getMarkerCol());
                    assertEquals(row - 2, slow.getTargetRow()); assertEquals(col + 3, slow.getTargetCol());
                    assertTrue(slow.isMarkerCell(row, col));
                    assertEquals(SlowItem.S_CELL_VALUE, slow.getShape()[row][col]);
                    int nextRow = col, nextCol = slow.getHeight() - 1 - row;
                    slow.rotate(); row = nextRow; col = nextCol;
                }
                assertEquals(r, slow.getMarkerRow()); assertEquals(c, slow.getMarkerCol());
            }
        }
    }

    @Test
    void rejectsEmptyAndOutOfBoundsMarkers() {
        TBlock source = new TBlock();
        for (int[] cell : new int[][]{{0,0},{-1,1},{3,1},{0,-1},{0,3}}) {
            assertThrows(IllegalArgumentException.class, () -> new SlowItem(source, cell[0], cell[1]));
        }
        assertThrows(NullPointerException.class, () -> new SlowItem(null, 0, 0));
        SlowItem slow = new SlowItem(source, 0, 1);
        assertFalse(slow.isMarkerCell(-1, 0)); assertFalse(slow.isMarkerCell(3, 0));
        assertFalse(slow.isMarkerCell(0, -1)); assertFalse(slow.isMarkerCell(0, 3));
        assertFalse(slow.isMarkerCell(0, 0));
    }
}
