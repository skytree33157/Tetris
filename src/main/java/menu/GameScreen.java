package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

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
    private P2PGameController p2pController;
    private volatile boolean gameStartAllowed = false;  // P2P 모드에서 onStarted() 대기용
    private volatile Runnable onGameOverCallback;  // P2P 게임 오버 시 호출할 콜백

    private JLabel levelLabel;
    private JLabel linesLabel;
    private JLabel scoreLabel;
    private JLabel difficultyLabel;
    private JLabel modeLabel;
    private JLabel connectionStatusLabel;
    private boolean gameOverHandled = false;
    private boolean closed = false;

    public GameScreen(GameMode mode) {
        this(mode, null);
    }

    public GameScreen(GameMode mode, P2PGameController p2pController) {
        super("TFSE");
        ui.AppIcon.apply(this);
        this.mode = mode;
        this.p2pController = p2pController;

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
                // P2P 모드: onStarted() 콜백을 기다림
                // 단일 플레이어 모드: 즉시 게임 시작
                if (p2pController == null) {
                    gameStartAllowed = true;
                    gameThread.start();
                }
                // P2P 모드는 onStarted() 콜백 이후에 시작됨
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

        // P2P 게임 상태 전송
        if (p2pController != null && p2pController.isRoundActive()) {
            try {
                p2pController.sendGameStatus();
            } catch (IOException exception) {
                // 연결 끊김은 P2PGameSession의 리스너에서 처리
                exception.printStackTrace();
            }
            
            // 상대 플레이어 게임 오버 감지
            network.GameStatus remoteStatus = p2pController.getRemoteStatus();
            if (remoteStatus != null && remoteStatus.isGameOver() && !gameOverHandled) {
                gameOverHandled = true;
                stopGame();
                dispose();
                
                // P2P 게임 오버 콜백 호출 (session 정리 등)
                if (onGameOverCallback != null) {
                    onGameOverCallback.run();
                }
                
                GameOverScreen gameOverScreen = new GameOverScreen(
                        frame.score(),
                        difficulty.name(),
                        mode.name());
                gameOverScreen.setVisible(true);
                return;
            }
        }

        // 로컬 게임 오버 처리
        if (frame.gameOver() && !gameOverHandled) {
            gameOverHandled = true;

            // P2P 게임이면 게임 오버를 상대에게 통지
            if (p2pController != null) {
                try {
                    p2pController.notifyGameOver();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }

            stopGame();
            dispose();

            // P2P 게임 오버 콜백 호출 (session 정리 등)
            if (onGameOverCallback != null) {
                onGameOverCallback.run();
            }

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

    public void stopGame() {
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
    
    // 연결 상태를 상단 바에 보여줌 (정상 제외)
    public void showConnectionStatus(String message, boolean isWarning) {
        if (connectionStatusLabel == null) {
            connectionStatusLabel = new JLabel(message);
            connectionStatusLabel.setFont(new Font("Courier", Font.BOLD, 14));
            if (isWarning) {
                connectionStatusLabel.setForeground(Color.YELLOW);
            } else {
                connectionStatusLabel.setForeground(Color.RED);
            }
            connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        } else {
            connectionStatusLabel.setText(message);
            if (isWarning) {
                connectionStatusLabel.setForeground(Color.YELLOW);
            } else {
                connectionStatusLabel.setForeground(Color.RED);
            }
        }
        
        // 상단에 상태 레이블 추가 (이미 추가되었다면 업데이트만)
        if (connectionStatusLabel.getParent() == null) {
            add(connectionStatusLabel, BorderLayout.NORTH);
            revalidate();
            repaint();
        }
    }
    
    // 정상 상태로 복구 시 상단 바 제거
    public void hideConnectionStatus() {
        if (connectionStatusLabel != null && connectionStatusLabel.getParent() != null) {
            remove(connectionStatusLabel);
            revalidate();
            repaint();
        }
    }

//------------------------------------------------------------------
    // 상대 플레이어의 게임 상태 업데이트
    public void updateRemoteStatus(network.GameStatus status) {
// 상대 플레이어 정보 표시
// 향후 UI 확장 시 상대 보드, 점수 등을 표시할 수 있음
        if (status != null) {
// 현재는 게임 화면에 표시하지 않음
// 필요시 별도 패널에 상대 플레이어 정보 추가
        }
    }
//------------------------------------------------------------------

    //P2P 게임 컨트롤러 설정
    public void setP2PController(P2PGameController controller) {
        this.p2pController = controller;
    }

    //게임 상태 관리자 접근
    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    //행동 컨트롤러 접근
    public ActionController getActionController() {
        return actionController;
    }

    //점수 관리자 접근
    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    //P2P 게임 오버 콜백 설정 - session 정리 등 필요한 작업 수행
    public void setOnGameOverCallback(Runnable callback) {
        this.onGameOverCallback = callback;
    }

    //P2P onStarted() 콜백: 게임 루프 시작 허용
    public void allowGameStart() {
        if (!gameStartAllowed && gameThread.getState() == Thread.State.NEW) {
            gameStartAllowed = true;
            gameThread.start();
        }
    }

    //P2P 다음 라운드 준비: 게임 상태 리셋
    public void resetForNextRound() {
        gameOverHandled = false;
        gameStartAllowed = false;
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