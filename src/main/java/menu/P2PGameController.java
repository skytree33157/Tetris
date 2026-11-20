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
public final class P2PGameController {
    private final P2PGameSession session;
    private final GameScreen gameScreen;
    private final GameStateManager gameStateManager;
    private final ScoreManager scoreManager;
    private final BattleController battle;
    private final BattleController.Player localPlayer;
    private volatile boolean roundActive = false;
    private volatile boolean ready = false;
    private volatile boolean localGameOver = false;
    private boolean battleStarted = false;

    public P2PGameController(
            P2PGameSession session,
            GameScreen gameScreen,
            GameStateManager gameStateManager,
            ScoreManager scoreManager,
            BattleController battle,
            BattleController.Player localPlayer) {
        this.session = session;
        this.gameScreen = gameScreen;
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;
        this.battle = battle;
        this.localPlayer = localPlayer;
    }

    // P2P 세션 onStarted() 이벤트 처리
    public void onSessionStarted() {
        roundActive = true;
        ready = false;
        // BattleController.start()는 한 번만 호출할 수 있으므로 중복 호출을 막는다
        synchronized (battle) {
            if (!battleStarted) {
                battleStarted = true;
                battle.start();
            }
        }
    }

    // 상대가 지운 줄만큼 받은 방해 줄을 로컬 플레이어 대기열에 추가
    public void onRemoteAttack(List<int[]> rows) {
        battle.receiveAttack(localPlayer, rows);
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

        // 상태 캡처와 공격 꺼내기는 경기 잠금 안에서, 소켓 전송은 잠금을 푼 뒤 한다.
        // 수신 스레드는 세션 잠금 → 경기 잠금 순서이므로 여기서 경기 잠금 중에 전송하면 교착이 생길 수 있다.
        GameStatus status;
        List<BattleController.Attack> attacks;
        synchronized (battle) {
            status = GameStatus.capture(battle, gameStateManager, scoreManager);
            attacks = battle.drainOutgoingAttacks();
        }
        // 상태 전송 시 라운드가 즉시 끝날 수 있으므로, 패배 판정 플래그를 먼저 세운다
        if (status.isGameOver()) {
            localGameOver = true;
        }
        session.sendStatus(status);
        for (BattleController.Attack attack : attacks) {
            session.sendAttack(attack.rows());
        }
    }

    // P2P 세션에 게임 종료 전달
    public void notifyGameOver() throws IOException {
        localGameOver = true;
        GameStatus finalStatus;
        synchronized (battle) {
            finalStatus = new GameStatus(
                    scoreManager.getScore(),
                    gameStateManager.getCurrentLevel(),
                    gameStateManager.getTotalLinesCleared(),
                    true); // gameOver = true
        }
        session.sendStatus(finalStatus);
    }

    // 라운드가 활성화되어있는지 확인
    public boolean isRoundActive() {
        return roundActive;
    }

    // 이 화면의 플레이어가 먼저 게임 오버되었는지 확인 (패배 판정용)
    public boolean isLocalGameOver() {
        return localGameOver;
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
