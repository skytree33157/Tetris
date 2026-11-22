package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import game.ActionController;
import game.GameMode;
import game.GameStateManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import network.GameStatus;
import network.P2PGameSession;
import org.junit.jupiter.api.Test;
import score.ScoreManager;

// 게임 오버 순간 상대에게 보내는 마지막 상태에 보드가 함께 실리는지 (실제 로컬 TCP 연결)
class P2PGameControllerTest {
    @Test
    void notifyGameOverSendsFinalBoardToOpponent() throws Exception {
        Board board = new Board();
        GameStateManager manager = new GameStateManager();
        ScoreManager scoreManager = new ScoreManager();
        ActionController action = new ActionController(board, new OBlock(), manager, scoreManager,
                Difficulty.NORMAL, GameMode.NORMAL, 0);
        int bottomRow = board.getHiddenRows() + board.getHeight() - 1;
        board.addBlock(0, bottomRow, new int[][] {{6, 6, 6}});

        CountDownLatch hostStarted = new CountDownLatch(1);
        CountDownLatch clientStarted = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<GameStatus> hostSaw = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (P2PGameSession.HostInvitation invitation =
                        P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            Future<P2PGameSession> accepted = executor.submit(invitation::accept);
            try (P2PGameSession client = P2PGameSession.connect("127.0.0.1");
                    P2PGameSession host = accepted.get(5, TimeUnit.SECONDS)) {
                host.setListener(new P2PGameSession.Listener() {
                    @Override
                    public void onStarted() {
                        hostStarted.countDown();
                    }

                    @Override
                    public void onRemoteStatus(GameStatus status) {
                        if (status.isGameOver()) {
                            hostSaw.set(status);
                            received.countDown();
                        }
                    }
                });
                client.setListener(new P2PGameSession.Listener() {
                    @Override
                    public void onStarted() {
                        clientStarted.countDown();
                    }
                });

                // 게임 화면 없이 컨트롤러만 사용 (화면 갱신은 이 테스트 범위가 아님)
                P2PGameController controller =
                        new P2PGameController(client, null, manager, action, scoreManager);
                controller.notifyPlayerReady();
                host.setReady();
                assertTrue(hostStarted.await(5, TimeUnit.SECONDS), "round should start on host");
                assertTrue(clientStarted.await(5, TimeUnit.SECONDS), "round should start on client");
                controller.onSessionStarted();

                controller.notifyGameOver();

                assertTrue(received.await(5, TimeUnit.SECONDS), "host should receive final status");
                GameStatus finalStatus = hostSaw.get();
                assertTrue(finalStatus.isGameOver());
                assertTrue(finalStatus.hasBoard());
                assertEquals(scoreManager.getScore(), finalStatus.getScore());
                int[] lastRow = finalStatus.getCells()[finalStatus.getCells().length - 1];
                assertEquals(6, lastRow[0]);
                assertEquals(6, lastRow[1]);
                assertEquals(6, lastRow[2]);
                assertEquals(0, lastRow[3]);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
