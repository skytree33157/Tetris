package board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ui.ItemAppearanceResolver;

public class Board {
    private static final int VISIBLE_ROW = 20;
    private static final int HIDDEN_ROWS = 3;
    private static final int ROW = VISIBLE_ROW + HIDDEN_ROWS;
    private static final int COL = 10;

    private int[][] board;
    private int lastClearedBonusLines;
    private int lastClearedSlowLines;

    public Board() {
        board = new int[ROW][COL];
    }

    public int[][] getBoard() {
        return board;
    }

    // 보드의 한 칸 값을 설정하는 메서드
    private void setCell(int row, int col, int value) {
        if (row >= 0 && row < ROW && col >= 0 && col < COL) {
            this.board[row][col] = value; //value는 블록의 종류
        }
    }

    // 보드에 블록을 추가하는 메서드
    public void addBlock(int x, int y, int[][] shape) {
        if (shape == null) {
            return;
        }

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int boardRow = y + row;
                    int boardCol = x + col;
                    setCell(boardRow, boardCol, shape[row][col]);
                }
            }
        }
    }

    public int getWidth() {
        return COL;
    }

    public int getHeight() {
        return VISIBLE_ROW;
    }

    public int getHiddenRows() {
        return HIDDEN_ROWS;
    }

    // 여유 공간에 블록이 고정됐는지 확인
    public boolean hasBlocksInHiddenRows() {
        for (int row = 0; row < HIDDEN_ROWS; row++) {
            for (int col = 0; col < COL; col++) {
                if (board[row][col] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getLastClearedBonusLines() {
        return lastClearedBonusLines;
    }

    public int getLastClearedSlowLines() {
        return lastClearedSlowLines;
    }

    // 보드의 꽉 찬 줄 제거 후 제거된 line 수 반환
    public int clearLines(){
        int linesCleared=0;
        lastClearedBonusLines = 0;
        lastClearedSlowLines = 0;

        // 맨 아래줄부터 검사해서 꽉 찬 줄이 있으면 isFull=true
        for(int curRow=ROW-1;curRow>=0;curRow--){
            boolean isFull=true;
            for(int curCol=0;curCol<COL;curCol++){
                if(board[curRow][curCol]==0){
                    isFull=false;
                    break;
                }
            }
            if(isFull){
                linesCleared++;
                
                // 삭제될 줄에 보너스, 슬로우 마커가 있는지 확인
                lastClearedBonusLines += countMarker(board[curRow], 'P');
                lastClearedSlowLines += countMarker(board[curRow], 'S');

                // 현재 줄부터 시작해서 한 줄씩 아래로 이동
                for (int r = curRow; r > 0; r--) {
                    for (int c = 0; c < COL; c++) {
                        board[r][c] = board[r - 1][c];
                    }
                }

                // 맨 윗줄은 초기화
                for (int c = 0; c < COL; c++) {
                    board[0][c] = 0;
                }

                // 한 칸씩 아래로 이동했으므로 curRow를 증가(현재 줄을 한 줄 밑으로 이동)
                // -> 현재 줄이 꽉 차 있을 수 있으므로 현재 줄부터 다시 검사하도록 함
                curRow++;
            }
        }
        return linesCleared;
    }

    // 아이템 마커 개수 확인 (P, S)
    private int countMarker(int[] row, char marker) {
        int count = 0;
        for (int value : row) {
            if(value!=0){
                if(ItemAppearanceResolver.fromBoardCell(value).symbol()==marker){
                    count++;
                }
            }
        }
        return count;
    }

    // 지정한 줄 삭제 후 한칸 씩 아래로 이동시킴(clearLine 아이템용)
    public boolean eraseLine(int row) {
        if (row < 0 || row >= ROW) {
            return false;
        }
        // 삭제될 줄에 보너스 마커가 있는지 확인
        lastClearedBonusLines = countMarker(board[row], 'P');
        lastClearedSlowLines = countMarker(board[row], 'S');

        for (int currentRow = row; currentRow > 0; currentRow--) {
            for (int col = 0; col < COL; col++) {
                board[currentRow][col] = board[currentRow - 1][col];
            }
        }

        for (int col = 0; col < COL; col++) {
            board[0][col] = 0;
        }
        return true;
    }

    // 지정한 행 아래의 블록을 삭제하는 메서드(무게추 아이템용)
    public void clearOneRowBelow(int startX, int bottomRow, int width) {
        int firstColumn = Math.max(0, startX);
        int lastColumn = Math.min(COL, startX + width);
        int row = bottomRow + 1;

        if (row < 0 || row >= ROW) {
            return;
        }
        // board[row][col]=0을 통해 지정한 행 아래의 블록을 삭제
        for (int col = firstColumn; col < lastColumn; col++) {
            board[row][col] = 0;
        }
    }
    
    // 꽉 찬 줄의 행 번호 목록 변환 (줄 삭제 애니매이션 용)
    public List<Integer> findFullRows() {
        List<Integer> fullRows = new ArrayList<>();
        for (int row = 0; row < ROW; row++) {
            boolean isFull = true;
            for (int col = 0; col < COL; col++) {
                if (board[row][col] == 0){
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                fullRows.add(row);
            }
        }
        return fullRows;
    }
    
    // 지정한 줄들을 한 번에 삭제하고 남은 줄을 아래로 내린 뒤 삭제한 줄 수 반환
    public int clearRows(Collection<Integer> rows) {
        int[][] newBoard = new int[ROW][COL];
        int writeRow = ROW - 1;
        int cleared = 0;
        lastClearedBonusLines = 0;
        lastClearedSlowLines = 0;

        // 아래 줄부터 남길 줄만 새 보드의 아래쪽부터 채움 -> 행 번호가 밀리는 문제 없음
        for (int row = ROW - 1; row >= 0; row--) {
            if (rows.contains(row)) {
                cleared++;
                // 삭제될 줄에 보너스/슬로우 마커가 있는지 확인
                lastClearedBonusLines += countMarker(board[row], 'P');
                lastClearedSlowLines += countMarker(board[row], 'S');
                continue;
            }
            newBoard[writeRow] = board[row].clone();
            writeRow--;
        }
        board = newBoard;
        return cleared;
    }

    // 폭탄 아이템 3*3 영역 삭제
    public void clearArea(int centerX, int centerY, int width, int height) {
        int columnRadius = width / 2;
        int rowRadius = height / 2;

        for (int row = centerY - rowRadius; row <= centerY + rowRadius; row++) {
            for (int col = centerX - columnRadius; col <= centerX + columnRadius; col++) {
                if (row >= 0 && row < ROW && col >= 0 && col < COL) {
                    board[row][col] = 0;
                }
            }
        }
    }

    // 충돌 확인 메서드(무게추 아이템용)
    public boolean hasBlockCollision(int[][] shape, int targetX, int targetY) {
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                // 해당 칸에 보드에 블록이 없으면 continue
                if (shape[row][col] == 0) {
                    continue;
                }
                int boardX = targetX + col;
                int boardY = targetY + row;
                // 보드 내부에 있는지 확인
                if (boardX >= 0 && boardX < COL && boardY >= 0 && boardY < ROW && board[boardY][boardX] != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isPerfectClear() {
            for (int col = 0; col < COL; col++) {
                if (board[ROW-1][col] != 0) {
                    return false;
                }
        }
        return true;
    }

    public boolean isValidPosition(int[][] shape, int targetX, int targetY) {
        // 블록의 각 칸을 검사하여 충돌 여부 확인
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                // 블록이 실제로 존재하는 칸만 검사
                if (shape[row][col] != 0) {
                    int boardX = targetX + col;
                    int boardY = targetY + row;

                    // 블록이 보드 내부에 있는지 검사
                    if (boardX < 0 || boardX >= COL || boardY >= ROW) {
                        return false;
                    }

                    // 다른 고정된 블록과 충돌하는지 검사 
                    if (boardY >= 0) {
                        if (board[boardY][boardX] != 0) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}