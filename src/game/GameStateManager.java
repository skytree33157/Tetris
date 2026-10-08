package game;

// 게임 상태(게임오버, 하강속도(레벨업)) 관리 클래스

public class GameStateManager {
    private volatile int dropSpeed = 1000; // 1000ms = 1s
    private int countBlock = 0;
    private int countClearLine = 0;
    private volatile int totalLinesCleared = 0; //레베업으로 리셋되지 않는 누적 줄 수 (HUD 표시)
    private int currentLevel = 1;
    private volatile boolean gameOver = false; // 게임 오버인지 아닌지 확인

    private static final int SPEED_DECREASE_AMOUNT = 100; // 한 레벨 당 감소할 속도
    private static final int MIN_DROP_SPEED = 100; // 최소 드롭 속도
    private static final int MAX_DROP_SPEED = 1000; // 최대 드롭 속도
    private static final int SLOW_ITEM_DELAY = 500; // S 한 줄당 증가할 하강 간격
    private static final int BLOCKS_FOR_LEVEL_UP = 10; // 블록 임계값
    private static final int LINES_FOR_LEVEL_UP = 10; // 삭제된 줄 임계값
    private static final int LINES_FOR_ITEM = 10; // 아이템 생성 줄 임계값

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

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
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
        return totalLinesCleared / LINES_FOR_ITEM > previousTotalLines / LINES_FOR_ITEM;
    }

    // 레벨 업(하강 속도 증가) 메서드
    private void levelUp() {
        boolean levelUp = false;

        // 블록이 임계값만큼 생성되었을 때
        if (countBlock >= BLOCKS_FOR_LEVEL_UP) {
            levelUp = true;
            countBlock = 0;
        }

        // 줄이 임계값만큼 삭제되었을 때
        if (countClearLine >= LINES_FOR_LEVEL_UP) {
            levelUp = true;
            countClearLine = 0;
        }

        // 위 조건 중 한 개 이상의 조건을 만족하고, 제한 속도보다 느릴 때만 속도 증가
        if (levelUp && dropSpeed-SPEED_DECREASE_AMOUNT > MIN_DROP_SPEED) {
            dropSpeed -= SPEED_DECREASE_AMOUNT;
            currentLevel++;
        }
    }
}
