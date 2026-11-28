package menu.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import app.AppState;
import app.AppStateManager;
import blocks.core.Block;
import blocks.core.BlockFactory;
import board.Board;
import difficulty.Difficulty;
import game.ActionController;
import game.AttackPreviewPanel;
import game.BlockPreviewPanel;
import game.RemoteBoardPanel;
import game.GameController;
import game.GameLoop;
import game.GameMode;
import game.GamePanel;
import game.GameStateManager;
import menu.BattleResult;
import menu.P2PGameController;
import menu.views.PauseView;
import menu.settings.AppSettings;
import network.GameStatus;
import menu.settings.KeyAction;
import score.ScoreManager;
import storage.ScoreStorage;
import ui.components.RoundedPanel;
import ui.theme.Theme;
import sound.SoundManager;


// 게임 화면(보드 + 사이드 정보 패널)을 담는 창
public class GameScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int RENDER_INTERVAL_MS = 50;
    private static final int SIDE_WIDTH = 210;
    private static final Color CONNECTION_NORMAL_COLOR = new Color(0x4CD964);
    private static final Color CONNECTION_LOST_COLOR = new Color(0xF26B6B);

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
    private final BlockPreviewPanel holdPreviewPanel;
    private final Difficulty difficulty;
    private final GameMode mode;
    private final int bestScore;
    private PauseView PauseView;
    private P2PGameController p2pController;
    private volatile boolean gameStartAllowed = false;  // P2P 모드에서 onStarted() 대기용
    private volatile Runnable onGameOverCallback;  // P2P 게임 오버 시 호출할 콜백
    private volatile Runnable p2pLobbyAction;      // P2P 결과 화면의 "대기실로"
    private volatile Runnable p2pLeaveAction;      // P2P 결과 화면 종료 시 세션 정리
    private JLabel connectionStatusLabel;
    private JPanel rightPanel;

    // P2P 대전: 상대 보드와 점수 (setP2PController 이후에만 생성)
    private RemoteBoardPanel remoteBoardPanel;
    private JLabel opponentScoreLabel;
    private JLabel opponentLevelLabel;
    private JLabel opponentLinesLabel;
    // 대전: 받은 공격 중 아직 보드에 들어가지 않은 줄
    private AttackPreviewPanel attackPreviewPanel;
    private JLabel attackCountLabel;
    // 시간 제한 대전의 제한 시간(초). 0이면 시간 제한 없음
    private int timeLimitSeconds;
    private JLabel timeCaptionLabel;
    private JLabel modeValueLabel;

private JLabel scoreLabel;
    private JLabel bestLabel;
    private JLabel levelLabel;
    private DebugPanel debugPanel;
    private JLabel linesLabel;
    private JLabel timeLabel;
    private JLabel nextLevelLabel;
    private JLabel comboLabel;
    private ProgressBar levelProgress;
    private final Map<KeyAction, JLabel> keyLabels = new EnumMap<>(KeyAction.class);

    private long elapsedMs = 0;
    private long lastTickMs = System.currentTimeMillis();
    private boolean gameOverHandled = false;
    // 결과 화면으로 넘어갔는지. 라운드 종료(resetForNextRound)에도 되돌리지 않는다.
    private boolean resultShown = false;
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
        actionController.setSoundListener(SoundManager.getInstance()::play);
        gameLoop = new GameLoop(actionController, gameStateManager, scoreManager);

        // 같은 시점의 게임 상태로 보드, NEXT, HOLD를 초기화
        ActionController.ViewSnapshot initialFrame = actionController.getViewSnapshot();

        gamePanel = new GamePanel(board);
        gamePanel.setSnapshot(initialFrame);
        blockPreviewPanel = new BlockPreviewPanel();
        blockPreviewPanel.setSnapshot(initialFrame.nextBlock());

        // FR-50: 홀드 블록 미리보기
        holdPreviewPanel = new BlockPreviewPanel();
        holdPreviewPanel.setSnapshot(initialFrame.heldBlock());
        gameController = new GameController(
            gameLoop, actionController, gameStateManager, scoreManager, appStateManager, 
                () -> {
                    SettingsScreen settingsScreen = new SettingsScreen(GameScreen.this, () -> {}, () -> {
                        stopGame();
                        dispose();
                        StartMenuScreen StartMenuScreen = new StartMenuScreen();
                        StartMenuScreen.setVisible(true);
                    }, true, this::suppressEscapeUntilReleased);
                    settingsScreen.setVisible(true);
                    if (closed) {
                        return;
                    }
                    refreshInputBindings();
                    refreshKeyGuide();
                    gamePanel.refreshCellSize();
                    pack();
                    if (PauseView != null){
                        PauseView.requestFocusInWindow();
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
        rightPanel = createRightPanel();
        root.add(rightPanel, BorderLayout.EAST);
        setContentPane(root);
        if (p2pController != null) {
            installOpponentColumn();
        }
        updateHud();

        pack();
        setLocationRelativeTo(null);

        setFocusable(true);
        gameController.installKeyBindings(getRootPane());

        addWindowListener(new WindowAdapter() {
          @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
                // 싱글 모드는 바로 시작, P2P 모드는 onStarted() 이후 allowGameStart()에서 시작
                if (GameScreen.this.p2pController == null) {
                    startGameLoop();
                }
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
        // P2P는 상대 준비를 기다리는 동안 시간이 흐르지 않게, 게임 루프가 시작된 뒤부터 센다
        boolean running = gameThread.getState() != Thread.State.NEW;
        if (running && !gameLoop.isPaused() && !gameStateManager.isGameOver()) {
            elapsedMs += now - lastTickMs;
        }
        lastTickMs = now;
        SoundManager.getInstance().setBgmPaused(gameLoop.isPaused());

        ActionController.ViewSnapshot frame = actionController.getViewSnapshot();

        gamePanel.setSnapshot(frame);
        blockPreviewPanel.setSnapshot(frame.nextBlock());
        holdPreviewPanel.setSnapshot(frame.heldBlock());
        updateAttackPreview(frame);
        updateHud();

        // 시간 제한 대전: 호스트가 시간 종료를 판정해 양쪽 점수를 확정한다.
        // 클라이언트는 호스트의 TIME_UP 메시지를 받을 때까지 계속 플레이한다.
        if (isTimeUpOnHost()) {
            int hostScore = scoreManager.getScore();
            GameStatus remote = p2pController.getRemoteStatus();
            int clientScore = remote == null ? 0 : remote.getScore();
            try {
                p2pController.sendTimeUp(hostScore, clientScore);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            finishByTime(hostScore, clientScore);
            return;
        }

        // P2P 게임 상태 전송
        if (p2pController != null && p2pController.isRoundActive()) {
            try {
                p2pController.sendGameStatus();
            } catch (IOException exception) {
                // 연결 끊김은 P2PGameSession의 리스너에서 처리
                exception.printStackTrace();
            }
        }

        // 로컬 게임 오버 처리
        if (frame.gameOver() && !gameOverHandled) {
            gameOverHandled = true;
            // P2P 게임은 게임 오버를 상대에게 통지하고, 승패 결과는 라운드 종료 시 P2PGameRunner에서 표시
            if (p2pController != null) {
                try {
                    p2pController.notifyGameOver();
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                return;
            }
            showGameOver(frame);
        }
    }

    /**
     * 대전 연결이 끊겼을 때 호출(EDT). 이미 끝난 게임이거나 상대가 게임 오버를 보낸 뒤라면
     * 연결 오류가 아니라 정상 종료이므로 결과 화면으로 넘어가고 true를 돌려준다.
     */
    public boolean finishBattleIfEnded() {
        if (resultShown || gameOverHandled) {
            return true;
        }
        GameStatus remote = p2pController == null ? null : p2pController.getRemoteStatus();
        if (remote != null && remote.isGameOver()) {
            showGameOver(actionController.getViewSnapshot());
            return true;
        }
        return false;
    }

    /** P2P 게임 오버로 승패가 정해졌을 때(EDT). 이미 결과가 떴으면 무시한다. */
    public void showRoundResult() {
        if (resultShown || p2pController == null) {
            return;
        }
        showGameOver(actionController.getViewSnapshot());
    }

    private boolean isTimeUpOnHost() {
        return timeLimitSeconds > 0
                && p2pController != null
                && p2pController.isHost()
                && p2pController.isRoundActive()
                && !gameOverHandled
                && elapsedMs >= timeLimitSeconds * 1000L;
    }

    /**
     * 시간 제한 대전 종료(EDT). 호스트는 직접, 클라이언트는 TIME_UP 메시지로 호출된다.
     * 점수가 높은 쪽이 이긴다.
     */
    public void finishByTime(int hostScore, int clientScore) {
        if (resultShown || gameOverHandled || p2pController == null) {
            return;
        }
        boolean host = p2pController.isHost();
        BattleResult result = BattleResult.fromP2PTimeUp(
                GameStatus.capture(actionController, gameStateManager, scoreManager),
                p2pController.getRemoteStatus(),
                host ? hostScore : clientScore,
                host ? clientScore : hostScore,
                modeLabel(),
                elapsedMs);
        finishBattle(actionController.getViewSnapshot(), result);
    }

    private String modeLabel() {
        if (timeLimitSeconds > 0) {
            return "시간 제한 모드";
        }
        return mode == GameMode.ITEM ? "아이템 모드" : "일반 모드";
    }

    private void showGameOver(ActionController.ViewSnapshot frame) {
        // 대전이면 게임을 멈추기 전에 양쪽 마지막 상태를 잡아 둔다
        BattleResult battleResult = p2pController == null ? null
                : BattleResult.fromP2P(
                        GameStatus.capture(actionController, gameStateManager, scoreManager),
                        p2pController.getRemoteStatus(),
                        modeLabel(),
                        elapsedMs,
                        p2pController.isLocalSurrendered() || p2pController.isRemoteSurrendered());
        finishBattle(frame, battleResult);
    }

    private void finishBattle(ActionController.ViewSnapshot frame, BattleResult battleResult) {
        gameOverHandled = true;
        resultShown = true;
        stopGame();
        dispose();

        // P2P 게임 오버 콜백 호출 (session 정리 등)
        if (onGameOverCallback != null) {
            onGameOverCallback.run();
        }

        // 대전: 승패 결과 화면 (혼자 하는 게임의 기록 등록 화면 대신)
        if (battleResult != null) {
            new BattleResultScreen(battleResult, p2pLobbyAction, p2pLeaveAction).setVisible(true);
            return;
        }

        GameOverScreen gameOverScreen = new GameOverScreen(
            frame.score(), difficulty.name(), mode.name(),
            frame.level(), frame.lines(), elapsedMs);
        gameOverScreen.setVisible(true);
    }

    private void startGameLoop() {
        lastTickMs = System.currentTimeMillis();
        gameThread.start();
        SoundManager.getInstance().startBgm();
    }

    private void updateHud() {
        int score = scoreManager.getScore();
        scoreLabel.setText(String.format("%,d", score));
        bestLabel.setText("최고 " + String.format("%,d", Math.max(bestScore, score)));

        int level = gameStateManager.getCurrentLevel();
        levelLabel.setText(String.valueOf(level));
        linesLabel.setText(String.valueOf(gameStateManager.getTotalLinesCleared()));

        // 시간 제한 대전은 남은 시간을 거꾸로 센다
        timeLabel.setText(GameHud.formatTime(timeLimitSeconds > 0
                ? Math.max(0L, timeLimitSeconds * 1000L - elapsedMs)
                : elapsedMs));

        // getLevelSettings(): {감소량, 최소속도, 레벨업 블록 수, 레벨업 줄 수, 현재속도}
        int[] lv = gameStateManager.getLevelSettings();
        int countBlock = gameStateManager.getCountBlock();
        int countLine = gameStateManager.getCountClearLine();
        nextLevelLabel.setText(GameHud.nextLevelText(level, countBlock, lv[2], countLine, lv[3]));
        levelProgress.setRatio(GameHud.levelProgress(level, countBlock, lv[2], countLine, lv[3]));

        comboLabel.setText(GameHud.comboText(scoreManager.getComboCount()));
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

        // FR-50: 홀드 블록 (디자인 시안의 왼쪽 맨 위)
        addBlock(col, previewCard("HOLD", holdPreviewPanel));

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
        JLabel levelCaption = caption("레벨");
        levelCard.add(levelCaption);
        levelLabel = text("1", Theme.display(24), Theme.TEXT);
        levelCard.add(levelLabel);
        if (p2pController == null) {
            debugPanel = new DebugPanel(this, "디버그 모드", actionController, gameStateManager, null, null,
                    on -> levelCaption.setText(on ? "레벨 [DEBUG]" : "레벨"));
            levelCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            levelCard.setToolTipText("클릭하면 디버그 모드 전환");
            levelCard.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    debugPanel.toggle();
                    requestFocusInWindow();
                }
            });
        }
        RoundedPanel linesCard = card();
        linesCard.add(caption("라인"));
        linesLabel = text("0", Theme.display(24), Theme.TEXT);
        linesCard.add(linesLabel);
        tiles.add(levelCard);
        tiles.add(linesCard);
        addBlock(col, tiles);

        RoundedPanel timeCard = card();
        timeLabel = text("00:00", Theme.display(13), Theme.TEXT);
        timeCaptionLabel = text("시간", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED);
        timeCard.add(row(timeCaptionLabel, timeLabel));
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

        RoundedPanel comboCard = new RoundedPanel(Theme.CARD, Theme.OUTLINE, Theme.RADIUS);
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

        addBlock(col, previewCard("NEXT", blockPreviewPanel));

        RoundedPanel infoCard = card();
        modeValueLabel = text(mode == GameMode.ITEM ? "아이템" : "일반", Theme.body(13, Font.BOLD), Theme.TEXT);
        infoCard.add(row(text("모드", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), modeValueLabel));
        infoCard.add(Box.createVerticalStrut(6));
        infoCard.add(row(text("난이도", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED),
                text(GameHud.difficultyLabel(difficulty), Theme.body(13, Font.BOLD), Theme.TEXT)));
        addBlock(col, infoCard);

        RoundedPanel keyCard = card();
        keyCard.add(caption("조작"));
        keyCard.add(Box.createVerticalStrut(8));
        addKeyRow(keyCard, "왼쪽 이동", KeyAction.MOVE_LEFT, null);
        addKeyRow(keyCard, "오른쪽 이동", KeyAction.MOVE_RIGHT, null);
        addKeyRow(keyCard, "회전", KeyAction.ROTATE, null);
        addKeyRow(keyCard, "소프트 드롭", KeyAction.MOVE_DOWN, null);
        addKeyRow(keyCard, "하드 드롭", KeyAction.HARD_DROP, null);
        addKeyRow(keyCard, "홀드", KeyAction.HOLD, null);
        addKeyRow(keyCard, "일시정지", null, "P, ESC");
        addKeyRow(keyCard, "종료", null, "Esc hold");
        refreshKeyGuide();
        addBlock(col, keyCard);

        col.add(Box.createVerticalGlue());
        return col;
    }

    // NEXT·HOLD처럼 제목과 블록 미리보기만 있는 카드
    private static RoundedPanel previewCard(String title, BlockPreviewPanel preview) {
        RoundedPanel card = card();
        card.add(caption(title));
        card.add(Box.createVerticalStrut(10));
        // 보드처럼 검은 상자 + 테두리 안에 미리보기
        JPanel previewWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        previewWrap.setBackground(Theme.BOARD_BG);
        previewWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        previewWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewWrap.add(preview);
        fixHeight(previewWrap);
        card.add(previewWrap);
        return card;
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
            e.getValue().setText(GameHud.keyName(settings.getKey(e.getKey())));
        }
    }

    // 같은 난이도·모드 기록 중 최고 점수
    private static int loadBestScore(Difficulty difficulty, GameMode mode) {
        return GameHud.bestScore(new ScoreStorage().load(), difficulty, mode);
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
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Theme.ACCENT);
            g2.fillRect(0, 0, (int) (getWidth() * ratio), getHeight());
            g2.dispose();
        }
    }

    public void stopGame() {
        closed = true;
        if (debugPanel != null) {
            debugPanel.dispose();
        }
        renderTimer.stop();
        gameController.shutdown();
        actionController.shutdown();
        gameThread.interrupt();
        SoundManager.getInstance().stopBgm();
    }

    // 정상 연결 상태를 초록색 글씨로 상단 바에 표시
    public void showConnectionNormal() {
        showConnectionStatus("연결 정상", CONNECTION_NORMAL_COLOR);
    }

    // 연결 지연 상태를 상단 바에 경고 색으로 표시
    public void showConnectionLatency() {
        showConnectionStatus("연결 지연 중...", Theme.BRONZE);
    }

    private void showConnectionStatus(String message, Color color) {
        if (connectionStatusLabel == null) {
            connectionStatusLabel = new JLabel(message);
            connectionStatusLabel.setFont(Theme.body(14, Font.BOLD));
            connectionStatusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        } else {
            connectionStatusLabel.setText(message);
        }
        connectionStatusLabel.setForeground(color);

        // 상단에 상태 레이블 추가 (이미 추가되었다면 업데이트만)
        // 창 크기를 다시 맞추지 않으면 고정 크기 창에서 보드 영역이 잘리므로 pack()으로 크기를 갱신한다.
        if (connectionStatusLabel.getParent() == null) {
            getContentPane().add(connectionStatusLabel, BorderLayout.NORTH);
            pack();
        }
    }

    // 상대 플레이어의 게임 상태 업데이트
    // 상대 보드와 점수·레벨·줄 수를 오른쪽 '상대' 열에 표시 (EDT에서 호출)
    public void updateRemoteStatus(GameStatus status) {
        if (status == null || remoteBoardPanel == null) {
            return;
        }
        remoteBoardPanel.setStatus(status);
        opponentScoreLabel.setText(String.format("%,d", status.getScore()));
        opponentLevelLabel.setText(String.valueOf(status.getLevel()));
        opponentLinesLabel.setText(String.valueOf(status.getLines()));
    }

    // P2P 게임 컨트롤러 설정. 처음 연결되면 상대 열을 붙이고 창 크기를 다시 맞춘다.
    public void setP2PController(P2PGameController controller) {
        this.p2pController = controller;
        // 네트워크 대전에서는 상대 보드가 멈추지 않도록 일시정지를 비활성화
        gameController.setPauseEnabled(controller == null);
        gameController.setSurrenderHandler(controller == null ? null : this::askSurrender);
        if (controller != null && remoteBoardPanel == null) {
            installOpponentColumn();
            pack();
            setLocationRelativeTo(null);
        }
    }

    // ESC를 떼면 항복 확인을 연다. 모달은 키 처리 밖(invokeLater)에서 띄워야 이벤트 흐름이 꼬이지 않는다.
    private void askSurrender() {
        SwingUtilities.invokeLater(() -> {
            if (resultShown || gameOverHandled || gameStateManager.isGameOver()) {
                return;
            }
            int choice = JOptionPane.showConfirmDialog(this,
                    "항복하시겠습니까?", "항복",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resultShown || gameOverHandled || gameStateManager.isGameOver()) {
                return;
            }
            // 취소·닫기: 게임은 확인창과 상관없이 계속 진행 중이므로 아무것도 하지 않는다
            if (choice == JOptionPane.YES_OPTION) {
                surrender();
            }
        });
    }

    // 항복은 자기 게임 오버와 같다. 패배를 보드 상태에 반영한 뒤 상대에게 알리면 라운드가 끝나고 결과 화면이 뜬다.
    private void surrender() {
        gameOverHandled = true;
        gameStateManager.setGameOver(true);
        try {
            p2pController.notifySurrender();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    // 오른쪽 패널 옆에 '상대' 열 추가: 상대 보드(작게) + 점수·레벨·줄 수
    private void installOpponentColumn() {
        timeLimitSeconds = p2pController.getTimeLimitSeconds();
        if (timeLimitSeconds > 0) {
            timeCaptionLabel.setText("남은 시간");
            modeValueLabel.setText("시간 제한 " + GameHud.formatTime(timeLimitSeconds * 1000L));
        }
        installAttackPreview();

        remoteBoardPanel = new RemoteBoardPanel();
        opponentScoreLabel = text("0", Theme.display(22), Theme.TEXT);
        opponentLevelLabel = text("1", Theme.body(13, Font.BOLD), Theme.TEXT);
        opponentLinesLabel = text("0", Theme.body(13, Font.BOLD), Theme.TEXT);

        JPanel col = column();

        RoundedPanel boardCard = card();
        boardCard.add(caption("상대"));
        boardCard.add(Box.createVerticalStrut(10));
        JPanel boardWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        boardWrap.setBackground(Theme.BOARD_BG);
        boardWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        boardWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        boardWrap.add(remoteBoardPanel);
        fixHeight(boardWrap);
        boardCard.add(boardWrap);
        addBlock(col, boardCard);

        RoundedPanel scoreCard = card();
        scoreCard.add(caption("상대 점수"));
        scoreCard.add(Box.createVerticalStrut(4));
        scoreCard.add(opponentScoreLabel);
        scoreCard.add(Box.createVerticalStrut(8));
        scoreCard.add(row(text("레벨", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), opponentLevelLabel));
        scoreCard.add(Box.createVerticalStrut(6));
        scoreCard.add(row(text("줄", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), opponentLinesLabel));
        addBlock(col, scoreCard);
        col.add(Box.createVerticalGlue());

        // 기존 오른쪽 패널과 상대 열을 나란히 둔다
        JPanel east = new JPanel();
        east.setOpaque(false);
        east.setLayout(new BoxLayout(east, BoxLayout.X_AXIS));
        getContentPane().remove(rightPanel);
        east.add(rightPanel);
        east.add(Box.createHorizontalStrut(24));
        east.add(col);
        getContentPane().add(east, BorderLayout.EAST);
        getContentPane().revalidate();
    }

    // NEXT 카드 바로 아래에 공격 대기 카드를 넣는다
    private void installAttackPreview() {
        attackPreviewPanel = new AttackPreviewPanel();
        attackCountLabel = text("0줄", Theme.body(12, Font.BOLD), Theme.TEXT_MUTED);

        RoundedPanel card = card();
        card.add(row(caption("공격 대기"), attackCountLabel));
        card.add(Box.createVerticalStrut(8));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setBackground(Theme.BOARD_BG);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(attackPreviewPanel);
        fixHeight(wrap);
        card.add(wrap);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        fixHeight(card);

        // rightPanel: [NEXT, 간격, 모드 카드, ...] 에서 NEXT와 간격 뒤에 끼운다
        rightPanel.add(card, 2);
        rightPanel.add(Box.createVerticalStrut(14), 3);
    }

    private void updateAttackPreview(ActionController.ViewSnapshot frame) {
        if (attackPreviewPanel == null) {
            return;
        }
        attackPreviewPanel.setRows(frame.pendingAttackRows());
        int count = attackPreviewPanel.getRowCount();
        attackCountLabel.setText(count + "줄");
        attackCountLabel.setForeground(count > 0 ? CONNECTION_LOST_COLOR : Theme.TEXT_MUTED);
    }

    // 게임 상태 관리자 접근
    public GameStateManager getGameStateManager() {
        return gameStateManager;
    }

    // 행동 컨트롤러 접근
    public ActionController getActionController() {
        return actionController;
    }

    // 점수 관리자 접근
    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    // P2P 게임 오버 콜백 설정 - session 정리 등 필요한 작업 수행
    public void setOnGameOverCallback(Runnable callback) {
        this.onGameOverCallback = callback;
    }

    // P2P 결과 화면에서 대기실로 돌아가기 / 나가기 동작 설정
    public void setP2PResultActions(Runnable onLobby, Runnable onLeave) {
        this.p2pLobbyAction = onLobby;
        this.p2pLeaveAction = onLeave;
    }

    // P2P onStarted() 콜백: 게임 루프 시작 허용
    public void allowGameStart() {
        if (!gameStartAllowed && gameThread.getState() == Thread.State.NEW) {
            gameStartAllowed = true;
            startGameLoop();
        }
    }

    // P2P 다음 라운드 준비: 게임 상태 리셋
    public void resetForNextRound() {
        gameOverHandled = false;
        gameStartAllowed = false;
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
        if (PauseView != null && PauseView.isVisible()) {
            //FR-37
            if(!gameLoop.isPaused()) {
                goToGame();
            }
            return;
        }

        PauseView = new PauseView(
            this::goToGame, 
            this::restartGame,
            gameController::openSettings,
            this::goToStartMenu, 
            this::exitApplication);
        setGlassPane((PauseView));
        PauseView.setVisible(true);
        PauseView.requestFocusInWindow();
    }

    private void hidePauseScreen() {
        if (PauseView != null) {
            PauseView.setVisible(false);
            PauseView = null;
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
        StartMenuScreen StartMenuScreen = new StartMenuScreen();
        StartMenuScreen.setVisible(true);
    }

    // 게임을 정리한 뒤 프로그램 종료
    private void exitApplication() {
        stopGame();
        dispose();
        System.exit(0);
    }
}