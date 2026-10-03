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
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import storage.ScoreStorage;

public class ScoreboardScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTextPane pane;

    private SimpleAttributeSet defaultStyle;

    private SimpleAttributeSet titleStyle;

    private ArrayList<ScoreRecord> records;

    private ScoreStorage storage;

    public ScoreboardScreen() {

        super("SeoulTech SE Tetris");

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 1000);
        setLocationRelativeTo(null);

        // 스코어보드를 표시할 영역
        pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setBackground(Color.BLACK);

        CompoundBorder border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 10),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 5)
        );

        pane.setBorder(border);

        getContentPane().add(pane, BorderLayout.CENTER);


        // 일반 글자 스타일
        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(defaultStyle, 20);
        StyleConstants.setFontFamily(defaultStyle, "Courier");
        StyleConstants.setBold(defaultStyle, true);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setAlignment(
                defaultStyle,
                StyleConstants.ALIGN_CENTER
        );


        // SCOREBOARD 제목 스타일
        titleStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(titleStyle, 24);
        StyleConstants.setFontFamily(titleStyle, "Courier");
        StyleConstants.setBold(titleStyle, true);
        StyleConstants.setForeground(titleStyle, Color.YELLOW);
        StyleConstants.setAlignment(
                titleStyle,
                StyleConstants.ALIGN_CENTER
        );


        // 저장된 점수 불러오기
        storage = new ScoreStorage();

        records = storage.load();

        // 점수가 높은 순서대로 정렬
        records.sort(
                Comparator.comparingInt(ScoreRecord::getScore)
                          .reversed()
        );


        // 화면 출력
        drawScreen();


        // Enter 키 입력을 받기 위한 KeyListener 등록
        addKeyListener(new ScoreboardKeyListener());
        setFocusable(true);


        // 창이 실제로 열린 다음 키보드 포커스를 요청
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }
        });
    }


    // 스코어보드 화면 출력
    private void drawScreen() {

        StringBuilder sb = new StringBuilder();

        sb.append("SCOREBOARD\n\n");

        // 기록이 10개보다 적으면 실제 기록 개수만큼만 출력
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

        // 선택지가 하나뿐이므로 ↑↓ 이동은 필요 없음
        sb.append("\n> 시작 메뉴 <");
        sb.append("\n\n[Enter] 선택");

        pane.setText(sb.toString());

        applyStyles();
    }


    // 글자 스타일 적용
    private void applyStyles() {

        StyledDocument doc = pane.getStyledDocument();

        // 전체 글자에 기본 스타일 적용
        doc.setParagraphAttributes(
                0,
                doc.getLength(),
                defaultStyle,
                false
        );

        String title = "SCOREBOARD";

        // SCOREBOARD 제목만 노란색으로 강조
        doc.setCharacterAttributes(
                0,
                title.length(),
                titleStyle,
                false
        );
    }


    // 시작 메뉴로 돌아가기
    private void backToMenu() {

        // 현재 스코어보드 창 닫기
        dispose();

        // 시작 메뉴 새로 생성
        StartMenu menu = new StartMenu();

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
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}