package menu;

import java.io.IOException;
import java.util.List;
import javax.swing.SwingUtilities;

import game.ActionController;
import game.BattleController;
import game.GameStateManager;
import menu.screens.GameScreen;
import network.GameStatus;
import network.P2PGameSession;
import score.ScoreManager;

//P2P 게임 화면과 P2PGameSession을 연결하는 컨트롤러
// 공격 규칙은 로컬 대전과 같은 BattleController를 쓴다. 이 PC의 플레이어만 등록하고,
// 상대에게 보낼 공격은 drainOutgoingAttacks()로 꺼내 세션으로 보낸다.
public final class P2PGameController {
    private static final BattleController.Player LOCAL = BattleController.Player.ONE;

    private final P2PGameSession session;
    private final GameScreen gameScreen;
    private final GameStateManager gameStateManager;
    private final ActionController actionController;
    private final ScoreManager scoreManager;
    private final BattleController battle = new BattleController();
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
        // 게임 루프가 돌기 전에 등록해야 한다 (등록하면 ActionController의 잠금이 경기 객체로 바뀐다)
        battle.registerPlayer(LOCAL, actionController);
    }

    // P2P 세션 onStarted() 이벤트 처리
    public void onSessionStarted() {
        roundActive = true;
        ready = false;
        synchronized (battle) {
            if (!battle.isRunning() && battle.getOutcome() == BattleController.Outcome.IN_PROGRESS) {
                battle.start();
            }
        }
    }

    // 상대가 보낸 공격 줄을 내 대기열에 넣는다. 다음 블록이 고정될 때 보드에 들어간다.
    public void onRemoteAttack(List<int[]> rows) {
        if (!roundActive) {
            return;
        }
        battle.receiveAttack(LOCAL, rows);
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

        // 지난 전송 이후 만든 공격을 먼저 보낸다. 소켓 쓰기는 경기 잠금 밖에서 한다.
        for (BattleController.Attack attack : battle.drainOutgoingAttacks()) {
            session.sendAttack(attack.rows());
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
            // 마지막 보드도 함께 보내 상대 화면에 끝난 모습이 남도록 한다
            GameStatus current = GameStatus.capture(actionController, gameStateManager, scoreManager);
            GameStatus finalStatus = new GameStatus(
                    current.getScore(),
                    current.getLevel(),
                    current.getLines(),
                    true, // gameOver = true
                    current.getCells());
            session.sendStatus(finalStatus);
        }
    }

    // 호스트 전용: 시간 제한이 끝났음을 확정 점수와 함께 알린다
    public void sendTimeUp(int hostScore, int clientScore) throws IOException {
        session.sendTimeUp(hostScore, clientScore);
        roundActive = false;
    }

    public boolean isHost() {
        return session.isServer();
    }

    public int getTimeLimitSeconds() {
        return session.getTimeLimitSeconds();
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
