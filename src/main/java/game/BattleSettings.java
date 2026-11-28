package game;

import java.util.Objects;

public final class BattleSettings {

    private final BattleMode mode;
    // 디버그 패널에서 남은 시간을 바꿀 수 있어 타이머 스레드와 공유하는 값이다.
    private volatile long timeLimitMillis;

    public BattleSettings(BattleMode mode, int timeLimitSeconds) {
        this.mode = Objects.requireNonNull(mode, "mode");

        if (mode == BattleMode.TIME_LIMIT) {
            if (timeLimitSeconds <= 0) {
                throw new IllegalArgumentException(
                        "시간제한 모드의 제한 시간은 0초보다 커야 합니다."
                );
            }
        } else if (timeLimitSeconds != 0) {
            throw new IllegalArgumentException(
                    "일반·아이템 모드의 제한 시간은 0이어야 합니다."
            );
        }

        this.timeLimitMillis = timeLimitSeconds * 1000L;
    }

    public BattleMode getMode() {
        return mode;
    }

    public int getTimeLimitSeconds() {
        return (int) (timeLimitMillis / 1000L);
    }

    /**
     * 현재 경과 시간 기준으로 남은 시간을 remainingMillis로 맞춘다.
     * 제한 시간 총량을 경과 시간만큼 늘려서, 이후 표시와 만료 판정이 함께 바뀐다.
     */
    public synchronized void setRemainingTimeMillis(long elapsedPlayMillis, long remainingMillis) {
        if (!isTimeLimited()) {
            throw new IllegalStateException("시간제한 모드에서만 남은 시간을 바꿀 수 있습니다.");
        }
        if (elapsedPlayMillis < 0 || remainingMillis <= 0) {
            throw new IllegalArgumentException("경과 시간은 음수일 수 없고, 남은 시간은 0보다 커야 합니다.");
        }
        timeLimitMillis = elapsedPlayMillis + remainingMillis;
    }

    public boolean isTimeLimited() {
        return mode == BattleMode.TIME_LIMIT;
    }

    public boolean hasTimeExpired(long elapsedPlayMillis) {
        if (elapsedPlayMillis < 0) {
            throw new IllegalArgumentException(
                    "경과 시간은 음수일 수 없습니다."
            );
        }

        return isTimeLimited()
                && elapsedPlayMillis >= timeLimitMillis;
    }

    public GameMode getGameMode() {
        return switch (mode) {
            case NORMAL, TIME_LIMIT -> GameMode.NORMAL;
            case ITEM -> GameMode.ITEM;
        };
    }

    public long getRemainingTimeMillis(long elapsedPlayMillis) {
        if (elapsedPlayMillis < 0) {
            throw new IllegalArgumentException(
                    "경과 시간은 음수일 수 없습니다."
            );
        }

        if (!isTimeLimited()) {
            return 0;
        }

        return Math.max(0L, timeLimitMillis - elapsedPlayMillis);
    }
}