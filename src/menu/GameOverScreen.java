package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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

    private int score;

    private String difficulty;

    private String mode;

    private ArrayList<ScoreRecord> records;
    private ArrayList<ScoreRecord> categoryRecords;

    private ScoreRecord newRecord;

    private ScoreStorage storage;

    private static final String[] MENU_ITEMS = {
        "스코어보드",
        "시작 메뉴",
        "프로그램 종료"
    };

    private int selectedIndex = 0;

    public GameOverScreen(int score, String difficulty, String mode) {

        super("SeoulTech SE Tetris");

        this.score = score;
        this.difficulty = difficulty;
        this.mode = mode;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(600, 700);
        setMinimumSize(new Dimension(600, 700));
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
        StyleConstants.setFontSize(gameOverStyle, 32);
        StyleConstants.setFontFamily(gameOverStyle, "Courier");
        StyleConstants.setBold(gameOverStyle, true);
        StyleConstants.setForeground(gameOverStyle, new Color(255, 82, 82));
        StyleConstants.setAlignment(
                gameOverStyle,
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

        categoryRecords = new ArrayList<>();

        for (ScoreRecord record : records) {
            if (record.getDifficulty().equals(difficulty) && record.getMode().equals(mode)) {
                categoryRecords.add(record);
            }
        }

        categoryRecords.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed());

        boolean canRecord;

        if (categoryRecords.size() < 10) {
            canRecord = true;
        } else {

            int tenthScore = categoryRecords.get(9).getScore();

            canRecord = score > tenthScore;
        }

        if (canRecord) {

            String name = JOptionPane.showInputDialog(
                    this,
                    "이름을 입력하세요.",
                    "NEW RECORD",
                    JOptionPane.PLAIN_MESSAGE
            );

            if (name == null || name.trim().isEmpty() || name.contains(",")) {
                name = "PLAYER";
            }

            newRecord = new ScoreRecord(name.trim(), score, difficulty, mode);

            records.add(newRecord);
            categoryRecords.add(newRecord);
            categoryRecords.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed());

            records.sort(
                    Comparator.comparingInt(ScoreRecord::getScore)
                              .reversed()
            );

            if (categoryRecords.size() > 10) {
                ScoreRecord lowestRecord = categoryRecords.get(categoryRecords.size()-1);
                records.remove(lowestRecord);
            }

            storage.save(records);
        }
    }

    private void drawScreen() {

        StringBuilder sb = new StringBuilder();

        sb.append("GAME OVER\n\n");
        sb.append("SCORE : ").append(score).append("\n");
        sb.append("DIFFICULTY: ").append(difficulty).append("\n");
        sb.append("MODE: ").append(mode).append("\n\n");

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

    private void openScoreboard() {
        dispose();

        ScoreboardScreen scoreboard = new ScoreboardScreen(difficulty, mode);
        scoreboard.setVisible(true);
    }

    private void applyStyles() {

        StyledDocument doc = pane.getStyledDocument();

        doc.setParagraphAttributes(
                0,
                doc.getLength(),
                defaultStyle,
                false
        );

        doc.setCharacterAttributes(
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
                openScoreboard(); //ScoreboardScreen 열기
                break;

            case 1:
                dispose(); //GameOverScreen 종료(프로그램 자체 x)

                StartMenu menu = new StartMenu(); //StartMenu 객체 생성
                menu.setVisible(true); //화면에 보여주기

                break;

            case 2:
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

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                select();
            } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                moveUp();
            } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                moveDown();
            }

        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}