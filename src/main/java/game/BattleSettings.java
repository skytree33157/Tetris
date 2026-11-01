package game;

import java.util.Objects;

public final class BattleSettings {

    private final BattleMode mode;
    private final int timeLimitSeconds;

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

        this.timeLimitSeconds = timeLimitSeconds;
    }

    public BattleMode getMode() {
        return mode;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
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
                && elapsedPlayMillis >= timeLimitSeconds * 1000L;
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

        long limitMillis = timeLimitSeconds * 1000L;
        return Math.max(0L, limitMillis - elapsedPlayMillis);
    }
}