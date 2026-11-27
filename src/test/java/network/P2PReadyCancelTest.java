package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import game.GameMode;

class P2PReadyCancelTest {

    private P2PGameSession host;
    private P2PGameSession client;

    private final SessionEvents hostEvents = new SessionEvents();
    private final SessionEvents clientEvents = new SessionEvents();

    @BeforeEach
    void setUp() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (P2PGameSession.HostInvitation invitation =
                P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {

            var accepted = executor.submit(invitation::accept);

            client = P2PGameSession.connect("127.0.0.1");
            host = accepted.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        host.setListener(hostEvents);
        client.setListener(clientEvents);
    }

    @AfterEach
    void tearDown() {
        closeQuietly(client);
        closeQuietly(host);
    }

    @Test
    void clientCanCancelAndPrepareAgain() throws Exception {
        client.setReady();
        assertReady(hostEvents, true);

        assertTrue(client.cancelReady());
        assertReady(hostEvents, false);

        // 참가자가 취소했으므로 방장만 준비해도 시작되지 않는다.
        host.setReady();
        assertReady(clientEvents, true);

        assertFalse(host.isRoundActive());
        assertFalse(client.isRoundActive());

        // 참가자가 다시 준비하면 양쪽 모두 시작한다.
        client.setReady();
        assertStarted();
    }

    @Test
    void hostCanCancelAndPrepareAgain() throws Exception {
        host.setReady();
        assertReady(clientEvents, true);

        assertTrue(host.cancelReady());
        assertReady(clientEvents, false);

        // 방장이 취소했으므로 참가자만 준비해도 시작되지 않는다.
        client.setReady();
        assertReady(hostEvents, true);

        assertFalse(host.isRoundActive());
        assertFalse(client.isRoundActive());

        host.setReady();
        assertStarted();
    }

    @Test
    void repeatedCancellationIsIgnoredAndChatStillWorks() throws Exception {
        // 아직 준비하지 않았으면 취소할 것이 없다.
        assertFalse(host.cancelReady());
        assertFalse(client.cancelReady());

        client.setReady();
        assertReady(hostEvents, true);

        assertTrue(client.cancelReady());
        assertReady(hostEvents, false);

        // 이미 취소했으므로 다시 취소하면 false다.
        assertFalse(client.cancelReady());

        // 준비 해제는 연결이나 채팅을 종료하지 않는다.
        client.sendChat("준비를 해제했어요.");
        assertEquals(
                "준비를 해제했어요.",
                hostEvents.chats.poll(5, TimeUnit.SECONDS)
        );

        host.sendChat("다시 준비하면 알려 주세요.");
        assertEquals(
                "다시 준비하면 알려 주세요.",
                clientEvents.chats.poll(5, TimeUnit.SECONDS)
        );
    }

    @Test
    void cannotCancelAfterGameStarts() throws Exception {
        client.setReady();
        host.setReady();
        assertStarted();

        assertFalse(host.cancelReady());
        assertFalse(client.cancelReady());

        assertTrue(host.isRoundActive());
        assertTrue(client.isRoundActive());
        assertTrue(host.isConnected());
        assertTrue(client.isConnected());
    }

    @Test
    void startedMatchSurvivesCrossedCancellation() throws Exception {
        CompletableFuture<Boolean> cancellation =
                new CompletableFuture<>();

        SessionEvents raceClientEvents = new SessionEvents() {
            @Override
            public void onRemoteReady(boolean ready) {
                super.onRemoteReady(ready);

                if (!ready) {
                    return;
                }

                try {
                    // 방장이 시작을 확정한 뒤 참가자의 취소를 전송한다.
                    // 참가자 수신 스레드는 아직 START를 처리하기 전이다.
                    if (!hostEvents.started.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "방장의 게임 시작이 확인되지 않았습니다."
                        );
                    }

                    cancellation.complete(client.cancelReady());
                } catch (Exception exception) {
                    if (exception instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }

                    cancellation.completeExceptionally(exception);
                }
            }
        };

        client.setListener(raceClientEvents);

        client.setReady();
        assertReady(hostEvents, true);

        host.setReady();

        // 취소 요청은 보냈지만 이미 확정된 게임 시작을 우선한다.
        assertTrue(cancellation.get(10, TimeUnit.SECONDS));
        assertTrue(
                raceClientEvents.started.await(5, TimeUnit.SECONDS)
        );

        assertTrue(host.isRoundActive());
        assertTrue(client.isRoundActive());

        // 취소 신호 뒤에 보낸 상태 메시지도 도착해야 한다.
        // 이를 통해 늦은 취소 때문에 연결이 끊어지지 않았는지 확인한다.
        client.sendStatus(new GameStatus(17, 1, 0, false));

        GameStatus received =
                hostEvents.statuses.poll(5, TimeUnit.SECONDS);

        assertNotNull(received);
        assertEquals(17, received.getScore());
        assertTrue(host.isConnected());
        assertTrue(client.isConnected());
    }

    private void assertStarted() throws InterruptedException {
        assertTrue(hostEvents.started.await(5, TimeUnit.SECONDS));
        assertTrue(clientEvents.started.await(5, TimeUnit.SECONDS));

        assertTrue(host.isRoundActive());
        assertTrue(client.isRoundActive());
    }

    private static void assertReady(
            SessionEvents events,
            boolean expected
    ) throws InterruptedException {
        assertEquals(
                Boolean.valueOf(expected),
                events.readyStates.poll(5, TimeUnit.SECONDS)
        );
    }

    private static void closeQuietly(P2PGameSession session) {
        if (session == null) {
            return;
        }

        try {
            session.close();
        } catch (IOException ignored) {
            // テスト終了時には相手が先に切断している場合がある。
        }
    }

    private static class SessionEvents
            implements P2PGameSession.Listener {

        final BlockingQueue<Boolean> readyStates =
                new LinkedBlockingQueue<>();
        final BlockingQueue<String> chats =
                new LinkedBlockingQueue<>();
        final BlockingQueue<GameStatus> statuses =
                new LinkedBlockingQueue<>();

        final CountDownLatch started = new CountDownLatch(1);

        @Override
        public void onRemoteReady(boolean ready) {
            readyStates.add(ready);
        }

        @Override
        public void onRemoteChat(String message) {
            chats.add(message);
        }

        @Override
        public void onRemoteStatus(GameStatus status) {
            statuses.add(status);
        }

        @Override
        public void onStarted() {
            started.countDown();
        }
    }
}