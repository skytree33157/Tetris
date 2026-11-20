package menu.screens;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.Timer;

import game.GameMode;
import menu.P2PGameRunner;
import menu.views.P2PHostView;
import network.LocalAddresses;
import network.P2PGameSession;

// 온라인 방 만들기: 게임 방식 선택 → 방 열기 → 상대 접속 대기
public class P2PHostScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final GameMode[] MODES = {GameMode.NORMAL, GameMode.ITEM};
    private static final String[] MODE_LABELS = {"일반", "아이템"};
    private static final String WAITING_TEXT = "상대를 기다리는 중";
    private static final String OPEN_TEXT = "방 열기";
    private static final String START_TEXT = "게임 시작";
    private static final String CANCEL_TEXT = "취소";

    private final P2PHostView view;
    private final Timer dotsTimer;
    private int modeIndex;
    private int dots;
    // 상대 접속을 기다리는 동안의 핸들 (접속하면 null)
    private P2PGameRunner.HostHandle hostHandle;
    // 접속한 상대의 로비 세션
    private P2PGameSession session;
    private boolean remoteReady;
    private boolean localReady;
    // 게임 화면으로 넘어가 이 창을 닫은 뒤인지
    private boolean left;

    public P2PHostScreen() {
        super("TFSE - 방 만들기");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        view = new P2PHostView(MODE_LABELS, P2PGameSession.DEFAULT_PORT,
                this::pickMode, this::togglePrimary, this::back);
        view.setAddresses(LocalAddresses.siteLocalIPv4());
        view.setModeIndex(modeIndex);
        setContentPane(view);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        // "기다리는 중." → ".." → "..." 반복
        dotsTimer = new Timer(450, e -> {
            dots = dots % 3 + 1;
            view.setStatus(WAITING_TEXT + ".".repeat(dots), false);
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> pickMode(Math.floorMod(modeIndex - 1, MODES.length));
                    case KeyEvent.VK_RIGHT -> pickMode(Math.floorMod(modeIndex + 1, MODES.length));
                    case KeyEvent.VK_ENTER -> togglePrimary();
                    case KeyEvent.VK_ESCAPE -> back();
                    default -> { }
                }
            }
        });
        setFocusable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                back();
            }
        });
    }

    private void pickMode(int index) {
        if (localReady || left) {
            return;
        }
        modeIndex = index;
        view.setModeIndex(index);
        requestFocusInWindow();
    }

    // Enter / 버튼: 방 열기 → (접속 대기 중이면 취소) → (상대 접속 후) 게임 시작
    private void togglePrimary() {
        if (left || localReady) {
            return;
        }
        if (session != null) {
            startGame();
        } else if (hostHandle != null) {
            cancelWaiting();
            view.setStatus("대기를 취소했어요.", false);
        } else {
            openRoom();
        }
        requestFocusInWindow();
    }

    private void openRoom() {
        try {
            hostHandle = P2PGameRunner.startServer(hostListener());
        } catch (IOException exception) {
            view.setStatus(P2PGameRunner.describeFailure(exception), true);
            return;
        }
        view.setPrimary(CANCEL_TEXT, true);
        startDots();
    }

    // 상대가 접속하면 게임 방식을 고르고 게임 시작을 누를 수 있다
    private void startGame() {
        try {
            session.setMode(MODES[modeIndex]);
            session.setReady();
        } catch (IOException exception) {
            view.setStatus(P2PGameRunner.describeFailure(exception), true);
            return;
        }
        localReady = true;
        view.setModeEnabled(false);
        view.setPrimary(WAITING_TEXT, false);
        startDots();
    }

    private void startDots() {
        dots = 0;
        view.setStatus(WAITING_TEXT + "...", false);
        dotsTimer.start();
    }

    private void cancelWaiting() {
        if (hostHandle != null) {
            hostHandle.cancel();
        }
        resetRoom();
    }

    // 방을 닫고 처음 상태(방 열기)로 되돌린다
    private void resetRoom() {
        hostHandle = null;
        session = null;
        remoteReady = false;
        localReady = false;
        dotsTimer.stop();
        view.setPrimary(OPEN_TEXT, true);
        view.setModeEnabled(true);
    }

    private void showConnectedStatus() {
        view.setStatus(remoteReady
                ? "상대가 준비했어요. 게임 방식을 고르고 게임 시작을 누르세요."
                : "상대가 접속했어요. 게임 방식을 고르고 게임 시작을 누르세요.", false);
    }

    private P2PGameRunner.ConnectListener hostListener() {
        return new P2PGameRunner.ConnectListener() {
            @Override
            public void onConnected(P2PGameSession connected) {
                if (!isDisplayable()) {
                    return;
                }
                hostHandle = null;
                session = connected;
                dotsTimer.stop();
                view.setPrimary(START_TEXT, true);
                showConnectedStatus();
            }

            @Override
            public void onRemoteReady(boolean ready) {
                if (!isDisplayable()) {
                    return;
                }
                remoteReady = ready;
                if (session != null && !localReady) {
                    showConnectedStatus();
                }
            }

            @Override
            public void onGameStarting() {
                // 세션은 게임 루프가 가져가므로 로비에서는 닫지 않는다
                left = true;
                session = null;
                dotsTimer.stop();
                dispose();
            }

            @Override
            public void onLobbyDisconnected(IOException exception) {
                if (!isDisplayable()) {
                    return;
                }
                resetRoom();
                view.setStatus("상대와의 연결이 끊어졌어요. 다시 방을 열어 주세요.", true);
            }

            @Override
            public void onFailure(IOException exception) {
                if (!isDisplayable()) {
                    return;
                }
                resetRoom();
                view.setStatus(P2PGameRunner.describeFailure(exception), true);
            }
        };
    }

    private void back() {
        if (hostHandle != null) {
            hostHandle.cancel();
            hostHandle = null;
        }
        if (session != null) {
            closeSession(session);
            session = null;
        }
        dotsTimer.stop();
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
