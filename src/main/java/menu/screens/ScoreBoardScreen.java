package menu.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import menu.ScoreRecord;
import ui.components.RoundedPanel;
import ui.components.SegmentedControl;
import ui.theme.Theme;

// 난이도·모드별 TOP 10을 보여주는 카드. 게임오버 화면과 스코어보드 화면이 함께 사용
public class ScoreBoardScreen extends RoundedPanel {

    private static final long serialVersionUID = 1L;
    private static final int ROW_COUNT = 10;
    private static final int ROW_HEIGHT = 36;
    private static final Color HIGHLIGHT = new Color(80, 150, 230, 46);

    private static final String[] DIFFICULTIES = {"EASY", "NORMAL", "HARD"};
    private static final String[] MODES = {"NORMAL", "ITEM"};

    private final SegmentedControl difficultyControl;
    private final SegmentedControl modeControl;
    private final ScoreRow[] rows = new ScoreRow[ROW_COUNT];
    private final JLabel emptyLabel = new JLabel("저장된 기록이 없습니다.");

    public ScoreBoardScreen(Consumer<String> onDifficulty, Consumer<String> onMode) {
        super(Theme.CARD, Theme.CARD_BORDER, 12);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel filters = new JPanel(new GridLayout(1, 2, 10, 0));
        filters.setOpaque(false);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        difficultyControl = new SegmentedControl(new String[] {"쉬움", "보통", "어려움"},
                i -> onDifficulty.accept(DIFFICULTIES[i]));
        modeControl = new SegmentedControl(new String[] {"일반", "아이템"},
                i -> onMode.accept(MODES[i]));
        filters.add(difficultyControl);
        filters.add(modeControl);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, filters.getPreferredSize().height));
        add(filters);
        add(Box.createVerticalStrut(10));

        add(createHeader());

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setAlignmentX(Component.LEFT_ALIGNMENT);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        // 기록 수와 관계없이 크기가 변하지 않도록 10줄 높이로 고정
        Dimension listSize = new Dimension(360, ROW_COUNT * ROW_HEIGHT);
        list.setPreferredSize(listSize);
        list.setMaximumSize(new Dimension(Integer.MAX_VALUE, listSize.height));

        emptyLabel.setFont(Theme.body(14, Font.PLAIN));
        emptyLabel.setForeground(Theme.TEXT_MUTED);
        emptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        emptyLabel.setBorder(BorderFactory.createEmptyBorder(14, 12, 0, 0));
        list.add(emptyLabel);

        for (int i = 0; i < ROW_COUNT; i++) {
            rows[i] = new ScoreRow();
            list.add(rows[i]);
        }
        add(list);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(Theme.CARD_DARK);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        header.add(fixedWidth(cell("순위", Theme.body(12, Font.BOLD), Theme.TEXT_MUTED, JLabel.CENTER)), BorderLayout.WEST);
        header.add(cell("이름", Theme.body(12, Font.BOLD), Theme.TEXT_MUTED, JLabel.LEFT), BorderLayout.CENTER);
        header.add(cell("점수", Theme.body(12, Font.BOLD), Theme.TEXT_MUTED, JLabel.RIGHT), BorderLayout.EAST);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));
        return header;
    }

    public void update(String difficulty, String mode, List<ScoreRecord> records, ScoreRecord highlight) {
        difficultyControl.setSelectedIndex(indexOf(DIFFICULTIES, difficulty));
        modeControl.setSelectedIndex(indexOf(MODES, mode));

        emptyLabel.setVisible(records.isEmpty());
        for (int i = 0; i < ROW_COUNT; i++) {
            if (i < records.size()) {
                rows[i].show(i, records.get(i), records.get(i) == highlight);
            } else {
                rows[i].setVisible(false);
            }
        }
        revalidate();
        repaint();
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private static JLabel cell(String text, Font font, Color color, int alignment) {
        JLabel label = new JLabel(text, alignment);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static JLabel fixedWidth(JLabel label) {
        label.setPreferredSize(new Dimension(44, label.getPreferredSize().height));
        return label;
    }

    private static class ScoreRow extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel rank = fixedWidth(cell(" ", Theme.display(15), Theme.TEXT_MUTED, JLabel.CENTER));
        private final JLabel name = cell(" ", Theme.body(15, Font.PLAIN), Theme.TEXT, JLabel.LEFT);
        private final JLabel score = cell(" ", Theme.display(15), Theme.TEXT, JLabel.RIGHT);
        private boolean highlighted;

        ScoreRow() {
            super(new BorderLayout(12, 0));
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.TRACK),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            setPreferredSize(new Dimension(360, ROW_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            add(rank, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            add(score, BorderLayout.EAST);
        }

        void show(int index, ScoreRecord record, boolean highlight) {
            highlighted = highlight;
            rank.setText(String.valueOf(index + 1));
            rank.setForeground(rankColor(index));
            name.setText(highlight ? record.getName() + "  NEW" : record.getName());
            name.setFont(Theme.body(15, highlight ? Font.BOLD : Font.PLAIN));
            score.setText(String.format("%,d", record.getScore()));
            setVisible(true);
        }

        private static Color rankColor(int index) {
            return switch (index) {
                case 0 -> Theme.GOLD;
                case 1 -> Theme.SILVER;
                case 2 -> Theme.BRONZE;
                default -> Theme.TEXT_MUTED;
            };
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (highlighted) {
                g.setColor(HIGHLIGHT);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }
}
