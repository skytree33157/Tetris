package menu.screens;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ui.components.RoundedPanel;
import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 일시정지 화면: 게임 창 전체를 어둡게 덮고 가운데에 메뉴 카드를 띄움
public class PauseScreen extends JPanel {

    private static final String[] MENU_ITEMS = {
        "계속하기",
        "다시 시작",
        "설정",
        "시작 화면",
        "게임 종료"
    };
    private static final Color DIM = new Color(5, 10, 25, 200);

    private final Runnable[] actions;
    private final ThemedButton[] buttons = new ThemedButton[MENU_ITEMS.length];
    private int selectedIndex;

    public PauseScreen(Runnable onResume, Runnable onRestart, Runnable onSettings,
                       Runnable onQuit, Runnable onExit) {
        actions = new Runnable[] {onResume, onRestart, onSettings, onQuit, onExit};

        setOpaque(false);
        setLayout(new GridBagLayout());
        setFocusable(true);

        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.OUTLINE, 16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        card.add(centered(new PauseIcon()));
        card.add(Box.createVerticalStrut(6));
        card.add(centered(label("일시정지", Theme.body(28, Font.BOLD), Theme.TEXT)));
        card.add(Box.createVerticalStrut(4));
        card.add(centered(label("P를 다시 누르면 계속합니다", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED)));
        card.add(Box.createVerticalStrut(18));

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            final int index = i;
            boolean primary = i == 0;
            ThemedButton b = new ThemedButton(MENU_ITEMS[i], primary ? Kind.PRIMARY : Kind.SECONDARY);
            b.setFocusable(false);   // 키 입력은 PauseScreen이 받도록
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            Dimension d = new Dimension(296, primary ? 50 : 46);
            b.setPreferredSize(d);
            b.setMaximumSize(d);
            b.addActionListener(e -> actions[index].run());
            buttons[i] = b;
            if (i > 0) card.add(Box.createVerticalStrut(10));
            card.add(b);
        }
        add(card);

        // 카드 바깥 클릭이 아래 게임 화면으로 전달되지 않도록 막음
        addMouseListener(new MouseAdapter() { });
        addKeyListener(new MenuKeyListener());
        updateSelection();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(DIM);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    private void updateSelection() {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelected(i == selectedIndex);
        }
        repaint();
    }

    private static JComponent centered(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private class MenuKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_UP -> {
                    selectedIndex = (selectedIndex - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
                    updateSelection();
                }
                case KeyEvent.VK_DOWN -> {
                    selectedIndex = (selectedIndex + 1) % MENU_ITEMS.length;
                    updateSelection();
                }
                case KeyEvent.VK_ENTER -> actions[selectedIndex].run();
                default -> { }
            }
        }
    }

    /** 일시정지 아이콘 (세로 막대 두 개) */
    private static class PauseIcon extends JComponent {
        private static final long serialVersionUID = 1L;

        PauseIcon() {
            Dimension d = new Dimension(36, 36);
            setPreferredSize(d);
            setMaximumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(9, 6, 5, 24, 4, 4);
            g2.fillRoundRect(22, 6, 5, 24, 4, 4);
            g2.dispose();
        }
    }
}
