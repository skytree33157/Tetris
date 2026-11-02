package menu.screens;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
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
import menu.StartMenu;
import menu.settings.AppSettings;
import menu.settings.KeyAction;
import menu.settings.SettingsScreen;
import score.ScoreManager;
import storage.ScoreStorage;
import ui.components.RoundedPanel;
import ui.theme.Theme;

// need refactoring 
import menu.GameOverScreen;
import menu.ScoreRecord;


// 게임 화면(보드 + 사이드 정보 패널)을 담는 창
public class GameScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int RENDER_INTERVAL_MS = 50;
    private static final int SIDE_WIDTH = 210;
    private static final int MAX_LEVEL = 10;

  private final GameStateManager gameStateManager;
    private final AppStateManager appStateManager;
    private final ActionController actionController;
    private final ScoreManager scoreManager;
    private final GameLoop gameLoop;
    private final GameController gameController;
    private final GamePanel gamePanel;
    private final JPanel boardHolder;
    private final Thread gameThread;
    private final Timer renderTimer;
    private final BlockPreviewPanel blockPreviewPanel;
    private final Difficulty difficulty;
    private final GameMode mode;
    private final int bestScore;
    private PauseScreen pauseScreen;

private JLabel scoreLabel;
    private JLabel bestLabel;
    private JLabel levelLabel;
    private JLabel linesLabel;
    private JLabel timeLabel;
    private JLabel nextLevelLabel;
    private JLabel comboLabel;
    private ProgressBar levelProgress;
    private final Map<KeyAction, JLabel> keyLabels = new EnumMap<>(KeyAction.class);

    private long elapsedMs = 0;
    private long lastTickMs = System.currentTimeMillis();
    private boolean gameOverHandled = false;
    private boolean closed = false;

    public GameScreen(GameMode mode) {
        
        super("TFSE");
        ui.AppIcon.apply(this);
        this.mode = mode;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        Board board = new Board();
        difficulty = AppSettings.getInstance().getDifficulty();
        bestScore = loadBestScore(difficulty, mode);
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
                    }, true, this::suppressEscapeUntilReleased);
                    settingsScreen.setVisible(true);
                    if (closed) {
                        return;
                    }
                    refreshInputBindings();
                    refreshKeyGuide();
                    gamePanel.refreshCellSize();
                    pack();
                    if (pauseScreen != null){
                        pauseScreen.requestFocusInWindow();
                    }
                },
                this::showPauseScreen,
                this::exitApplication
        );

        boardHolder = createBoardHolder();

        JPanel root = new JPanel(new BorderLayout(24, 0));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        root.add(createLeftPanel(), BorderLayout.WEST);
        root.add(boardHolder, BorderLayout.CENTER);
        root.add(createRightPanel(), BorderLayout.EAST);
        setContentPane(root);
        updateHud();

        pack();
        setLocationRelativeTo(null);

        setFocusable(true);
        gameController.installKeyBindings(getRootPane());

        addWindowListener(new WindowAdapter() {
          @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
                lastTickMs = System.currentTimeMillis();
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

    // 주기적으로 상태를 화면에 반영하고, 게임오버 시 결과 화면으로 전환
    private void onTick(){
        long now = System.currentTimeMillis();
        if (!gameLoop.isPaused() && !gameStateManager.isGameOver()) {
            elapsedMs += now - lastTickMs;
        }
        lastTickMs = now;

        gamePanel.setCurrentBlock(actionController.getCurrentBlock());
        gamePanel.setClearingRows(actionController.getClearingRows());
        gamePanel.setClearingCells(actionController.getClearingCells());
        blockPreviewPanel.setBlock(actionController.getNextBlock());
        updateHud();
        gamePanel.repaint();

        if (gameStateManager.isGameOver() && !gameOverHandled) {
            gameOverHandled = true;
            stopGame();
            dispose();
            GameOverScreen gameOverScreen = new GameOverScreen(
                scoreManager.getScore(), difficulty.name(), mode.name());
            gameOverScreen.setVisible(true);
        }
    }

    private void updateHud() {
        int score = scoreManager.getScore();
        scoreLabel.setText(String.format("%,d", score));
        bestLabel.setText("최고 " + String.format("%,d", Math.max(bestScore, score)));

        int level = gameStateManager.getCurrentLevel();
        levelLabel.setText(String.valueOf(level));
        linesLabel.setText(String.valueOf(gameStateManager.getTotalLinesCleared()));

        long sec = elapsedMs / 1000;
        timeLabel.setText(String.format("%02d:%02d", sec / 60, sec % 60));

        // getLevelSettings(): {감소량, 최소속도, 레벨업 블록 수, 레벨업 줄 수, 현재속도}
        int[] lv = gameStateManager.getLevelSettings();
        if (level >= MAX_LEVEL) {
            nextLevelLabel.setText("MAX");
            levelProgress.setRatio(1.0);
        } else {
            int blocksLeft = lv[2] - gameStateManager.getCountBlock();
            int linesLeft = lv[3] - gameStateManager.getCountClearLine();
            nextLevelLabel.setText("블록 " + blocksLeft + " · 라인 " + linesLeft);
            levelProgress.setRatio(Math.max(
                    gameStateManager.getCountBlock() / (double) lv[2],
                    gameStateManager.getCountClearLine() / (double) lv[3]));
        }

        int combo = scoreManager.getComboCount();
        comboLabel.setText(combo >= 2 ? "x" + combo : "-");
    }

    // ---------- 레이아웃 ----------

    private JPanel createBoardHolder() {
        JPanel frame = new JPanel(new BorderLayout());
        frame.setBackground(Theme.BOARD_BG);
        frame.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 2),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        frame.add(gamePanel);

        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(frame);
        return holder;
    }

    // ---------- 왼쪽 패널 ----------
    
    private JPanel createLeftPanel() {
        JPanel col = column();

        RoundedPanel scoreCard = card();
        scoreCard.add(caption("점수"));
        scoreCard.add(Box.createVerticalStrut(4));
        scoreLabel = text("0", Theme.display(28), Theme.TEXT);
        scoreCard.add(scoreLabel);
        bestLabel = text("최고 0", Theme.body(12, Font.PLAIN), Theme.TEXT_MUTED);
        scoreCard.add(bestLabel);
        addBlock(col, scoreCard);

        JPanel tiles = new JPanel(new GridLayout(1, 2, 10, 0));
        tiles.setOpaque(false);
        RoundedPanel levelCard = card();
        levelCard.add(caption("레벨"));
        levelLabel = text("1", Theme.display(24), Theme.TEXT);
        levelCard.add(levelLabel);
        RoundedPanel linesCard = card();
        linesCard.add(caption("라인"));
        linesLabel = text("0", Theme.display(24), Theme.TEXT);
        linesCard.add(linesLabel);
        tiles.add(levelCard);
        tiles.add(linesCard);
        addBlock(col, tiles);

        RoundedPanel timeCard = card();
        timeLabel = text("00:00", Theme.display(13), Theme.TEXT);
        timeCard.add(row(text("시간", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), timeLabel));
        timeCard.add(Box.createVerticalStrut(8));
        timeCard.add(text("다음 레벨까지", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        timeCard.add(Box.createVerticalStrut(2));
        nextLevelLabel = text("", Theme.body(13, Font.PLAIN), Theme.TEXT);
        timeCard.add(nextLevelLabel);

        timeCard.add(Box.createVerticalStrut(6));
        levelProgress = new ProgressBar();
        timeCard.add(levelProgress);
        addBlock(col, timeCard);

        col.add(Box.createVerticalGlue());

        RoundedPanel comboCard = new RoundedPanel(Theme.CARD, Theme.PRIMARY, Theme.RADIUS);
        comboCard.setLayout(new BoxLayout(comboCard, BoxLayout.Y_AXIS));
        comboCard.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        comboCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        comboCard.add(text("COMBO", Theme.display(18), Theme.ACCENT));
        comboLabel = text("-", Theme.body(12, Font.PLAIN), Theme.TEXT_MUTED);
        comboCard.add(comboLabel);
        fixHeight(comboCard);
        col.add(comboCard);

        return col;
    }

    // ---------- 오른쪽 패널 ----------


    private JPanel createRightPanel() {
        JPanel col = column();

        RoundedPanel nextCard = card();
        nextCard.add(caption("NEXT"));
        nextCard.add(Box.createVerticalStrut(10));
        JPanel previewWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        previewWrap.setOpaque(false);
        previewWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewWrap.add(blockPreviewPanel);
        fixHeight(previewWrap);
        nextCard.add(previewWrap);
        addBlock(col, nextCard);

        RoundedPanel infoCard = card();
        infoCard.add(row(text("모드", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED),
                text(mode == GameMode.ITEM ? "아이템" : "일반", Theme.body(13, Font.BOLD), Theme.TEXT)));
        infoCard.add(Box.createVerticalStrut(6));
        infoCard.add(row(text("난이도", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED),
                text(difficultyLabel(difficulty), Theme.body(13, Font.BOLD), Theme.TEXT)));
        addBlock(col, infoCard);

        RoundedPanel keyCard = card();
        keyCard.add(caption("조작"));
        keyCard.add(Box.createVerticalStrut(8));
        addKeyRow(keyCard, "왼쪽 이동", KeyAction.MOVE_LEFT, null);
        addKeyRow(keyCard, "오른쪽 이동", KeyAction.MOVE_RIGHT, null);
        addKeyRow(keyCard, "회전", KeyAction.ROTATE, null);
        addKeyRow(keyCard, "소프트 드롭", KeyAction.MOVE_DOWN, null);
        addKeyRow(keyCard, "하드 드롭", null, "Space");
        addKeyRow(keyCard, "일시정지", null, "P");
        addKeyRow(keyCard, "설정", null, "Esc");
        refreshKeyGuide();
        addBlock(col, keyCard);

        col.add(Box.createVerticalGlue());
        return col;
    }

    private void addKeyRow(JPanel card, String name, KeyAction action, String fixedKey) {
        JLabel cap = keyCap(fixedKey == null ? "" : fixedKey);
        if (action != null) {
            keyLabels.put(action, cap);
        }
        card.add(row(text(name, Theme.body(13, Font.PLAIN), Theme.TEXT_SOFT), cap));
        card.add(Box.createVerticalStrut(6));
    }

    // 설정에서 키를 바꾼 뒤에도 안내가 맞도록 다시 읽음
    private void refreshKeyGuide() {
        AppSettings settings = AppSettings.getInstance();
        for (Map.Entry<KeyAction, JLabel> e : keyLabels.entrySet()) {
            e.getValue().setText(keyName(settings.getKey(e.getKey())));
        }
    }

    private static String keyName(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.VK_LEFT -> "←";
            case KeyEvent.VK_RIGHT -> "→";
            case KeyEvent.VK_UP -> "↑";
            case KeyEvent.VK_DOWN -> "↓";
            default -> KeyEvent.getKeyText(keyCode);
        };
    }

    private static String difficultyLabel(Difficulty d) {
        return switch (d) {
            case EASY -> "쉬움";
            case NORMAL -> "보통";
            case HARD -> "어려움";
        };
    }

    // 같은 난이도·모드 기록 중 최고 점수
    private static int loadBestScore(Difficulty difficulty, GameMode mode) {
        int best = 0;
        for (ScoreRecord r : new ScoreStorage().load()) {
            if (difficulty.name().equals(r.getDifficulty()) && mode.name().equals(r.getMode())) {
                best = Math.max(best, r.getScore());
            }
        }
        return best;
    }

    // ---------- 작은 UI 헬퍼 ----------

    private static JPanel column() {
        JPanel col = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(SIDE_WIDTH, super.getPreferredSize().height);
            }
        };
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        return col;
    }

    private static RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(Theme.CARD, Theme.CARD_BORDER, Theme.RADIUS);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static void addBlock(JPanel col, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        fixHeight(c);
        col.add(c);
        col.add(Box.createVerticalStrut(14));
    }

    // BoxLayout이 세로로 늘리지 않도록 최대 높이를 선호 높이로 고정
    private static void fixHeight(JComponent c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
    }

    private static JLabel caption(String s) {
        return text(s, Theme.body(12, Font.BOLD), Theme.TEXT_MUTED);
    }

    private static JLabel text(String s, Font font, java.awt.Color color) {
        JLabel l = new JLabel(s);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JPanel row(JComponent left, JComponent right) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        r.add(left, BorderLayout.WEST);
        r.add(right, BorderLayout.EAST);
        fixHeight(r);
        return r;
    }

    private static JLabel keyCap(String s) {
        JLabel l = new JLabel(s, JLabel.CENTER);
        l.setFont(Theme.body(12, Font.PLAIN));
        l.setForeground(Theme.TEXT);
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 3, 1, Theme.OUTLINE),
                BorderFactory.createEmptyBorder(1, 7, 1, 7)));
        return l;
    }

    /** 6px 진행바 */
    private static class ProgressBar extends JComponent {
        private static final long serialVersionUID = 1L;
        private double ratio;

        ProgressBar() {
            setPreferredSize(new Dimension(100, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        void setRatio(double r) {
            ratio = Math.max(0, Math.min(1, r));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.TRACK);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g2.setColor(Theme.ACCENT);
            g2.fillRoundRect(0, 0, (int) (getWidth() * ratio), getHeight(), 6, 6);
            g2.dispose();
        }
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
            //FR-37
            if(!gameLoop.isPaused()) {
                goToGame();
            }
            return;
        }

        pauseScreen = new PauseScreen(
            this::goToGame, 
            this::restartGame,
            gameController::openSettings,
            this::goToStartMenu, 
            this::exitApplication);
        setGlassPane((pauseScreen));
        pauseScreen.setVisible(true);
        pauseScreen.requestFocusInWindow();
    }

    private void hidePauseScreen() {
        if (pauseScreen != null) {
            pauseScreen.setVisible(false);
            pauseScreen = null;
        }
    }

    // 게임 화면으로 돌아가기
    private void goToGame() {
        hidePauseScreen();
        gameController.resumeGame();
        requestFocusInWindow();
    }

    // 현재 판을 버리고 같은 모드로 새 게임
    private void restartGame() {
        stopGame();
        hidePauseScreen();
        dispose();
        GameScreen gameScreen = new GameScreen(mode);
        gameScreen.setVisible(true);
    }

    // 시작 화면으로 돌아가기
    private void goToStartMenu() {
        stopGame();
        hidePauseScreen();
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