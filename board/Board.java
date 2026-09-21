package board;

public class Board {
    private static final int ROW = 20;
    private static final int COL = 10;

    private int[][] board;

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
        return ROW;
    }

    // 보드의 꽉 찬 줄 제거 후 제거된 line 수 반환
    public int clearLines(){
        int linesCleared=0;

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
}