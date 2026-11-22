package menu.screens;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;

import game.GameMode;
import menu.RoomGameController;
import menu.RoomGameRunner;
import menu.views.P2PHostView;
import network.LocalAddresses;
import network.RoomMember;
import network.RoomProtocol;

// 온라인 방 만들기: 게임 방식 선택 → 방 열기 → 참가자 모이면 게임 시작 → 모두 준비하면 대전 시작
public class RoomHostScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final GameMode[] MODES = {GameMode.NORMAL, GameMode.ITEM};
    private static final String[] MODE_LABELS = {"일반", "아이템"};
    private static final int MIN_PLAYERS_TO_START = 2;
    private static final String OPEN_TEXT = "방 열기";
    private static final String START_TEXT = "게임 시작";
    private static final String WAITING_START_TEXT = "준비 대기 중";

    private final P2PHostView view;
    private final RoomGameController controller;
    private RoomGameRunner.HostHandle hostHandle;
    private int modeIndex;
    private int playerCount;
    // 게임 시작을 눌러 모드가 확정되었는지 (확정 후에는 모드 변경과 재시작을 막는다)
    private boolean startRequested;
    // 게임 화면으로 넘어가 이 창을 닫은 뒤인지
    private boolean left;

    public RoomHostScreen() {
        super("TFSE - 방 만들기");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        controller = new RoomGameController(new RoomGameController.LobbyListener() {
            @Override
            public void onRoster(List<RoomMember> members, int mySlot) {
                if (!isDisplayable()) {
                    return;
                }
                playerCount = members.size();
                view.setRoster(members, mySlot);
                if (startRequested) {
                    return;
                }
                view.setPrimary(START_TEXT, playerCount >= MIN_PLAYERS_TO_START);
                view.setStatus(playerCount >= MIN_PLAYERS_TO_START
                        ? "참가자가 모였어요. 게임 시작을 누르세요."
                        : "상대가 접속하기를 기다리는 중이에요.", false);
            }

            @Override
            public void onGameStarting() {
                left = true;
                dispose();
            }

            @Override
            public void onFailure(String message) {
                resetRoom();
                view.setStatus(message, true);
            }

            @Override
            public void onLobbyDisconnected(String message) {
                resetRoom();
                view.setStatus("연결이 끊어졌어요. 다시 방을 열어 주세요.", true);
            }
        });

        view = new P2PHostView(MODE_LABELS, RoomProtocol.DEFAULT_PORT,
                this::pickMode, this::togglePrimary, this::back);
        view.setAddresses(LocalAddresses.siteLocalIPv4());
        view.setModeIndex(modeIndex);
        view.setPrimary(OPEN_TEXT, true);
        setContentPane(view);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

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
        if (left || startRequested) {
            return;
        }
        modeIndex = index;
        view.setModeIndex(index);
        requestFocusInWindow();
    }

    // Enter / 버튼: 방이 없으면 방 열기, 방이 있으면 게임 시작
    private void togglePrimary() {
        if (left) {
            return;
        }
        if (hostHandle == null) {
            openRoom();
        } else {
            startGame();
        }
        requestFocusInWindow();
    }

    private void openRoom() {
        try {
            hostHandle = RoomGameRunner.openRoom(RoomGameRunner.defaultPlayerName(), controller);
        } catch (IOException exception) {
            view.setStatus(RoomGameRunner.describeFailure(exception), true);
            return;
        }
        view.setPrimary(START_TEXT, false);
        view.setStatus("상대가 접속하기를 기다리는 중이에요.", false);
    }

    // 게임 시작을 누르면 모드가 확정되고, 모든 참가자가 준비될 때까지 대기한다
    private void startGame() {
        if (startRequested || playerCount < MIN_PLAYERS_TO_START) {
            return;
        }
        if (!hostHandle.startGame(MODES[modeIndex])) {
            view.setStatus("게임을 시작할 수 없어요. 다시 시도해 주세요.", true);
            return;
        }
        startRequested = true;
        view.setModeEnabled(false);
        view.setPrimary(WAITING_START_TEXT, false);
        view.setStatus("모든 참가자가 준비 완료하면 게임이 시작돼요.", false);
    }

    // 방을 닫고 처음 상태(방 열기)로 되돌린다
    private void resetRoom() {
        if (hostHandle != null) {
            hostHandle.cancel();
            hostHandle = null;
        }
        playerCount = 0;
        startRequested = false;
        view.setModeEnabled(true);
        view.setPrimary(OPEN_TEXT, true);
    }

    private void back() {
        if (left) {
            return;
        }
        if (hostHandle != null) {
            hostHandle.cancel();
            hostHandle = null;
        }
        dispose();
        new BattleMenuScreen().setVisible(true);
    }
}
