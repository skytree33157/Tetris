package menu.screens;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.Timer;

import blocks.core.BlockFactory;
import blocks.core.BlockType;
import game.ActionController;
import game.BattleController;
import game.BattleSettings;
import game.GameStateManager;
import item.core.ItemType;
import ui.theme.Theme;

/**
 * 디버그 모드 패널. 게임을 멈추지 않는 별도 창으로, 블록·아이템 생성과 레벨·하강 속도 조절을 제공한다.
 * 아이템 줄 간격은 아이템 모드에서만 효과가 있고, 로컬 대전에서는 공격 대기 행 최대 수도 조절한다.
 * 레벨 칸 클릭으로 켜고 끄며, 창을 닫아도 디버그 모드는 해제된다.
 */
final class DebugPanel extends JDialog {
    private static final int SYNC_INTERVAL_MS = 300;
    private static final int MAX_REMAINING_MINUTES = 999;
    private static final BlockType[] BLOCKS = {
            BlockType.I, BlockType.O, BlockType.T, BlockType.S, BlockType.Z, BlockType.J, BlockType.L
    };

    private final ActionController action;
    private final GameStateManager state;
    private final BattleController battle;
    private final BattleSettings settings;
    private final Consumer<Boolean> onToggle;
    private final JSlider levelSlider = new JSlider(1, GameStateManager.MAX_LEVEL, 1);
    private final JSlider speedSlider = new JSlider(
            GameStateManager.DEBUG_MIN_DROP_SPEED, GameStateManager.DEBUG_MAX_DROP_SPEED, 1000);
    private final JSlider itemLinesSlider = new JSlider(
            GameStateManager.MIN_LINES_FOR_ITEM, GameStateManager.MAX_LINES_FOR_ITEM,
            GameStateManager.DEFAULT_LINES_FOR_ITEM);
    private final JSlider attackRowsSlider = new JSlider(
            BattleController.MIN_MAX_PENDING_ATTACK_ROWS, BattleController.MAX_MAX_PENDING_ATTACK_ROWS,
            BattleController.MAX_MAX_PENDING_ATTACK_ROWS);
    private final JLabel levelValue = text("", 13);
    private final JLabel speedValue = text("", 13);
    private final JLabel itemLinesValue = text("", 13);
    private final JLabel attackRowsValue = text("", 13);
    private final JLabel status = text(" ", 12);
    private final JTextField remainingMinutesField = new JTextField("3", 5);
    // 게임 쪽에서 레벨업이 일어나도 패널 값이 따라가도록 주기적으로 맞춘다
    private final Timer syncTimer = new Timer(SYNC_INTERVAL_MS, e -> syncFromGame());
    private boolean active;
    private boolean syncing;

    /**
     * battle이 null이면 단일 플레이 디버그 패널이다. 대전이면 공격 대기 행 최대 수를 함께 조절한다.
     * settings가 시간제한 모드일 때만 남은 시간 조절 항목을 보여준다.
     */
    DebugPanel(Window owner, String title, ActionController action, GameStateManager state,
            BattleController battle, BattleSettings settings, Consumer<Boolean> onToggle) {
        super(owner, title, ModalityType.MODELESS);
        this.action = action;
        this.state = state;
        this.battle = battle;
        this.settings = settings;
        this.onToggle = onToggle;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                setActive(false);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                syncTimer.stop();
            }
        });

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        root.add(caption("블록 생성 (현재 낙하 블록과 교체)"));
        JPanel blockRow = row();
        for (BlockType type : BLOCKS) {
            blockRow.add(button(type.name(), () -> {
                action.debugSetSpawnSource(() -> BlockFactory.createBlock(type));
                spawn(action.debugReplaceCurrentBlock(BlockFactory.createBlock(type)), type.name() + " 블록");
            }));
        }
        blockRow.add(button("해제", () -> {
            action.debugSetSpawnSource(null);
            status.setText("원래 생성 규칙으로 복귀");
        }));
        root.add(blockRow);
        root.add(caption("아이템 생성"));
        JPanel itemRow = row();
        for (ItemType type : ItemType.values()) {
            itemRow.add(button(itemLabel(type), () -> {
                action.debugSetSpawnSource(() -> BlockFactory.createItem(type, state.getDifficulty()));
                spawn(action.debugReplaceCurrentBlock(BlockFactory.createItem(type, state.getDifficulty())),
                        itemLabel(type));
            }));
        }
        root.add(itemRow);

        root.add(caption("레벨"));
        levelSlider.setMajorTickSpacing(1);
        levelSlider.setSnapToTicks(true);
        levelSlider.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            action.debugSetLevel(levelSlider.getValue());
            syncFromGame();
        });
        root.add(sliderRow(levelSlider, levelValue));

        root.add(caption("드롭 속도 (ms, 작을수록 빠름)"));
        speedSlider.setMajorTickSpacing(GameStateManager.DEBUG_MAX_DROP_SPEED
                - GameStateManager.DEBUG_MIN_DROP_SPEED);
        speedSlider.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            action.debugSetDropSpeed(speedSlider.getValue());
            speedValue.setText(speedSlider.getValue() + " ms");
        });
        root.add(sliderRow(speedSlider, speedValue));

        root.add(caption("아이템 등장 줄 간격 (아이템 모드에서 적용)"));
        itemLinesSlider.setMajorTickSpacing(1);
        itemLinesSlider.setSnapToTicks(true);
        itemLinesSlider.addChangeListener(e -> {
            if (syncing) {
                return;
            }
            action.debugSetLinesForItem(itemLinesSlider.getValue());
            itemLinesValue.setText(itemLinesSlider.getValue() + " 줄");
        });
        root.add(sliderRow(itemLinesSlider, itemLinesValue));

        if (battle != null) {
            root.add(caption("공격 대기 행 최대 수 (로컬 대전 공통)"));
            attackRowsSlider.setMajorTickSpacing(1);
            attackRowsSlider.setSnapToTicks(true);
            attackRowsSlider.addChangeListener(e -> {
                if (syncing) {
                    return;
                }
                battle.setMaxPendingAttackRows(attackRowsSlider.getValue());
                attackRowsValue.setText(attackRowsSlider.getValue() + " 줄");
            });
            root.add(sliderRow(attackRowsSlider, attackRowsValue));

            if (settings != null && settings.isTimeLimited()) {
                root.add(caption("남은 시간 (분 입력 후 확인, 시간제한 모드)"));
                JPanel timeRow = row();
                timeRow.add(remainingMinutesField);
                timeRow.add(button("확인", this::applyRemainingMinutes));
                root.add(timeRow);
            }
        }

        root.add(Box.createVerticalStrut(10));
        root.add(status);

        setContentPane(root);
        syncFromGame();
        pack();
        setResizable(false);
    }

    /** 레벨 칸 클릭 등으로 디버그 모드를 켜거나 끈다. */
    void toggle() {
        setActive(!active);
    }

    boolean isDebugOn() {
        return active;
    }

    void setActive(boolean on) {
        if (active == on) {
            return;
        }
        active = on;
        if (on) {
            syncFromGame();
            status.setText(" ");
            Window owner = getOwner();
            if (owner != null) {
                setLocation(owner.getX() + owner.getWidth() + 8, owner.getY());
            }
            syncTimer.start();
            setVisible(true);
        } else {
            syncTimer.stop();
            // 디버그 모드를 끄면 고정 생성을 해제하고 무작위 생성으로 되돌린다
            action.debugSetSpawnSource(null);
            setVisible(false);
        }
        onToggle.accept(on);
    }

    // 게임 상태를 슬라이더에 반영한다. 사용자가 드래그 중인 슬라이더는 건드리지 않는다.
    private void syncFromGame() {
        syncing = true;
        try {
            int level = state.getCurrentLevel();
            int speed = state.getDropSpeed();
            if (!levelSlider.getValueIsAdjusting()) {
                levelSlider.setValue(level);
            }
            if (!speedSlider.getValueIsAdjusting()) {
                speedSlider.setValue(speed);
            }
            if (!itemLinesSlider.getValueIsAdjusting()) {
                itemLinesSlider.setValue(state.getLinesForItem());
            }
            itemLinesValue.setText(state.getLinesForItem() + " 줄");
            if (battle != null && !attackRowsSlider.getValueIsAdjusting()) {
                attackRowsSlider.setValue(battle.getMaxPendingAttackRows());
                attackRowsValue.setText(battle.getMaxPendingAttackRows() + " 줄");
            }
            levelValue.setText("Lv " + level);
            speedValue.setText(speed + " ms");
        } finally {
            syncing = false;
        }
    }

    // 남은 시간을 입력한 분으로 맞춘다. 경과 시간과 제한 총량을 같은 잠금 안에서 계산해 어긋나지 않게 한다.
    private void applyRemainingMinutes() {
        int minutes;
        try {
            minutes = Integer.parseInt(remainingMinutesField.getText().trim());
        } catch (NumberFormatException e) {
            status.setText("분은 정수로 입력해 주세요");
            return;
        }
        if (minutes < 1 || minutes > MAX_REMAINING_MINUTES) {
            status.setText("1~" + MAX_REMAINING_MINUTES + " 분 사이로 입력해 주세요");
            return;
        }
        synchronized (battle) {
            if (!battle.isRunning()) {
                status.setText("진행 중인 경기가 없습니다");
                return;
            }
            settings.setRemainingTimeMillis(battle.getElapsedPlayMillis(), minutes * 60_000L);
        }
        status.setText("남은 시간을 " + minutes + "분으로 설정함");
    }

    private void spawn(boolean applied, String name) {
        status.setText(applied
                ? name + " 생성됨"
                : "지금은 적용할 수 없습니다 (줄 삭제 중, 일시정지, 게임 오버)");
    }

    private static String itemLabel(ItemType type) {
        return switch (type) {
            case LINE_CLEAR -> "줄 삭제";
            case WEIGHT -> "무게";
            case BOMB -> "폭탄";
            case BONUS -> "보너스";
            case SLOW -> "슬로우";
        };
    }

    private static JButton button(String label, Runnable onClick) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        button.addActionListener(e -> onClick.run());
        return button;
    }

    private static JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private static JPanel sliderRow(JSlider slider, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(slider, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        slider.setOpaque(false);
        return row;
    }

    private static JLabel caption(String s) {
        JLabel label = text(s, 12);
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(8, 0, 2, 0));
        return label;
    }

    private static JLabel text(String s, int size) {
        JLabel label = new JLabel(s);
        label.setFont(Theme.body(size, Font.PLAIN));
        label.setForeground(Theme.TEXT);
        return label;
    }
}
