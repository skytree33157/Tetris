package menu.screens;

import java.util.concurrent.ExecutionException;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;

import menu.views.LobbyChatPanel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.Timer;

import menu.BattleModes;
import menu.P2PGameRunner;
import menu.views.P2PHostView;
import network.LocalAddresses;
import network.P2PGameSession;

// 온라인 방 만들기: 게임 방식 선택 → 방 열기 → 상대 접속 대기
public class P2PHostScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String WAITING_TEXT = "상대를 기다리는 중";
    private static final String OPEN_TEXT = "방 열기";
    private static final String START_TEXT = "게임 시작";
    private static final String CANCEL_TEXT = "취소";
    private static final String CANCEL_READY_TEXT = "준비 해제";

    private final P2PHostView view;
    private final LobbyChatPanel chatPanel;
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

        chatPanel = new LobbyChatPanel(
                P2PGameSession.MAX_CHAT_LENGTH,
                this::sendChat
        );

        view = new P2PHostView(BattleModes.labels(), P2PGameSession.DEFAULT_PORT,
                this::pickMode, this::togglePrimary, this::back);

        view.addChatPanel(chatPanel);
        view.setAddresses(LocalAddresses.siteLocalIPv4());
        view.setModeIndex(modeIndex);
        view.setModeDescription(BattleModes.description(modeIndex));
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
                    case KeyEvent.VK_LEFT -> pickMode(Math.floorMod(modeIndex - 1, BattleModes.count()));
                    case KeyEvent.VK_RIGHT -> pickMode(Math.floorMod(modeIndex + 1, BattleModes.count()));
                    case KeyEvent.VK_ENTER -> {
                        if (e.getModifiersEx() == 0) {
                            togglePrimary();
                            e.consume();
                        }
                    }
                    default -> { }
                }
            }
        });
        setFocusable(true);
        // 채팅 입력 중에도 Esc로 뒤로 가기
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");

        getRootPane().getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                back();
            }
        });

        // Ctrl + Enter로 기존 주 버튼 동작 실행
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(
                        KeyStroke.getKeyStroke(
                                KeyEvent.VK_ENTER,
                                KeyEvent.CTRL_DOWN_MASK
                        ),
                        "primary"
                );

        getRootPane().getActionMap().put("primary", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                togglePrimary();
            }
        });
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

    private void sendChat(String message) {
        P2PGameSession target = session;

        if (target == null || left || !isDisplayable()) {
            chatPanel.setChatEnabled(false);
            chatPanel.sendFailed("연결 후 대기 중에 전송할 수 있어요.");
            return;
        }

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                target.sendChat(message);
                return null;
            }

            @Override
            protected void done() {
                // 이전 연결의 전송 결과가 새 대기실에 반영되지 않게 한다.
                if (left || !isDisplayable() || session != target) {
                    return;
                }

                try {
                    get();
                    chatPanel.sendSucceeded(message);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    chatPanel.sendFailed("전송 결과를 확인하지 못했어요.");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    chatPanel.sendFailed(
                            cause == null ? null : cause.getMessage()
                    );
                }
            }
        }.execute();
    }

    private void pickMode(int index) {
        if (localReady || left) {
            return;
        }
        modeIndex = index;
        view.setModeIndex(index);
        view.setModeDescription(BattleModes.description(index));
        requestFocusInWindow();
    }

    // 방 열기/취소, 접속 후에는 준비/준비 해제를 전환한다.
    private void togglePrimary() {
        if (left) {
            return;
        }
        if (session != null) {
            if (localReady) {
                cancelReady();
            } else {
                startGame();
            }
        } else if (hostHandle != null) {
            cancelWaiting();
            view.setStatus("대기를 취소했어요.", false);
        } else {
            openRoom();
        }
        if (session != null && !left) {
            chatPanel.focusInput();
        } else {
            requestFocusInWindow();
        }
    }

    private void openRoom() {
        chatPanel.resetChat();
        try {
            hostHandle = P2PGameRunner.startServer(BattleModes.settings(modeIndex), hostListener());
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
            session.setMode(BattleModes.settings(modeIndex).getGameMode());
            session.setReady();
        } catch (IOException exception) {
            view.setStatus(P2PGameRunner.describeFailure(exception), true);
            return;
        }
        localReady = true;
        view.setModeEnabled(false);
        view.setPrimary(CANCEL_READY_TEXT, true);
        startDots();
    }

    private void cancelReady() {
        if (session == null || left) {
            return;
        }

        try {
            if (!session.cancelReady()) {
                // 이미 게임 시작이 확정됐다면 화면 상태를 바꾸지 않는다.
                return;
            }
        } catch (IOException exception) {
            view.setStatus(
                    P2PGameRunner.describeFailure(exception),
                    true
            );
            return;
        }

        localReady = false;
        dotsTimer.stop();

        view.setModeEnabled(true);
        view.setPrimary(START_TEXT, true);
        showConnectedStatus();
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
        chatPanel.resetChat();
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
                chatPanel.setChatEnabled(true);
                chatPanel.focusInput();
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
            public void onRemoteChat(String message) {
                if (!isDisplayable() || left) {
                    return;
                }

                chatPanel.appendRemoteMessage(message);
            }

            @Override
            public void onGameStarting() {
                // 세션은 게임 루프가 가져가므로 로비에서는 닫지 않는다
                left = true;
                chatPanel.setChatEnabled(false);
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
        if (left) {
            return;
        }
        left = true;
        chatPanel.setChatEnabled(false);
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
