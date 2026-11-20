package menu.views;

import static menu.views.LobbyParts.COLUMN_WIDTH;
import static menu.views.LobbyParts.card;
import static menu.views.LobbyParts.fixHeight;
import static menu.views.LobbyParts.label;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ui.components.RoundedPanel;
import ui.components.SegmentedControl;
import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 온라인 방 만들기 화면 레이아웃. 동작은 P2PHostScreen이 담당
public class P2PHostView extends JPanel {

    private final SegmentedControl modeControl;
    private final JPanel addressList = new JPanel();
    private final JLabel status = label(" ", Theme.body(14, Font.PLAIN), Theme.TEXT_SOFT);
    private final ThemedButton primaryButton = new ThemedButton("방 열기", Kind.PRIMARY);
    private final ThemedButton backButton = new ThemedButton("뒤로", Kind.SECONDARY);

    public P2PHostView(String[] modeLabels, int port, IntConsumer onModePick,
                       Runnable onPrimary, Runnable onBack) {
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(960, 720));
        setLayout(new GridBagLayout());

        JPanel col = LobbyParts.column();

        col.add(label("방 만들기", Theme.body(28, Font.BOLD), Theme.TEXT));
        col.add(label("방을 열고 상대에게 아래 주소를 알려 주세요", Theme.body(14, Font.PLAIN), Theme.TEXT_MUTED));
        col.add(Box.createVerticalStrut(20));

        // 게임 방식
        RoundedPanel modeCard = card();
        modeCard.add(label("게임 방식", Theme.body(15, Font.BOLD), Theme.TEXT));
        modeCard.add(Box.createVerticalStrut(10));
        modeControl = new SegmentedControl(modeLabels, onModePick);
        modeControl.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeCard.add(modeControl);
        col.add(fixHeight(modeCard));
        col.add(Box.createVerticalStrut(14));

        // 내 주소 (게임 보드처럼 검은 상자)
        RoundedPanel addressCard = card();
        addressCard.add(label("내 주소", Theme.body(15, Font.BOLD), Theme.TEXT));
        addressCard.add(Box.createVerticalStrut(10));
        RoundedPanel lcd = new RoundedPanel(Theme.BOARD_BG, Theme.OUTLINE, Theme.RADIUS);
        lcd.setLayout(new BoxLayout(lcd, BoxLayout.Y_AXIS));
        lcd.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        lcd.setAlignmentX(Component.LEFT_ALIGNMENT);
        addressList.setOpaque(false);
        addressList.setLayout(new BoxLayout(addressList, BoxLayout.Y_AXIS));
        addressList.setAlignmentX(Component.LEFT_ALIGNMENT);
        lcd.add(addressList);
        lcd.add(Box.createVerticalStrut(6));
        lcd.add(label("PORT " + port, Theme.display(14), Theme.TEXT_MUTED));
        addressCard.add(lcd);
        addressCard.add(Box.createVerticalStrut(10));
        addressCard.add(label("상대는 '온라인 참여하기'에서 이 주소를 입력하면 돼요.",
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        addressCard.add(label("처음 방을 열 때 Windows 방화벽 허용 창이 뜨면 '허용'을 눌러 주세요.",
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        col.add(addressCard);
        col.add(Box.createVerticalStrut(16));

        col.add(status);
        col.add(Box.createVerticalStrut(12));

        primaryButton.addActionListener(e -> onPrimary.run());
        backButton.addActionListener(e -> onBack.run());
        col.add(LobbyParts.buttonRow(primaryButton, backButton));
        col.add(Box.createVerticalStrut(16));
        col.add(label("← → 방식 선택   Enter 방 열기/게임 시작/취소   Esc 뒤로", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));

        for (Component c : col.getComponents()) {
            if (c instanceof javax.swing.JComponent jc) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
            }
        }
        col.setPreferredSize(new Dimension(COLUMN_WIDTH, col.getPreferredSize().height));
        add(col);
    }

    public void setAddresses(List<String> addresses) {
        addressList.removeAll();
        if (addresses.isEmpty()) {
            addressList.add(label("IP를 찾지 못했어요", Theme.body(18, Font.BOLD), Theme.TEXT));
            addressList.add(label("네트워크 연결을 확인해 주세요", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        } else {
            // 첫 번째(실제 랜카드일 가능성이 높은) 주소를 크게, 나머지는 작게
            addressList.add(label(addresses.get(0), Theme.display(30), Theme.TEXT));
            for (int i = 1; i < Math.min(addresses.size(), 3); i++) {
                addressList.add(label(addresses.get(i), Theme.display(16), Theme.TEXT_MUTED));
            }
        }
        revalidate();
        repaint();
    }

    public void setModeIndex(int index) {
        modeControl.setSelectedIndex(index);
    }

    public void setModeEnabled(boolean enabled) {
        modeControl.setEnabled(enabled);
    }

    public void setPrimary(String text, boolean enabled) {
        primaryButton.setText(text);
        primaryButton.setEnabled(enabled);
        repaint();
    }

    public void setStatus(String text, boolean warning) {
        LobbyParts.setStatus(status, text, warning);
    }
}
