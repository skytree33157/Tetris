package menu;

import network.GameStatus;

/**
 * 대전 결과. 화면은 왼쪽/오른쪽 플레이어 기준으로 그린다.
 * P2P에서는 왼쪽 = 나, 오른쪽 = 상대이고 제목을 "승리/패배"로 쓴다(personal).
 * 로컬 2인 대전에서는 "1P 승리" 형식으로 쓴다.
 */
public record BattleResult(Side left, Side right, Outcome outcome, boolean personal,
                           String modeLabel, long elapsedMs) {

    public enum Outcome { LEFT_WINS, RIGHT_WINS, DRAW }

    /** 한 플레이어의 최종 기록과 마지막 보드(보이는 줄). */
    public record Side(String name, int score, int level, int lines, boolean toppedOut, int[][] cells) {
        public Side {
            cells = copy(cells == null ? new int[0][0] : cells);
        }

        @Override
        public int[][] cells() {
            return copy(cells);
        }

        // 결과 화면의 보드 패널에 그대로 넘길 수 있는 상태
        public GameStatus toStatus() {
            return new GameStatus(score, Math.max(1, Math.min(10, level)), lines, toppedOut, cells);
        }
    }

    /**
     * P2P 대전 결과. 먼저 블록이 천장에 닿은 쪽이 진다.
     *
     * @param mine   내 마지막 상태 (보드 포함)
     * @param remote 상대가 마지막으로 보낸 상태. 아직 받지 못했으면 null
     */
    public static BattleResult fromP2P(GameStatus mine, GameStatus remote, String modeLabel, long elapsedMs) {
        Side me = side("나", mine);
        Side opponent = side("상대", remote);
        return new BattleResult(me, opponent, judge(me.toppedOut(), opponent.toppedOut()),
                true, modeLabel, elapsedMs);
    }

    // 왼쪽이 끝났는지, 오른쪽이 끝났는지로 승패를 정한다
    static Outcome judge(boolean leftToppedOut, boolean rightToppedOut) {
        if (leftToppedOut == rightToppedOut) {
            return Outcome.DRAW;
        }
        return leftToppedOut ? Outcome.RIGHT_WINS : Outcome.LEFT_WINS;
    }

    public boolean leftWon() {
        return outcome == Outcome.LEFT_WINS;
    }

    public boolean rightWon() {
        return outcome == Outcome.RIGHT_WINS;
    }

    /** 큰 제목. P2P는 내 기준 "승리/패배", 로컬은 "1P 승리". */
    public String headline() {
        if (outcome == Outcome.DRAW) {
            return "무승부";
        }
        if (personal) {
            return leftWon() ? "승리" : "패배";
        }
        return (leftWon() ? left.name() : right.name()) + " 승리";
    }

    /** 제목 아래 한 줄 설명. */
    public String reason() {
        if (outcome == Outcome.DRAW) {
            return left.toppedOut() ? "두 사람이 동시에 끝났어요" : "아직 승부가 나지 않았어요";
        }
        if (personal) {
            return leftWon() ? "상대 블록이 먼저 천장에 닿았어요" : "블록이 먼저 천장에 닿았어요";
        }
        String loser = leftWon() ? right.name() : left.name();
        return loser + "의 블록이 먼저 천장에 닿았어요";
    }

    private static Side side(String name, GameStatus status) {
        if (status == null) {
            return new Side(name, 0, 1, 0, false, new int[0][0]);
        }
        return new Side(name, status.getScore(), status.getLevel(), status.getLines(),
                status.isGameOver(), status.getCells());
    }

    private static int[][] copy(int[][] source) {
        int[][] result = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            result[i] = source[i].clone();
        }
        return result;
    }
}
