package menu.screens;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 스코어보드 화면의 디자인. 기록 처리와 키 입력은 ScoreboardScreen이 담당
public class ScoreboardView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ScoreBoardScreen scoreBoard;

    public ScoreboardView(Runnable onBack, Runnable onReset,
                          Consumer<String> onDifficulty, Consumer<String> onMode) {
        super(new BorderLayout(0, 18));
        setBackground(Theme.BG);
        setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        JLabel title = new JLabel("스코어보드");
        title.setFont(Theme.body(28, Font.BOLD));
        title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        scoreBoard = new ScoreBoardScreen(onDifficulty, onMode);
        add(scoreBoard, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(button("기록 초기화", Kind.SECONDARY, onReset), BorderLayout.WEST);
        ThemedButton back = button("시작 메뉴", Kind.PRIMARY, onBack);
        back.setSelected(true);   // Enter를 누르면 동작하는 버튼
        footer.add(back, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
    }

    private static ThemedButton button(String text, Kind kind, Runnable action) {
        ThemedButton button = new ThemedButton(text, kind);
        button.setFocusable(false);   // 키 입력은 ScoreboardScreen이 받도록
        button.setFont(Theme.body(15, kind == Kind.PRIMARY ? Font.BOLD : Font.PLAIN));
        button.setPreferredSize(new Dimension(150, 48));
        button.addActionListener(e -> action.run());
        return button;
    }

    public ScoreBoardScreen ScoreBoard() {
        return scoreBoard;
    }
}
