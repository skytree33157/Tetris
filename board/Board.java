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

    public void setBlock(int row, int col, int value) {
        if (row >= 0 && row < ROW && col >= 0 && col < COL) {
            this.board[row][col] = value;
        }
    }

    public void addBlock(int x, int y, int[][] shape) {
        if (shape == null) {
            return;
        }

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] != 0) {
                    int boardRow = y + row;
                    int boardCol = x + col;
                    setBlock(boardRow, boardCol, shape[row][col]);
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
}