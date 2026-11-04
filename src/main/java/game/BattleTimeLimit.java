package game;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

public final class BattleTimeLimit implements AutoCloseable {

    private static final long CHECK_INTERVAL_MS = 50;

    private final BattleSettings settings;
    private final BattleController battle;
    private final LongSupplier elapsedPlayMillisSupplier;

    private ScheduledExecutorService timer;
    private ScheduledFuture<?> timerTask;
    private boolean started;
    private boolean closed;

    public BattleTimeLimit(
            BattleSettings settings,
            BattleController battle) {
        this(settings, battle, battle::getElapsedPlayMillis);
    }

    BattleTimeLimit(
            BattleSettings settings,
            BattleController battle,
            LongSupplier elapsedPlayMillisSupplier) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.battle = Objects.requireNonNull(battle, "battle");
        this.elapsedPlayMillisSupplier = Objects.requireNonNull(
                elapsedPlayMillisSupplier,
                "elapsedPlayMillisSupplier"
        );
    }

    public void start(
            IntSupplier oneScoreSupplier,
            IntSupplier twoScoreSupplier) {
        Objects.requireNonNull(oneScoreSupplier, "oneScoreSupplier");
        Objects.requireNonNull(twoScoreSupplier, "twoScoreSupplier");

        synchronized (battle) {
            if (closed || started) {
                throw new IllegalStateException(
                        "타이머는 한 번만 시작할 수 있습니다."
                );
            }

            if (!battle.isRunning()) {
                throw new IllegalStateException(
                        "경기를 시작한 후 타이머를 시작해야 합니다."
                );
            }

            started = true;

            if (!settings.isTimeLimited()) {
                return;
            }

            timer = Executors.newSingleThreadScheduledExecutor(task -> {
                Thread thread = new Thread(task, "battle-time-limit");
                thread.setDaemon(true);
                return thread;
            });

            timerTask = timer.scheduleAtFixedRate(
                    () -> checkOnTick(
                            oneScoreSupplier,
                            twoScoreSupplier
                    ),
                    0,
                    CHECK_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void checkOnTick(
            IntSupplier oneScoreSupplier,
            IntSupplier twoScoreSupplier) {
        synchronized (battle) {
            if (closed) {
                return;
            }

            if (!battle.isRunning()) {
                close();
                return;
            }

            if (battle.isPaused()) {
                return;
            }

            try {
                checkAndFinish(
                        oneScoreSupplier.getAsInt(),
                        twoScoreSupplier.getAsInt()
                );
            } catch (RuntimeException | Error failure) {
                close();
                throw failure;
            }
        }
    }

    public boolean checkAndFinish(int oneScore, int twoScore) {
        synchronized (battle) {
            if (closed
                    || !battle.isRunning()
                    || battle.isPaused()
                    || !settings.isTimeLimited()) {
                return false;
            }

            long elapsedPlayMillis =
                    elapsedPlayMillisSupplier.getAsLong();

            if (!settings.hasTimeExpired(elapsedPlayMillis)) {
                return false;
            }

            boolean finished = battle.finishByTime(oneScore, twoScore);

            if (finished) {
                close();
            }

            return finished;
        }
    }

    @Override
    public void close() {
        synchronized (battle) {
            if (closed) {
                return;
            }

            closed = true;

            if (timerTask != null) {
                timerTask.cancel(false);
            }

            if (timer != null) {
                timer.shutdown();
            }
        }
    }
}