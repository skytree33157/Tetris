package menu.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.border.CompoundBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import blocks.style.ColorMode;

public class SettingsScreen extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String[] MENU_ITEMS = {
        "화면 크기",
        "색맹 모드",
        "조작 키 설정",
        "스코어보드 초기화",
        "기본값 복원",
        "게임 종료",
        "저장하고 뒤로가기"
    };

    private static final int SCREEN_SIZE_INDEX = 0;
    private static final int COLOR_MODE_INDEX = 1;
    private static final int KEY_CONFIG_INDEX = 2;
    private static final int RESET_SCOREBOARD_INDEX = 3;
    private static final int RESTORE_DEFAULTS_INDEX = 4;
    private static final int QUIT_GAME_INDEX = 5;
    private static final int SAVE_AND_BACK_INDEX = 6;

    private final AppSettings settings = AppSettings.getInstance();
    private final KeyAction[] REBIND_ORDER = KeyAction.values();

    private JTextPane pane;
    private SimpleAttributeSet defaultStyle;
    private SimpleAttributeSet selectedStyle;
    private int selectedIndex = 0;

    // 조작 키 재설정 중일 때 진행 단계 (-1이면 재설정 중이 아님)
    private int rebindStep = -1;
    private final Runnable onClose;
    private final Runnable onQuit;

    public SettingsScreen(Frame owner, Runnable onClose, Runnable onQuit) {
        super(owner, "SeoulTech SE Tetris - 설정", true); // modal
        this.onClose = onClose;
        this.onQuit = onQuit;
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setSize(420, 420);
        setLocationRelativeTo(owner); // owner 중앙에 위치

        pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setBackground(Color.BLACK);
        CompoundBorder border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 10),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 5));
        pane.setBorder(border);
        this.getContentPane().add(pane, BorderLayout.CENTER);

        defaultStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(defaultStyle, 18);
        StyleConstants.setFontFamily(defaultStyle, "Courier");
        StyleConstants.setBold(defaultStyle, true);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);
        StyleConstants.setAlignment(defaultStyle, StyleConstants.ALIGN_CENTER);

        selectedStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(selectedStyle, 18);
        StyleConstants.setFontFamily(selectedStyle, "Courier");
        StyleConstants.setBold(selectedStyle, true);
        StyleConstants.setForeground(selectedStyle, Color.YELLOW);
        StyleConstants.setAlignment(selectedStyle, StyleConstants.ALIGN_CENTER);

        addKeyListener(new SettingsKeyListener());
        setFocusable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveAndGoBack();
            }
        });

        drawMenu();
    }

    private void drawMenu() {
        StringBuilder sb = new StringBuilder();
        sb.append("SETTINGS\n\n");

        for (int i = 0; i < MENU_ITEMS.length; i++) {
            String line = MENU_ITEMS[i] + valueSuffix(i);
            if (i == selectedIndex) {
                sb.append("> ").append(line).append(" <\n");
            } else {
                sb.append("  ").append(line).append("\n");
            }
        }

        sb.append("\n[Up/Down] 이동   [Left/Right] 값 변경   [Enter] 선택");

        pane.setText(sb.toString());
        applyStyles();
    }

    private String valueSuffix(int index) {
        return switch (index) {
            case SCREEN_SIZE_INDEX -> ": " + screenSizeLabel(settings.getScreenSize());
            case COLOR_MODE_INDEX -> ": " + colorModeLabel(settings.getColorMode());
            default -> "";
        };
    }

    private String screenSizeLabel(ScreenSize size) {
        return switch (size) {
            case SMALL -> "작게";
            case MEDIUM -> "보통";
            case LARGE -> "크게";
        };
    }

    private String colorModeLabel(ColorMode mode) {
        return switch (mode) {
            case NORMAL -> "일반";
            case PROTANOPIA -> "적색맹";
            case DEUTERANOPIA -> "녹색맹";
            case TRITANOPIA -> "청황색맹";
        };
    }

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

    private void moveUp() {
        selectedIndex = (selectedIndex - 1 + MENU_ITEMS.length) % MENU_ITEMS.length;
        drawMenu();
    }

    private void moveDown() {
        selectedIndex = (selectedIndex + 1) % MENU_ITEMS.length;
        drawMenu();
    }

    // 화면 크기 / 색맹 모드처럼 값을 순환시키는 항목 처리
    private void changeValue(int direction) {
        switch (selectedIndex) {
            case SCREEN_SIZE_INDEX -> {
                ScreenSize[] values = ScreenSize.values();
                int next = (settings.getScreenSize().ordinal() + direction + values.length) % values.length;
                settings.setScreenSize(values[next]);
                drawMenu();
            }
            case COLOR_MODE_INDEX -> {
                ColorMode[] values = ColorMode.values();
                int next = (settings.getColorMode().ordinal() + direction + values.length) % values.length;
                settings.setColorMode(values[next]);
                drawMenu();
            }
            default -> { }
        }
    }

    private void select() {
        switch (selectedIndex) {
            case SCREEN_SIZE_INDEX, COLOR_MODE_INDEX -> changeValue(1);
            case KEY_CONFIG_INDEX -> startKeyRebind();
            case RESET_SCOREBOARD_INDEX -> resetScoreboard();
            case RESTORE_DEFAULTS_INDEX -> restoreDefaults();
            case QUIT_GAME_INDEX -> quitGame();
            case SAVE_AND_BACK_INDEX -> saveAndGoBack();
        }
    }

    private void startKeyRebind() {
        rebindStep = 0;
        drawRebindPrompt();
    }

    private void drawRebindPrompt() {
        KeyAction action = REBIND_ORDER[rebindStep];
        pane.setText("KEY CONFIG\n\n" + actionLabel(action) + " 키를 누르세요\n\n(ESC: 취소)");
        applyStyles();
    }

    private String actionLabel(KeyAction action) {
        return switch (action) {
            case MOVE_LEFT -> "왼쪽 이동";
            case MOVE_RIGHT -> "오른쪽 이동";
            case MOVE_DOWN -> "아래 이동";
            case ROTATE -> "회전";
        };
    }

    // TODO: FR-24에서 스코어보드 파일 저장이 구현되면 실제 저장소를 초기화하도록 연동
    private void resetScoreboard() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "스코어보드를 초기화할까요?",
                "스코어보드 초기화",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "스코어보드가 초기화되었습니다.");
        }
        drawMenu();
    }

    private void restoreDefaults() {
        settings.resetToDefaults();
        drawMenu();
    }

    private void saveAndGoBack() {
        settings.save();
        dispose();
        onClose.run();
    }

    private void quitGame() {
        int result = JOptionPane.showConfirmDialog(
                this, "게임을 종료할까요?", "게임 종료", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            onQuit.run();
        } else {
            drawMenu();
        }
    }

    private void handleRebindKey(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            rebindStep = -1;
            drawMenu();
            return;
        }

        KeyAction action = REBIND_ORDER[rebindStep];
        settings.setKey(action, e.getKeyCode());

        rebindStep++;
        if (rebindStep >= REBIND_ORDER.length) {
            rebindStep = -1;
            drawMenu();
        } else {
            drawRebindPrompt();
        }
    }

    private class SettingsKeyListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (rebindStep >= 0) {
                handleRebindKey(e);
                return;
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP -> moveUp();
                case KeyEvent.VK_DOWN -> moveDown();
                case KeyEvent.VK_LEFT -> changeValue(-1);
                case KeyEvent.VK_RIGHT -> changeValue(1);
                case KeyEvent.VK_ENTER -> select();
                case KeyEvent.VK_ESCAPE -> saveAndGoBack();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
