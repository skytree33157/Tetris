package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class LatencyStatusTrackerTest {

    @Test
    void noMessageWhenNoPeerIsLagging() {
        LatencyStatusTracker tracker = new LatencyStatusTracker();

        assertTrue(tracker.message().isEmpty());
    }

    @Test
    void messageListsEveryLaggingPeerInSlotOrder() {
        LatencyStatusTracker tracker = new LatencyStatusTracker();
        tracker.markLagging(2);
        tracker.markLagging(1);

        assertEquals("P2, P3 연결 지연 중...", tracker.message().orElseThrow());
    }

    @Test
    void recoveringOnePeerKeepsTheOtherPeerInTheMessage() {
        LatencyStatusTracker tracker = new LatencyStatusTracker();
        tracker.markLagging(1);
        tracker.markLagging(2);

        tracker.markRecovered(1);

        assertEquals("P3 연결 지연 중...", tracker.message().orElseThrow());
    }

    @Test
    void departedPeerIsRemovedAndLabelClearsWhenNoneRemain() {
        LatencyStatusTracker tracker = new LatencyStatusTracker();
        tracker.markLagging(1);
        tracker.markLagging(3);

        tracker.keepOnly(List.of(0, 1));
        assertEquals("P2 연결 지연 중...", tracker.message().orElseThrow());

        tracker.keepOnly(List.of(0));
        assertTrue(tracker.message().isEmpty());
    }
}
