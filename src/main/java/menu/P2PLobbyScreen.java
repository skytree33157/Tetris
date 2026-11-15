package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import network.P2PMatchSession;

// 임시 UI

// 네트워크 대전 대기실: 참가자 목록을 보여주고, 호스트가 대전을 시작
public final class P2PLobbyScreen extends JFrame {
    private static final long serialVersionUID = 1L;

    private final boolean host;
    private final JLabel countLabel;
    private final JLabel rosterLabel;
    private final JButton startButton;

    public P2PLobbyScreen(boolean host, Runnable onStart, Runnable onExit) {
        super("TFSE - 대기실");
        ui.AppIcon.apply(this);
        this.host = host;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(Color.BLACK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        countLabel = createLabel("참가자 0 / " + P2PMatchSession.MAX_PLAYERS, 20);
        rosterLabel = createLabel("", 16);
        rosterLabel.setVerticalAlignment(SwingConstants.TOP);

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setBackground(Color.BLACK);
        startButton = new JButton("대전 시작");
        startButton.setEnabled(false);
        startButton.addActionListener(e -> onStart.run());
        JButton exitButton = new JButton("나가기");
        exitButton.addActionListener(e -> onExit.run());
        buttons.add(host ? startButton : new JLabel(""));
        buttons.add(exitButton);

        content.add(countLabel, BorderLayout.NORTH);
        content.add(rosterLabel, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
        setSize(360, 320);
        setLocationRelativeTo(null);
    }

    // 참가자 목록 갱신 (슬롯 번호 기준)
    public void updateRoster(List<Integer> slots, int mySlot) {
        countLabel.setText("참가자 " + slots.size() + " / " + P2PMatchSession.MAX_PLAYERS);

        StringBuilder text = new StringBuilder("<html>");
        for (int slot : slots) {
            text.append("P").append(slot + 1);
            if (slot == mySlot) {
                text.append(" (나)");
            }
            text.append("<br>");
        }
        text.append("</html>");
        rosterLabel.setText(text.toString());

        startButton.setEnabled(host && slots.size() >= P2PMatchSession.MIN_PLAYERS_TO_START);
    }

    private JLabel createLabel(String text, int size) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Courier", Font.BOLD, size));
        return label;
    }
}
