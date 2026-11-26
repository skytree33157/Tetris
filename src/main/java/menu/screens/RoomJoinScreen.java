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

import menu.RoomGameController;
import menu.RoomGameRunner;
import menu.views.P2PJoinView;
import network.RecentPeerStore;
import network.RoomMember;

// 온라인 참여하기: IP 입력(또는 최근 접속 선택) → 접속 → 방장이 게임을 시작할 때까지 대기
public class RoomJoinScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private final P2PJoinView view;
    private final RoomGameController controller;
    private final RecentPeerStore recentPeers = new RecentPeerStore();
    private boolean connecting;
    private boolean joined;
    private boolean ready;
    private boolean left;

    public RoomJoinScreen() {
        super("TFSE - 온라인 참여");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        controller = new RoomGameController(new RoomGameController.LobbyListener() {
            @Override
            public void onRoster(List<RoomMember> members, int mySlot) {
                if (!isDisplayable()) {
                    return;
                }
                connecting = false;
                joined = true;
                view.setJoined(ready);
                view.setRoster(members, mySlot);
                view.setStatus(ready
                        ? "준비 완료했어요. 방장이 게임을 시작할 때까지 기다려 주세요."
                        : "방에 접속했어요. 준비되면 준비 버튼을 눌러 주세요.", false);
            }

            @Override
            public void onGameStarting() {
                left = true;
                dispose();
            }

            @Override
            public void onFailure(String message) {
                if (!isDisplayable()) {
                    return;
                }
                resetConnecting(message);
            }

            @Override
            public void onLobbyDisconnected(String message) {
                if (!isDisplayable()) {
                    return;
                }
                joined = false;
                ready = false;
                view.setRoster(List.of(), -1);
                resetConnecting("방장과의 연결이 끊어졌어요. 다시 접속해 주세요.");
            }
        });

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
    }

    private static List<String> loadRecent() {
        try {
            return new RecentPeerStore().load();
        } catch (IOException e) {
            return List.of();
        }
    }

    private void pickRecent(String address) {
        if (!connecting && !joined) {
            view.setAddress(address);
        }
    }

    private void connect() {
        if (connecting || left) {
            return;
        }
        if (joined) {
            markReady();
            return;
        }
        String address = view.getAddress();
        if (address.isEmpty()) {
            view.setStatus("주소를 입력해 주세요.", true);
            return;
        }
        connecting = true;
        view.setConnecting(true);
        view.setStatus("연결 중...", false);
        RoomGameRunner.join(address, RoomGameRunner.defaultPlayerName(), controller, recentPeers);
    }

    // 준비 버튼: 서버에 준비 완료를 알리고, 모두 준비되면 방장이 시작한다
    private void markReady() {
        if (ready) {
            return;
        }
        ready = true;
        controller.sendReady();
        view.setJoined(true);
        view.setStatus("준비 완료했어요. 방장이 게임을 시작할 때까지 기다려 주세요.", false);
    }

    private void resetConnecting(String message) {
        connecting = false;
        joined = false;
        ready = false;
        view.setConnecting(false);
        view.setStatus(message, true);
        view.getAddressField().requestFocusInWindow();
    }

    private void back() {
        // 연결 시도 중에는 결과를 기다려야 하므로 창을 닫지 않는다
        if (connecting || left) {
            return;
        }
        dispose();
        new BattleMenuScreen().setVisible(true);
    }
}
