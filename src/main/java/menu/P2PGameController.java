package menu;

import java.util.List;

import game.ActionController;
import game.GameStateManager;
import game.MatchLink;
import score.ScoreManager;
import network.GameStatus;
import network.P2PMatchSession;

// 로컬 게임 엔진과 N인 네트워크 매치 세션을 연결하는 어댑터
public final class P2PGameController implements MatchLink {
    private final P2PMatchSession session;
    private final GameStateManager gameStateManager;
    private final ActionController actionController;
    private final ScoreManager scoreManager;

    public P2PGameController(
            P2PMatchSession session,
            GameStateManager gameStateManager,
            ActionController actionController,
            ScoreManager scoreManager) {
        this.session = session;
        this.gameStateManager = gameStateManager;
        this.actionController = actionController;
        this.scoreManager = scoreManager;
    }

    public void attach() {
        actionController.attachMatchLink(this);
    }

    @Override
    public boolean isRunning() {
        return session.isMatchRunning();
    }

    @Override
    public void publishAttack(List<int[]> rows) {
        session.requestAttack(rows);
    }

    @Override
    public void reportGameOver() {
        session.sendStatus(new GameStatus(
                scoreManager.getScore(),
                gameStateManager.getCurrentLevel(),
                gameStateManager.getTotalLinesCleared(),
                true));
    }

    // 진행 중인 게임의 현재 상태를 상대 플레이어들에게 전송
    public void sendGameStatus() {
        if (!session.isMatchRunning()) {
            return;
        }
        GameStatus status;
        synchronized (actionController) {
            status = GameStatus.capture(actionController, gameStateManager, scoreManager);
        }
        session.sendStatus(status);
    }
}
