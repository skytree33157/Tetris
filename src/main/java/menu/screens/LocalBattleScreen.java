package menu.screens;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import game.ActionController;
import game.AttackPreviewPanel;
import game.BattleController;
import game.BattleController.Player;
import game.BattleSettings;
import game.BlockPreviewPanel;
import game.GamePanel;
import game.LocalBattleSession;
import game.LocalBattleSession.Opponent;
import menu.BattleModes;
import menu.BattleResult;
import menu.settings.AppSettings;
import menu.views.PauseView;
import sound.SoundManager;
import ui.components.RoundedPanel;
import ui.theme.Theme;

/**
 * 한 창에 1P와 상대(2P 또는 CPU)의 보드를 나란히 보여 준다.
 * 경기 규칙(공격·승패·시간 제한)은 LocalBattleSession이 처리하고, 이 화면은 그리기와 입력만 맡는다.
 *
 * 1P: A D 이동, W 회전, S 소프트 드롭, Space 하드 드롭, 왼쪽 Shift 홀드
 * 2P: ← → 이동, ↑ 회전, ↓ 소프트 드롭, Enter 하드 드롭, 오른쪽 Shift 홀드
 * 공통: P / Esc 일시정지
 */
public class LocalBattleScreen extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final int RENDER_INTERVAL_MS = 50;
    private static final int SIDE_WIDTH = 168;
    private static final int CENTER_WIDTH = 176;
    private static final Color ATTACK_ALERT = new Color(0xF26B6B);

    private final BattleSettings settings;
    private final Opponent opponent;
    private final LocalBattleSession session;
    private final BattleController battle;
    private final Timer renderTimer;
    private final Map<Player, PlayerView> views = new EnumMap<>(Player.class);
    // 회전·하드 드롭·홀드는 키를 누르고 있어도 한 번만 동작하게 한다 (자동 반복 무시)
    private final Set<Integer> heldOnceKeys = new HashSet<>();

    private JLabel timeCaption;
    private JLabel timeLabel;
    private PauseView pauseView;
    private boolean started;
    private boolean finished;

    public LocalBattleScreen(BattleSettings settings) {
        this(settings, Opponent.HUMAN);
    }

    LocalBattleScreen(BattleSettings settings, Opponent opponent) {
        super(opponent == Opponent.CPU ? "TFSE - 컴퓨터 대전" : "TFSE - 로컬 대전");
        ui.AppIcon.apply(this);
        this.settings = settings;
        this.opponent = opponent;
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        session = new LocalBattleSession(settings, AppSettings.getInstance().getDifficulty(), opponent);
        battle = session.getBattleController();
        for (Player player : Player.values()) {
            session.getPlayer(player).action().setSoundListener(SoundManager.getInstance()::play);
            views.put(player, new PlayerView(playerName(player), session.getPlayer(player)));
        }

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.X_AXIS));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        PlayerView one = views.get(Player.ONE);
        PlayerView two = views.get(Player.TWO);
        root.add(one.sideColumn);
        root.add(Box.createHorizontalStrut(16));
        root.add(one.boardFrame);
        root.add(Box.createHorizontalStrut(20));
        root.add(createCenterColumn());
        root.add(Box.createHorizontalStrut(20));
        root.add(two.boardFrame);
        root.add(Box.createHorizontalStrut(16));
        root.add(two.sideColumn);
        setContentPane(root);

        renderTimer = new Timer(RENDER_INTERVAL_MS, e -> render());
        render();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        setFocusable(true);
        addKeyListener(new PlayerKeys());
        installPauseKeys();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                requestFocusInWindow();
                startBattle();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                leaveTo(BattleMenuScreen::new);
            }
        });
    }

    private void startBattle() {
        if (started || finished) {
            return;
        }
        started = true;
        try {
            session.start();
        } catch (RuntimeException | Error failure) {
            dispose();
            throw failure;
        }
        renderTimer.start();
        SoundManager.getInstance().startBgm();
    }

    // ---------- 그리기 ----------

    private void render() {
        BattleController.ViewSnapshot snapshot = battle.getViewSnapshot();
        for (Player player : Player.values()) {
            ActionController.ViewSnapshot frame = snapshot.players().get(player);
            if (frame != null) {
                views.get(player).update(frame);
            }
        }

        long elapsed = snapshot.elapsedPlayMillis();
        timeLabel.setText(GameHud.formatTime(settings.isTimeLimited()
                ? settings.getRemainingTimeMillis(elapsed)
                : elapsed));
        SoundManager.getInstance().setBgmPaused(snapshot.paused());

        if (!finished && started && snapshot.outcome() != BattleController.Outcome.IN_PROGRESS
                && snapshot.outcome() != BattleController.Outcome.CANCELLED) {
            showResult(snapshot);
        }
    }

    private void showResult(BattleController.ViewSnapshot snapshot) {
        BattleResult result = BattleResult.fromLocal(
                side(playerName(Player.ONE), snapshot.players().get(Player.ONE)),
                side(playerName(Player.TWO), snapshot.players().get(Player.TWO)),
                toResultOutcome(snapshot.outcome()),
                snapshot.endReason() == BattleController.EndReason.TIME_LIMIT,
                BattleModes.modeLabel(settings),
                snapshot.elapsedPlayMillis());
        leaveTo(() -> new BattleResultScreen(result, opponent == Opponent.CPU ? this::newRound : null));
    }

    private String playerName(Player player) {
        if (player == Player.ONE) {
            return "1P";
        }
        return opponent == Opponent.CPU ? "CPU" : "2P";
    }

    static BattleResult.Outcome toResultOutcome(BattleController.Outcome outcome) {
        return switch (outcome) {
            case ONE_WINS -> BattleResult.Outcome.LEFT_WINS;
            case TWO_WINS -> BattleResult.Outcome.RIGHT_WINS;
            default -> BattleResult.Outcome.DRAW;
        };
    }

    // 결과 화면에 넘길 기록: 숨은 줄을 뺀 보이는 보드
    static BattleResult.Side side(String name, ActionController.ViewSnapshot frame) {
        if (frame == null) {
            return new BattleResult.Side(name, 0, 1, 0, false, new int[0][0]);
        }
        int[][] board = frame.board();
        int hidden = Math.min(frame.hiddenRows(), board.length);
        int[][] visible = new int[board.length - hidden][];
        for (int r = 0; r < visible.length; r++) {
            visible[r] = board[r + hidden].clone();
        }
        return new BattleResult.Side(name, frame.score(), frame.level(), frame.lines(),
                frame.gameOver(), visible);
    }

    // ---------- 입력 ----------

    private static boolean isSecondPlayerKey(KeyEvent event) {
        return switch (event.getKeyCode()) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_ENTER -> true;
            case KeyEvent.VK_SHIFT -> event.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT;
            default -> false;
        };
    }

    private final class PlayerKeys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (finished || pauseView != null || battle.isPaused()) {
                return;
            }
            if (!session.acceptsKeyboardInput(Player.TWO) && isSecondPlayerKey(e)) {
                return;
            }
            ActionController one = session.getPlayer(Player.ONE).action();
            ActionController two = session.getPlayer(Player.TWO).action();
            int code = e.getKeyCode();
            switch (code) {
                case KeyEvent.VK_A -> one.moveLeftAction();
                case KeyEvent.VK_D -> one.moveRightAction();
                case KeyEvent.VK_S -> one.softDropAction();
                case KeyEvent.VK_W -> once(code, one::rotateAction);
                case KeyEvent.VK_SPACE -> once(code, one::hardDropWithScoreAction);
                case KeyEvent.VK_LEFT -> two.moveLeftAction();
                case KeyEvent.VK_RIGHT -> two.moveRightAction();
                case KeyEvent.VK_DOWN -> two.softDropAction();
                case KeyEvent.VK_UP -> once(code, two::rotateAction);
                case KeyEvent.VK_ENTER -> once(code, two::hardDropWithScoreAction);
                case KeyEvent.VK_SHIFT -> {
                    if (e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                        once(-KeyEvent.VK_SHIFT, two::holdAction);
                    } else {
                        once(KeyEvent.VK_SHIFT, one::holdAction);
                    }
                }
                default -> { }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int code = e.getKeyCode();
            if (code == KeyEvent.VK_SHIFT && e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT) {
                code = -KeyEvent.VK_SHIFT;
            }
            heldOnceKeys.remove(code);
        }

        private void once(int key, Runnable action) {
            if (heldOnceKeys.add(key)) {
                action.run();
            }
        }
    }

    // P / Esc: 일시정지 화면이 포커스를 가져가도 동작하도록 창 전체 단축키로 등록
    private void installPauseKeys() {
        JComponent rootPane = getRootPane();
        for (int code : new int[] {KeyEvent.VK_P, KeyEvent.VK_ESCAPE}) {
            String name = "local-battle-pause-" + code;
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(code, 0), name);
            rootPane.getActionMap().put(name, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    togglePause();
                }
            });
        }
    }

    private void togglePause() {
        if (finished || !started) {
            return;
        }
        if (pauseView != null) {
            resume();
            return;
        }
        battle.setPaused(true);
        if (!battle.isPaused()) {
            return;  // 이미 끝난 경기
        }
        pauseView = new PauseView(
                this::resume,
                () -> leaveTo(this::newRound),
                this::openSettings,
                () -> leaveTo(StartMenuScreen::new),
                this::exitApplication);
        setGlassPane(pauseView);
        pauseView.setVisible(true);
        pauseView.requestFocusInWindow();
    }

    private void resume() {
        if (pauseView != null) {
            pauseView.setVisible(false);
            pauseView = null;
        }
        heldOnceKeys.clear();
        battle.setPaused(false);
        requestFocusInWindow();
    }

    private void openSettings() {
        new SettingsScreen(this, () -> { }, () -> leaveTo(StartMenuScreen::new), true).setVisible(true);
        if (finished) {
            return;
        }
        // 화면 크기를 바꿨으면 보드 칸 크기도 다시 맞춘다
        for (PlayerView view : views.values()) {
            view.gamePanel.refreshCellSize();
        }
        pack();
        if (pauseView != null) {
            pauseView.requestFocusInWindow();
        }
    }

    private LocalBattleScreen newRound() {
        return new LocalBattleScreen(settings, opponent);
    }

    private void leaveTo(Supplier<? extends JFrame> next) {
        if (!finished) {
            dispose();
            next.get().setVisible(true);
        }
    }

    @Override
    public void dispose() {
        if (!finished) {
            finished = true;
            renderTimer.stop();
            session.close();
            SoundManager.getInstance().stopBgm();
        }
        super.dispose();
    }

    private void exitApplication() {
        dispose();
        System.exit(0);
    }

    // ---------- 레이아웃 ----------

    private JPanel createCenterColumn() {
        JPanel col = column(CENTER_WIDTH);

        RoundedPanel modeCard = card();
        modeCard.add(caption("모드"));
        modeCard.add(Box.createVerticalStrut(4));
        modeCard.add(text(BattleModes.modeLabel(settings), Theme.body(15, Font.BOLD), Theme.TEXT));
        modeCard.add(Box.createVerticalStrut(10));
        timeCaption = caption(settings.isTimeLimited() ? "남은 시간" : "경과 시간");
        modeCard.add(timeCaption);
        timeLabel = text("00:00", Theme.display(24), Theme.TEXT);
        modeCard.add(timeLabel);
        addBlock(col, modeCard);

        addBlock(col, keyGuideCard("1P", new String[][] {
            {"이동", "A  D"}, {"회전", "W"}, {"소프트", "S"}, {"하드", "Space"}, {"홀드", "L Shift"}}));
        if (opponent == Opponent.CPU) {
            RoundedPanel cpuCard = card();
            cpuCard.add(text("CPU", Theme.display(16), Theme.ACCENT));
            cpuCard.add(Box.createVerticalStrut(8));
            cpuCard.add(caption("컴퓨터가 조작합니다"));
            addBlock(col, cpuCard);
        } else {
            addBlock(col, keyGuideCard("2P", new String[][] {
                {"이동", "←  →"}, {"회전", "↑"}, {"소프트", "↓"}, {"하드", "Enter"}, {"홀드", "R Shift"}}));
        }

        RoundedPanel pauseCard = card();
        pauseCard.add(row(text("일시정지", Theme.body(13, Font.PLAIN), Theme.TEXT_SOFT), keyCap("P, Esc")));
        addBlock(col, pauseCard);

        col.add(Box.createVerticalGlue());
        return col;
    }

    private static RoundedPanel keyGuideCard(String title, String[][] rows) {
        RoundedPanel card = card();
        card.add(text(title, Theme.display(16), Theme.ACCENT));
        card.add(Box.createVerticalStrut(8));
        for (String[] r : rows) {
            card.add(row(text(r[0], Theme.body(12, Font.PLAIN), Theme.TEXT_SOFT), keyCap(r[1])));
            card.add(Box.createVerticalStrut(5));
        }
        return card;
    }

    /** 한 플레이어의 보드와 옆 정보 열 */
    private static final class PlayerView {
        final GamePanel gamePanel;
        final JPanel boardFrame;
        final JPanel sideColumn;
        final BlockPreviewPanel nextPreview = new BlockPreviewPanel();
        final BlockPreviewPanel holdPreview = new BlockPreviewPanel();
        final AttackPreviewPanel attackPreview = new AttackPreviewPanel();
        final JLabel scoreLabel = text("0", Theme.display(24), Theme.TEXT);
        final JLabel levelLabel = text("1", Theme.body(14, Font.BOLD), Theme.TEXT);
        final JLabel linesLabel = text("0", Theme.body(14, Font.BOLD), Theme.TEXT);
        final JLabel attackCount = text("0줄", Theme.body(12, Font.BOLD), Theme.TEXT_MUTED);

        PlayerView(String name, LocalBattleSession.PlayerGame game) {
            gamePanel = new GamePanel(game.board());
            gamePanel.setMaxCellSize(fitCellSize(game.board().getWidth(),
                    game.board().getHeight() + game.board().getHiddenRows()));
            ActionController.ViewSnapshot first = game.action().getViewSnapshot();
            gamePanel.setSnapshot(first);
            nextPreview.setSnapshot(first.nextBlock());
            holdPreview.setSnapshot(first.heldBlock());

            JPanel frame = new JPanel(new BorderLayout());
            frame.setBackground(Theme.BOARD_BG);
            frame.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.OUTLINE, 2),
                    BorderFactory.createEmptyBorder(4, 4, 4, 4)));
            frame.add(gamePanel);
            boardFrame = new JPanel(new java.awt.GridBagLayout());
            boardFrame.setOpaque(false);
            boardFrame.add(frame);

            sideColumn = column(SIDE_WIDTH);
            JLabel title = text(name, Theme.display(22), Theme.ACCENT);
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            sideColumn.add(title);
            sideColumn.add(Box.createVerticalStrut(10));

            addBlock(sideColumn, previewCard("HOLD", holdPreview));
            addBlock(sideColumn, previewCard("NEXT", nextPreview));

            RoundedPanel scoreCard = card();
            scoreCard.add(caption("점수"));
            scoreCard.add(Box.createVerticalStrut(2));
            scoreCard.add(scoreLabel);
            scoreCard.add(Box.createVerticalStrut(6));
            scoreCard.add(row(text("레벨", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), levelLabel));
            scoreCard.add(Box.createVerticalStrut(4));
            scoreCard.add(row(text("라인", Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED), linesLabel));
            addBlock(sideColumn, scoreCard);

            RoundedPanel attackCard = card();
            attackCard.add(row(caption("공격 대기"), attackCount));
            attackCard.add(Box.createVerticalStrut(8));
            attackCard.add(boxed(attackPreview));
            addBlock(sideColumn, attackCard);

            sideColumn.add(Box.createVerticalGlue());
        }

        void update(ActionController.ViewSnapshot frame) {
            gamePanel.setSnapshot(frame);
            nextPreview.setSnapshot(frame.nextBlock());
            holdPreview.setSnapshot(frame.heldBlock());
            scoreLabel.setText(String.format("%,d", frame.score()));
            levelLabel.setText(String.valueOf(frame.level()));
            linesLabel.setText(String.valueOf(frame.lines()));
            attackPreview.setRows(frame.pendingAttackRows());
            int count = attackPreview.getRowCount();
            attackCount.setText(count + "줄");
            attackCount.setForeground(count > 0 ? ATTACK_ALERT : Theme.TEXT_MUTED);
        }
    }

    // 보드 두 개와 정보 열이 모니터 작업 영역 안에 들어가는 가장 큰 칸 크기
    private static int fitCellSize(int cols, int rows) {
        java.awt.Rectangle area;
        try {
            area = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        } catch (java.awt.HeadlessException e) {
            return Integer.MAX_VALUE;
        }
        return fitCellSize(area.width, area.height, cols, rows);
    }

    static int fitCellSize(int areaWidth, int areaHeight, int cols, int rows) {
        // 창 테두리·제목 표시줄·여백, 그리고 보드 밖 열(정보 열 2개 + 가운데 열 + 간격)
        int chromeHeight = 40 + 40 + 16;
        int chromeWidth = 2 * SIDE_WIDTH + CENTER_WIDTH + 2 * 16 + 2 * 20 + 48 + 2 * 12 + 16;
        int byHeight = (areaHeight - chromeHeight) / rows;
        int byWidth = (areaWidth - chromeWidth) / (2 * cols);
        return Math.max(14, Math.min(byHeight, byWidth));
    }

    // ---------- 작은 UI 헬퍼 (GameScreen과 같은 모양) ----------

    private static RoundedPanel previewCard(String title, JComponent preview) {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.CARD_BORDER, Theme.RADIUS);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        card.add(caption(title));
        card.add(Box.createVerticalStrut(6));
        card.add(boxed(preview));
        return card;
    }

    private static JPanel boxed(JComponent content) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        wrap.setBackground(Theme.BOARD_BG);
        wrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE, 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(content);
        fixHeight(wrap);
        return wrap;
    }

    private static JPanel column(int width) {
        JPanel col = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(width, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(width, Integer.MAX_VALUE);
            }
        };
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        return col;
    }

    private static RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(Theme.CARD, Theme.CARD_BORDER, Theme.RADIUS);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static void addBlock(JPanel col, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        fixHeight(c);
        col.add(c);
        col.add(Box.createVerticalStrut(12));
    }

    private static void fixHeight(JComponent c) {
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
    }

    private static JLabel caption(String s) {
        return text(s, Theme.body(12, Font.BOLD), Theme.TEXT_MUTED);
    }

    private static JLabel text(String s, Font font, Color color) {
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
}
