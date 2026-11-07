package menu.screens;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import menu.ScoreRecord;
import menu.views.ScoreboardView;
import storage.ScoreStorage;

public class ScoreboardScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ScoreboardView view;

    private ArrayList<ScoreRecord> records;
    private ArrayList<ScoreRecord> categoryRecords;

    private ScoreStorage storage;

    private String difficulty = "NORMAL";
    private String mode = "NORMAL";

    public ScoreboardScreen() {

        super("TFSE");
        ui.AppIcon.apply(this);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        view = new ScoreboardView(this::backToMenu, this::resetRecords,
                value -> {
                    difficulty = value;
                    refresh();
                },
                value -> {
                    mode = value;
                    refresh();
                });
        setContentPane(view);

        // 저장된 점수 불러오기
        storage = new ScoreStorage();

        records = storage.load();

        // 점수가 높은 순서대로 정렬
        records.sort(
                Comparator.comparingInt(ScoreRecord::getScore).reversed());

        refresh();

        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        // Enter 키 입력을 받기 위한 KeyListener 등록
        addKeyListener(new ScoreboardKeyListener());
        setFocusable(true);
        setFocusTraversalKeysEnabled(false); // tab의 기본 포커스 이동 기능 끔(keyPressed()에서 tab 키를 쓰기 위해)

        // 창이 실제로 열린 다음 키보드 포커스를 요청
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                backToMenu();
            }
        });
    }

    public ScoreboardScreen(String difficulty, String mode) {
        this();

        this.difficulty = difficulty;
        this.mode = mode;

        refresh();
    }

    // 현재 난이도·모드의 기록을 다시 골라 화면에 반영
    private void refresh() {
        categoryRecords = GameOverScreen.selectCategoryRecords(records, difficulty, mode);
        view.ScoreBoard().update(difficulty, mode, categoryRecords, null);
    }

    private void resetRecords() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "스코어보드를 초기화할까요?",
                "스코어보드 초기화",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            if (storage.clear()) {
                records.clear();
                refresh();
            } else {
                JOptionPane.showMessageDialog(this, "스코어보드 초기화에 실패했습니다.");
            }
        }
        requestFocusInWindow();
    }

    // 시작 메뉴로 돌아가기
    private void backToMenu() {

        // 현재 스코어보드 창 닫기
        dispose();

        // 시작 메뉴 새로 생성
        StartMenuScreen menu = new StartMenuScreen();

        // 시작 메뉴 화면 표시
        menu.setVisible(true);
    }

    // 스코어보드 화면의 키보드 입력 처리
    private class ScoreboardKeyListener implements KeyListener {

        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {

            // Enter를 누르면 시작 메뉴로 돌아감
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                backToMenu();
            } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) { // 오른쪽 방향키를 누를 때마다 EASY -> NORMAL -> HARD로 이동함
                if (difficulty.equals("EASY")) {
                    difficulty = "NORMAL";
                } else if (difficulty.equals("NORMAL")) {
                    difficulty = "HARD";
                } else {
                    difficulty = "EASY";
                }

                refresh();
            } else if (e.getKeyCode() == KeyEvent.VK_LEFT) { // 왼쪽 방향키를 누르면 오른쪽 때의 반대로 이동함
                if (difficulty.equals("HARD")) {
                    difficulty = "NORMAL";
                } else if (difficulty.equals("NORMAL")) {
                    difficulty = "EASY";
                } else {
                    difficulty = "HARD";
                }

                refresh();
            } else if (e.getKeyCode() == KeyEvent.VK_TAB) { // 탭 키를 눌러 NORMAL -> ITEM으로 이동
                if (mode.equals("NORMAL")) {
                    mode = "ITEM";
                } else {
                    mode = "NORMAL";
                }

                refresh();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
