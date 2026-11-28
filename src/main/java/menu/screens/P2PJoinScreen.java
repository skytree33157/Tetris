package menu.screens;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import menu.P2PGameRunner;
import menu.views.P2PJoinView;
import network.P2PGameSession;
import network.RecentPeerStore;

// 온라인 참여하기: IP 입력(또는 최근 접속 선택) → 접속 → 게임 시작 요청 후 방장을 기다림
public class P2PJoinScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String CONNECTING_TEXT = "연결 중";
    private static final String WAITING_TEXT = "상대를 기다리는 중";

    private final P2PJoinView view;
    private final Timer dotsTimer;
    private boolean connecting;
    // 접속이 끝나 로비에 머무는 동안의 세션 (게임 화면으로 넘어가면 null)
    private P2PGameSession session;
    private boolean localReady;
    private boolean remoteReady;
    private String statusPrefix = CONNECTING_TEXT;
    private int dots;

    public P2PJoinScreen() {
        this(null);
    }

    // 게임 종료 후 같은 연결을 이어받아 로비로 돌아온다 (resumed가 null이면 새 접속 화면)
    public P2PJoinScreen(P2PGameSession resumed) {
        super("TFSE - 온라인 참여");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        view = new P2PJoinView(this::connect, this::back, this::pickRecent, address -> {
            pickRecent(address);
            connect();
        });
        List<String> recent = loadRecent();
        view.setRecentAddresses(recent);
        if (!recent.isEmpty()) {
            view.getAddressField().setText(recent.get(0));
        }
        setContentPane(view);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        dotsTimer = new Timer(450, e -> {
            dots = dots % 3 + 1;
            view.setStatus(statusPrefix + ".".repeat(dots), false);
        });

        // 입력칸에 포커스가 있어도 Esc로 뒤로 가기
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getRootPane().getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                back();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                view.getAddressField().requestFocusInWindow();
                view.getAddressField().selectAll();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                back();
            }
        });

        if (resumed != null) {
            P2PGameRunner.resumeLobby(resumed, lobbyListener());
        }
    }

    private static List<String> loadRecent() {
        try {
            return new RecentPeerStore().load();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void pickRecent(String address) {
        if (!connecting) {
            view.setAddress(address);
        }
    }

    private void connect() {
        if (session != null) {
            startGame();
            return;
        }
        if (connecting) {
            return;
        }
        String address = view.getAddress();
        if (address.isEmpty()) {
            view.setStatus("주소를 입력해 주세요.", true);
            return;
        }
        connecting = true;
        view.setConnecting(true);
        statusPrefix = CONNECTING_TEXT;
        dots = 0;
        view.setStatus(CONNECTING_TEXT + "...", false);
        dotsTimer.start();

        P2PGameRunner.joinServer(address, lobbyListener());
    }

    private P2PGameRunner.ConnectListener lobbyListener() {
        return new P2PGameRunner.ConnectListener() {
            @Override
            public void onConnected(P2PGameSession connected) {
                if (!isDisplayable()) {
                    return;
                }
                connecting = false;
                session = connected;
                dotsTimer.stop();
                view.setLobby(false);
                updateLobbyStatus();
            }

            @Override
            public void onRemoteReady(boolean ready) {
                if (!isDisplayable()) {
                    return;
                }
                remoteReady = ready;
                if (session != null && !localReady) {
                    updateLobbyStatus();
                }
            }

            @Override
            public void onGameStarting() {
                // 세션은 게임 루프가 가져가므로 로비에서는 더 이상 닫지 않는다
                session = null;
                dotsTimer.stop();
                dispose();
            }

            @Override
            public void onLobbyDisconnected(IOException exception) {
                if (!isDisplayable()) {
                    return;
                }
                // 게임 중 끊김과 같이 오류 창을 띄운 뒤 P2P 초기 화면으로 돌아간다
                dotsTimer.stop();
                session = null;
                dispose();
                P2PGameRunner.showDisconnectDialog(exception);
                new P2PJoinScreen().setVisible(true);
            }

            @Override
            public void onFailure(IOException exception) {
                if (!isDisplayable()) {
                    return;
                }
                connecting = false;
                dotsTimer.stop();
                view.setConnecting(false);
                view.setStatus(P2PGameRunner.describeFailure(exception), true);
                view.getAddressField().requestFocusInWindow();
            }
        };
    }

    // 방장이 준비했는지에 따라 안내 문구를 고른다
    private void updateLobbyStatus() {
        view.setStatus(remoteReady
                ? "방장이 준비했어요. 게임 시작을 누르면 바로 시작해요."
                : "방에 접속했어요. 게임 시작을 누르고 방장을 기다려 주세요.", false);
    }

    // 게임 시작 요청: 방장이 게임 방식을 고르고 시작할 때까지 대기한다
    private void startGame() {
        if (localReady || session == null) {
            return;
        }
        try {
            session.setReady();
        } catch (IOException exception) {
            view.setStatus(P2PGameRunner.describeFailure(exception), true);
            return;
        }
        localReady = true;
        statusPrefix = WAITING_TEXT;
        dots = 0;
        view.setLobby(true);
        view.setStatus(WAITING_TEXT + "...", false);
        dotsTimer.start();
    }

    private void back() {
        // 연결 시도 중이면 최대 5초 안에 끝나므로 기다리게 한다 (중간에 창을 닫으면 결과를 받을 곳이 없음)
        if (connecting) {
            return;
        }
        dotsTimer.stop();
        if (session != null) {
            closeSession(session);
            session = null;
        }
        dispose();
        new BattleMenuScreen().setVisible(true);
    }

    private static void closeSession(P2PGameSession target) {
        try {
            target.close();
        } catch (IOException ignored) {
            // 이미 끊어진 연결이면 닫을 것이 없음
        }
    }
}

