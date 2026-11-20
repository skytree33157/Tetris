package menu.views;

import static menu.views.LobbyParts.COLUMN_WIDTH;
import static menu.views.LobbyParts.card;
import static menu.views.LobbyParts.fixHeight;
import static menu.views.LobbyParts.label;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ui.components.RoundedPanel;
import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 온라인 참여하기 화면 레이아웃. 동작은 P2PJoinScreen이 담당
public class P2PJoinView extends JPanel {

    private static final String CONNECT_TEXT = "접속";
    private static final String START_TEXT = "게임 시작";
    private static final String WAITING_TEXT = "대기 중";

    private final JTextField addressField = new JTextField();
    private final JPanel recentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final List<ThemedButton> recentButtons = new ArrayList<>();
    private final JLabel status = label(" ", Theme.body(14, Font.PLAIN), Theme.TEXT_SOFT);
    private final ThemedButton connectButton = new ThemedButton("접속", Kind.PRIMARY);
    private final ThemedButton backButton = new ThemedButton("뒤로", Kind.SECONDARY);
    private final Consumer<String> onPickRecent;
    private final Consumer<String> onConnectRecent;

    /**
     * @param onPickRecent    최근 주소를 한 번 눌렀을 때 (입력칸 채우기)
     * @param onConnectRecent 최근 주소를 두 번 눌렀을 때 (바로 접속)
     */
    public P2PJoinView(Runnable onConnect, Runnable onBack,
                       Consumer<String> onPickRecent, Consumer<String> onConnectRecent) {
        this.onPickRecent = onPickRecent;
        this.onConnectRecent = onConnectRecent;
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(960, 720));
        setLayout(new GridBagLayout());

        JPanel col = LobbyParts.column();
        col.add(label("온라인 참여하기", Theme.body(28, Font.BOLD), Theme.TEXT));
        col.add(label("방을 만든 사람의 화면에 나온 주소를 입력하세요", Theme.body(14, Font.PLAIN), Theme.TEXT_MUTED));
        col.add(Box.createVerticalStrut(20));

        // 주소 입력칸 (게임 보드처럼 검은 칸)
        RoundedPanel inputCard = card();
        inputCard.add(label("상대 IP 주소", Theme.body(15, Font.BOLD), Theme.TEXT));
        inputCard.add(Box.createVerticalStrut(10));
        addressField.setFont(Theme.display(26));
        addressField.setForeground(Theme.TEXT);
        addressField.setCaretColor(Theme.TEXT);
        addressField.setBackground(Theme.BOARD_BG);
        addressField.setSelectionColor(Theme.ACCENT);
        addressField.setSelectedTextColor(Theme.ON_ACCENT);
        addressField.setDisabledTextColor(Theme.TEXT_MUTED);
        addressField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 2),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        addressField.setAlignmentX(Component.LEFT_ALIGNMENT);
        addressField.setMaximumSize(new Dimension(COLUMN_WIDTH, addressField.getPreferredSize().height));
        addressField.addActionListener(e -> onConnect.run());   // Enter
        inputCard.add(addressField);
        inputCard.add(Box.createVerticalStrut(8));
        inputCard.add(label("예: 192.168.0.12  ·  같은 공유기(와이파이)에 연결돼 있어야 해요",
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        col.add(fixHeight(inputCard));
        col.add(Box.createVerticalStrut(14));

        // 최근 접속
        RoundedPanel recentCard = card();
        recentCard.add(label("최근 접속", Theme.body(15, Font.BOLD), Theme.TEXT));
        recentCard.add(Box.createVerticalStrut(10));
        recentRow.setOpaque(false);
        recentRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        recentCard.add(recentRow);
        recentCard.add(Box.createVerticalStrut(8));
        recentCard.add(label("한 번 누르면 입력칸에 채워지고, 두 번 누르면 바로 접속해요.",
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        col.add(recentCard);
        col.add(Box.createVerticalStrut(16));

        col.add(status);
        col.add(Box.createVerticalStrut(12));
        connectButton.addActionListener(e -> onConnect.run());
        backButton.addActionListener(e -> onBack.run());
        col.add(LobbyParts.buttonRow(connectButton, backButton));
        col.add(Box.createVerticalStrut(16));
        col.add(label("Enter 접속/게임 시작   Esc 뒤로", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));

        for (Component c : col.getComponents()) {
            if (c instanceof JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        col.setPreferredSize(new Dimension(COLUMN_WIDTH, col.getPreferredSize().height + 40));
        add(col);
    }

    public JTextField getAddressField() {
        return addressField;
    }

    public String getAddress() {
        return addressField.getText().trim();
    }

    public void setAddress(String address) {
        addressField.setText(address);
        addressField.requestFocusInWindow();
        addressField.selectAll();
    }

    public void setRecentAddresses(List<String> addresses) {
        recentRow.removeAll();
        recentButtons.clear();
        if (addresses.isEmpty()) {
            recentRow.add(label("아직 접속 기록이 없어요", Theme.body(14, Font.PLAIN), Theme.TEXT_MUTED));
        }
        for (String address : addresses) {
            ThemedButton b = new ThemedButton(address, Kind.SECONDARY);
            b.setFont(Theme.display(13));
            b.setFocusable(false);
            b.setPreferredSize(new Dimension(Math.max(120, b.getPreferredSize().width + 24), 36));
            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!b.isEnabled()) {
                        return;
                    }
                    if (e.getClickCount() >= 2) {
                        onConnectRecent.accept(address);
                    } else {
                        onPickRecent.accept(address);
                    }
                }
            });
            recentButtons.add(b);
            recentRow.add(b);
        }
        revalidate();
        repaint();
    }

    // 연결 시도 중에는 입력과 버튼을 막는다
    public void setConnecting(boolean connecting) {
        addressField.setEnabled(!connecting);
        connectButton.setText(CONNECT_TEXT);
        connectButton.setEnabled(!connecting);
        setRecentEnabled(!connecting);
        repaint();
    }

    // 방에 접속한 뒤: waiting=false면 "게임 시작" 버튼, waiting=true면 게임 시작 요청 후 "대기 중"
    public void setLobby(boolean waiting) {
        addressField.setEnabled(false);
        setRecentEnabled(false);
        connectButton.setText(waiting ? WAITING_TEXT : START_TEXT);
        connectButton.setEnabled(!waiting);
        repaint();
    }

    private void setRecentEnabled(boolean enabled) {
        for (ThemedButton b : recentButtons) {
            b.setEnabled(enabled);
        }
    }

    public void setStatus(String text, boolean warning) {
        LobbyParts.setStatus(status, text, warning);
    }
}
