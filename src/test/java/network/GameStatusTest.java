package network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.ActionController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    @Test // 상대 화면용 보드: 숨은 줄을 빼고 떨어지는 블록을 겹친다. 보드 밖 칸은 버린다.
    void composeDropsHiddenRowsAndOverlaysFallingBlock() {
        int[][] board = new int[4][3];   // 숨은 줄 1 + 보이는 줄 3
        board[3][0] = 7;
        ActionController.BlockSnapshot falling =
                new ActionController.BlockSnapshot(1, 0, new int[][] {{2, 2}, {0, 2}});

        int[][] visible = GameStatus.compose(board, 1, falling);

        // 블록 첫 줄(y=0)은 숨은 줄이라 버려지고, 둘째 줄의 칸만 보이는 첫 줄에 들어간다
        assertArrayEquals(new int[][] {{0, 0, 2}, {0, 0, 0}, {7, 0, 0}}, visible);
        // 오른쪽 밖으로 나간 칸은 무시
        int[][] clipped = GameStatus.compose(new int[2][2], 0,
                new ActionController.BlockSnapshot(1, 0, new int[][] {{3, 3}}));
        assertArrayEquals(new int[][] {{0, 3}, {0, 0}}, clipped);
    }

    @Test // 보드까지 포함해 보내고 받은 값이 그대로인지
    void writeAndReadKeepBoard() throws IOException {
        int[][] cells = {{0, 1, -16777000}, {5, 0, 0}};
        GameStatus sent = new GameStatus(1200, 3, 8, false, cells);

        GameStatus received = roundTrip(sent);

        assertEquals(1200, received.getScore());
        assertEquals(3, received.getLevel());
        assertEquals(8, received.getLines());
        assertFalse(received.isGameOver());
        assertTrue(received.hasBoard());
        assertArrayEquals(cells, received.getCells());
    }

    @Test // 보드 없이 만든 상태도 주고받을 수 있음 (빈 보드)
    void statusWithoutBoardRoundTrips() throws IOException {
        GameStatus received = roundTrip(new GameStatus(5, 1, 0, true));

        assertFalse(received.hasBoard());
        assertTrue(received.isGameOver());
    }

    @Test // 너무 큰 보드 크기나 잘못된 모양은 거부
    void rejectsInvalidBoardShapes() throws IOException {
        assertThrows(IllegalArgumentException.class,
                () -> new GameStatus(0, 1, 0, false, new int[GameStatus.MAX_ROWS + 1][10]));
        assertThrows(IllegalArgumentException.class,
                () -> new GameStatus(0, 1, 0, false, new int[][] {{1, 2}, {3}}));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(0);
        out.writeInt(1);
        out.writeInt(0);
        out.writeBoolean(false);
        out.writeShort(1000);   // 행 수가 상한을 넘음
        out.writeShort(10);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertThrows(IOException.class, () -> GameStatus.readFrom(in));
    }

    @Test // 받은 쪽이 배열을 바꿔도 원본은 그대로
    void cellsAreDefensivelyCopied() {
        int[][] cells = {{1, 0}};
        GameStatus status = new GameStatus(0, 1, 0, false, cells);
        cells[0][0] = 9;
        status.getCells()[0][1] = 9;

        assertArrayEquals(new int[][] {{1, 0}}, status.getCells());
    }

    private static GameStatus roundTrip(GameStatus status) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        status.writeTo(new DataOutputStream(bytes));
        return GameStatus.readFrom(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
    }
}
