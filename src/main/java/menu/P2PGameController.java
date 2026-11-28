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
// 공격 규칙은 로컬 대전과 같은 BattleController를 쓴다. 이 PC의 플레이어(localPlayer)만 사용하고,
// 상대에게 보낼 공격은 drainOutgoingAttacks()로 꺼내 세션으로 보낸다.
public final class P2PGameController {
    private final P2PGameSession session;
    private final GameScreen gameScreen;
    private final GameStateManager gameStateManager;
    private final ScoreManager scoreManager;
    private final BattleController battle;
    private final BattleController.Player localPlayer;
    private final ActionController localAction;
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
            BattleController.Player localPlayer,
            ActionController localAction) {
        this.session = session;
        this.gameScreen = gameScreen;
        this.gameStateManager = gameStateManager;
        this.scoreManager = scoreManager;
        this.battle = battle;
        this.localPlayer = localPlayer;
        this.localAction = localAction;
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
        if (!roundActive) {
            return;
        }
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
            status = GameStatus.capture(localAction, gameStateManager, scoreManager);
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
        notifyGameOver(false);
    }

    // 항복으로 게임을 끝낼 때 사용한다. 상대 화면의 종료 사유가 항복으로 표시된다.
    public void notifySurrender() throws IOException {
        notifyGameOver(true);
    }

    private void notifyGameOver(boolean surrender) throws IOException {
        localGameOver = true;
        GameStatus finalStatus;
        synchronized (battle) {
            // 마지막 보드도 함께 보내 상대 화면에 끝난 모습이 남도록 한다
            GameStatus current = GameStatus.capture(localAction, gameStateManager, scoreManager);
            finalStatus = new GameStatus(
                    current.getScore(),
                    current.getLevel(),
                    current.getLines(),
                    true, // gameOver = true
                    current.getCells());
        }
        if (surrender) {
            session.sendSurrender(finalStatus);
        } else {
            session.sendStatus(finalStatus);
        }
    }

    public boolean isLocalSurrendered() {
        return session.isLocalSurrendered();
    }

    public boolean isRemoteSurrendered() {
        return session.isRemoteSurrendered();
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
