package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import game.ActionController;
import game.GameStateManager;
import score.ScoreManager;


public final class GameStatus {
    private final int score;
    private final int level;
    private final int lines;
    private final boolean gameOver;

    public GameStatus(int score, int level, int lines, boolean gameOver) {
        this.score = score;
        this.level = level;
        this.lines = lines;
        this.gameOver = gameOver;
    }

    // 현재 상태 복사, 잠금
    public static GameStatus capture(ActionController actionController, GameStateManager gameStateManager, ScoreManager scoreManager) {
        synchronized (actionController) {
            return new GameStatus(
                    scoreManager.getScore(),
                    gameStateManager.getCurrentLevel(),
                    gameStateManager.getTotalLinesCleared(),
                    gameStateManager.isGameOver());
        }
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

    // score, level, lines, gameOver 순서로 outputStream에 작성
    void writeTo(DataOutputStream output) throws IOException {
        output.writeInt(score);
        output.writeInt(level);
        output.writeInt(lines);
        output.writeBoolean(gameOver);
    }

    // inputStream에서 score, level, lines, gameOver 순서대로 읽고 값의 유효성 확인
    // 잘못된 값은 IOException으로 변환
    static GameStatus readFrom(DataInputStream input) throws IOException {
        try {
            return new GameStatus(
                    input.readInt(),
                    input.readInt(),
                    input.readInt(),
                    input.readBoolean());
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid player status received", exception);
        }
    }
}
