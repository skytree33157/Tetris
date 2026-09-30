package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import storage.ScoreStorage;

public class GameOverScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextPane pane;

    private SimpleAttributeSet defaultStyle;
    private SimpleAttributeSet gameOverStyle;
    private SimpleAttributeSet newRecordStyle;

    private int score;

    private ArrayList<ScoreRecord> records;

    private ScoreRecord newRecord;

    private ScoreStorage storage;

    private static final String[] MENU_ITEMS = {
        "시작 메뉴",
        "프로그램 종료"
    };

    private int selectedIndex = 0;

    public GameOverScreen(int score) {

        super("SeoulTech SE Tetris");

        this.score = score;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 1000);
        setLocationRelativeTo(null);

        pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setBackground(Color.BLACK);

        CompoundBorder border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 10),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 5));

        pane.setBorder(border);

        getContentPane().add(pane, BorderLayout.CENTER);

        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(defaultStyle, 20);
        StyleConstants.setFontFamily(defaultStyle, "Courier");
        StyleConstants.setBold(defaultStyle, true);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setAlignment(
                defaultStyle,
                StyleConstants.ALIGN_CENTER
        );

        gameOverStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(gameOverStyle, 24);
        StyleConstants.setFontFamily(gameOverStyle, "Courier");
        StyleConstants.setBold(gameOverStyle, true);
        StyleConstants.setForeground(gameOverStyle, Color.YELLOW);
        StyleConstants.setAlignment(
                gameOverStyle,
                StyleConstants.ALIGN_CENTER
        );

        // 새 기록 강조
        newRecordStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(newRecordStyle, 20);
        StyleConstants.setFontFamily(newRecordStyle, "Courier");
        StyleConstants.setBold(newRecordStyle, true);
        StyleConstants.setForeground(newRecordStyle, Color.YELLOW);
        StyleConstants.setAlignment(
                newRecordStyle,
                StyleConstants.ALIGN_CENTER
        );

        storage = new ScoreStorage();

        records = storage.load();

        records.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed());

        checkNewRecord();

        drawScreen();

        addKeyListener(new MenuKeyListener());
        setFocusable(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }
        });
    }


    private void checkNewRecord() {

        boolean canRecord;

        if (records.size() < 10) {
            canRecord = true;
        } else {

            int tenthScore = records.get(9).getScore();

            canRecord = score > tenthScore;
        }

        if (canRecord) {

            String name = JOptionPane.showInputDialog(
                    this,
                    "이름을 입력하세요.",
                    "NEW RECORD",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (name == null || name.trim().isEmpty()) {
                name = "PLAYER";
            }

            newRecord = new ScoreRecord(name.trim(), score);

            records.add(newRecord);

            records.sort(
                    Comparator.comparingInt(ScoreRecord::getScore)
                              .reversed()
            );

            if (records.size() > 10) {
                records.remove(records.size() - 1);
            }

            storage.save(records);
        }
    }

    private void drawScreen() {

        StringBuilder sb = new StringBuilder();

        sb.append("GAME OVER\n\n");
        sb.append("SCORE : ").append(score).append("\n\n");
        sb.append("SCOREBOARD\n");

        int count = Math.min(records.size(), 10);

        for (int i = 0; i < count; i++) {

            ScoreRecord record = records.get(i);

            sb.append(i + 1)
              .append(". ")
              .append(record.getName())
              .append("  ")
              .append(record.getScore())
              .append("\n");
        }

        sb.append("\n");

        for (int i = 0; i < MENU_ITEMS.length; i++) {

            if (i == selectedIndex) {
                sb.append("> ")
                  .append(MENU_ITEMS[i])
                  .append(" <\n");
            } else {
                sb.append("  ")
                  .append(MENU_ITEMS[i])
                  .append("\n");
            }
        }

        sb.append("\n[Up/Down] 이동   [Enter] 선택");

        pane.setText(sb.toString());

        applyStyles();
    }

    private void applyStyles() {

        StyledDocument doc = pane.getStyledDocument();

        doc.setParagraphAttributes(
                0,
                doc.getLength(),
                defaultStyle,
                false
        );

        String firstLine = "GAME OVER";

        doc.setCharacterAttributes(
                0,
                firstLine.length(),
                gameOverStyle,
                false
        );

        if (newRecord != null) {

            String[] lines = pane.getText().split("\n");

            int offset = 0;

            for (String line : lines) {

                if (line.contains(newRecord.getName())
                        && line.contains(String.valueOf(newRecord.getScore()))) {

                    doc.setCharacterAttributes(
                            offset,
                            line.length(),
                            newRecordStyle,
                            false
                    );

                    break;
                }

                offset += line.length() + 1;
            }
        }
    }

    private void moveUp() {

        selectedIndex =
                (selectedIndex - 1 + MENU_ITEMS.length)
                % MENU_ITEMS.length;

        drawScreen();
    }

    private void moveDown() {

        selectedIndex =
                (selectedIndex + 1)
                % MENU_ITEMS.length;

        drawScreen();
    }

    private void select() {

        switch (selectedIndex) {

            case 0:
                dispose(); //GameOverScreen 종료(프로그램 자체 x)

                StartMenu menu = new StartMenu(); //StartMenu 객체 생성
                menu.setVisible(true); //화면에 보여주기

                break;

            case 1:
                System.exit(0); //아예 프로그램 종료
                break;
        }
    }

    private class MenuKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {

            switch (e.getKeyCode()) {

                case KeyEvent.VK_UP:
                    moveUp();
                    break;

                case KeyEvent.VK_DOWN:
                    moveDown();
                    break;

                case KeyEvent.VK_ENTER:
                    select();
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}