package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import game.ActionController;
import game.GameStateManager;
import score.ScoreManager;


public final class GameStatus {
    // 받은 보드 크기 상한. 잘못된(또는 악의적인) 값으로 큰 배열을 만들지 않도록 제한
    static final int MAX_ROWS = 40;
    static final int MAX_COLS = 20;

    private static final int[][] NO_BOARD = new int[0][0];

    private final int score;
    private final int level;
    private final int lines;
    private final boolean gameOver;
    // 상대 화면에 그릴 보드: 보이는 줄만, 떨어지는 블록까지 합친 칸 값 (BlockRenderer.drawCells 인코딩)
    private final int[][] cells;

    public GameStatus(int score, int level, int lines, boolean gameOver) {
        this(score, level, lines, gameOver, NO_BOARD);
    }

    public GameStatus(int score, int level, int lines, boolean gameOver, int[][] cells) {
        // 유효성 검사: 점수와 줄 수는 음수가 아니어야 함
        if (score < 0) {
            throw new IllegalArgumentException("점수가 음수");
        }
        if (lines < 0) {
            throw new IllegalArgumentException("줄이 음수");
        }
        // 레벨은 1~10 범위여야 함
        if (level < 1 || level > 10) {
            throw new IllegalArgumentException("이상한 레벨");
        }
        validateBoardSize(cells);

        this.score = score;
        this.level = level;
        this.lines = lines;
        this.gameOver = gameOver;
        this.cells = copy(cells);
    }

    // 현재 상태 복사, 잠금
    public static GameStatus capture(ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager) {
        synchronized (actionController) {
            ActionController.ViewSnapshot frame = actionController.getViewSnapshot();
            return new GameStatus(
                    scoreManager.getScore(),
                    gameStateManager.getCurrentLevel(),
                    gameStateManager.getTotalLinesCleared(),
                    gameStateManager.isGameOver(),
                    compose(frame.board(), frame.hiddenRows(), frame.currentBlock()));
        }
    }

    /**
     * 고정된 보드에 떨어지는 블록을 겹친 뒤, 위쪽 숨은 줄을 빼고 보이는 줄만 돌려준다.
     * 블록에서 보드 밖으로 나간 칸은 버린다.
     */
    static int[][] compose(int[][] board, int hiddenRows, ActionController.BlockSnapshot falling) {
        int totalRows = board.length;
        int visibleRows = Math.max(0, totalRows - hiddenRows);
        int cols = totalRows == 0 ? 0 : board[0].length;
        int[][] visible = new int[visibleRows][cols];
        for (int r = 0; r < visibleRows; r++) {
            System.arraycopy(board[r + hiddenRows], 0, visible[r], 0, cols);
        }
        if (falling == null) {
            return visible;
        }
        int[][] shape = falling.cells();
        for (int r = 0; r < shape.length; r++) {
            int row = falling.y() + r - hiddenRows;
            if (row < 0 || row >= visibleRows) {
                continue;
            }
            for (int c = 0; c < shape[r].length; c++) {
                int col = falling.x() + c;
                if (shape[r][c] != 0 && col >= 0 && col < cols) {
                    visible[row][col] = shape[r][c];
                }
            }
        }
        return visible;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getLines() {
        return lines;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // 상대 보드 칸 (복사본). 보드 정보가 없으면 0×0
    public int[][] getCells() {
        return copy(cells);
    }

    public boolean hasBoard() {
        return cells.length > 0 && cells[0].length > 0;
    }

    // score, level, lines, gameOver, 행 수, 열 수, 칸 값 순서로 outputStream에 작성
    void writeTo(DataOutputStream output) throws IOException {
        output.writeInt(score);
        output.writeInt(level);
        output.writeInt(lines);
        output.writeBoolean(gameOver);
        int rows = cells.length;
        int cols = rows == 0 ? 0 : cells[0].length;
        output.writeShort(rows);
        output.writeShort(cols);
        for (int[] row : cells) {
            for (int value : row) {
                output.writeInt(value);
            }
        }
    }

    // inputStream에서 writeTo와 같은 순서로 읽고 값의 유효성 확인
    // 잘못된 값은 IOException으로 변환
    static GameStatus readFrom(DataInputStream input) throws IOException {
        int score = input.readInt();
        int level = input.readInt();
        int lines = input.readInt();
        boolean gameOver = input.readBoolean();
        int rows = input.readShort();
        int cols = input.readShort();
        if (rows < 0 || rows > MAX_ROWS || cols < 0 || cols > MAX_COLS) {
            throw new IOException("Invalid board size received: " + rows + "x" + cols);
        }
        int[][] cells = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = input.readInt();
            }
        }
        try {
            return new GameStatus(score, level, lines, gameOver, cells);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid player status received", exception);
        }
    }

    private static void validateBoardSize(int[][] cells) {
        if (cells == null) {
            throw new IllegalArgumentException("보드가 null");
        }
        if (cells.length > MAX_ROWS) {
            throw new IllegalArgumentException("보드 행이 너무 많음");
        }
        int cols = cells.length == 0 ? 0 : cells[0].length;
        if (cols > MAX_COLS) {
            throw new IllegalArgumentException("보드 열이 너무 많음");
        }
        for (int[] row : cells) {
            if (row == null || row.length != cols) {
                throw new IllegalArgumentException("보드 행 길이가 다름");
            }
        }
    }

    private static int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }
        return result;
    }
}
