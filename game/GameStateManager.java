package game;

// 게임 상태(속도, 점수, 하강속도(레벨업)) 관리 클래스

public class GameStateManager {
    private int dropSpeed = 1000; // 1000ms = 1s
    private int countBlock = 0;
    private int countClearLine = 0;

    private static final int SPEED_DECREASE_AMOUNT = 100; // 한 레벨 당 감소할 속도
    private static final int MIN_DROP_SPEED = 100; // 최소 드롭 속도
    private static final int BLOCKS_FOR_LEVEL_UP = 10; // 블록 임계값
    private static final int LINES_FOR_LEVEL_UP = 10; // 삭제된 줄 임계값

    public int getDropSpeed() {
        return dropSpeed;
    }

    public int getCountBlock() {
        return countBlock;
    }

    public int getCountClearLine() {
        return countClearLine;
    }

    // 블록이 바닥에 고정될 때마다 호출
    public void onBlockPlaced() {
        countBlock++;
        increaseSpeed();
    }

    // 줄이 삭제될 때마다 호출
    public void onLinesCleared(int linesCleared) {
        countClearLine += linesCleared;
        increaseSpeed();
    }

    // 하강 속도 증가시키는 메서드
    private void increaseSpeed() {
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
        if (levelUp && dropSpeed > MIN_DROP_SPEED) {
            dropSpeed -= SPEED_DECREASE_AMOUNT;
        }
    }
}
