package menu.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import game.RemoteBoardPanel;
import menu.BattleResult;
import menu.screens.GameHud;
import ui.components.RoundedPanel;
import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 대전 결과 화면 레이아웃: 승패 제목 → 양쪽 카드(마지막 보드 + 점수) → 이동 버튼. 동작은 BattleResultScreen
public class BattleResultView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ThemedButton[] buttons;

    public BattleResultView(BattleResult result, String[] menuItems, IntConsumer onMenu) {
        super(new BorderLayout(0, 22));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(28, 48, 28, 48));
        setPreferredSize(new Dimension(960, 720));

        add(createHeader(result), BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 24, 0));
        cards.setOpaque(false);
        cards.add(playerCard(result.left(), result.leftWon()));
        cards.add(playerCard(result.right(), result.rightWon()));
        add(cards, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonRow.setOpaque(false);
        buttons = new ThemedButton[menuItems.length];
        for (int i = 0; i < menuItems.length; i++) {
            final int index = i;
            boolean primary = i == 0;
            ThemedButton b = new ThemedButton(menuItems[i], primary ? Kind.PRIMARY : Kind.SECONDARY);
            b.setFocusable(false);   // 키 입력은 BattleResultScreen이 받도록
            b.setFont(Theme.body(16, primary ? Font.BOLD : Font.PLAIN));
            b.setPreferredSize(new Dimension(192, 42));
            b.addActionListener(e -> onMenu.accept(index));
            buttons[i] = b;
            buttonRow.add(b);
        }
        footer.add(buttonRow);
        footer.add(Box.createVerticalStrut(12));
        footer.add(centered(label("← → 선택   Enter 확인   Esc 대전 메뉴",
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED)));
        add(footer, BorderLayout.SOUTH);
    }

    public void setSelectedIndex(int index) {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setSelected(i == index);
        }
        repaint();
    }

    private static JComponent createHeader(BattleResult result) {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        Color headlineColor = result.outcome() == BattleResult.Outcome.DRAW ? Theme.TEXT
                : (result.personal() && !result.leftWon()) ? Theme.TEXT_MUTED : Theme.ACCENT;
        header.add(centered(label(result.headline(), Theme.body(44, Font.BOLD), headlineColor)));
        header.add(Box.createVerticalStrut(6));
        header.add(centered(label(result.reason(), Theme.body(15, Font.PLAIN), Theme.TEXT_SOFT)));
        header.add(Box.createVerticalStrut(4));
        header.add(centered(label(result.modeLabel() + " · " + GameHud.formatTime(result.elapsedMs()),
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED)));
        return header;
    }

    // 플레이어 한 명의 카드: 이름(+WIN 배지), 마지막 보드, 점수·레벨·줄
    private static JComponent playerCard(BattleResult.Side side, boolean winner) {
        RoundedPanel card = new RoundedPanel(Theme.CARD, winner ? Theme.ACCENT : Theme.CARD_BORDER, Theme.RADIUS);
        card.setLayout(new BorderLayout(18, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(winner ? Theme.ACCENT : Theme.CARD_BORDER, winner ? 2 : 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        // 마지막 보드 (진 쪽은 패널의 GAME OVER 덮개가 그대로 나온다)
        RemoteBoardPanel board = new RemoteBoardPanel();
        board.setStatus(side.toStatus());
        JPanel boardWrap = new JPanel(new BorderLayout());
        boardWrap.setBackground(Theme.BOARD_BG);
        boardWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        boardWrap.add(board);
        JPanel boardHolder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boardHolder.setOpaque(false);
        boardHolder.add(boardWrap);
        card.add(boardHolder, BorderLayout.WEST);

        JPanel stats = new JPanel();
        stats.setOpaque(false);
        stats.setLayout(new BoxLayout(stats, BoxLayout.Y_AXIS));

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameRow.add(label(side.name(), Theme.body(20, Font.BOLD), Theme.TEXT));
        if (winner) {
            JLabel badge = label("WIN", Theme.body(12, Font.BOLD), Theme.ON_ACCENT);
            badge.setOpaque(true);
            badge.setBackground(Theme.ACCENT);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            nameRow.add(badge);
        }
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, nameRow.getPreferredSize().height));
        stats.add(nameRow);
        stats.add(Box.createVerticalStrut(18));

        stats.add(left(label("점수", Theme.body(12, Font.PLAIN), Theme.TEXT_MUTED)));
        stats.add(left(label(String.format("%,d", side.score()), Theme.display(30), Theme.TEXT)));
        stats.add(Box.createVerticalStrut(14));
        stats.add(statRow("레벨", String.valueOf(side.level())));
        stats.add(Box.createVerticalStrut(6));
        stats.add(statRow("줄", String.valueOf(side.lines())));
        stats.add(Box.createVerticalGlue());
        card.add(stats, BorderLayout.CENTER);
        return card;
    }

    private static JComponent statRow(String name, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(label(name, Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), BorderLayout.WEST);
        row.add(label(value, Theme.body(15, Font.BOLD), Theme.TEXT), BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private static JComponent centered(JComponent c) {
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        return c;
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }
}
