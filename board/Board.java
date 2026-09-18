package board;

public class Board {
    private static final int ROW=20;
    private static final int COL=10;

    private int[][] board;

    // 게임보드 constructor
    public Board() {
        board = new int[ROW][COL];
    }

    // 게임보드 반환
    public int[][] getBoard() {
        return board;
    }

    // 최하단으로 내려온 block 위치를 보드에 저장
    public void setBlock(int row, int col, int value) {
        if(row>=0 && row<ROW && col>=0 && col<COL)
            this.board[row][col] = value;
    }
}