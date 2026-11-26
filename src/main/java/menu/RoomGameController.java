package menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import game.ActionController;
import game.BattleController;
import game.GameMode;
import menu.screens.BattleMenuScreen;
import menu.screens.BattleResultScreen;
import menu.screens.GameScreen;
import network.GameStatus;
import network.RoomClient;
import network.RoomMember;

// 20명 방의 한 참가자 쪽 상태를 관리한다. 네트워크 콜백은 리더 스레드에서 오므로 UI 변경은 EDT에서 수행한다.
public final class RoomGameController implements RoomClient.Listener {

    // 대기실 화면 갱신용 콜백
    public interface LobbyListener {
        void onRoster(List<RoomMember> members, int mySlot);

        void onGameStarting();

        void onFailure(String message);

        void onLobbyDisconnected(String message);
    }

    private static final String LOCAL_MARK = " (나)";
    private static final String OUT_NOTICE = "탈락했습니다. 결과는 대전이 끝나면 표시됩니다.";

    private final LobbyListener lobby;
    private volatile RoomClient client;
    private volatile BattleController battle;
    private GameScreen screen;
    private int mySlot = -1;
    private List<String> names = new ArrayList<>();
    private boolean[] alive = new boolean[0];
    private GameStatus[] statuses = new GameStatus[0];
    // 서버가 지정한 공격 대상 슬롯(-1이면 없음)
    private int targetSlot = -1;
    private GameMode roundMode = GameMode.NORMAL;
    private int roundTimeLimitSeconds;
    private long roundStartedAt;
    private boolean roundActive;
    private boolean outReported;
    private boolean finished;

    public RoomGameController(LobbyListener lobby) {
        this.lobby = lobby;
    }

    // 접속이 끝난 뒤 클라이언트를 연결한다. 연결 전에 도착한 콜백은 EDT 큐에서 처리된다.
    public void attach(RoomClient client) {
        this.client = client;
    }

    // GameScreen이 주기적으로 호출한다. 로컬 상태를 서버에 보내고, 공격 행은 같은 락 안에서 꺼낸다.
    public void sendGameStatus() {
        RoomClient current = client;
        BattleController currentBattle = battle;
        if (!roundActive || current == null || currentBattle == null) {
            return;
        }
        GameScreen currentScreen = screen;
        GameStatus status;
        List<BattleController.Attack> attacks;
        synchronized (currentBattle) {
            status = GameStatus.capture(currentScreen.getActionController(),
                    currentScreen.getGameStateManager(), currentScreen.getScoreManager());
            attacks = currentBattle.drainOutgoingAttacks();
        }
        current.sendStatus(status);
        // 공격은 줄 클리어마다 별개로 전달해야 각각 다른 상대에게 간다
        for (BattleController.Attack attack : attacks) {
            current.sendAttack(attack.rows());
        }
    }

    // 로컬 플레이어가 게임 오버되면 한 번만 서버에 알린다.
    public void notifyLocalGameOver() {
        if (outReported) {
            return;
        }
        outReported = true;
        roundActive = false;
        if (mySlot >= 0 && mySlot < alive.length) {
            alive[mySlot] = false;
        }
        RoomClient current = client;
        if (current != null) {
            current.sendOut();
        }
        if (screen != null) {
            screen.showNotice(OUT_NOTICE);
            screen.setPlayerLines(buildPlayerLines());
        }
    }

    public boolean isRoundActive() {
        return roundActive;
    }

    // Tab 키로 공격 상대 변경을 요청한다. 실제 상대 선택과 화면 갱신은 서버 응답(onAttackTarget)에서 처리한다.
    public void requestTargetChange() {
        RoomClient current = client;
        if (roundActive && current != null) {
            current.sendChangeTarget();
        }
    }

    // 로비에서 준비 버튼을 누르면 서버에 준비 완료를 알린다
    public void sendReady() {
        RoomClient current = client;
        if (current != null) {
            current.sendReady();
        }
    }

    @Override
    public void onRoster(int mySlot, List<RoomMember> members) {
        SwingUtilities.invokeLater(() -> {
            this.mySlot = mySlot;
            this.names = new ArrayList<>();
            for (RoomMember member : members) {
                names.add(member.name());
            }
            lobby.onRoster(members, mySlot);
        });
    }

    @Override
    public void onStart(GameMode mode, int timeLimitSeconds) {
        SwingUtilities.invokeLater(() -> startRound(mode, timeLimitSeconds));
    }

    private void startRound(GameMode mode, int timeLimitSeconds) {
        if (screen != null) {
            return;
        }
        alive = new boolean[names.size()];
        Arrays.fill(alive, true);
        statuses = new GameStatus[names.size()];
        targetSlot = -1;
        roundMode = mode;
        roundTimeLimitSeconds = timeLimitSeconds;
        roundStartedAt = System.currentTimeMillis();
        BattleController localBattle = new BattleController(false);
        battle = localBattle;
        screen = new GameScreen(mode, this, timeLimitSeconds);
        // 로컬 엔진은 자신을 ONE으로만 등록한다. 공격 수신은 receiveAttack(ONE, ...)으로 처리한다.
        localBattle.registerPlayer(BattleController.Player.ONE, screen.getActionController());
        localBattle.start();
        roundActive = true;
        screen.setPlayerLines(buildPlayerLines());
        screen.setVisible(true);
        screen.allowGameStart();
        lobby.onGameStarting();
    }

    @Override
    public void onStatus(int slot, GameStatus status) {
        SwingUtilities.invokeLater(() -> {
            if (slot >= 0 && slot < statuses.length) {
                statuses[slot] = status;
            }
            if (screen != null) {
                screen.updateRemoteStatus(targetStatus());
                screen.setPlayerLines(buildPlayerLines());
            }
        });
    }

    @Override
    public void onAttackTarget(int slot) {
        SwingUtilities.invokeLater(() -> {
            targetSlot = slot;
            if (screen != null) {
                screen.updateRemoteStatus(targetStatus());
                screen.setPlayerLines(buildPlayerLines());
            }
        });
    }

    // 상대 보드에는 서버가 지정한 공격 대상의 보드를 표시한다. 대상이 없거나 아직 상태가 없으면 null이다.
    private GameStatus targetStatus() {
        if (targetSlot < 0 || targetSlot >= statuses.length) {
            return null;
        }
        return statuses[targetSlot];
    }

    @Override
    public void onAttack(List<int[]> rows) {
        SwingUtilities.invokeLater(() -> {
            BattleController current = battle;
            if (current != null) {
                current.receiveAttack(BattleController.Player.ONE, rows);
            }
        });
    }

    @Override
    public void onPlayerOut(int slot) {
        SwingUtilities.invokeLater(() -> {
            if (slot >= 0 && slot < alive.length) {
                alive[slot] = false;
            }
            if (screen != null) {
                screen.setPlayerLines(buildPlayerLines());
            }
        });
    }

    @Override
    public void onRoomEnd(List<Integer> rankingSlots) {
        SwingUtilities.invokeLater(() -> {
            finished = true;
            roundActive = false;
            BattleResult result = buildRoomResult(rankingSlots);
            closeGameScreen();
            closeClient();
            if (result != null) {
                new BattleResultScreen(result).setVisible(true);
            } else {
                new BattleMenuScreen().setVisible(true);
            }
        });
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> {
            if (screen == null) {
                lobby.onFailure(message);
                return;
            }
            showMessageAndReturn(message);
        });
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            if (finished) {
                return;
            }
            if (screen == null) {
                lobby.onLobbyDisconnected("호스트와의 연결이 끊어졌습니다.");
                return;
            }
            showMessageAndReturn("호스트와의 연결이 끊어졌습니다.");
        });
    }

    // 게임 화면에 표시할 참가자 목록 (슬롯 번호와 생존 여부 포함)
    private List<String> buildPlayerLines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            String line = (i + 1) + ". " + names.get(i) + (i == mySlot ? LOCAL_MARK : "");
            if (i < alive.length && !alive[i]) {
                line += " [탈락]";
            }
            lines.add(line);
        }
        return lines;
    }

    // 온라인 결과: 왼쪽 = 1등, 오른쪽 = 로컬 플레이어(1등이면 2등). 순위표는 전체 순위를 쓴다.
    private BattleResult buildRoomResult(List<Integer> rankingSlots) {
        if (rankingSlots.isEmpty() || mySlot < 0) {
            return null;
        }
        // 로컬 상태는 서버 방송을 기다리지 않고 지금 화면에서 직접 읽는다
        GameStatus localStatus = captureLocalStatus();
        List<BattleResult.Standing> standings = new ArrayList<>();
        for (int i = 0; i < rankingSlots.size(); i++) {
            int slot = rankingSlots.get(i);
            GameStatus status = slot == mySlot ? localStatus : statusOf(slot);
            int score = status == null ? 0 : status.getScore();
            standings.add(new BattleResult.Standing(i + 1, nameOf(slot), score, slot == mySlot));
        }
        int firstSlot = rankingSlots.get(0);
        boolean localIsFirst = firstSlot == mySlot;
        int otherSlot = localIsFirst
                ? (rankingSlots.size() > 1 ? rankingSlots.get(1) : -1)
                : mySlot;
        BattleResult.Side first = BattleResult.side(nameOf(firstSlot),
                firstSlot == mySlot ? localStatus : statusOf(firstSlot));
        BattleResult.Side other = BattleResult.side(
                otherSlot >= 0 ? nameOf(otherSlot) : "-",
                otherSlot == mySlot ? localStatus : statusOf(otherSlot));
        return BattleResult.fromRoom(first, other, standings,
                roomModeLabel(), System.currentTimeMillis() - roundStartedAt);
    }

    private String roomModeLabel() {
        if (roundTimeLimitSeconds > 0) {
            return "시간 제한 모드";
        }
        return roundMode == GameMode.ITEM ? "아이템 모드" : "일반 모드";
    }

    private GameStatus captureLocalStatus() {
        BattleController currentBattle = battle;
        GameScreen currentScreen = screen;
        if (currentBattle == null || currentScreen == null) {
            return statusOf(mySlot);
        }
        synchronized (currentBattle) {
            return GameStatus.capture(currentScreen.getActionController(),
                    currentScreen.getGameStateManager(), currentScreen.getScoreManager());
        }
    }

    private GameStatus statusOf(int slot) {
        return slot >= 0 && slot < statuses.length ? statuses[slot] : null;
    }

    private String nameOf(int slot) {
        return slot >= 0 && slot < names.size() ? names.get(slot) : "?";
    }

    private void showMessageAndReturn(String message) {
        finished = true;
        roundActive = false;
        closeGameScreen();
        JOptionPane.showMessageDialog(null, message, "대전 종료", JOptionPane.WARNING_MESSAGE);
        returnToMenu();
    }

    private void closeGameScreen() {
        BattleController current = battle;
        if (current != null) {
            current.close();
        }
        if (screen != null) {
            screen.stopGame();
            screen.dispose();
            screen = null;
        }
        battle = null;
    }

    // 모든 경로에서 연결을 정리하고 대전 메뉴로 돌아간다.
    private void returnToMenu() {
        closeClient();
        new BattleMenuScreen().setVisible(true);
    }

    private void closeClient() {
        RoomClient current = client;
        if (current != null) {
            current.close();
        }
    }
}
