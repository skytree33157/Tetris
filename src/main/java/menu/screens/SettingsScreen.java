package menu.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import blocks.style.ColorMode;
import difficulty.Difficulty;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import menu.settings.ScreenSize;
import sound.SoundEvent;
import sound.SoundManager;
import storage.ScoreStorage;
import ui.components.RoundedPanel;
import ui.components.SegmentedControl;
import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.components.VolumeSlider;
import ui.theme.Theme;

public class SettingsScreen extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final int MIN_WIDTH = 880;
    private static final int VOLUME_STEP = 10;

    // ↑↓로 이동하는 순서. 왼쪽 열(게임 → 사운드) → 오른쪽 열(키 → 데이터) → 하단 버튼
    private static final int SCREEN_SIZE_INDEX = 0;
    private static final int COLOR_MODE_INDEX = 1;
    private static final int DIFFICULTY_INDEX = 2;
    private static final int SOUND_INDEX = 3;
    private static final int BGM_INDEX = 4;
    private static final int SFX_INDEX = 5;
    private static final int KEY_CONFIG_INDEX = 6;
    private static final int RESET_SCOREBOARD_INDEX = 7;
    private static final int RESTORE_DEFAULTS_INDEX = 8;
    private static final int QUIT_GAME_INDEX = 9;
    private static final int SAVE_AND_BACK_INDEX = 10;
    private static final int ROW_COUNT = 11;

    private final AppSettings settings = AppSettings.getInstance();
    private final KeyAction[] REBIND_ORDER = KeyAction.values();

    // 행 번호로 접근. 해당 종류가 아닌 행은 null
    private final JLabel[] rowLabels = new JLabel[ROW_COUNT];
    private final SegmentedControl[] segments = new SegmentedControl[ROW_COUNT];
    private final VolumeSlider[] sliders = new VolumeSlider[ROW_COUNT];
    private final JLabel[] volumeValues = new JLabel[ROW_COUNT];
    private final ThemedButton[] rowButtons = new ThemedButton[ROW_COUNT];

    private final Map<KeyAction, JLabel> keyNameLabels = new EnumMap<>(KeyAction.class);
    private final Map<KeyAction, JLabel> keyCaps = new EnumMap<>(KeyAction.class);
    private final JLabel promptLabel = text(" ", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED);
    private final JLabel warningLabel = text(" ", Theme.body(13, Font.BOLD), Theme.BRONZE);
    private int selectedIndex = 0;

    // 조작 키 재설정 중일 때 진행 단계 (-1이면 재설정 중이 아님)
    private int rebindStep = -1;
    // 모든 키 입력이 끝나야 설정에 반영 (중간 취소 시 기존 키 유지)
    private final Map<KeyAction, Integer> pendingKeys = new EnumMap<>(KeyAction.class);
    private String rebindWarning = "";
    private final Runnable onClose;
    private final Runnable onQuit;
    private final Runnable onEscapeClose;
    private final boolean inGame;

    public SettingsScreen(Frame owner, Runnable onClose, Runnable onQuit) {
        this(owner, onClose, onQuit, false);
    }

    public SettingsScreen(Frame owner, Runnable onClose, Runnable onQuit, boolean inGame) {
        this(owner, onClose, onQuit, inGame, () -> {});
    }

    public SettingsScreen(Frame owner, Runnable onClose, Runnable onQuit, boolean inGame,
            Runnable onEscapeClose) {
        super(owner, "TFSE - 설정", true); // modal
        this.onClose = onClose;
        this.onQuit = onQuit;
        this.onEscapeClose = onEscapeClose;
        this.inGame = inGame;
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        setContentPane(createContent());
        drawMenu();
        setResizable(false);
        pack();
        setSize(Math.max(getWidth(), MIN_WIDTH), getHeight());
        setLocationRelativeTo(owner); // owner 중앙에 위치

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
    }

    // ---------- 레이아웃 ----------

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        root.add(text("설정", Theme.body(28, Font.BOLD), Theme.TEXT), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.add(createLeftColumn());
        columns.add(createRightColumn());
        root.add(columns, BorderLayout.CENTER);

        root.add(createFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createLeftColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 20));
        column.setOpaque(false);
        column.add(createGameCard(), BorderLayout.CENTER);
        column.add(createSoundCard(), BorderLayout.SOUTH);
        return column;
    }

    private JPanel createGameCard() {
        RoundedPanel card = card();
        card.add(caption("게임"));
        card.add(Box.createVerticalStrut(16));

        addSegmentRow(card, SCREEN_SIZE_INDEX, "화면 크기", screenSizeLabels(),
                i -> settings.setScreenSize(ScreenSize.values()[i]));
        card.add(Box.createVerticalStrut(18));
        addSegmentRow(card, COLOR_MODE_INDEX, "블록 색상", colorModeLabels(),
                i -> settings.setColorMode(ColorMode.values()[i]));
        card.add(Box.createVerticalStrut(18));
        addSegmentRow(card, DIFFICULTY_INDEX, "난이도", difficultyLabels(),
                i -> settings.setDifficulty(Difficulty.values()[i]));

        if (inGame) {
            segments[DIFFICULTY_INDEX].setEnabled(false);
            card.add(Box.createVerticalStrut(6));
            card.add(text("게임 중에는 난이도를 바꿀 수 없습니다", Theme.body(12, Font.PLAIN), Theme.TEXT_MUTED));
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel createSoundCard() {
        RoundedPanel card = card();
        card.add(caption("사운드"));
        card.add(Box.createVerticalStrut(16));

        addSegmentRow(card, SOUND_INDEX, "사운드", new String[] {"켜기", "끄기"}, i -> {
            settings.setSoundEnabled(i == 0);
            afterSoundChange(false);
        });
        card.add(Box.createVerticalStrut(18));
        addVolumeRow(card, BGM_INDEX, "배경음", v -> {
            settings.setBgmVolume(v);
            afterSoundChange(false);
        });
        card.add(Box.createVerticalStrut(14));
        addVolumeRow(card, SFX_INDEX, "효과음", v -> {
            settings.setSfxVolume(v);
            afterSoundChange(true);
        });
        return card;
    }

    private void addSegmentRow(JPanel card, int row, String label, String[] options, IntConsumer apply) {
        rowLabels[row] = text(label, Theme.body(15, Font.BOLD), Theme.TEXT);
        card.add(rowLabels[row]);
        card.add(Box.createVerticalStrut(8));

        SegmentedControl control = new SegmentedControl(options, i -> {
            if (rebindStep >= 0) {
                return;
            }
            selectedIndex = row;
            apply.accept(i);
            drawMenu();
        });
        control.setAlignmentX(Component.LEFT_ALIGNMENT);
        fixHeight(control);
        segments[row] = control;
        card.add(control);
    }

    private void addVolumeRow(JPanel card, int row, String label, IntConsumer apply) {
        rowLabels[row] = text(label, Theme.body(15, Font.BOLD), Theme.TEXT);
        volumeValues[row] = text(" ", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(rowLabels[row], BorderLayout.WEST);
        header.add(volumeValues[row], BorderLayout.EAST);
        fixHeight(header);
        card.add(header);
        card.add(Box.createVerticalStrut(6));

        VolumeSlider slider = new VolumeSlider(VOLUME_STEP, v -> {
            if (rebindStep >= 0 || !settings.isSoundEnabled()) {
                return;
            }
            selectedIndex = row;
            apply.accept(v);
            drawMenu();
        });
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        fixHeight(slider);
        sliders[row] = slider;
        card.add(slider);
    }

    private JPanel createRightColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 20));
        column.setOpaque(false);
        column.add(createKeyCard(), BorderLayout.CENTER);
        column.add(createManageCard(), BorderLayout.SOUTH);
        return column;
    }

    private JPanel createKeyCard() {
        RoundedPanel card = card();
        card.add(caption("키 설정"));
        card.add(Box.createVerticalStrut(10));

        for (KeyAction action : REBIND_ORDER) {
            JLabel name = text(actionLabel(action), Theme.body(15, Font.PLAIN), Theme.TEXT_SOFT);
            JLabel cap = keyCap();
            keyNameLabels.put(action, name);
            keyCaps.put(action, cap);
            card.add(keyRow(name, cap));
        }

        card.add(fixedKeyRow("일시정지", "P, ESC"));
        card.add(fixedKeyRow("종료", "Esc hold"));
        card.add(Box.createVerticalStrut(10));
        card.add(promptLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(warningLabel);
        card.add(Box.createVerticalGlue());
        card.add(Box.createVerticalStrut(10));

        ThemedButton change = rowButton(KEY_CONFIG_INDEX, "키 변경", Kind.SECONDARY, 15);
        change.setAlignmentX(Component.LEFT_ALIGNMENT);
        change.setPreferredSize(new Dimension(100, 44));
        change.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        card.add(change);
        return card;
    }

    private JPanel createManageCard() {
        RoundedPanel card = card();
        card.add(caption("데이터"));
        card.add(Box.createVerticalStrut(12));

        JPanel buttons = new JPanel(new GridLayout(1, 0, 10, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(sized(rowButton(RESET_SCOREBOARD_INDEX, "스코어보드 초기화", Kind.SECONDARY, 14), 44));
        ThemedButton restore = rowButton(RESTORE_DEFAULTS_INDEX, "기본값 복원", Kind.SECONDARY, 14);
        if (isMenuItemAvailable(RESTORE_DEFAULTS_INDEX)) {
            buttons.add(sized(restore, 44));
        }
        fixHeight(buttons);
        card.add(buttons);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        String quitLabel = inGame ? "시작 메뉴" : "게임 종료";
        footer.add(sized(rowButton(QUIT_GAME_INDEX, quitLabel, Kind.SECONDARY, 15), 48), BorderLayout.WEST);
        footer.add(sized(rowButton(SAVE_AND_BACK_INDEX, "저장하고 뒤로가기", Kind.PRIMARY, 15), 48), BorderLayout.EAST);
        return footer;
    }

    private ThemedButton rowButton(int row, String label, Kind kind, int fontSize) {
        ThemedButton button = new ThemedButton(label, kind);
        button.setFocusable(false);   // 키 입력은 설정 창이 받도록
        button.setFont(Theme.body(fontSize, kind == Kind.PRIMARY ? Font.BOLD : Font.PLAIN));
        button.addActionListener(e -> {
            if (rebindStep >= 0) {
                return;
            }
            selectedIndex = row;
            select();
        });
        rowButtons[row] = button;
        return button;
    }

    private static ThemedButton sized(ThemedButton button, int height) {
        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 32, height));
        return button;
    }

    private static RoundedPanel card() {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.CARD_BORDER, 12);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        return card;
    }

    private static JLabel caption(String label) {
        return text(label, Theme.body(13, Font.BOLD), Theme.TEXT_MUTED);
    }

    private static JLabel text(String label, Font font, Color color) {
        JLabel view = new JLabel(label);
        view.setFont(font);
        view.setForeground(color);
        view.setAlignmentX(Component.LEFT_ALIGNMENT);
        return view;
    }

    private static JLabel keyCap() {
        JLabel cap = new JLabel(" ", JLabel.CENTER);
        cap.setFont(Theme.body(13, Font.PLAIN));
        cap.setForeground(Theme.TEXT);
        cap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 3, 1, Theme.OUTLINE),
                BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        return cap;
    }

    private static JPanel keyRow(JLabel name, JLabel cap) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        row.add(name, BorderLayout.WEST);
        row.add(cap, BorderLayout.EAST);
        fixHeight(row);
        return row;
    }

    private static JPanel fixedKeyRow(String label, String keys) {
        JLabel cap = keyCap();
        cap.setText(keys);
        cap.setForeground(Theme.TEXT_MUTED);
        return keyRow(text(label, Theme.body(15, Font.PLAIN), Theme.TEXT_MUTED), cap);
    }

    // BoxLayout이 세로로 늘리지 않도록 최대 높이를 선호 높이로 고정
    private static void fixHeight(JComponent component) {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
    }

    // ---------- 화면 갱신 ----------

    private void drawMenu() {
        boolean rebinding = rebindStep >= 0;
        boolean soundOn = settings.isSoundEnabled();

        segments[SCREEN_SIZE_INDEX].setSelectedIndex(settings.getScreenSize().ordinal());
        segments[COLOR_MODE_INDEX].setSelectedIndex(settings.getColorMode().ordinal());
        segments[DIFFICULTY_INDEX].setSelectedIndex(settings.getDifficulty().ordinal());
        segments[SOUND_INDEX].setSelectedIndex(soundOn ? 0 : 1);

        updateVolumeRow(BGM_INDEX, settings.getBgmVolume(), soundOn);
        updateVolumeRow(SFX_INDEX, settings.getSfxVolume(), soundOn);

        for (int row = 0; row < KEY_CONFIG_INDEX; row++) {
            boolean selected = !rebinding && row == selectedIndex;
            boolean dimmed = sliders[row] != null && !soundOn;
            rowLabels[row].setForeground(selected ? Theme.ACCENT : dimmed ? Theme.TEXT_MUTED : Theme.TEXT);
            if (segments[row] != null) {
                segments[row].setHighlighted(selected);
            }
            if (sliders[row] != null) {
                sliders[row].setHighlighted(selected);
            }
        }
        for (int row = KEY_CONFIG_INDEX; row < ROW_COUNT; row++) {
            rowButtons[row].setSelected(!rebinding && row == selectedIndex);
        }

        KeyAction waiting = rebinding ? REBIND_ORDER[rebindStep] : null;
        for (KeyAction action : REBIND_ORDER) {
            Integer pending = pendingKeys.get(action);
            keyCaps.get(action).setText(keyName(pending != null ? pending : settings.getKey(action)));
            keyNameLabels.get(action).setForeground(action == waiting ? Theme.ACCENT : Theme.TEXT_SOFT);
        }

        if (rebinding) {
            promptLabel.setText(actionLabel(waiting) + " 키를 누르세요 (P 사용 불가, Esc 취소)");
            promptLabel.setForeground(Theme.ACCENT);
        } else {
            promptLabel.setText("키 변경을 누르면 위에서부터 차례로 입력합니다");
            promptLabel.setForeground(Theme.TEXT_MUTED);
        }
        warningLabel.setText(rebindWarning.isEmpty() ? " " : rebindWarning);
    }

    private void updateVolumeRow(int row, int volume, boolean soundOn) {
        sliders[row].setValue(volume);
        sliders[row].setEnabled(soundOn);
        volumeValues[row].setText(soundOn ? volume + "%" : "꺼짐");
    }

    private static String keyName(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            case KeyEvent.VK_SPACE -> "Space"; // 로케일에 따라 "스페이스"로 나오지 않게
            default -> KeyEvent.getKeyText(keyCode);
        };
    }

    private static String[] screenSizeLabels() {
        ScreenSize[] values = ScreenSize.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = screenSizeLabel(values[i]);
        }
        return labels;
    }

    private static String[] colorModeLabels() {
        ColorMode[] values = ColorMode.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = colorModeLabel(values[i]);
        }
        return labels;
    }

    private static String[] difficultyLabels() {
        Difficulty[] values = Difficulty.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = difficultyLabel(values[i]);
        }
        return labels;
    }

    private static String screenSizeLabel(ScreenSize size) {
        return switch (size) {
            case SMALL -> "작게";
            case MEDIUM -> "보통";
            case LARGE -> "크게";
        };
    }

    private static String colorModeLabel(ColorMode mode) {
        return switch (mode) {
            case NORMAL -> "일반";
            case PROTANOPIA -> "적색맹";
            case DEUTERANOPIA -> "녹색맹";
            case TRITANOPIA -> "청황색맹";
            case TFSE -> "TFSE";
        };
    }

    private static String difficultyLabel(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "쉬움";
            case NORMAL -> "보통";
            case HARD -> "어려움";
        };
    }

    // ---------- 동작 ----------

    private void moveUp() {
        moveSelection(-1);
    }

    private void moveDown() {
        moveSelection(1);
    }

    private void moveSelection(int direction) {
        do {
            selectedIndex = (selectedIndex + direction + ROW_COUNT) % ROW_COUNT;
        } while (!isMenuItemAvailable(selectedIndex));
        drawMenu();
    }

    private boolean isMenuItemAvailable(int index) {
        return !inGame || index != RESTORE_DEFAULTS_INDEX;
    }

    // 화면 크기 / 색맹 모드처럼 값을 순환시키는 항목과 볼륨 조절 처리
    private void changeValue(int direction) {
        if (inGame && selectedIndex == DIFFICULTY_INDEX) {
            return;
        }

        switch (selectedIndex) {
            case SCREEN_SIZE_INDEX -> {
                ScreenSize[] values = ScreenSize.values();
                int next = (settings.getScreenSize().ordinal() + direction + values.length) % values.length;
                settings.setScreenSize(values[next]);
            }
            case COLOR_MODE_INDEX -> {
                ColorMode[] values = ColorMode.values();
                int next = (settings.getColorMode().ordinal() + direction + values.length) % values.length;
                settings.setColorMode(values[next]);
            }
            case DIFFICULTY_INDEX ->
                settings.setDifficulty(cycleDifficulty(settings.getDifficulty(), direction));
            case SOUND_INDEX -> {
                settings.setSoundEnabled(!settings.isSoundEnabled());
                afterSoundChange(false);
            }
            case BGM_INDEX -> {
                if (!settings.isSoundEnabled()) {
                    return;
                }
                settings.setBgmVolume(settings.getBgmVolume() + direction * VOLUME_STEP);
                afterSoundChange(false);
            }
            case SFX_INDEX -> {
                if (!settings.isSoundEnabled()) {
                    return;
                }
                settings.setSfxVolume(settings.getSfxVolume() + direction * VOLUME_STEP);
                afterSoundChange(true);
            }
            default -> {
                return;
            }
        }
        drawMenu();
    }

    // 사운드 설정은 저장 전에도 바로 들리도록 반영
    private void afterSoundChange(boolean previewEffect) {
        SoundManager sound = SoundManager.getInstance();
        sound.applySettings();
        if (previewEffect) {
            sound.play(SoundEvent.MOVE);
        }
    }

    static Difficulty cycleDifficulty(Difficulty current, int direction) {
        Difficulty[] values = Difficulty.values();
        int next = (current.ordinal() + direction % values.length + values.length) % values.length;
        return values[next];
    }

    private void select() {
        if (!isMenuItemAvailable(selectedIndex)) {
            return;
        }

        switch (selectedIndex) {
            case SCREEN_SIZE_INDEX, COLOR_MODE_INDEX, DIFFICULTY_INDEX, SOUND_INDEX -> changeValue(1);
            case KEY_CONFIG_INDEX -> startKeyRebind();
            case RESET_SCOREBOARD_INDEX -> resetScoreboard();
            case RESTORE_DEFAULTS_INDEX -> restoreDefaults();
            case QUIT_GAME_INDEX -> quitGame();
            case SAVE_AND_BACK_INDEX -> saveAndGoBack();
            default -> { } // 볼륨 행은 ←/→로만 조절
        }
    }

    private void startKeyRebind() {
        rebindStep = 0;
        pendingKeys.clear();
        rebindWarning = "";
        drawMenu();
    }

    private String actionLabel(KeyAction action) {
        return switch (action) {
            case MOVE_LEFT -> "왼쪽 이동";
            case MOVE_RIGHT -> "오른쪽 이동";
            case ROTATE -> "회전";
            case MOVE_DOWN -> "소프트 드롭";
            case HARD_DROP -> "하드 드롭";
            case HOLD -> "홀드";
        };
    }

    private void resetScoreboard() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "스코어보드를 초기화할까요?",
                "스코어보드 초기화",
                JOptionPane.YES_NO_OPTION
        );

        if (result == JOptionPane.YES_OPTION) {
            ScoreStorage storage = new ScoreStorage();
            if (storage.clear()) {
                JOptionPane.showMessageDialog(this, "스코어보드가 초기화되었습니다.");
            }
            else {
                JOptionPane.showMessageDialog(this, "스코어보드 초기화에 실패했습니다.");
            }
        }
        drawMenu();
    }

    private void restoreDefaults() {
        settings.resetToDefaults();
        afterSoundChange(false);
        drawMenu();
    }

    private void saveAndGoBack() {
        settings.save();
        dispose();
        onClose.run();
    }

    private void quitGame() {
        String prompt = inGame ? "시작 화면으로 이동할까요?" : "게임을 종료할까요?";
        String title = inGame ? "시작 화면으로 이동" : "게임 종료";
        int result = JOptionPane.showConfirmDialog(
            this, prompt, title, JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            dispose();
            onQuit.run();
        } else {
            drawMenu();
        }
    }

    private void handleRebindKey(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_ESCAPE) {
            // 취소 시 아무것도 적용하지 않아 기존 키가 유지됨
            rebindStep = -1;
            pendingKeys.clear();
            rebindWarning = "";
            drawMenu();
            return;
        }

        if (keyCode == KeyEvent.VK_P) {
            rebindWarning = "P는 일시정지 키라 사용할 수 없습니다";
            drawMenu();
            return;
        }

        if (pendingKeys.containsValue(keyCode)) {
            rebindWarning = "이미 지정한 키입니다";
            drawMenu();
            return;
        }

        rebindWarning = "";
        pendingKeys.put(REBIND_ORDER[rebindStep], keyCode);

        rebindStep++;
        if (rebindStep >= REBIND_ORDER.length) {
            pendingKeys.forEach(settings::setKey);
            pendingKeys.clear();
            rebindStep = -1;
        }
        drawMenu();
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

            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                onEscapeClose.run();
                saveAndGoBack();
                return;
            }

            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP -> moveUp();
                case KeyEvent.VK_DOWN -> moveDown();
                case KeyEvent.VK_LEFT -> changeValue(-1);
                case KeyEvent.VK_RIGHT -> changeValue(1);
                case KeyEvent.VK_ENTER -> select();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    }
}
