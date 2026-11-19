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
import network.RecentPeerStore;

// 온라인 참여하기: IP 입력(또는 최근 접속 선택) → 접속
public class P2PJoinScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String CONNECTING_TEXT = "연결 중";

    private final P2PJoinView view;
    private final Timer dotsTimer;
    private boolean connecting;
    private int dots;

    public P2PJoinScreen() {
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
            view.setStatus(CONNECTING_TEXT + ".".repeat(dots), false);
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
        dots = 0;
        view.setStatus(CONNECTING_TEXT + "...", false);
        dotsTimer.start();

        P2PGameRunner.joinServer(address, new P2PGameRunner.ConnectListener() {
            @Override
            public void onConnected() {
                dotsTimer.stop();
                dispose();
            }

            @Override
            public void onFailure(IOException exception) {
                dotsTimer.stop();
                connecting = false;
                view.setConnecting(false);
                view.setStatus(P2PGameRunner.describeFailure(exception), true);
                view.getAddressField().requestFocusInWindow();
            }
        });
    }

    private void back() {
        // 연결 시도 중이면 최대 5초 안에 끝나므로 기다리게 한다 (중간에 창을 닫으면 결과를 받을 곳이 없음)
        if (connecting) {
            return;
        }
        dispose();
        new BattleMenuScreen().setVisible(true);
    }
}
