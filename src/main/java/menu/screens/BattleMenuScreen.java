package menu.screens;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import app.AppState;
import app.AppStateManager;
import menu.views.StartMenuView;
import sound.SoundManager;

// 대전 메뉴: 로컬 대전 / 온라인 방 만들기 / 온라인 참여하기
public class BattleMenuScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int LOCAL_INDEX = 0;
    static final int HOST_INDEX = 1;
    static final int JOIN_INDEX = 2;
    static final int BACK_INDEX = 3;

    private static final String[] MENU_ITEMS = {
        "로컬 대전 (준비 중)",
        "온라인 방 만들기",
        "온라인 참여하기",
        "뒤로"
    };

    // 로컬 2인 대전 화면이 생기면 true로 바꾼다
    private static final boolean LOCAL_BATTLE_AVAILABLE = false;

    private final StartMenuView view;
    private final AppStateManager stateManager = new AppStateManager(AppState.BATTLE_MENU);
    private int selectedIndex;

    public BattleMenuScreen() {
        super("TFSE - 대전");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        view = new StartMenuView("BATTLE", "친구와 한 판 겨뤄 보세요", false, MENU_ITEMS, 3, index -> {
            selectedIndex = index;
            select();
        });
        view.setHint("↑ ↓ 선택   Enter 확인   Esc 뒤로");
        view.setItemEnabled(LOCAL_INDEX, isEnabledItem(LOCAL_INDEX));
        setContentPane(view);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        selectedIndex = nextEnabled(-1, 1);
        view.setSelectedIndex(selectedIndex);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> move(-1);
                    case KeyEvent.VK_DOWN -> move(1);
                    case KeyEvent.VK_ENTER -> select();
                    case KeyEvent.VK_ESCAPE -> backToStart();
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
                backToStart();
            }
        });
    }

    static boolean isEnabledItem(int index) {
        return index != LOCAL_INDEX || LOCAL_BATTLE_AVAILABLE;
    }

    // 비활성 항목을 건너뛰며 다음 선택 위치를 구한다 (위아래 끝에서 순환)
    static int nextEnabled(int from, int step) {
        int index = from;
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            index = Math.floorMod(index + step, MENU_ITEMS.length);
            if (isEnabledItem(index)) {
                return index;
            }
        }
        return from;
    }

    private void move(int step) {
        selectedIndex = nextEnabled(selectedIndex, step);
        view.setSelectedIndex(selectedIndex);
    }

    private void select() {
        if (!isEnabledItem(selectedIndex)) {
            return;
        }
        switch (selectedIndex) {
            case HOST_INDEX -> open(new RoomHostScreen());
            case JOIN_INDEX -> open(new RoomJoinScreen());
            case BACK_INDEX -> backToStart();
            default -> { }
        }
    }

    private void open(JFrame next) {
        dispose();
        next.setVisible(true);
    }

    private void backToStart() {
        stateManager.transitionTo(AppState.START_MENU);
        open(new StartMenuScreen());
    }
}
