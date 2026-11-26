package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class RoomRulesTest {

    @Test
    void pickTargetNeverChoosesSenderOrDeadPlayers() {
        RoomRules rules = new RoomRules(4);
        rules.eliminate(2);
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            int target = rules.pickTarget(0, random);
            assertTrue(target == 1 || target == 3, "unexpected target " + target);
        }
    }

    @Test
    void pickTargetReturnsMinusOneWhenNoOpponentIsAlive() {
        RoomRules rules = new RoomRules(3);
        rules.eliminate(1);
        rules.eliminate(2);
        assertEquals(-1, rules.pickTarget(0, new Random(1)));
    }

    @Test
    void eliminateIgnoresRepeatedElimination() {
        RoomRules rules = new RoomRules(3);
        assertTrue(rules.eliminate(1));
        assertFalse(rules.eliminate(1));
        assertEquals(2, rules.aliveCount());
    }

    @Test
    void eliminationRankingPutsLastSurvivorFirstThenLatestEliminatedFirst() {
        RoomRules rules = new RoomRules(4);
        rules.eliminate(0);
        rules.eliminate(2);
        rules.eliminate(3);
        assertEquals(List.of(1, 3, 2, 0), rules.rankingByElimination());
    }

    @Test
    void scoreRankingOrdersSurvivorsByScoreWithSlotTieBreak() {
        RoomRules rules = new RoomRules(4);
        rules.eliminate(3);
        assertEquals(List.of(1, 0, 2, 3), rules.rankingByScore(new int[] {500, 900, 500, 100}));
    }
}
