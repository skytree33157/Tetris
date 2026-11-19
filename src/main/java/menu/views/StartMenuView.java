package menu.views;

import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;

public class StartMenuView extends JPanel {

    private static final int LOGO_SIZE = 112;   // 시작 화면 TFSE 글자 크기

    private final ThemedButton[] buttons;
    private final JLabel hint = new JLabel("↑ ↓ 선택   Enter 확인");

    public StartMenuView(String[] items, IntConsumer onSelect) {
        this(null, "소프트웨어공학 4팀 프로젝트", true, items, 3, onSelect);
    }

    /**
     * 메뉴 화면 공용 레이아웃. 대전 메뉴 등에서도 재사용한다.
     *
     * @param titleText    제목 글자. null이면 생략 (시작 화면은 TFSE 로고 글자가 제목 역할)
     * @param primaryCount 앞에서부터 Primary 버튼으로 그릴 항목 수
     */
    public StartMenuView(String titleText, String subtitleText, boolean showLogo,
                         String[] items, int primaryCount, IntConsumer onSelect) {
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(960, 720));
        setLayout(new GridBagLayout());

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        if (showLogo) {
            // 시작 화면 제목: 블록 없이 포인트 색 글자
            JLabel logo = new JLabel("TFSE");
            logo.setFont(Theme.display(LOGO_SIZE));
            logo.setForeground(Theme.ACCENT);
            col.add(centered(logo));
            col.add(Box.createVerticalStrut(6));
        }

        if (titleText != null) {
            JLabel title = new JLabel(titleText);
            title.setFont(Theme.display(46));
            title.setForeground(Theme.TEXT);
            col.add(centered(title));
        }

        JLabel sub = new JLabel(subtitleText);
        sub.setFont(Theme.body(15, Font.PLAIN));
        sub.setForeground(Theme.TEXT_MUTED);
        col.add(centered(sub));
        col.add(Box.createVerticalStrut(40));

        // 앞의 primaryCount개 항목은 Primary, 나머지는 Secondary
        buttons = new ThemedButton[items.length];
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            boolean primary = i < primaryCount;
            ThemedButton b = new ThemedButton(items[i], primary ? Kind.PRIMARY : Kind.SECONDARY);
            b.setFocusable(false);   // 키 입력은 StartMenuScreen(JFrame)가 받도록
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setFont(Theme.body(primary ? 16 : 15, primary ? Font.BOLD : Font.PLAIN));
            Dimension d = new Dimension(192, 42);
            b.setPreferredSize(d);
            b.setMaximumSize(d);
            b.addActionListener(e -> onSelect.accept(index));
            buttons[i] = b;
            if (i > 0) col.add(Box.createVerticalStrut(5));
            col.add(b);
        }

        col.add(Box.createVerticalStrut(24));
        hint.setFont(Theme.body(13, Font.PLAIN));
        hint.setForeground(Theme.TEXT_MUTED);
        col.add(centered(hint));

        add(col);
    }

    public void setHint(String text) {
        hint.setText(text);
    }

    public void setItemEnabled(int index, boolean enabled) {
        buttons[index].setEnabled(enabled);
    }

    public void setSelectedIndex(int index) {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelected(i == index);
        }
        repaint();
    }

    private static JComponent centered(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

}
