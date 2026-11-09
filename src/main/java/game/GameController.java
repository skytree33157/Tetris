package game;

import app.AppState;
import app.AppStateManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import score.ScoreManager;

/**
 * 키 바인딩과 Swing 타이머는 EDT에서 생성/설치/종료한다.
 * 경기 상태 변경은 ActionController에 위임한다.
 */
public class GameController extends KeyAdapter {

    static final int ESCAPE_HOLD_THRESHOLD_MS = 1000;
    private static final AtomicLong IDS = new AtomicLong();

    /**
     * 대전에서는 서로 다른 키 구성을 전달한다.
     * P와 ESC는 경기 공통 키이며, hold가 0이면 홀드를 비활성화한다.
     */
    public record KeyBindings(
            int left, int right, int down,
            int rotate, int hardDrop, int hold) {

        // 기존 대전 코드의 5개 키 생성 방식 유지
        public KeyBindings(
                int left, int right, int down,
                int rotate, int hardDrop) {
            this(left, right, down, rotate, hardDrop, 0);
        }

        public KeyBindings {
            Set<Integer> unique = new HashSet<>();

            for (int key : new int[]{left, right, down, rotate, hardDrop}) {
                if (key <= 0
                        || key == KeyEvent.VK_P
                        || key == KeyEvent.VK_ESCAPE
                        || !unique.add(key)) {
                    throw new IllegalArgumentException(
                            "Movement keys must be distinct and cannot use P/ESC"
                    );
                }
            }

            if (hold != 0) {
                if (hold < 0
                        || hold == KeyEvent.VK_P
                        || hold == KeyEvent.VK_ESCAPE
                        || !unique.add(hold)) {
                    throw new IllegalArgumentException(
                            "Hold key must be distinct and cannot use P/ESC"
                    );
                }
            }
        }

        static KeyBindings fromSettings() {
            AppSettings settings = AppSettings.getInstance();

            return new KeyBindings(
                    settings.getKey(KeyAction.MOVE_LEFT),
                    settings.getKey(KeyAction.MOVE_RIGHT),
                    settings.getKey(KeyAction.MOVE_DOWN),
                    settings.getKey(KeyAction.ROTATE),
                    KeyEvent.VK_SPACE,
                    settings.getKey(KeyAction.HOLD)
            );
        }

        Set<Integer> codes() {
            Set<Integer> result =
                    new HashSet<>(Set.of(left, right, down, rotate, hardDrop));

            if (hold != 0) {
                result.add(hold);
            }

            return result;
        }
    }

    private final GameLoop gameLoop;
    private final ActionController actionController;
    private final GameStateManager gameStateManager;
    private final AppStateManager appStateManager;
    private final Runnable pauseHandler;
    private final Runnable openSettingAction;
    private final Runnable exitHandler;
    private final KeyBindings fixedBindings;
    private final boolean commonKeys;

    private final String bindingPrefix =
            "game.key." + IDS.incrementAndGet() + ".";

    private KeyBindings bindings;
    private JRootPane installedRoot;

    private final Set<Integer> boundKeyCodes = new HashSet<>();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> blockedUntilRelease = new HashSet<>();

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean downPressed;
    private int lastHorizontalDirection;

    private final Timer keyTimer;
    private final Timer escapeHoldTimer;

    private boolean escapePressed;
    private boolean escapeHoldTriggered;
    private boolean ignoreEscapeUntilReleased;
    private boolean stopped;
    private long seenPauseRevision;

    public GameController(
            GameLoop loop,
            ActionController action,
            GameStateManager state,
            ScoreManager score,
            AppStateManager app,
            Runnable settings,
            Runnable pause,
            Runnable exit) {

        this(loop, action, state, score, app,
                settings, pause, exit, null, true);
    }

    /** 같은 창에서는 commonKeys=true를 한 플레이어에게만 지정한다. */
    public GameController(
            GameLoop loop,
            ActionController action,
            GameStateManager state,
            ScoreManager score,
            AppStateManager app,
            Runnable settings,
            Runnable pause,
            Runnable exit,
            KeyBindings bindings,
            boolean commonKeys) {

        this.gameLoop = loop;
        this.actionController = action;
        this.gameStateManager = state;
        this.appStateManager = app;
        this.openSettingAction = settings;
        this.pauseHandler = pause;
        this.exitHandler = exit;
        this.fixedBindings = bindings;
        this.bindings = bindings == null
                ? KeyBindings.fromSettings()
                : bindings;
        this.commonKeys = commonKeys;

        seenPauseRevision = action.pauseRevision();

        keyTimer = new Timer(60, e -> responseInput());
        keyTimer.setInitialDelay(250);

        escapeHoldTimer = new Timer(ESCAPE_HOLD_THRESHOLD_MS, e -> {
            refreshPauseRevision();

            if (!stopped
                    && !action.isStopped()
                    && escapePressed
                    && !ignoreEscapeUntilReleased) {
                escapeHoldTriggered = true;
                exitHandler.run();
            }
        });

        escapeHoldTimer.setRepeats(false);
    }

    public void shutdown() {
        stopped = true;
        resetHeldInput();
        removeBindings();
    }

    private String actionName(int code, boolean released) {
        return bindingPrefix
                + (released ? "released." : "pressed.")
                + code;
    }

    private void removeBindings() {
        if (installedRoot == null) {
            return;
        }

        InputMap inputs =
                installedRoot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = installedRoot.getActionMap();

        for (int code : boundKeyCodes) {
            for (boolean released : new boolean[]{false, true}) {
                KeyStroke stroke =
                        KeyStroke.getKeyStroke(code, 0, released);
                String name = actionName(code, released);

                if (name.equals(inputs.get(stroke))) {
                    inputs.remove(stroke);
                }

                actions.remove(name);
            }
        }

        boundKeyCodes.clear();
        installedRoot = null;
    }

    public void installKeyBindings(JRootPane root) {
        if (stopped) {
            return;
        }

        KeyBindings next = fixedBindings == null
                ? KeyBindings.fromSettings()
                : fixedBindings;

        Set<Integer> codes = new HashSet<>(next.codes());

        if (commonKeys) {
            codes.add(KeyEvent.VK_P);
            codes.add(KeyEvent.VK_ESCAPE);
        }

        InputMap inputs =
                root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        // 다른 플레이어의 키를 덮어쓰지 않도록 먼저 검사
        for (int code : codes) {
            for (boolean released : new boolean[]{false, true}) {
                Object owner = inputs.get(
                        KeyStroke.getKeyStroke(code, 0, released)
                );

                if (owner instanceof String name
                        && name.startsWith("game.key.")
                        && !name.startsWith(bindingPrefix)) {
                    throw new IllegalArgumentException(
                            "Key already belongs to another player: " + code
                    );
                }
            }
        }

        removeBindings();

        bindings = next;
        installedRoot = root;
        boundKeyCodes.addAll(codes);

        for (int code : codes) {
            for (boolean released : new boolean[]{false, true}) {
                String name = actionName(code, released);

                inputs.put(
                        KeyStroke.getKeyStroke(code, 0, released),
                        name
                );

                root.getActionMap().put(name, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        if (released) {
                            handleKeyReleased(code);
                        } else {
                            handleKeyPressed(code);
                        }
                    }
                });
            }
        }

        resetHeldInput();
    }

    private void resetHeldInput() {
        blockedUntilRelease.addAll(pressedKeys);
        blockedUntilRelease.remove(KeyEvent.VK_P);
        blockedUntilRelease.remove(KeyEvent.VK_ESCAPE);

        pressedKeys.removeIf(
                code -> code != KeyEvent.VK_P
                        && code != KeyEvent.VK_ESCAPE
        );

        leftPressed = false;
        rightPressed = false;
        downPressed = false;
        lastHorizontalDirection = 0;

        keyTimer.stop();
        escapeHoldTimer.stop();
        escapePressed = false;
        escapeHoldTriggered = false;
    }

    private void refreshPauseRevision() {
        long revision = actionController.pauseRevision();

        if (seenPauseRevision != revision) {
            resetHeldInput();
            seenPauseRevision = revision;
        }
    }

    private void responseInput() {
        if (stopped || actionController.isStopped()) {
            shutdown();
            return;
        }

        synchronized (actionController.stateLock()) {
            refreshPauseRevision();

            if (gameLoop.isPaused()
                    || actionController.isStopped()
                    || gameStateManager.isGameOver()) {
                return;
            }

            if (leftPressed
                    && (!rightPressed || lastHorizontalDirection < 0)) {
                actionController.moveLeftAction();
            } else if (rightPressed) {
                actionController.moveRightAction();
            }

            if (downPressed) {
                actionController.softDropAction();
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        handleKeyPressed(event.getKeyCode());
    }

    private void handleKeyPressed(int code) {
        refreshPauseRevision();

        if (stopped || actionController.isStopped()) {
            shutdown();
            return;
        }

        boolean common =
                code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE;

        if ((common && !commonKeys)
                || (!common && !bindings.codes().contains(code))) {
            return;
        }

        if (code == KeyEvent.VK_ESCAPE && ignoreEscapeUntilReleased) {
            return;
        }

        if (blockedUntilRelease.contains(code) || !pressedKeys.add(code)) {
            return;
        }

        if (gameStateManager.isGameOver()) {
            return;
        }

        if (code == KeyEvent.VK_P) {
            togglePauseMenu();
            return;
        }

        if (code == KeyEvent.VK_ESCAPE) {
            escapePressed = true;
            escapeHoldTriggered = false;
            escapeHoldTimer.restart();
            return;
        }

        synchronized (actionController.stateLock()) {
            refreshPauseRevision();

            if (blockedUntilRelease.contains(code)
                    || actionController.isStopped()) {
                return;
            }

            if (gameLoop.isPaused()) {
                blockedUntilRelease.add(code);
                return;
            }

            if (code == bindings.left()) {
                leftPressed = true;
                lastHorizontalDirection = -1;
                actionController.moveLeftAction();
                keyTimer.restart();
            } else if (code == bindings.right()) {
                rightPressed = true;
                lastHorizontalDirection = 1;
                actionController.moveRightAction();
                keyTimer.restart();
            } else if (code == bindings.down()) {
                downPressed = true;
                actionController.softDropAction();
                keyTimer.restart();
            } else if (code == bindings.rotate()) {
                actionController.rotateAction();
            } else if (code == bindings.hold()) {
                actionController.holdAction();
            } else if (code == bindings.hardDrop()) {
                actionController.hardDropWithScoreAction();
            }
        }
    }

    public void resumeGame() {
        if (stopped || actionController.isStopped()) {
            return;
        }

        actionController.setPaused(false);
        refreshPauseRevision();
        appStateManager.transitionTo(AppState.PLAYING);
    }

    @Override
    public void keyReleased(KeyEvent event) {
        handleKeyReleased(event.getKeyCode());
    }

    private void handleKeyReleased(int code) {
        refreshPauseRevision();

        if (stopped || actionController.isStopped()) {
            shutdown();
            return;
        }

        pressedKeys.remove(code);
        blockedUntilRelease.remove(code);

        if (code == KeyEvent.VK_ESCAPE && commonKeys) {
            if (ignoreEscapeUntilReleased) {
                ignoreEscapeUntilReleased = false;
                return;
            }

            if (!escapePressed) {
                return;
            }

            escapePressed = false;
            escapeHoldTimer.stop();

            if (!escapeHoldTriggered && !gameStateManager.isGameOver()) {
                togglePauseMenu();
            }

            escapeHoldTriggered = false;
            return;
        }

        if (code == bindings.left()) {
            leftPressed = false;
        } else if (code == bindings.right()) {
            rightPressed = false;
        } else if (code == bindings.down()) {
            downPressed = false;
        }

        if (!leftPressed && !rightPressed && !downPressed) {
            keyTimer.stop();
        }
    }

    public void suppressEscapeUntilReleased() {
        ignoreEscapeUntilReleased = true;
    }

    // P와 ESC 짧게 누르기가 공유하는 일시정지 메뉴 열기/닫기
    private void togglePauseMenu() {
        gameLoop.togglePause();
        refreshPauseRevision();

        appStateManager.transitionTo(
                gameLoop.isPaused() ? AppState.PAUSED : AppState.PLAYING
        );

        pauseHandler.run();
    }

    // 일시정지 메뉴의 '설정' 버튼에서 호출
    public void openSettings() {
        boolean wasPaused;

        synchronized (actionController.stateLock()) {
            wasPaused = gameLoop.isPaused();
            actionController.setPaused(true);
        }

        refreshPauseRevision();
        resetHeldInput();
        appStateManager.transitionTo(AppState.SETTINGS);

        try {
            openSettingAction.run();
        } finally {
            if (!stopped && !actionController.isStopped()) {
                actionController.setPaused(wasPaused);
                refreshPauseRevision();

                appStateManager.transitionTo(
                        wasPaused ? AppState.PAUSED : AppState.PLAYING
                );
            }
        }
    }
}