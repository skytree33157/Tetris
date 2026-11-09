package game;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import blocks.core.BlockFactory;
import board.Board;
import difficulty.Difficulty;
import score.ScoreManager;

public final class LocalBattleSession implements AutoCloseable {

    public record PlayerGame(
            Board board,
            GameStateManager state,
            ScoreManager score,
            ActionController action,
            GameLoop loop) {
    }

    private final BattleSettings settings;
    private final BattleController battle;
    private final BattleTimeLimit timeLimit;

    private final Map<BattleController.Player, PlayerGame> players =
            new EnumMap<>(BattleController.Player.class);

    private final Map<BattleController.Player, Thread> threads =
            new EnumMap<>(BattleController.Player.class);

    private boolean started;
    private boolean closed;

    public LocalBattleSession(
            BattleSettings settings,
            Difficulty difficulty) {
        this.settings = Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(difficulty, "difficulty");

        battle = new BattleController();

        for (BattleController.Player player
                : BattleController.Player.values()) {
            PlayerGame game = createPlayer(difficulty);

            players.put(player, game);
            battle.registerPlayer(player, game.action());

            Thread thread = new Thread(
                    game.loop(),
                    "battle-player-" + player.name()
            );
            thread.setDaemon(true);
            threads.put(player, thread);
        }

        timeLimit = new BattleTimeLimit(settings, battle);
    }

    private PlayerGame createPlayer(Difficulty difficulty) {
        Board board = new Board();

        GameStateManager state = new GameStateManager();
        state.setDifficulty(difficulty);

        ScoreManager score = new ScoreManager(difficulty);

        ActionController action = new ActionController(
                board,
                BlockFactory.createRandomBlock(difficulty),
                state,
                score,
                difficulty,
                settings.getGameMode(),
                ActionController.CLEAR_ANIMATION_MS
        );

        GameLoop loop = new GameLoop(action, state, score);

        return new PlayerGame(board, state, score, action, loop);
    }

    public BattleSettings getSettings() {
        return settings;
    }

    public BattleController getBattleController() {
        return battle;
    }

    public PlayerGame getPlayer(BattleController.Player player) {
        return players.get(Objects.requireNonNull(player, "player"));
    }

    public void start() {
        synchronized (battle) {
            if (closed || started) {
                throw new IllegalStateException(
                        "대전은 한 번만 시작할 수 있습니다."
                );
            }

            started = true;

            try {
                battle.start();

                if (!battle.isRunning()) {
                    timeLimit.close();
                    return;
                }

                timeLimit.start(
                        getPlayer(BattleController.Player.ONE)
                                .action()::getScore,
                        getPlayer(BattleController.Player.TWO)
                                .action()::getScore
                );

                for (Thread thread : threads.values()) {
                    thread.start();
                }
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }
    }

    @Override
    public void close() {
        synchronized (battle) {
            if (closed) {
                return;
            }

            closed = true;

            timeLimit.close();
            battle.close();

            for (Thread thread : threads.values()) {
                thread.interrupt();
            }
        }
    }
}