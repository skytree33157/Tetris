package menu;

import java.io.IOException;
import javax.swing.SwingUtilities;

import game.ActionController;
import game.GameMode;
import game.GameStateManager;
import network.GameStatus;
import network.P2PGameSession;
import score.ScoreManager;

//P2P 게임 화면과 P2PGameSession을 연결하는 컨트롤러
public final class P2PGameController {
    private final P2PGameSession session;
    private final GameScreen gameScreen;
    private final GameStateManager gameStateManager;
    private final ActionController actionController;
    private final ScoreManager scoreManager;
    private volatile boolean roundActive = false;
    private volatile boolean ready = false;

    public P2PGameController(
            P2PGameSession session,
            GameScreen gameScreen,
            GameStateManager gameStateManager,
            ActionController actionController,
            ScoreManager scoreManager) {
        this.session = session;
        this.gameScreen = gameScreen;
        this.gameStateManager = gameStateManager;
        this.actionController = actionController;
        this.scoreManager = scoreManager;
    }

    // P2P 세션 onStarted() 이벤트 처리
    public void onSessionStarted() {
        roundActive = true;
        ready = false;
    }

    // P2P 세션 onRemoteStatus() 이벤트 처리
    public void onRemoteStatus(GameStatus status) {
        // 상대 플레이어의 게임 상태 수신
        SwingUtilities.invokeLater(() -> {
            gameScreen.updateRemoteStatus(status);
        });
    }

    // P2P 세션 onRoundFinished() 이벤트 처리
    public void onRoundFinished() {
        roundActive = false;
    }

    // 게임 준비 상태를 P2P 세션에 전달
    public void notifyPlayerReady() throws IOException {
        if (!ready) {
            ready = true;
            session.setReady();
        }
    }

    //게임 상태를 P2P 세션으로 전송
    public void sendGameStatus() throws IOException {
        if (!roundActive) {
            return;
        }

        // 현재 게임 상태 캡처
        synchronized (actionController) {
            GameStatus status = GameStatus.capture(
                    actionController,
                    gameStateManager,
                    scoreManager);
            session.sendStatus(status);
        }
    }

    // P2P 세션에 게임 종료 전달
    public void notifyGameOver() throws IOException {
        synchronized (actionController) {
            GameStatus finalStatus = new GameStatus(
                    scoreManager.getScore(),
                    gameStateManager.getCurrentLevel(),
                    gameStateManager.getTotalLinesCleared(),
                    true); // gameOver = true
            session.sendStatus(finalStatus);
        }
    }

    // 라운드가 활성화되어있는지 확인
    public boolean isRoundActive() {
        return roundActive;
    }

    public boolean isReady() {
        return ready;
    }

    public P2PGameSession getSession() {
        return session;
    }

    // 상대방 마지막 상태 조회
    public GameStatus getRemoteStatus() {
        return session.getRemoteStatus();
    }
}
