package menu.screens;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import menu.BattleModes;
import menu.views.StartMenuView;
import sound.SoundManager;

// 로컬 대전 방식 선택: 일반 / 아이템 / 시간 제한 → 로컬 대전 화면
public class LocalBattleSetupScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int BACK_INDEX = BattleModes.count();

    private final StartMenuView view;
    private int selectedIndex;

    public LocalBattleSetupScreen() {
        super("TFSE - 로컬 대전");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        view = new StartMenuView("LOCAL", "한 키보드로 둘이서 겨뤄 보세요", false, menuItems(),
                BattleModes.count(), index -> {
                    selectedIndex = index;
                    select();
                });
        view.setHint("1P  A D W S · Space · L Shift      2P  ← → ↑ ↓ · Enter · R Shift");
        view.setSelectedIndex(selectedIndex);
        setContentPane(view);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> move(-1);
                    case KeyEvent.VK_DOWN -> move(1);
                    case KeyEvent.VK_ENTER -> select();
                    case KeyEvent.VK_ESCAPE -> back();
                    default -> { }
                }
            }
        });
        setFocusable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
                SoundManager.getInstance().ensureBgm();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                back();
            }
        });
    }

    // "일반 모드", "아이템 모드", "시간 제한 모드 (3:00)", "뒤로"
    static String[] menuItems() {
        String[] labels = BattleModes.labels();
        String[] items = new String[labels.length + 1];
        for (int i = 0; i < labels.length; i++) {
            items[i] = labels[i] + " 모드";
        }
        items[BattleModes.TIME_LIMIT_INDEX] += " (" + GameHud.formatTime(BattleModes.TIME_LIMIT_SECONDS * 1000L) + ")";
        items[BACK_INDEX] = "뒤로";
        return items;
    }

    private void move(int step) {
        selectedIndex = Math.floorMod(selectedIndex + step, BACK_INDEX + 1);
        view.setSelectedIndex(selectedIndex);
    }

    private void select() {
        if (selectedIndex == BACK_INDEX) {
            back();
            return;
        }
        // 2P가 Enter로 하드 드롭하므로, 선택에 쓴 Enter가 대전 화면으로 넘어가지 않게 창을 먼저 닫는다
        dispose();
        new LocalBattleScreen(BattleModes.settings(selectedIndex)).setVisible(true);
    }

    private void back() {
        dispose();
        new BattleMenuScreen().setVisible(true);
    }
}
