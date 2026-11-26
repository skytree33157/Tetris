package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import blocks.core.BlockType;

// 방 서버와 클라이언트가 공유하는 프로토콜 상수와 공격 줄 인코딩
public final class RoomProtocol {
    public static final int DEFAULT_PORT = 57823;
    public static final int MAGIC = 0x54524F4D;
    public static final int MAX_PARTICIPANTS = 20;
    static final int KEEP_ALIVE_INTERVAL_MS = 2_000;
    static final int READ_TIMEOUT_MS = 10_000;
    static final int HANDSHAKE_TIMEOUT_MS = 5_000;
    static final int BOARD_WIDTH = 10;
    static final int MAX_ATTACK_ROWS = 10;

    // 클라이언트 -> 서버
    static final int C_STATUS = 3;
    static final int C_CLOSE = 4;
    static final int C_KEEP_ALIVE = 5;
    static final int C_ATTACK = 6;
    static final int C_OUT = 7;
    static final int C_READY = 8;
    static final int C_CHANGE_TARGET = 9;

    // 서버 -> 클라이언트
    static final int S_ROSTER = 10;
    static final int S_START = 11;
    static final int S_STATUS = 12;
    static final int S_ATTACK = 13;
    static final int S_PLAYER_OUT = 14;
    static final int S_ROOM_END = 15;
    static final int S_ERROR = 16;
    static final int S_TARGET = 17;
    static final int S_KEEP_ALIVE = 5;
    static final int S_CLOSE = 4;

    private RoomProtocol() {
    }

    static void writeRows(DataOutputStream output, List<int[]> rows) throws IOException {
        if (rows.isEmpty() || rows.size() > MAX_ATTACK_ROWS) {
            throw new IOException("Invalid attack row count");
        }
        output.writeByte(rows.size());
        for (int[] row : rows) {
            if (row.length != BOARD_WIDTH) {
                throw new IOException("Invalid attack row width");
            }
            for (int cell : row) {
                output.writeByte(cell);
            }
        }
    }

    static List<int[]> readRows(DataInputStream input) throws IOException {
        int count = input.readUnsignedByte();
        if (count < 1 || count > MAX_ATTACK_ROWS) {
            throw new IOException("Invalid attack row count");
        }
        List<int[]> rows = new ArrayList<>(count);
        for (int r = 0; r < count; r++) {
            int[] row = new int[BOARD_WIDTH];
            for (int c = 0; c < BOARD_WIDTH; c++) {
                int cell = input.readUnsignedByte();
                if (cell != 0 && cell != BlockType.ATTACK.getValue()) {
                    throw new IOException("Invalid attack cell");
                }
                row[c] = cell;
            }
            rows.add(row);
        }
        return rows;
    }
}
