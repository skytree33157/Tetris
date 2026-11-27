package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import game.GameMode;

class P2PChatTest {

    private P2PGameSession host;
    private P2PGameSession client;

    // 수신한 메시지를 네트워크 스레드에서 안전하게 전달받는다.
    private final BlockingQueue<String> hostMessages =
            new LinkedBlockingQueue<>();
    private final BlockingQueue<String> clientMessages =
            new LinkedBlockingQueue<>();

    private final CountDownLatch hostStarted = new CountDownLatch(1);
    private final CountDownLatch clientStarted = new CountDownLatch(1);

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

        host.setListener(new P2PGameSession.Listener() {
            @Override
            public void onRemoteChat(String message) {
                hostMessages.add(message);
            }

            @Override
            public void onStarted() {
                hostStarted.countDown();
            }
        });

        client.setListener(new P2PGameSession.Listener() {
            @Override
            public void onRemoteChat(String message) {
                clientMessages.add(message);
            }

            @Override
            public void onStarted() {
                clientStarted.countDown();
            }
        });
    }

    @AfterEach
    void tearDown() {
        closeQuietly(client);
        closeQuietly(host);
    }

    @Test
    void exchangesKoreanChatInBothDirections() throws Exception {
        String hostMessage = "안녕하세요! 방장입니다.";
        String clientMessage = "반갑습니다! 참가자입니다.";

        host.sendChat(hostMessage);
        client.sendChat(clientMessage);

        assertEquals(
                hostMessage,
                clientMessages.poll(5, TimeUnit.SECONDS)
        );
        assertEquals(
                clientMessage,
                hostMessages.poll(5, TimeUnit.SECONDS)
        );

        // 채팅만으로 게임이 시작되면 안 된다.
        assertFalse(host.isRoundActive());
        assertFalse(client.isRoundActive());
    }

    @Test
    void sendsMessageAtMaximumLength() throws Exception {
        String message = "가".repeat(P2PGameSession.MAX_CHAT_LENGTH);

        host.sendChat(message);

        assertEquals(
                message,
                clientMessages.poll(5, TimeUnit.SECONDS)
        );
    }

    @Test
    void rejectsInvalidMessagesWithoutBreakingConnection() throws Exception {
        assertThrows(
                NullPointerException.class,
                () -> host.sendChat(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> host.sendChat("")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> host.sendChat("   ")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> host.sendChat(
                        "가".repeat(P2PGameSession.MAX_CHAT_LENGTH + 1)
                )
        );

        // 잘못된 입력을 거절한 뒤에도 정상 메시지는 전달돼야 한다.
        host.sendChat("정상 메시지입니다.");

        assertEquals(
                "정상 메시지입니다.",
                clientMessages.poll(5, TimeUnit.SECONDS)
        );
    }

    @Test
    void allowsChatWhenOnlyOnePlayerIsReady() throws Exception {
        client.setReady();
        client.sendChat("준비됐어요!");

        assertEquals(
                "준비됐어요!",
                hostMessages.poll(5, TimeUnit.SECONDS)
        );

        assertFalse(host.isRoundActive());
        assertFalse(client.isRoundActive());

        host.sendChat("잠시만 기다려 주세요.");

        assertEquals(
                "잠시만 기다려 주세요.",
                clientMessages.poll(5, TimeUnit.SECONDS)
        );
    }

    @Test
    void exchangesChatAfterGameStarts() throws Exception {
        client.setReady();
        host.setReady();

        assertTrue(hostStarted.await(5, TimeUnit.SECONDS));
        assertTrue(clientStarted.await(5, TimeUnit.SECONDS));

        String hostMessage = "게임 중 방장 메시지";
        String clientMessage = "게임 중 참가자 메시지";

        host.sendChat(hostMessage);
        client.sendChat(clientMessage);

        assertEquals(
                hostMessage,
                clientMessages.poll(5, TimeUnit.SECONDS)
        );
        assertEquals(
                clientMessage,
                hostMessages.poll(5, TimeUnit.SECONDS)
        );

        // 채팅을 주고받아도 라운드는 계속 활성 상태여야 한다.
        assertTrue(host.isRoundActive());
        assertTrue(client.isRoundActive());
    }

    @Test
    void rejectsChatAfterConnectionCloses() throws Exception {
        host.close();

        assertThrows(
                IOException.class,
                () -> host.sendChat("연결 종료 후 메시지")
        );
    }

    private static void closeQuietly(P2PGameSession session) {
        if (session == null) {
            return;
        }

        try {
            session.close();
        } catch (IOException ignored) {
            // 테스트 정리 중에는 상대가 이미 연결을 닫았을 수 있다.
        }
    }
}