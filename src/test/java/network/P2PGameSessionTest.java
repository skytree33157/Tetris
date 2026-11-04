package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.GameMode;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class P2PGameSessionTest {
    @Test
    void synchronizesServerModeReadinessPlayerStatusAndRoundCompletion() throws Exception {
        // 각 비동기 이벤트의 도착을 기다려 네트워크 스레드와 테스트 스레드 사이의 경쟁을 피한다.
        CountDownLatch serverStarted = new CountDownLatch(1);
        CountDownLatch clientStarted = new CountDownLatch(1);
        CountDownLatch serverStartedAgain = new CountDownLatch(1);
        CountDownLatch clientStartedAgain = new CountDownLatch(1);
        CountDownLatch serverReceivedStatus = new CountDownLatch(1);
        CountDownLatch clientReceivedStatus = new CountDownLatch(1);
        CountDownLatch serverRoundFinished = new CountDownLatch(1);
        CountDownLatch clientRoundFinished = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger serverStartCount =
                new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger clientStartCount =
                new java.util.concurrent.atomic.AtomicInteger();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 테스트 내부의 로컬 호스트를 이용해 실제 TCP 핸드셰이크와 양방향 메시지를 검증한다.
        try (P2PGameSession.HostInvitation invitation =
                        P2PGameSession.HostInvitation.open(GameMode.ITEM)) {
            // accept()는 클라이언트 접속을 기다리므로 별도 스레드에서 실행한다.
            var accepted = executor.submit(invitation::accept);
            try (P2PGameSession client = P2PGameSession.connect("127.0.0.1");
                    P2PGameSession server = accepted.get(5, TimeUnit.SECONDS)) {
                // 클라이언트는 연결 시 호스트가 선택한 모드를 받아야 한다.
                assertEquals(GameMode.ITEM, client.getMode());

                // 첫 시작과 다음 라운드 시작을 각각 확인할 수 있도록 시작 횟수를 구분한다.
                server.setListener(new P2PGameSession.Listener() {
                    @Override
                    public void onStarted() {
                        if (serverStartCount.getAndIncrement() == 0) {
                            serverStarted.countDown();
                        } else {
                            serverStartedAgain.countDown();
                        }
                    }

                    @Override
                    public void onRemoteStatus(GameStatus status) {
                        assertEquals(73, status.getScore());
                        serverReceivedStatus.countDown();
                    }

                    @Override
                    public void onRoundFinished() {
                        serverRoundFinished.countDown();
                    }
                });
                client.setListener(new P2PGameSession.Listener() {
                    @Override
                    public void onStarted() {
                        if (clientStartCount.getAndIncrement() == 0) {
                            clientStarted.countDown();
                        } else {
                            clientStartedAgain.countDown();
                        }
                    }

                    @Override
                    public void onRemoteStatus(GameStatus status) {
                        assertEquals(42, status.getScore());
                        clientReceivedStatus.countDown();
                    }

                    @Override
                    public void onRoundFinished() {
                        clientRoundFinished.countDown();
                    }
                });

                // 한쪽만 준비된 상태에서는 시작되지 않고, 양쪽이 준비된 후에만 양쪽이 시작 이벤트를 받는다.
                client.setReady();
                assertFalse(serverStarted.await(200, TimeUnit.MILLISECONDS));
                server.setReady();
                assertTrue(serverStarted.await(5, TimeUnit.SECONDS));
                assertTrue(clientStarted.await(5, TimeUnit.SECONDS));

                // 각 피어가 보낸 점수 상태가 반대편 세션의 리스너에 전달되는지 확인한다.
                server.sendStatus(status(42, false));
                client.sendStatus(status(73, false));
                assertTrue(serverReceivedStatus.await(5, TimeUnit.SECONDS));
                assertTrue(clientReceivedStatus.await(5, TimeUnit.SECONDS));

                // 한 플레이어의 게임 오버 상태가 공유되면 양쪽 라운드가 종료된다.
                server.sendStatus(status(42, true));
                assertTrue(serverRoundFinished.await(5, TimeUnit.SECONDS));
                assertTrue(clientRoundFinished.await(5, TimeUnit.SECONDS));

                // 라운드 종료 후 연결을 재사용해 다시 양쪽 준비 및 시작이 가능한지 검증한다.
                client.setReady();
                server.setReady();
                assertTrue(serverStartedAgain.await(5, TimeUnit.SECONDS));
                assertTrue(clientStartedAgain.await(5, TimeUnit.SECONDS));
                assertTrue(server.isRoundActive());
                assertTrue(client.isRoundActive());
            }
        } finally {
            // 테스트 실패 여부와 관계없이 대기 스레드가 남지 않게 정리한다.
            executor.shutdownNow();
        }
    }

    @Test
    void hostInvitationCanBeClosedWhileWaitingForAClient() throws Exception {
        // 접속을 기다리는 서버 소켓을 닫으면 accept()가 예외로 종료되어야 한다.
        P2PGameSession.HostInvitation invitation =
                P2PGameSession.HostInvitation.open(GameMode.NORMAL);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            var accepting = executor.submit(() -> {
                try {
                    invitation.accept();
                } catch (IOException expectedWhenClosed) {
                    return true;
                }
                return false;
            });
            invitation.close();
            assertTrue(accepting.get(5, TimeUnit.SECONDS));
        } finally {
            // 예외가 발생하더라도 테스트 실행기를 종료한다.
            executor.shutdownNow();
        }
    }

    /** 테스트에서 전송할 간단한 플레이어 상태를 만든다. */
    private static GameStatus status(int score, boolean gameOver) {
        return new GameStatus(score, 1, 0, gameOver);
    }
}
