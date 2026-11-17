package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GameStatusTest {
    @Test
    void carriesPlayerStatisticsAndGameOverState() {
        // 생성된 상태 객체가 전달받은 네 종류의 플레이어 데이터를 그대로 보존하는지 확인한다.
        GameStatus status = new GameStatus(100, 2, 4, true);

        assertEquals(100, status.getScore());
        assertEquals(2, status.getLevel());
        assertEquals(4, status.getLines());
        assertTrue(status.isGameOver());
    }

    @Test
    void rejectsInvalidPlayerStatistics() {
        // 네트워크로 전달될 수 있는 상태는 점수/줄 수가 음수가 아니고 레벨이 1~10이어야 한다.
        assertThrows(IllegalArgumentException.class, () -> new GameStatus(-1, 1, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new GameStatus(0, 11, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new GameStatus(0, 1, -1, false));
    }
}
