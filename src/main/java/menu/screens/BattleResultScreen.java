package menu.screens;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Supplier;

import javax.swing.JFrame;

import menu.BattleResult;
import menu.views.BattleResultView;

// 대전 결과 화면: 승패와 양쪽 기록을 보여주고 대전 메뉴/시작 메뉴로 이동
public class BattleResultScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int BATTLE_MENU_INDEX = 0;
    static final int START_MENU_INDEX = 1;
    private static final int REPLAY_INDEX = 2;
    private static final String[] MENU_ITEMS = {"대전 메뉴", "시작 메뉴"};
    private static final String[] COMPUTER_MENU_ITEMS = {"대전 메뉴", "시작 메뉴", "재경기"};

    private final BattleResultView view;
    private final Supplier<JFrame> rematch;
    private final String[] menuItems;
    private int selectedIndex = BATTLE_MENU_INDEX;

    public BattleResultScreen(BattleResult result) {
        this(result, null);
    }

    BattleResultScreen(BattleResult result, Supplier<JFrame> rematch) {
        super("TFSE - 대전 결과");
        this.rematch = rematch;
        menuItems = rematch == null ? MENU_ITEMS : COMPUTER_MENU_ITEMS;
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        view = new BattleResultView(result, menuItems, index -> {
            selectedIndex = index;
            select();
        });
        view.setSelectedIndex(selectedIndex);
        setContentPane(view);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_UP -> move(-1);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN -> move(1);
                    case KeyEvent.VK_ENTER -> select();
                    case KeyEvent.VK_ESCAPE -> open(new BattleMenuScreen());
                    default -> { }
                }
            }
        });
        setFocusable(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                open(new BattleMenuScreen());
            }
        });
    }

    // 버튼 선택 위치를 한 칸 옮긴다 (양 끝에서 순환)
    static int next(int from, int step) {
        return next(from, step, MENU_ITEMS.length);
    }

    private static int next(int from, int step, int itemCount) {
        return Math.floorMod(from + step, itemCount);
    }

    private void move(int step) {
        selectedIndex = next(selectedIndex, step, menuItems.length);
        view.setSelectedIndex(selectedIndex);
    }

    private void select() {
        if (selectedIndex == REPLAY_INDEX && rematch != null) {
            open(rematch.get());
        } else if (selectedIndex == START_MENU_INDEX) {
            open(new StartMenuScreen());
        } else {
            open(new BattleMenuScreen());
        }
    }

    private void open(JFrame next) {
        dispose();
        next.setVisible(true);
    }
}
