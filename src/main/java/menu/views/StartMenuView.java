package menu.views;

import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntConsumer;

public class StartMenuView extends JPanel {

    private final ThemedButton[] buttons;

    public StartMenuView(String[] items, IntConsumer onSelect) {
        setBackground(Theme.BG);
        setPreferredSize(new Dimension(960, 720));
        setLayout(new GridBagLayout());

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(centered(new LogoPanel()));
        col.add(Box.createVerticalStrut(20));

        JLabel title = new JLabel("TETRIS");
        title.setFont(Theme.display(46));
        title.setForeground(Theme.TEXT);
        col.add(centered(title));

        JLabel sub = new JLabel("소프트웨어공학 4팀 프로젝트");
        sub.setFont(Theme.body(15, Font.PLAIN));
        sub.setForeground(Theme.TEXT_MUTED);
        col.add(centered(sub));
        col.add(Box.createVerticalStrut(32));

        // 앞의 두 항목(일반/아이템 모드)은 Primary, 나머지는 Secondary
        buttons = new ThemedButton[items.length];
        for (int i = 0; i < items.length; i++) {
            final int index = i;
            boolean primary = i < 2;
            ThemedButton b = new ThemedButton(items[i], primary ? Kind.PRIMARY : Kind.SECONDARY);
            b.setFocusable(false);   // 키 입력은 StartMenuScreen(JFrame)가 받도록
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            Dimension d = new Dimension(300, primary ? 54 : 50);
            b.setPreferredSize(d);
            b.setMaximumSize(d);
            b.addActionListener(e -> onSelect.accept(index));
            buttons[i] = b;
            if (i > 0) col.add(Box.createVerticalStrut(12));
            col.add(b);
        }

        col.add(Box.createVerticalStrut(28));
        JLabel hint = new JLabel("↑ ↓ 선택   Enter 확인");
        hint.setFont(Theme.body(13, Font.PLAIN));
        hint.setForeground(Theme.TEXT_MUTED);
        col.add(centered(hint));

        add(col);
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

    /** T / F / S / E 2x2 로고 */
    private static class LogoPanel extends JPanel {
        private static final Color[] COLORS = {
            new Color(0x1E3C8C), new Color(0x3264C8), new Color(0x5096E6), new Color(0x82BEF5)
        };
        private static final String[] LETTERS = {"T", "F", "S", "E"};
        private static final int CELL = 76, GAP = 6;

        LogoPanel() {
            setOpaque(false);
            Dimension d = new Dimension(CELL * 2 + GAP, CELL * 2 + GAP);
            setPreferredSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(Theme.display(34));
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i < 4; i++) {
                int x = (i % 2) * (CELL + GAP), y = (i / 2) * (CELL + GAP);
                g2.setColor(COLORS[i]);
                g2.fillRect(x, y, CELL, CELL);
                g2.setColor(new Color(255, 255, 255, 72));   // 좌상 하이라이트
                g2.fillRect(x, y, CELL, 5);
                g2.fillRect(x, y, 5, CELL);
                g2.setColor(new Color(0, 0, 0, 77));         // 우하 그림자
                g2.fillRect(x, y + CELL - 5, CELL, 5);
                g2.fillRect(x + CELL - 5, y, 5, CELL);
                g2.setColor(i == 3 ? Theme.BG : Theme.TEXT);
                g2.drawString(LETTERS[i],
                        x + (CELL - fm.stringWidth(LETTERS[i])) / 2,
                        y + (CELL + fm.getAscent() - fm.getDescent()) / 2);
            }
            g2.dispose();
        }
    }
}
