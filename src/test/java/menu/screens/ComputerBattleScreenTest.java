package menu.screens;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ComputerBattleScreenTest {

    private Set<Window> existingWindows;

    @BeforeEach
    void recordExistingWindows() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing 창을 띄울 수 있는 환경에서 실행");
        SwingUtilities.invokeAndWait(() -> existingWindows = Set.of(Window.getWindows()));
    }

    @AfterEach
    void disposeTestWindows() throws Exception {
        if (existingWindows != null) {
            SwingUtilities.invokeAndWait(() -> Arrays.stream(Window.getWindows())
                    .filter(window -> !existingWindows.contains(window))
                    .forEach(Window::dispose));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "컴퓨터 대전, 일반 모드, 일반 모드, CPU",
        "컴퓨터 대전, 아이템 모드, 아이템 모드, CPU",
        "컴퓨터 대전, 시간 제한 모드 (03:00), 시간 제한 모드, CPU",
        "로컬 대전, 일반 모드, 일반 모드, 2P"
    })
    void menuOpensSelectedModeAgainstSelectedOpponent(
            String route, String option, String mode, String opponent) throws Exception {
        SwingUtilities.invokeAndWait(() -> assertBattle(openMatch(route, option), mode, opponent));
    }

    @Test
    void computerRematchPreservesModeFromPauseAndResultScreens() throws Exception {
        CountDownLatch resultOpened = new CountDownLatch(1);
        AWTEventListener listener = event -> {
            if (event instanceof WindowEvent change && change.getID() == WindowEvent.WINDOW_OPENED
                    && change.getWindow() instanceof BattleResultScreen
                    && !existingWindows.contains(change.getWindow())) {
                resultOpened.countDown();
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.WINDOW_EVENT_MASK);

        try {
            LocalBattleScreen[] first = new LocalBattleScreen[1];
            SwingUtilities.invokeAndWait(() -> first[0] = openMatch("컴퓨터 대전", "시간 제한 모드 (03:00)"));
            SwingUtilities.invokeAndWait(() -> {
                LocalBattleScreen game = first[0];
                var root = game.getRootPane();
                Object pause = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0));
                root.getActionMap().get(pause).actionPerformed(new ActionEvent(root, 0, "pause"));
                assertTrue(game.getGlassPane().isVisible());

                click(game.getGlassPane(), "다시 시작");
                assertFalse(game.isDisplayable());
                LocalBattleScreen replay = showing(LocalBattleScreen.class);
                assertNotSame(game, replay);
                assertBattle(replay, "시간 제한 모드", "CPU");
            });

            SwingUtilities.invokeAndWait(() -> {
                LocalBattleScreen game = showing(LocalBattleScreen.class);
                // 2P 하드 드롭 키로 CPU를 조작할 수 없어야 한다.
                for (int attempt = 0; attempt < 30; attempt++) {
                    pressKey(game, KeyEvent.VK_ENTER, '\n');
                }
                // 같은 자리에서 계속 고정해 1P만 게임 오버 상태로 만든다.
                for (int attempt = 0; attempt < 30; attempt++) {
                    pressKey(game, KeyEvent.VK_SPACE, ' ');
                }
            });
            assertTrue(resultOpened.await(5, TimeUnit.SECONDS), "게임 오버 후 결과 화면이 열려야 한다");

            SwingUtilities.invokeAndWait(() -> {
                BattleResultScreen result = showing(BattleResultScreen.class);
                assertTrue(containsLabel(result, "CPU 승리"));
                click(result, "재경기");
                assertFalse(result.isDisplayable());
                assertBattle(showing(LocalBattleScreen.class), "시간 제한 모드", "CPU");
            });
        } finally {
            Toolkit.getDefaultToolkit().removeAWTEventListener(listener);
        }
    }

    private LocalBattleScreen openMatch(String route, String option) {
        BattleMenuScreen menu = new BattleMenuScreen();
        menu.setVisible(true);
        click(menu, route);
        LocalBattleSetupScreen setup = showing(LocalBattleSetupScreen.class);
        click(setup, option);
        return showing(LocalBattleScreen.class);
    }

    private static void assertBattle(LocalBattleScreen game, String mode, String opponent) {
        assertTrue(containsLabel(game, mode), "선택한 대전 모드: " + mode);
        assertTrue(containsLabel(game, "1P"), "사람 슬롯 1P");
        assertTrue(containsLabel(game, opponent), "선택한 상대: " + opponent);
        assertFalse(containsLabel(game, opponent.equals("CPU") ? "2P" : "CPU"));
    }

    private static void click(Component root, String label) {
        find(root, JButton.class, button -> label.equals(button.getText()))
                .orElseThrow(() -> new AssertionError("메뉴 버튼 없음: " + label))
                .doClick(0);
    }

    private static boolean containsLabel(Component root, String label) {
        return find(root, JLabel.class, item -> label.equals(item.getText())).isPresent();
    }

    private static <T extends Component> Optional<T> find(Component root, Class<T> type, Predicate<T> match) {
        if (type.isInstance(root) && match.test(type.cast(root))) {
            return Optional.of(type.cast(root));
        }
        if (root instanceof Container parent) {
            for (Component child : parent.getComponents()) {
                Optional<T> found = find(child, type, match);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private <T extends Window> T showing(Class<T> type) {
        return Arrays.stream(Window.getWindows())
                .filter(window -> !existingWindows.contains(window) && window.isShowing())
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("대전 화면이 열리지 않음: " + type.getSimpleName()));
    }

    private static void pressKey(LocalBattleScreen game, int code, char character) {
        long now = System.currentTimeMillis();
        KeyEvent press = new KeyEvent(game, KeyEvent.KEY_PRESSED, now, 0, code, character);
        KeyEvent release = new KeyEvent(game, KeyEvent.KEY_RELEASED, now, 0, code, character);
        Arrays.stream(game.getKeyListeners()).forEach(listener -> {
            listener.keyPressed(press);
            listener.keyReleased(release);
        });
    }
}
