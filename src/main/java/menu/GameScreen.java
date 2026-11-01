package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import app.AppState;
import app.AppStateManager;
import blocks.core.Block;
import blocks.core.BlockFactory;
import board.Board;
import difficulty.Difficulty;
import game.ActionController;
import game.BlockPreviewPanel;
import game.GameController;
import game.GameLoop;
import game.GameMode;
import game.GamePanel;
import game.GameStateManager;
import menu.settings.AppSettings;
import menu.settings.SettingsScreen;
import score.ScoreManager;

// 게임 화면(보드 + 사이드 정보 패널)을 담는 창
public class GameScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int RENDER_INTERVAL_MS = 50;

    private final GameStateManager gameStateManager;
    private final AppStateManager appStateManager;
    private final ActionController actionController;
    private final ScoreManager scoreManager;
    private final GameLoop gameLoop;
    private final GameController gameController;
    private final GamePanel gamePanel;
    private final Thread gameThread;
    private final Timer renderTimer;
    private final BlockPreviewPanel blockPreviewPanel;
    private final BlockPreviewPanel holdPreviewPanel;
    private final Difficulty difficulty;
    private final GameMode mode;

    private PauseScreen pauseScreen;

    private JLabel levelLabel;
    private JLabel linesLabel;
    private JLabel scoreLabel;
    private JLabel difficultyLabel;
    private JLabel modeLabel;

    private boolean gameOverHandled = false;
    private boolean closed = false;

    public GameScreen(GameMode mode) {
        super("TFSE");
        ui.AppIcon.apply(this);
        this.mode = mode;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Board board = new Board();
        difficulty = AppSettings.getInstance().getDifficulty();

        gameStateManager = new GameStateManager();
        gameStateManager.setDifficulty(difficulty);
        appStateManager = new AppStateManager(AppState.PLAYING);

        Block firstBlock = BlockFactory.createRandomBlock(difficulty);
        scoreManager = new ScoreManager(difficulty);

        actionController = new ActionController(
                board,
                firstBlock,
                gameStateManager,
                scoreManager,
                difficulty,
                mode,
                ActionController.CLEAR_ANIMATION_MS
        );

        gameLoop = new GameLoop(
                actionController,
                gameStateManager,
                scoreManager
        );

        // 같은 시점의 게임 상태로 보드, NEXT, HOLD를 초기화
        ActionController.ViewSnapshot initialFrame =
                actionController.getViewSnapshot();

        gamePanel = new GamePanel(board);
        gamePanel.setSnapshot(initialFrame);

        blockPreviewPanel = new BlockPreviewPanel();
        blockPreviewPanel.setSnapshot(initialFrame.nextBlock());

        // FR-50: 홀드 블록 미리보기
        holdPreviewPanel = new BlockPreviewPanel();
        holdPreviewPanel.setSnapshot(initialFrame.heldBlock());

        gameController = new GameController(
                gameLoop,
                actionController,
                gameStateManager,
                scoreManager,
                appStateManager,
                () -> {
                    SettingsScreen settingsScreen = new SettingsScreen(
                            GameScreen.this,
                            () -> {},
                            () -> {
                                stopGame();
                                dispose();

                                StartMenu startMenu = new StartMenu();
                                startMenu.setVisible(true);
                            },
                            true,
                            this::suppressEscapeUntilReleased
                    );

                    settingsScreen.setVisible(true);

                    if (closed) {
                        return;
                    }

                    refreshInputBindings();
                    gamePanel.refreshCellSize();
                    pack();
                },
                this::showPauseScreen,
                this::exitApplication
        );

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        setFocusable(true);
        gameController.installKeyBindings(getRootPane());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
                gameThread.start();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopGame();
                dispose();
                System.exit(0);
            }
        });

        gameThread = new Thread(gameLoop, "game-loop");
        gameThread.setDaemon(true);

        // 게임 진행은 GameLoop가 담당하고, 화면은 주기적으로 갱신
        renderTimer = new Timer(RENDER_INTERVAL_MS, e -> onTick());
        renderTimer.start();
    }

    // 주기적으로 상태를 화면에 반영하고, 게임 오버 시 결과 화면으로 전환
    private void onTick() {
        ActionController.ViewSnapshot frame =
                actionController.getViewSnapshot();

        gamePanel.setSnapshot(frame);
        blockPreviewPanel.setSnapshot(frame.nextBlock());
        holdPreviewPanel.setSnapshot(frame.heldBlock());

        levelLabel.setText("LEVEL: " + frame.level());
        linesLabel.setText("LINES: " + frame.lines());
        scoreLabel.setText("SCORE: " + frame.score());

        if (frame.gameOver() && !gameOverHandled) {
            gameOverHandled = true;

            stopGame();
            dispose();

            GameOverScreen gameOverScreen = new GameOverScreen(
                    frame.score(),
                    difficulty.name(),
                    mode.name()
            );
            gameOverScreen.setVisible(true);
        }
    }

    // 사이드 패널
    private JPanel createSidePanel() {
        JPanel side = new JPanel();
        side.setBackground(Color.BLACK);
        side.setPreferredSize(new Dimension(170, 0));
        side.setBorder(
                BorderFactory.createEmptyBorder(20, 15, 20, 15)
        );

        // 각 항목이 필요한 높이만 차지하도록 세로 배치
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

        modeLabel = createInfoLabel("MODE: " + mode.name());
        difficultyLabel = createInfoLabel("DIFF: " + difficulty.name());
        levelLabel = createInfoLabel(
                "LEVEL: " + gameStateManager.getCurrentLevel()
        );
        linesLabel = createInfoLabel(
                "LINES: " + gameStateManager.getTotalLinesCleared()
        );
        scoreLabel = createInfoLabel("SCORE: " + scoreManager.getScore());

        blockPreviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        blockPreviewPanel.setMaximumSize(
                blockPreviewPanel.getPreferredSize()
        );

        holdPreviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        holdPreviewPanel.setMaximumSize(
                holdPreviewPanel.getPreferredSize()
        );

        side.add(modeLabel);
        side.add(Box.createVerticalStrut(8));
        side.add(difficultyLabel);
        side.add(Box.createVerticalStrut(15));

        side.add(createInfoLabel("NEXT"));
        side.add(Box.createVerticalStrut(4));
        side.add(blockPreviewPanel);
        side.add(Box.createVerticalStrut(12));

        side.add(createInfoLabel("HOLD"));
        side.add(Box.createVerticalStrut(4));
        side.add(holdPreviewPanel);
        side.add(Box.createVerticalStrut(15));

        side.add(levelLabel);
        side.add(Box.createVerticalStrut(8));
        side.add(linesLabel);
        side.add(Box.createVerticalStrut(8));
        side.add(scoreLabel);

        return side;
    }

    // 사이드 패널 정보 라벨 공통 생성
    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Courier", Font.PLAIN, 16));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void stopGame() {
        closed = true;
        renderTimer.stop();
        gameController.shutdown();
        actionController.shutdown();
        gameThread.interrupt();
    }

    private void refreshInputBindings() {
        gameController.installKeyBindings(getRootPane());
        requestFocusInWindow();
    }

    private void suppressEscapeUntilReleased() {
        gameController.suppressEscapeUntilReleased();
    }

    // 일시정지 화면 표시
    private void showPauseScreen() {
        if (pauseScreen != null && pauseScreen.isVisible()) {
            if (!gameLoop.isPaused()) {
                goToGame();
            }
            return;
        }

        pauseScreen = new PauseScreen(
                this::goToGame,
                this::goToStartMenu,
                this::exitApplication
        );

        remove(gamePanel);
        add(pauseScreen, BorderLayout.CENTER);
        revalidate();
        repaint();
        pauseScreen.requestFocusInWindow();
    }

    // 게임 화면으로 돌아가기
    private void goToGame() {
        if (pauseScreen != null) {
            remove(pauseScreen);
            add(gamePanel, BorderLayout.CENTER);
            revalidate();
            repaint();
            pauseScreen = null;
        }

        gameController.resumeGame();
        requestFocusInWindow();
    }

    // 시작 화면으로 돌아가기
    private void goToStartMenu() {
        stopGame();

        if (pauseScreen != null) {
            remove(pauseScreen);
            pauseScreen = null;
        }

        dispose();

        StartMenu startMenu = new StartMenu();
        startMenu.setVisible(true);
    }

    // 게임을 정리한 뒤 프로그램 종료
    private void exitApplication() {
        stopGame();
        dispose();
        System.exit(0);
    }
}