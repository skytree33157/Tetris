package menu.screens;

import java.awt.event.KeyEvent;
import java.util.List;

import difficulty.Difficulty;
import game.GameMode;
import menu.ScoreRecord;

// 게임 화면 정보 패널에 표시할 값을 계산 (창 없이 테스트할 수 있도록 GameScreen에서 분리)
public final class GameHud {

    public static final int MAX_LEVEL = 10; // GameStateManager.MAX_LEVEL과 동일

    private GameHud() {}

    // 경과 시간을 mm:ss로 표시 (60분 이상이면 분이 그대로 늘어남)
    public static String formatTime(long elapsedMs) {
        long seconds = Math.max(0, elapsedMs) / 1000;
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    // 블록 수와 줄 수 중 먼저 차는 쪽 기준의 다음 레벨 진행률 (0.0 ~ 1.0)
    public static double levelProgress(int level, int countBlock, int blocksForLevelUp,
                                       int countClearLine, int linesForLevelUp) {
        if (level >= MAX_LEVEL) {
            return 1.0;
        }
        double ratio = Math.max(countBlock / (double) blocksForLevelUp,
                countClearLine / (double) linesForLevelUp);
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    public static String nextLevelText(int level, int countBlock, int blocksForLevelUp,
                                       int countClearLine, int linesForLevelUp) {
        if (level >= MAX_LEVEL) {
            return "MAX";
        }
        return "블록 " + (blocksForLevelUp - countBlock) + " · 라인 " + (linesForLevelUp - countClearLine);
    }

    // 콤보는 2연속부터 표시
    public static String comboText(int combo) {
        return combo >= 2 ? "x" + combo : "-";
    }

    public static String keyName(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            default -> KeyEvent.getKeyText(keyCode);
        };
    }

    public static String difficultyLabel(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "쉬움";
            case NORMAL -> "보통";
            case HARD -> "어려움";
        };
    }

    // 같은 난이도·모드 기록 중 최고 점수 (기록이 없으면 0)
    public static int bestScore(List<ScoreRecord> records, Difficulty difficulty, GameMode mode) {
        int best = 0;
        for (ScoreRecord record : records) {
            if (difficulty.name().equals(record.getDifficulty()) && mode.name().equals(record.getMode())) {
                best = Math.max(best, record.getScore());
            }
        }
        return best;
    }
}
