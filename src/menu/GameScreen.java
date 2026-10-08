package menu;

import app.AppState;
import app.AppStateManager;
import blocks.core.Block;
import blocks.core.BlockFactory;
import board.Board;
import game.ActionController;
import game.BlockPreviewPanel;
import game.GameController;
import game.GameLoop;
import game.GamePanel;
import game.GameStateManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
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
    private PauseScreen pauseScreen;

    private  JLabel levelLabel;
    private  JLabel linesLabel;
    private  JLabel scoreLabel;
    private boolean gameOverHandled = false;


    public GameScreen() {
        
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Board board = new Board();
        gameStateManager = new GameStateManager();
        appStateManager = new AppStateManager(AppState.PLAYING);
        Block firstBlock = BlockFactory.createRandomBlock();
        scoreManager = new ScoreManager();
        actionController = new ActionController(board, firstBlock, gameStateManager, scoreManager);
        gameLoop = new GameLoop(actionController, gameStateManager, scoreManager);

        gamePanel = new GamePanel(board);
        gamePanel.setCurrentBlock(actionController.getCurrentBlock());
        blockPreviewPanel = new BlockPreviewPanel();
        blockPreviewPanel.setBlock(actionController.getNextBlock());
        gameController = new GameController(
            gameLoop, actionController, gameStateManager, scoreManager, appStateManager, 
                () -> {
                    SettingsScreen settingsScreen = new SettingsScreen(GameScreen.this, () -> {}, () -> {
                        stopGame();
                        dispose();
                        StartMenu startMenu = new StartMenu();
                        startMenu.setVisible(true);
                    });
                    settingsScreen.setVisible(true);
                    gamePanel.refreshCellSize();
                    pack();
                },
                this::showPauseScreen
        );

        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        add(createSidePanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);

        setFocusable(true);
        addKeyListener(gameController);

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
        // GameLoop를 직접 호출하지 않고, 백그라운드에서 mutate되는 Board를 주기적으로 다시 그리기만 함
        renderTimer = new Timer(RENDER_INTERVAL_MS, e -> onTick());
        renderTimer.start();
    }

    // 주기적으로 상태를 화면에 반영하고, 게임오서 시 결과 화면으로 전환
    private void onTick(){
        gamePanel.setCurrentBlock(actionController.getCurrentBlock());
        blockPreviewPanel.setBlock(actionController.getNextBlock());
        levelLabel.setText("LEVEL: " + gameStateManager.getCurrentLevel());
        linesLabel.setText("LINES: " + gameStateManager.getTotalLinesCleared());
        scoreLabel.setText("SCORE: " + scoreManager.getScore());
        gamePanel.repaint();

        if (gameStateManager.isGameOver() && !gameOverHandled) {
            gameOverHandled = true;
            stopGame();
            dispose();
            GameOverScreen gameOverScreen = new GameOverScreen(scoreManager.getScore(), "NORMAL", "NORMAL");
            gameOverScreen.setVisible(true);
        }
    }
    
    // 사이드 패널 (임시)
    private JPanel createSidePanel() {
        JPanel side = new JPanel();
        side.setBackground(Color.BLACK);
        side.setPreferredSize(new Dimension(160, 0));
        side.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
        side.setLayout(new GridLayout(6, 1, 0, 10));

        JLabel title = new JLabel("TETRIS");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Courier", Font.BOLD, 20));

        levelLabel = new JLabel("LEVEL: " + gameStateManager.getCurrentLevel());
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setFont(new Font("Courier", Font.PLAIN, 16));

        linesLabel = new JLabel("LINES: " + gameStateManager.getTotalLinesCleared());
        linesLabel.setForeground(Color.WHITE);
        linesLabel.setFont(new Font("Courier", Font.PLAIN, 16));

        scoreLabel = new JLabel("SCORE: " + scoreManager.getScore());
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Courier", Font.PLAIN, 16));

        side.add(title);
        side.add(blockPreviewPanel);
        side.add(levelLabel);
        side.add(linesLabel);
        side.add(scoreLabel);

        return side;
    }

    private void stopGame() {
        renderTimer.stop();
        gameController.shutdown();
        gameThread.interrupt();
    }

    // 일시정지 화면 표시
    private void showPauseScreen() {
        if (pauseScreen != null && pauseScreen.isVisible()) {
            return;
        }

        pauseScreen = new PauseScreen(this::goToGame, this::goToStartMenu);
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
}
