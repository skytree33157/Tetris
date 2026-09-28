package score;

public class ScoreManager {

    private int score = 0;
    private int comboCount = 0;

    private static final int SOFT_DROP_POINT = 1;
    private static final int HARD_DROP_POINT = 2;

    private static final int SINGLE_SCORE = 100;
    private static final int DOUBLE_SCORE = 300;
    private static final int TRIPLE_SCORE = 500;
    private static final int TETRIS_SCORE = 800;

    private static final double MAX_LEVEL_MULTIPLIER = 5.0;
    private static final double MAX_COMBO_MULTIPLIER = 2.0;
    private static final double PERFECT_CLEAR_MULTIPLIER = 2.0;

    private static final double MAX_RAW_MULTIPLIER =
            MAX_LEVEL_MULTIPLIER
                    * MAX_COMBO_MULTIPLIER
                    * PERFECT_CLEAR_MULTIPLIER;

    private static final double MIN_FINAL_MULTIPLIER = 1.0;
    private static final double MAX_FINAL_MULTIPLIER = 10.0;

    // 블럭 하강 점수
    public synchronized int addDropScore(int distance, DropType type, int level) {
        if (distance <= 0) return 0;

        validateLevel(level);

        int pointPerCell = switch (type) {
            case AUTO -> getAutoDropMultiplier(level);
            case SOFT -> SOFT_DROP_POINT;
            case HARD -> HARD_DROP_POINT;
        };

        int addedScore = distance * pointPerCell;
        score += addedScore;

        return addedScore;
    }

    private void validateLevel(int level) {
        if (level < 1 || level > 10)
            throw new IllegalArgumentException("level must be between 1 and 10");
    }

    // 줄 삭제 및 추가 점수 계산
    public synchronized int addLineClearScore(int linesCleared, int level, boolean perfectClear) {
        validateLevel(level);

        // 줄 삭제 실패 시 콤보 초기화
        if (linesCleared == 0) {
            comboCount = 0;
            return 0;
        }

        if (linesCleared < 0 || linesCleared > 4)
            throw new IllegalArgumentException("linesCleared must be between 0 and 4");

        comboCount++;

        int baseScore = getLineClearBaseScore(linesCleared);
        double levelMultiplier = getLevelMultiplier(level);
        double comboMultiplier = getComboMultiplier(comboCount);
        double perfectMultiplier = perfectClear ? PERFECT_CLEAR_MULTIPLIER : 1.0;

        double rawMultiplier = levelMultiplier * comboMultiplier * perfectMultiplier;
        double finalMultiplier = normalizeMultiplier(rawMultiplier);

        int addedScore = (int) Math.round(baseScore * finalMultiplier);
        score += addedScore;

        return addedScore;
    }

    // 현재 누적 점수
    public synchronized int getScore() {return score;}

    public synchronized int getComboCount() {return comboCount;}

    // 새 게임 시작 시 초기화
    public synchronized void reset() {
        score = 0;
        comboCount = 0;
    }

    // 자동 하강 속도별 추가 점수
    private int getAutoDropMultiplier(int level) {
        return switch (level) {
            case 1 -> 1;
            case 2, 3 -> 2;
            case 4, 5 -> 3;
            case 6, 7 -> 4;
            case 8, 9, 10 -> 5;
            default -> throw new IllegalArgumentException(
                    "level must be between 1 and 10"
            );
        };
    }

    // 줄 삭제 점수의 레벨별 배율
    private double getLevelMultiplier(int level) {
        return switch (level) {
            case 1, 2 -> 1.0;
            case 3, 4 -> 2.0;
            case 5, 6 -> 3.0;
            case 7, 8 -> 4.0;
            case 9, 10 -> 5.0;
            default -> throw new IllegalArgumentException(
                    "level must be between 1 and 10"
            );
        };
    }

    // 연속 줄 삭제 배율
    private double getComboMultiplier(int comboCount) {
        if (comboCount <= 1) return 1.0;

        double multiplier = 1.0 + ((comboCount - 1) * 0.1);

        return Math.min(multiplier, MAX_COMBO_MULTIPLIER);
    }

    // 한 번에 삭제한 줄 수별 기본 점수
    private int getLineClearBaseScore(int linesCleared) {
        return switch (linesCleared) {
            case 1 -> SINGLE_SCORE;
            case 2 -> DOUBLE_SCORE;
            case 3 -> TRIPLE_SCORE;
            case 4 -> TETRIS_SCORE;
            default -> 0;
        };
    }

    // raw 배율을 1~10 범위로 선형 정규화
    private double normalizeMultiplier(double rawMultiplier) {
        double clamped = Math.max(1.0, Math.min(rawMultiplier, MAX_RAW_MULTIPLIER));

        double ratio = (clamped - 1.0) / (MAX_RAW_MULTIPLIER - 1.0);

        double normalized =
                MIN_FINAL_MULTIPLIER
                        + (MAX_FINAL_MULTIPLIER - MIN_FINAL_MULTIPLIER)
                        * ratio;

        return Math.min(normalized, MAX_FINAL_MULTIPLIER);
    }
}