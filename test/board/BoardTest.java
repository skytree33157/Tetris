package board;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// FR-30 줄 삭제 애니메이션용 Board 메서드
class BoardTest {
    private Board board;
    private int bottomRow; // 숨김 줄을 포함한 보드 배열의 맨 아래 행

    @BeforeEach
    void setUp() {
        board = new Board();
        bottomRow = board.getHiddenRows() + board.getHeight() - 1;
    }

    private void fillRow(int row) {
        int[][] line = new int[1][board.getWidth()];
        for (int col = 0; col < board.getWidth(); col++) {
            line[0][col] = 1;
        }
        board.addBlock(0, row, line);
    }

    @Test // 꽉 찬 줄이 없으면 빈 목록
    void findFullRowsReturnsEmptyWhenNoFullRow() {
        board.addBlock(0, bottomRow, new int[][] {{1, 1, 1}});
        assertTrue(board.findFullRows().isEmpty());
    }

    @Test // 꽉 찬 줄만 찾고 보드는 바꾸지 않음
    void findFullRowsFindsOnlyFullRowsWithoutClearing() {
        fillRow(bottomRow - 2);
        fillRow(bottomRow);
        assertEquals(List.of(bottomRow - 2, bottomRow), board.findFullRows());
        assertEquals(1, board.getBoard()[bottomRow][0]);
    }

    @Test // 떨어진 두 줄을 지우면 그 사이 줄과 윗줄이 정확히 내려옴
    void clearRowsRemovesGivenRowsAndShiftsDown() {
        fillRow(bottomRow);
        board.addBlock(0, bottomRow - 1, new int[][] {{2}});
        fillRow(bottomRow - 2);
        board.addBlock(0, bottomRow - 3, new int[][] {{3}});

        int cleared = board.clearRows(List.of(bottomRow - 2, bottomRow));

        assertEquals(2, cleared);
        assertEquals(2, board.getBoard()[bottomRow][0]);
        assertEquals(3, board.getBoard()[bottomRow - 1][0]);
        assertEquals(0, board.getBoard()[bottomRow - 2][0]);
        assertTrue(board.findFullRows().isEmpty());
    }

    @Test // 마커 없는 줄만 지우면 보너스/슬로우 집계는 0
    void clearRowsResetsItemMarkerCounts() {
        fillRow(bottomRow);

        board.clearRows(List.of(bottomRow));

        assertEquals(0, board.getLastClearedBonusLines());
        assertEquals(0, board.getLastClearedSlowLines());
    }
}
