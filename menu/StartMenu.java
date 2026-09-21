package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import app.AppState;
import app.AppStateManager;

public class StartMenu extends JFrame {

    private static final long serialVersionUID = 1L;

    // 메뉴 항목 목록
    private static final String[] MENU_ITEMS = {
        "게임 시작",
        "설정",
        "스코어보드",
        "게임 종료"
    };

    private JTextPane pane;
    private SimpleAttributeSet defaultStyle; 
    private SimpleAttributeSet selectedStyle;
    private int selectedIndex = 0; // 메뉴 항목 인덱스
    private final AppStateManager stateManager = new AppStateManager(AppState.START_MENU);

    public StartMenu() {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(400, 400);
        setLocationRelativeTo(null);

        pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setBackground(Color.BLACK);
        CompoundBorder border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 10),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 5));
        pane.setBorder(border);
        this.getContentPane().add(pane, BorderLayout.CENTER);

        // 일반 폰트 (흰색)
        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(defaultStyle, 20);
        StyleConstants.setFontFamily(defaultStyle, "Courier");
        StyleConstants.setBold(defaultStyle, true);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setAlignment(defaultStyle, StyleConstants.ALIGN_CENTER);

        // 선택 강조 폰트 (노란색)
        selectedStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(selectedStyle, 20);
        StyleConstants.setFontFamily(selectedStyle, "Courier");
        StyleConstants.setBold(selectedStyle, true);
        StyleConstants.setForeground(selectedStyle, Color.YELLOW);
        StyleConstants.setAlignment(selectedStyle, StyleConstants.ALIGN_CENTER);
        
        // 키보드 입력을 받으려면 포커스 필요
        addKeyListener(new MenuKeyListener());
        setFocusable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        drawMenu();
    }

    // 현재 selectedIndex 기준으로 메뉴 전체를 다시 그림
    // 선택된 항목 앞뒤에 "> <" 표시를 붙여 구분
    private void drawMenu() { 
        StringBuilder sb = new StringBuilder();
        sb.append("TETRIS\n\n");
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            if (i == selectedIndex) {
                sb.append("> ").append(MENU_ITEMS[i]).append(" <\n");
            } else {
                sb.append("  ").append(MENU_ITEMS[i]).append("\n");
            }
        }
        sb.append("\n[Up/Down] 이동   [Enter] 선택");

        pane.setText(sb.toString());
        applyStyles();
    }

    // 폰트 적용
    private void applyStyles() {
        StyledDocument doc = pane.getStyledDocument();
        doc.setParagraphAttributes(0, doc.getLength(), defaultStyle, false);

        String[] lines = pane.getText().split("\n");
        int offset = 0;
        for (String line : lines) {
            if (line.startsWith(">")) {
                doc.setCharacterAttributes(offset, line.length(), selectedStyle, false);
            }
            offset += line.length() + 1;
        }
    }

    // 인덱스 선택
    private void moveUp() {
        selectedIndex = (selectedIndex - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
        drawMenu();
    }

    private void moveDown() {
        selectedIndex = (selectedIndex + 1) % MENU_ITEMS.length;
        drawMenu();
    }

    // 화면 선택
    private void select() {
        switch (selectedIndex) {
            case 0:
                // TODO: game 담당자가 게임 화면 전환 로직 연결
                System.out.println("게임 시작 선택됨 (미구현)");
                break;
            case 1:
                // TODO: settings 담당자가 설정 화면 전환 로직 연결
                System.out.println("설정 선택됨 (미구현)");
                break;
            case 2:
                // TODO: score/storage 담당자가 스코어보드 화면 전환 로직 연결
                System.out.println("스코어보드 선택됨 (미구현)");
                break;
            case 3:
                exitApplication();
                break;
        }
    }

    // 상태를 EXIT로 전이한 뒤 창을 정리하고 종료
    private void exitApplication() {
        stateManager.transitionTo(AppState.EXIT);
        dispose();
        System.exit(0);
    }

    // 이벤트 리스너
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
