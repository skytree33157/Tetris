package menu.screens;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import menu.BattleResult;
import menu.views.BattleResultView;

// 대전 결과 화면: 승패와 양쪽 기록을 보여주고 대전 메뉴/시작 메뉴로 이동.
// 네트워크 대전이면 대기실로 돌아가는 메뉴를 추가로 보여준다.
public class BattleResultScreen extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int BATTLE_MENU_INDEX = 0;
    static final int START_MENU_INDEX = 1;
    private static final int P2P_LOBBY_INDEX = 0;
    private static final String[] MENU_ITEMS = {"대전 메뉴", "시작 메뉴"};
    private static final String[] P2P_MENU_ITEMS = {"대기실로", "대전 메뉴"};

    private final BattleResultView view;
    private final String[] menuItems;
    // 네트워크 대전 전용: 대기실로 돌아가기 / 대전을 떠나기 (로컬 대전이면 null)
    private final Runnable onLobby;
    private final Runnable onLeave;
    private int selectedIndex = BATTLE_MENU_INDEX;

    public BattleResultScreen(BattleResult result) {
        this(result, null, null);
    }

    public BattleResultScreen(BattleResult result, Runnable onLobby, Runnable onLeave) {
        super("TFSE - 대전 결과");
        ui.AppIcon.apply(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.onLobby = onLobby;
        this.onLeave = onLeave;
        this.menuItems = onLobby == null ? MENU_ITEMS : P2P_MENU_ITEMS;

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
                    case KeyEvent.VK_ESCAPE -> escape();
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
                escape();
            }
        });
    }

    // 버튼 선택 위치를 한 칸 옮긴다 (양 끝에서 순환)
    static int next(int from, int step) {
        return Math.floorMod(from + step, MENU_ITEMS.length);
    }

    private void move(int step) {
        selectedIndex = next(selectedIndex, step);
        view.setSelectedIndex(selectedIndex);
    }

    private void select() {
        if (onLobby != null) {
            if (selectedIndex == P2P_LOBBY_INDEX) {
                dispose();
                onLobby.run();
            } else {
                leaveToBattleMenu();
            }
            return;
        }
        if (selectedIndex == START_MENU_INDEX) {
            open(new StartMenuScreen());
        } else {
            open(new BattleMenuScreen());
        }
    }

    // ESC/닫기: 네트워크 대전이면 대기실로, 로컬 대전이면 대전 메뉴로 돌아간다
    private void escape() {
        if (onLobby != null) {
            dispose();
            onLobby.run();
        } else {
            open(new BattleMenuScreen());
        }
    }

    // 네트워크 대전을 떠나 대전 메뉴로 나간다 (세션을 먼저 닫는다)
    private void leaveToBattleMenu() {
        if (onLeave != null) {
            onLeave.run();
        }
        open(new BattleMenuScreen());
    }

    private void open(JFrame next) {
        dispose();
        next.setVisible(true);
    }
}
