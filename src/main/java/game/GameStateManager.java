package game;

import difficulty.Difficulty;

// 게임 상태(게임오버, 하강속도(레벨업)) 관리 클래스

public class GameStateManager {
    private volatile int dropSpeed = 1000; // 1000ms = 1s
    private int countBlock = 0;
    private int countClearLine = 0;
    private volatile int totalLinesCleared = 0; //레베업으로 리셋되지 않는 누적 줄 수 (HUD 표시)
    private volatile int currentLevel = 1;
    private volatile boolean gameOver = false; // 게임 오버인지 아닌지 확인
    private Difficulty difficulty = Difficulty.NORMAL; // 게임 난이도

    private static final int SPEED_DECREASE_AMOUNT = 100; // 한 레벨 당 감소할 속도
    private static final int MIN_DROP_SPEED = 100; // 최소 드롭 속도
    private static final int MAX_DROP_SPEED = 1000; // 최대 드롭 속도
    private static final int SLOW_ITEM_DELAY = 500; // S 한 줄당 증가할 하강 간격
    private static final int BLOCKS_FOR_LEVEL_UP = 10; // 블록 임계값
    private static final int LINES_FOR_LEVEL_UP = 10; // 삭제된 줄 임계값
    public static final int DEFAULT_LINES_FOR_ITEM = 10; // 아이템 생성 줄 임계값 기본값
    public static final int MIN_LINES_FOR_ITEM = 1;
    public static final int MAX_LINES_FOR_ITEM = 10;
    private int linesForItem = DEFAULT_LINES_FOR_ITEM;
    public static final int MAX_LEVEL = 10;
    // 디버그 모드에서 직접 지정할 수 있는 하강 속도 범위 (일반 진행 범위보다 넓다)
    public static final int DEBUG_MIN_DROP_SPEED = 50;
    public static final int DEBUG_MAX_DROP_SPEED = 2000;

    public int getDropSpeed() {
        return dropSpeed;
    }

    public int getCountBlock() {
        return countBlock;
    }

    public int getCountClearLine() {
        return countClearLine;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public int getTotalLinesCleared(){
        return totalLinesCleared;
    }

    public int[] getLevelSettings() {
        return new int[] {SPEED_DECREASE_AMOUNT, MIN_DROP_SPEED, BLOCKS_FOR_LEVEL_UP, LINES_FOR_LEVEL_UP, dropSpeed};
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    // 디버그 모드: 레벨을 지정하면 그 레벨의 기본 하강 속도로 맞추고 레벨업 진행도를 초기화한다.
    public void setDebugLevel(int level) {
        currentLevel = Math.max(1, Math.min(MAX_LEVEL, level));
        dropSpeed = Math.max(MIN_DROP_SPEED, MAX_DROP_SPEED - (currentLevel - 1) * SPEED_DECREASE_AMOUNT);
        countBlock = 0;
        countClearLine = 0;
    }

    // 디버그 모드: 하강 속도를 직접 지정한다 (ms, 작을수록 빠름).
    public void setDebugDropSpeed(int speedMs) {
        dropSpeed = Math.max(DEBUG_MIN_DROP_SPEED, Math.min(DEBUG_MAX_DROP_SPEED, speedMs));
    }

    // 디버그 모드: 아이템이 등장하는 누적 삭제 줄 간격을 지정한다.
    public void setDebugLinesForItem(int lines) {
        linesForItem = Math.max(MIN_LINES_FOR_ITEM, Math.min(MAX_LINES_FOR_ITEM, lines));
    }

    public int getLinesForItem() {
        return linesForItem;
    }

    public void applySlowItem(int slowLines) {
        dropSpeed = Math.min(MAX_DROP_SPEED, dropSpeed + slowLines * SLOW_ITEM_DELAY);
    }

    public void updateLevelUp(int linesCleared) {
        countBlock++;
        totalLinesCleared += linesCleared;
        countClearLine += linesCleared;
        levelUp();
    }

    // 이번 줄 삭제로 누적 줄 수가 10줄 단위를 넘었는지 확인
    public boolean shouldSpawnItem(int previousTotalLines) {
        return totalLinesCleared / linesForItem > previousTotalLines / linesForItem;
    }

    // 레벨 업(하강 속도 증가) 메서드
    private void levelUp() {
        boolean levelUp = false;
        boolean isLevelUPByLine = false;

        // 블록이 임계값만큼 생성되었을 때
        if (countBlock >= BLOCKS_FOR_LEVEL_UP) {
            levelUp = true;
            countBlock = 0;
        }

        // 줄이 임계값만큼 삭제되었을 때
        if (countClearLine >= LINES_FOR_LEVEL_UP) {
            levelUp = true;
            isLevelUPByLine = true;
            countClearLine = 0;
        }

        // 위 조건 중 한 개 이상의 조건을 만족하고, 제한 속도보다 느릴 때만 속도 증가
        if (levelUp){
            int currentDecreaseAmount = SPEED_DECREASE_AMOUNT;
            if(isLevelUPByLine) {
                if(difficulty == Difficulty.EASY) {
                    currentDecreaseAmount *= 0.8;
                } else if(difficulty == Difficulty.NORMAL) {
                    currentDecreaseAmount *= 1.0;
                } else if(difficulty == Difficulty.HARD) {
                    currentDecreaseAmount *= 1.2;
                }
            }
            dropSpeed = Math.max(MIN_DROP_SPEED, dropSpeed - currentDecreaseAmount);
            currentLevel = Math.min(MAX_LEVEL, currentLevel + 1);
        }
    }
}
