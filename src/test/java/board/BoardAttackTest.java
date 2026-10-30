package board;

import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoardAttackTest {
    @Test
    void insertsRowsAtBottomInOrderAndCopiesCells() {
        Board board = new Board();
        board.addBlock(0, 15, new int[][] {{2}});
        int[] top = row(1), bottom = row(2);
        assertFalse(board.insertAttackRows(List.of(top, bottom)));
        assertEquals(2, board.getBoard()[13][0]);
        assertEquals(0, board.getBoard()[21][1]);
        assertEquals(0, board.getBoard()[22][2]);
        assertEquals(11, board.getBoard()[22][0]);
        bottom[0] = 0;
        assertEquals(11, board.getBoard()[22][0]);
    }

    @Test
    void reportsOccupiedCellsPushedOutOfTheArray() {
        Board board = new Board();
        board.addBlock(0, 0, new int[][] {{1}});
        assertTrue(board.insertAttackRows(List.of(row(0))));
    }

    @Test
    void rejectsInvalidBatchBeforeChangingBoard() {
        Board board = new Board();
        board.addBlock(0, 22, new int[][] {{2}});
        assertThrows(IllegalArgumentException.class,
                () -> board.insertAttackRows(List.of(row(0), new int[9])));
        assertThrows(IllegalArgumentException.class,
                () -> board.insertAttackRows(List.of(new int[]{1,0,0,0,0,0,0,0,0,0})));
        assertEquals(2, board.getBoard()[22][0]);
        assertFalse(board.insertAttackRows(List.of()));
    }

    @Test
    void partialBombDamageIsNotRowDeletionButEmptiedRowsAre() {
        Board board = new Board();
        board.addBlock(3, 20, new int[][] {{1}, {1}});
        board.addBlock(0, 22, new int[][] {{1,0,0,1}});
        assertEquals(List.of(20,21), board.findRowsEmptiedByArea(3,21,3,3));
        assertEquals(1, board.getBoard()[20][3]); // query must not mutate
        board.clearArea(3,21,3,3);
        assertEquals(1, board.getBoard()[22][0]);
        assertEquals(0, board.getBoard()[22][3]);
    }

    @Test
    void perfectClearRequiresEveryRowToBeEmpty() {
        Board board = new Board();
        assertTrue(board.isPerfectClear());
        board.addBlock(0, 10, new int[][] {{1}});
        assertFalse(board.isPerfectClear());
    }

    private static int[] row(int hole) {
        int[] row = new int[10];
        Arrays.fill(row, 11);
        row[hole] = 0;
        return row;
    }
}
