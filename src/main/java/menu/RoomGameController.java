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
                screen.updateRemoteStatus(bestOpponentStatus());
                screen.setPlayerLines(buildPlayerLines());
            }
        });
    }

    // 20명 방이라 한 명만 보여줄 수 있으므로, 살아 있는 상대 중 점수가 가장 높은 보드를 표시한다.
    private GameStatus bestOpponentStatus() {
        GameStatus best = null;
        for (int i = 0; i < statuses.length; i++) {
            if (i == mySlot || statuses[i] == null || i >= alive.length || !alive[i]) {
                continue;
            }
            if (best == null || statuses[i].getScore() > best.getScore()) {
                best = statuses[i];
            }
        }
        return best;
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
            closeGameScreen();
            showRanking(rankingSlots);
            returnToMenu();
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

    private void showRanking(List<Integer> rankingSlots) {
        StringBuilder text = new StringBuilder("순위\n");
        for (int i = 0; i < rankingSlots.size(); i++) {
            int slot = rankingSlots.get(i);
            String name = slot >= 0 && slot < names.size() ? names.get(slot) : "?";
            text.append(i + 1).append("위: ").append(name);
            if (slot == mySlot) {
                text.append(LOCAL_MARK);
            }
            text.append('\n');
        }
        JOptionPane.showMessageDialog(null, text.toString(), "대전 결과", JOptionPane.INFORMATION_MESSAGE);
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
        RoomClient current = client;
        if (current != null) {
            current.close();
        }
        new BattleMenuScreen().setVisible(true);
    }
}
