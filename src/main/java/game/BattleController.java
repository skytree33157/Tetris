package game;

import blocks.core.BlockType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * FR-41~43의 화면/통신과 독립된 경기 관리자.
 * 로컬 플레이어는 start() 전에 등록한다. P2P에서는 이 PC의 플레이어만 등록하고,
 * 미등록 상대에게 보낼 공격은 drainOutgoingAttacks()로 꺼낸다.
 * 등록된 ActionController와 같은 잠금을 사용하므로 공격·고정·종료가 서로 교차하지 않는다.
 * 이 잠금 안에서는 소켓, UI 콜백, 다른 경기의 컨트롤러를 호출하지 않는다.
 */
public final class BattleController implements AutoCloseable {
    public enum Player {
        ONE, TWO;
        public Player opponent() { return this == ONE ? TWO : ONE; }
    }

    public enum Outcome { IN_PROGRESS, ONE_WINS, TWO_WINS, DRAW, CANCELLED }
    public enum EndReason { TOP_OUT, TIME_LIMIT, CANCELLED }

    /** 행과 순서를 복사한다. rows()도 복사본을 반환하므로 네트워크/UI와 배열을 공유하지 않는다. */
    public record Attack(Player sender, List<int[]> rows) {
        public Attack {
            Objects.requireNonNull(sender, "sender");
            rows = copyAttackRows(rows);
        }
        @Override public List<int[]> rows() { return copyAttackRows(rows); }
    }

    private final Map<Player, ActionController> players = new EnumMap<>(Player.class);
    // 이 PC에 실제 상대 컨트롤러가 없을 때만 통신 계층이 꺼내갈 공격을 보관한다.
    private final List<Attack> outgoingAttacks = new ArrayList<>();
    // true: 로컬/서버에서 승패 확정, false: 클라이언트에서 서버 결과 대기.
    private final boolean resultAuthority;
    private final List<Player> outgoingGameOvers = new ArrayList<>();
    // 전송 대기 목록을 비운 뒤에도 같은 탈락을 다시 보고하지 않도록 별도로 기억한다.
    private final EnumSet<Player> reportedGameOvers = EnumSet.noneOf(Player.class);
    private boolean started;
    private boolean running;
    private Outcome outcome = Outcome.IN_PROGRESS;
    private EndReason endReason;
    private boolean paused;
    private long startedNanos;
    private long pausedNanos;
    private long pauseStartedNanos;
    private long finishedNanos;

    public record ViewSnapshot(Map<Player, ActionController.ViewSnapshot> players,
            boolean paused, Outcome outcome, EndReason endReason, long elapsedPlayMillis) {
        public ViewSnapshot { players = Map.copyOf(players); }
    }

    /** 양쪽 플레이어와 결과를 같은 시점에 조회한다. 실제 UI 갱신은 반환 후 EDT에서 한다. */
    public synchronized ViewSnapshot getViewSnapshot() {
        Map<Player, ActionController.ViewSnapshot> views = new EnumMap<>(Player.class);
        players.forEach((player, action) -> views.put(player, action.getViewSnapshot()));
        return new ViewSnapshot(views, paused, outcome, endReason, getElapsedPlayMillis());
    }

    /** 정지 상태는 경기 전체에서 하나만 관리한다. UI/네트워크 콜백은 잠금 안에서 호출하지 않는다. */
    public synchronized void setPaused(boolean paused) {
        if (!running || this.paused == paused || !reportedGameOvers.isEmpty()) return;
        long now = System.nanoTime();
        if (paused) pauseStartedNanos = now;
        else pausedNanos += now - pauseStartedNanos;
        this.paused = paused;
        players.values().forEach(action -> action.setLocalPaused(paused));
    }

    public synchronized boolean isPaused() { return paused; }

    /** 시간제한 담당자는 이 진행 시간을 사용하면 일시정지 시간을 제외할 수 있다. */
    public synchronized long getElapsedPlayMillis() {
        if (!started) return 0;
        long now = finishedNanos != 0 ? finishedNanos : System.nanoTime();
        long pendingPause = paused ? now - pauseStartedNanos : 0;
        return Math.max(0, (now - startedNanos - pausedNanos - pendingPause) / 1_000_000L);
    }

    /** 로컬 경기와 P2P 서버는 최종 결과를 확정한다. */
    public BattleController() { this(true); }

    /** P2P 클라이언트는 false: 탈락을 보고하고 서버의 최종 결과를 기다린다. */
    public BattleController(boolean resultAuthority) { this.resultAuthority = resultAuthority; }

    /** 등록은 실행 스레드/입력/타이머를 시작하기 전에 끝낸다. 원격 플레이어 객체는 필요하지 않다. */
    public synchronized void registerPlayer(Player player, ActionController action) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        if (started || outcome != Outcome.IN_PROGRESS) throw new IllegalStateException("Match already started/closed");
        if (players.containsKey(player) || players.containsValue(action)) {
            throw new IllegalArgumentException("Player/controller already registered");
        }
        // attachBattle에서 플레이어의 잠금을 이 경기 객체로 바꾼다. 실행 전 등록해야 하는 이유다.
        action.attachBattle(this, player);
        players.put(player, action);
    }

    /** P2P의 양쪽 준비 확인은 통신 담당자가 끝낸 뒤 호출한다. */
    public synchronized void start() {
        if (started || outcome != Outcome.IN_PROGRESS) throw new IllegalStateException("Match cannot restart");
        if (players.isEmpty()) throw new IllegalStateException("At least one local player is required");
        started = true;
        running = true;
        startedNanos = System.nanoTime();
        players.values().forEach(action -> action.setLocalPaused(false));
        boolean oneOver = isGameOver(Player.ONE), twoOver = isGameOver(Player.TWO);
        if (oneOver && twoOver && resultAuthority) finish(Outcome.DRAW, EndReason.TOP_OUT);
        else {
            if (oneOver) reportGameOver(Player.ONE);
            if (twoOver) reportGameOver(Player.TWO);
        }
    }

    public synchronized boolean isRunning() { return running; }
    public synchronized Outcome getOutcome() { return outcome; }
    public synchronized EndReason getEndReason() { return endReason; }

    private boolean isGameOver(Player player) {
        ActionController action = players.get(player);
        return action != null && action.isGameOver();
    }

    // ActionController가 실제 삭제를 완료한 뒤 호출한다. 동일 경기 잠금 안에서만 로컬 전달한다.
    synchronized void publishAttack(Player sender, List<int[]> rows) {
        if (!running || rows.size() < 2) return;
        Player target = sender.opponent();
        ActionController receiver = players.get(target);
        // 로컬은 바로 상대 대기열로, P2P는 전송용 데이터로 넘긴다. 공격 생성 규칙은 동일하다.
        if (receiver != null) receiver.receiveAttackRows(rows);
        else outgoingAttacks.add(new Attack(sender, rows));
    }

    /** P2P 수신용. 경기 ID/중복 메시지 검사는 통신 계층에서 수행한 뒤 한 번만 호출한다. */
    public synchronized int receiveAttack(Player target, List<int[]> rows) {
        Objects.requireNonNull(target, "target");
        if (!running) return 0;
        ActionController receiver = players.get(target);
        if (receiver == null) throw new IllegalArgumentException("Target is not local");
        return receiver.receiveAttackRows(rows);
    }

    /** 원격 상대에게 전송할 공격을 정확히 한 번 꺼낸다. 소켓 쓰기는 이 메서드가 반환된 뒤 수행한다. */
    public synchronized List<Attack> drainOutgoingAttacks() {
        List<Attack> result = List.copyOf(outgoingAttacks);
        // 다음 조회에서 같은 공격을 다시 보내지 않도록 꺼낸 항목은 제거한다.
        outgoingAttacks.clear();
        return result;
    }

    /** 첫 종료 판정만 유효하다. 원격 탈락도 같은 API로 보고할 수 있다. */
    public synchronized boolean reportGameOver(Player loser) {
        Objects.requireNonNull(loser, "loser");
        if (!running || !reportedGameOvers.add(loser)) return false;
        if (!resultAuthority) {
            outgoingGameOvers.add(loser);
            // 클라이언트가 자체 승패를 먼저 확정하면 동시 탈락 때 서로 다른 결과가 된다.
            players.values().forEach(ActionController::shutdown);
            return true;
        }
        return finish(loser == Player.ONE ? Outcome.TWO_WINS : Outcome.ONE_WINS, EndReason.TOP_OUT);
    }

    /** 클라이언트가 서버에 보고할 탈락 정보를 꺼낸다. 실제 전송은 반환 후 수행한다. */
    public synchronized List<Player> drainOutgoingGameOvers() {
        List<Player> result = List.copyOf(outgoingGameOvers);
        outgoingGameOvers.clear();
        return result;
    }

    /** 클라이언트 전용. 검증된 서버 결과를 수신한 뒤 호출한다. */
    public synchronized boolean acceptRemoteResult(Outcome result, EndReason reason) {
        if (resultAuthority) throw new IllegalStateException("Only a replica accepts remote results");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(reason, "reason");
        if (result == Outcome.IN_PROGRESS || (result == Outcome.CANCELLED) != (reason == EndReason.CANCELLED)) {
            throw new IllegalArgumentException("Invalid terminal result");
        }
        return finish(result, reason);
    }

    /** FR-40 확장 지점. 타이머/서버가 만료를 확인한 뒤 확정된 양쪽 점수를 전달한다. */
    public synchronized boolean finishByTime(int oneScore, int twoScore) {
        if (oneScore < 0 || twoScore < 0) throw new IllegalArgumentException("Scores must be non-negative");
        if (!resultAuthority) return false;
        return finish(oneScore == twoScore ? Outcome.DRAW
                : oneScore > twoScore ? Outcome.ONE_WINS : Outcome.TWO_WINS, EndReason.TIME_LIMIT);
    }

    private boolean finish(Outcome result, EndReason reason) {
        // 뒤늦은 상대 탈락이나 시간 만료가 이미 확정한 승자를 바꾸지 못하게 한다.
        if (!running) return false;
        running = false;
        finishedNanos = System.nanoTime();
        outcome = result;
        endReason = reason;
        outgoingAttacks.clear();
        outgoingGameOvers.clear();
        // 화면 전환만으로는 부족하다. 양쪽 조작·점수 처리와 예약된 행 삭제도 멈춘다.
        players.values().forEach(ActionController::shutdown);
        return true;
    }

    /** 연결 종료/화면 이탈용. 재경기는 새 BattleController와 새 플레이어 객체로 시작한다. */
    @Override public synchronized void close() {
        // 승패가 정해진 뒤 창을 닫더라도 기존 결과를 CANCELLED로 덮어쓰지 않는다.
        if (outcome == Outcome.IN_PROGRESS) {
            running = false;
            finishedNanos = System.nanoTime();
            outcome = Outcome.CANCELLED;
            endReason = EndReason.CANCELLED;
            outgoingAttacks.clear();
            outgoingGameOvers.clear();
        }
        players.values().forEach(ActionController::shutdown);
    }

    // 리스트만 복사하면 내부 int[]는 공유된다. 검증 후 각 행까지 복사해 소유권을 분리한다.
    static List<int[]> copyAttackRows(List<int[]> rows) {
        Objects.requireNonNull(rows, "rows");
        List<int[]> result = new ArrayList<>();
        for (int[] row : rows) {
            if (row == null || row.length != 10) throw new IllegalArgumentException("Attack row must have 10 cells");
            for (int cell : row) {
                if (cell != 0 && cell != BlockType.ATTACK.getValue()) {
                    throw new IllegalArgumentException("Invalid attack cell");
                }
            }
            result.add(row.clone());
        }
        return List.copyOf(result);
    }
}
