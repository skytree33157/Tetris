package menu.views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import ui.components.RoundedPanel;
import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 게임오버 화면의 디자인. 기록 처리와 키 입력은 GameOverScreen이 담당
public class GameOverView extends JPanel {

    private static final long serialVersionUID = 1L;

    public static final int MAX_NAME_LENGTH = 10;

    /** 결과 카드에 표시할 값 */
    public record Result(int score, String difficulty, String mode, int level, int lines, long elapsedMs,
                         boolean canRecord, boolean newBest) { }

    private final ScoreBoardPanel ScoreBoard;
    private final ThemedButton[] menuButtons;
    private final JTextField nameField = new JTextField();
    private final ThemedButton registerButton = new ThemedButton("등록", Kind.PRIMARY);

    public GameOverView(Result result, String[] menuItems, Consumer<Integer> onMenu, Runnable onRegister,
                        Consumer<String> onDifficulty, Consumer<String> onMode) {
        super(new BorderLayout(0, 20));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        ScoreBoard = new ScoreBoardPanel(onDifficulty, onMode);

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.add(createResultCard(result, onRegister));
        columns.add(ScoreBoard);
        add(columns, BorderLayout.CENTER);

        JPanel footer = new JPanel(new GridLayout(1, 0, 12, 0));
        footer.setOpaque(false);
        menuButtons = new ThemedButton[menuItems.length];
        for (int i = 0; i < menuItems.length; i++) {
            final int index = i;
            ThemedButton button = new ThemedButton(menuItems[i], i == 0 ? Kind.PRIMARY : Kind.SECONDARY);
            button.setFocusable(false);   // 키 입력은 GameOverScreen이 받도록
            button.setFont(Theme.body(16, i == 0 ? Font.BOLD : Font.PLAIN));
            button.setPreferredSize(new Dimension(200, 50));
            button.addActionListener(e -> onMenu.accept(index));
            menuButtons[i] = button;
            footer.add(button);
        }
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createResultCard(Result result, Runnable onRegister) {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.OUTLINE, 16);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        card.add(Box.createVerticalGlue());
        card.add(centered(label("GAME OVER", Theme.display(34), Theme.TEXT)));
        card.add(Box.createVerticalStrut(8));
        if (result.newBest()) {
            JLabel badge = label("최고 기록 갱신!", Theme.body(13, Font.BOLD), Theme.BG);
            badge.setOpaque(true);
            badge.setBackground(Theme.ACCENT);
            badge.setBorder(BorderFactory.createEmptyBorder(3, 12, 3, 12));
            card.add(centered(badge));
            card.add(Box.createVerticalStrut(8));
        }
        card.add(Box.createVerticalStrut(10));
        card.add(centered(label("최종 점수", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED)));
        card.add(centered(label(String.format("%,d", result.score()), Theme.display(38), Theme.TEXT)));
        card.add(Box.createVerticalStrut(16));

        long seconds = result.elapsedMs() / 1000;
        JPanel tiles = new JPanel(new GridLayout(1, 3, 10, 0));
        tiles.setOpaque(false);
        tiles.add(tile("레벨", String.valueOf(result.level())));
        tiles.add(tile("라인", String.valueOf(result.lines())));
        tiles.add(tile("시간", String.format("%02d:%02d", seconds / 60, seconds % 60)));
        card.add(centered(fixHeight(tiles)));
        card.add(Box.createVerticalStrut(12));

        card.add(centered(label(difficultyLabel(result.difficulty()) + " · " + modeLabel(result.mode()),
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED)));
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(24));

        if (result.canRecord()) {
            card.add(centered(label("스코어보드에 이름 등록 (최대 " + MAX_NAME_LENGTH + "자)",
                    Theme.body(13, Font.PLAIN), Theme.TEXT_SOFT)));
            card.add(Box.createVerticalStrut(6));
            card.add(centered(createNameRow(onRegister)));
        } else {
            card.add(centered(label("이번 점수는 TOP 10에 들지 못했습니다",
                    Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED)));
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JComponent createNameRow(Runnable onRegister) {
        nameField.setDocument(new PlainDocument() {
            private static final long serialVersionUID = 1L;

            @Override
            public void insertString(int offset, String text, AttributeSet attributes) throws BadLocationException {
                if (text == null) {
                    return;
                }
                int room = MAX_NAME_LENGTH - getLength();
                if (room > 0) {
                    super.insertString(offset, text.substring(0, Math.min(room, text.length())), attributes);
                }
            }
        });
        nameField.setFont(Theme.body(15, Font.PLAIN));
        nameField.setBackground(Theme.BG);
        nameField.setForeground(Theme.TEXT);
        nameField.setCaretColor(Theme.TEXT);
        nameField.setDisabledTextColor(Theme.TEXT_MUTED);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 2),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)));
        nameField.addActionListener(e -> onRegister.run());

        registerButton.setFocusable(false);
        registerButton.setFont(Theme.body(15, Font.BOLD));
        registerButton.setPreferredSize(new Dimension(80, 44));
        registerButton.addActionListener(e -> onRegister.run());

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(nameField, BorderLayout.CENTER);
        row.add(registerButton, BorderLayout.EAST);
        return fixHeight(row);
    }

    private static JPanel tile(String title, String value) {
        RoundedPanel tile = new RoundedPanel(Theme.CARD_DARK, Theme.CARD_DARK, Theme.RADIUS);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));
        tile.add(centered(label(title, Theme.body(12, Font.PLAIN), Theme.TEXT_MUTED)));
        tile.add(centered(label(value, Theme.display(20), Theme.TEXT)));
        return tile;
    }

    private static JLabel label(String text, Font font, java.awt.Color color) {
        JLabel view = new JLabel(text);
        view.setFont(font);
        view.setForeground(color);
        return view;
    }

    private static JComponent centered(JComponent component) {
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        return component;
    }

    // BoxLayout이 세로로 늘리지 않도록 최대 높이를 선호 높이로 고정
    private static JComponent fixHeight(JComponent component) {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    private static String difficultyLabel(String difficulty) {
        return switch (difficulty) {
            case "EASY" -> "쉬움";
            case "HARD" -> "어려움";
            default -> "보통";
        };
    }

    private static String modeLabel(String mode) {
        return "ITEM".equals(mode) ? "아이템 모드" : "일반 모드";
    }

    public ScoreBoardPanel ScoreBoard() {
        return ScoreBoard;
    }

    public String nameText() {
        return nameField.getText();
    }

    public void focusName() {
        nameField.requestFocusInWindow();
    }

    // 등록이 끝나면 다시 입력할 수 없게 막음
    public void lockName() {
        nameField.setEnabled(false);
        registerButton.setEnabled(false);
    }

    public void setSelectedIndex(int index) {
        for (int i = 0; i < menuButtons.length; i++) {
            menuButtons[i].setSelected(i == index);
        }
        repaint();
    }
}
