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

    private final P2PHostView view;
    private final Timer dotsTimer;
    private int modeIndex;
    private int dots;
    private P2PGameRunner.HostHandle hostHandle;

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

    private boolean isWaiting() {
        return hostHandle != null;
    }

    private void pickMode(int index) {
        if (isWaiting()) {
            return;
        }
        modeIndex = index;
        view.setModeIndex(index);
        requestFocusInWindow();
    }

    private void togglePrimary() {
        if (isWaiting()) {
            cancelWaiting();
            view.setStatus("대기를 취소했어요.", false);
        } else {
            openRoom();
        }
        requestFocusInWindow();
    }

    private void openRoom() {
        try {
            hostHandle = P2PGameRunner.startServer(MODES[modeIndex], new P2PGameRunner.ConnectListener() {
                @Override
                public void onConnected() {
                    dotsTimer.stop();
                    hostHandle = null;
                    dispose();
                }

                @Override
                public void onFailure(IOException exception) {
                    stopWaiting();
                    view.setStatus(P2PGameRunner.describeFailure(exception), true);
                }
            });
        } catch (IOException exception) {
            view.setStatus(P2PGameRunner.describeFailure(exception), true);
            return;
        }
        view.setWaiting(true);
        dots = 0;
        view.setStatus(WAITING_TEXT + "...", false);
        dotsTimer.start();
    }

    private void cancelWaiting() {
        if (hostHandle != null) {
            hostHandle.cancel();
        }
        stopWaiting();
    }

    private void stopWaiting() {
        hostHandle = null;
        dotsTimer.stop();
        view.setWaiting(false);
    }

    private void back() {
        cancelWaiting();
        dispose();
        new BattleMenuScreen().setVisible(true);
    }
}
