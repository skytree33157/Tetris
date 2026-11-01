package game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class BattleSettingsTest {

    @ParameterizedTest
    @CsvSource({
        "179999, false",
        "180000, true",
        "180001, true"
    })
    void detectsTimeLimitBoundary(long elapsedMillis, boolean expected) {
        BattleSettings settings =
                new BattleSettings(BattleMode.TIME_LIMIT, 180);

        assertEquals(expected, settings.hasTimeExpired(elapsedMillis));
    }

    @ParameterizedTest
    @EnumSource(
            value = BattleMode.class,
            names = {"NORMAL", "ITEM"}
    )
    void untimedModesDoNotExpire(BattleMode mode) {
        BattleSettings settings = new BattleSettings(mode, 0);

        assertFalse(settings.hasTimeExpired(1_000_000L));
    }

    @ParameterizedTest
    @CsvSource({
        "NORMAL, 0, NORMAL",
        "ITEM, 0, ITEM",
        "TIME_LIMIT, 180, NORMAL"
    })
    void mapsBattleModeToGameMode(
            BattleMode battleMode,
            int timeLimitSeconds,
            GameMode expected) {

        BattleSettings settings =
                new BattleSettings(battleMode, timeLimitSeconds);

        assertEquals(expected, settings.getGameMode());
    }

    @ParameterizedTest
    @CsvSource({
        "TIME_LIMIT, 0",
        "TIME_LIMIT, -1",
        "NORMAL, 1",
        "NORMAL, -1",
        "ITEM, 1",
        "ITEM, -1"
    })
    void rejectsInvalidTimeLimit(BattleMode mode, int seconds) {
        assertThrows(
                IllegalArgumentException.class,
                () -> new BattleSettings(mode, seconds)
        );
    }

    @Test
    void rejectsMissingMode() {
        assertThrows(
                NullPointerException.class,
                () -> new BattleSettings(null, 0)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "0, 180000",
        "179999, 1",
        "180000, 0",
        "180001, 0"
    })
    void calculatesRemainingTime(long elapsedMillis, long expectedMillis) {
        BattleSettings settings =
                new BattleSettings(BattleMode.TIME_LIMIT, 180);

        assertEquals(
                expectedMillis,
                settings.getRemainingTimeMillis(elapsedMillis)
        );
    }
}