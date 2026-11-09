package menu.screens;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JFrame;

import game.GameMode;
import menu.ScoreRecord;
import menu.views.GameOverView;
import storage.ScoreStorage;

public class GameOverScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_NAME = "PLAYER";

    private final GameOverView view;

    private int score;

    private String difficulty;

    private String mode;

    private String viewDifficulty;
    private String viewMode;

    private ArrayList<ScoreRecord> records;
    private ArrayList<ScoreRecord> categoryRecords;

    private ScoreRecord newRecord;

    private ScoreStorage storage;

    private final boolean canRecord;

    private static final String[] MENU_ITEMS = {
        "다시 하기",
        "시작 메뉴",
        "프로그램 종료"
    };

    private int selectedIndex = 0;

    public GameOverScreen(int score, String difficulty, String mode, int level, int lines, long elapsedMs) {

        super("TFSE");
        ui.AppIcon.apply(this);

        this.score = score;
        this.difficulty = difficulty;
        this.mode = mode;

        this.viewDifficulty = difficulty;
        this.viewMode = mode;

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        storage = new ScoreStorage();

        records = storage.load();

        records.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed());

        categoryRecords = selectCategoryRecords(records, difficulty, mode);

        canRecord = canRegisterRecord(categoryRecords, score);

        view = new GameOverView(
                new GameOverView.Result(score, difficulty, mode, level, lines, elapsedMs,
                        canRecord, isNewBest(categoryRecords, score)),
                MENU_ITEMS,
                index -> {
                    selectedIndex = index;
                    select();
                },
                this::registerRecord,
                value -> {
                    viewDifficulty = value;
                    refreshRecords();
                },
                value -> {
                    viewMode = value;
                    refreshRecords();
                });
        setContentPane(view);

        refreshRecords();
        view.setSelectedIndex(selectedIndex);

        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new MenuKeyListener());
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                if (canRecord) {
                    view.focusName();
                } else {
                    requestFocusInWindow();
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                registerRecord();
            }
        });
    }

    // 해당 난이도·모드의 기록을 점수 내림차순으로 전달받음
    public static boolean canRegisterRecord(
            ArrayList<ScoreRecord> sortedRecords, int score) {

        if (sortedRecords.size() < 10) {
            return true;
        }

        int tenthScore = sortedRecords.get(9).getScore();
        return score > tenthScore;
    }

    // 해당 난이도·모드의 기록을 점수 내림차순으로 전달받음
    public static boolean isNewBest(ArrayList<ScoreRecord> sortedRecords, int score) {
        if (score <= 0) {
            return false;
        }
        return sortedRecords.isEmpty() || score > sortedRecords.get(0).getScore();
    }

    // 쉼표는 저장 형식의 구분자라 이름에 쓸 수 없음
    public static String normalizeName(String name) {
        if (name == null || name.trim().isEmpty() || name.contains(",")) {
            return DEFAULT_NAME;
        }
        String trimmed = name.trim();
        return trimmed.length() > GameOverView.MAX_NAME_LENGTH
                ? trimmed.substring(0, GameOverView.MAX_NAME_LENGTH)
                : trimmed;
    }

    public static ArrayList<ScoreRecord> selectCategoryRecords(
        ArrayList<ScoreRecord> records,
        String difficulty,
        String mode) {

        ArrayList<ScoreRecord> selected = new ArrayList<>();

        for (ScoreRecord record : records) {
            if (record.getDifficulty().equals(difficulty)
                    && record.getMode().equals(mode)) {
                selected.add(record);
            }
        }

        selected.sort(
                Comparator.comparingInt(ScoreRecord::getScore).reversed()
        );

        return selected;
    }

    public static void retainTopTen(
            ArrayList<ScoreRecord> records,
            ArrayList<ScoreRecord> sortedCategoryRecords) {
        while (sortedCategoryRecords.size() > 10) {
            records.remove(sortedCategoryRecords.remove(sortedCategoryRecords.size() - 1));
        }
    }

    // 기록 조회 난이도 순환: EASY → NORMAL → HARD → EASY (direction이 음수면 반대로)
    public static String nextDifficulty(String current, int direction) {
        String[] order = {"EASY", "NORMAL", "HARD"};
        int index = java.util.Arrays.asList(order).indexOf(current);
        if (index < 0) {
            index = 1;
        }
        int step = direction < 0 ? -1 : 1;
        return order[(index + step + order.length) % order.length];
    }

    // 기록 조회 모드 전환: NORMAL ↔ ITEM
    public static String nextMode(String current) {
        return "NORMAL".equals(current) ? "ITEM" : "NORMAL";
    }

    // 자격이 있는데 아직 등록하지 않았으면 입력한 이름으로 등록 (화면을 떠날 때도 호출)
    private void registerRecord() {

        if (!canRecord || newRecord != null) {
            return;
        }

        newRecord = new ScoreRecord(normalizeName(view.nameText()), score, difficulty, mode);

        records.add(newRecord);

        ArrayList<ScoreRecord> category = selectCategoryRecords(records, difficulty, mode);

        records.sort(
                Comparator.comparingInt(ScoreRecord::getScore)
                          .reversed()
        );

        retainTopTen(records, category);

        storage.save(records);

        view.lockName();
        refreshRecords();
        requestFocusInWindow();
    }

    private void refreshRecords() {
        categoryRecords =
                selectCategoryRecords(records, viewDifficulty, viewMode);
        view.ScoreBoard().update(viewDifficulty, viewMode, categoryRecords, newRecord);
    }

    private void moveUp() {

        selectedIndex =
                (selectedIndex - 1 + MENU_ITEMS.length)
                % MENU_ITEMS.length;

        view.setSelectedIndex(selectedIndex);
    }

    private void moveDown() {

        selectedIndex =
                (selectedIndex + 1)
                % MENU_ITEMS.length;

        view.setSelectedIndex(selectedIndex);
    }

    private void select() {

        registerRecord();

        switch (selectedIndex) {

            case 0:
                dispose();

                GameScreen gameScreen = new GameScreen(GameMode.valueOf(mode)); // 같은 모드로 새 게임
                gameScreen.setVisible(true);

                break;

            case 1:
                dispose(); //GameOverScreen 종료(프로그램 자체 x)

                StartMenuScreen menu = new StartMenuScreen(); //StartMenuScreen 객체 생성
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
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                viewDifficulty = nextDifficulty(viewDifficulty, 1);
                refreshRecords();
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                viewDifficulty = nextDifficulty(viewDifficulty, -1);
                refreshRecords();
            } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                viewMode = nextMode(viewMode);
                refreshRecords();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
