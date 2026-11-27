package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import network.P2PGameSession;

class P2PLobbyChatTest {

    private final LobbyListener host = new LobbyListener();
    private final LobbyListener client = new LobbyListener();

    private P2PGameRunner.HostHandle handle;

    @BeforeEach
    void setUp() throws Exception {
        // 실제 대기 화면과 같은 Runner 연결 경로를 사용한다.
        handle = P2PGameRunner.startServer(host);
        P2PGameRunner.joinServer("127.0.0.1", client);

        assertTrue(
                host.connected.await(5, TimeUnit.SECONDS),
                "방장 연결이 완료되지 않았습니다."
        );
        assertTrue(
                client.connected.await(5, TimeUnit.SECONDS),
                "참가자 연결이 완료되지 않았습니다."
        );
    }

    @AfterEach
    void tearDown() {
        closeQuietly(client.session);
        closeQuietly(host.session);

        if (handle != null) {
            handle.cancel();
        }
    }

    @Test
    void deliversChatInBothDirectionsOnEdt() throws Exception {
        host.session.sendChat("안녕하세요! 방장입니다.");
        client.session.sendChat("반갑습니다! 참가자입니다.");

        assertEquals(
                "안녕하세요! 방장입니다.",
                client.messages.poll(5, TimeUnit.SECONDS)
        );
        assertEquals(
                "반갑습니다! 참가자입니다.",
                host.messages.poll(5, TimeUnit.SECONDS)
        );

        // 화면용 리스너는 Swing 화면 처리 스레드에서 호출돼야 한다.
        assertTrue(host.allChatsOnEdt.get());
        assertTrue(client.allChatsOnEdt.get());
    }

    @Test
    void preservesChatOrder() throws Exception {
        List<String> messages = List.of(
                "첫 번째 메시지",
                "두 번째 메시지",
                "세 번째 메시지"
        );

        for (String message : messages) {
            host.session.sendChat(message);
        }

        for (String expected : messages) {
            assertEquals(
                    expected,
                    client.messages.poll(5, TimeUnit.SECONDS)
            );
        }
    }

    @Test
    void forwardsChatAfterOnlyOnePlayerIsReady() throws Exception {
        client.session.setReady();

        // 준비 알림도 기존 경로로 정상 전달돼야 한다.
        assertTrue(host.remoteReady.await(5, TimeUnit.SECONDS));

        client.session.sendChat("준비됐어요!");
        assertEquals(
                "준비됐어요!",
                host.messages.poll(5, TimeUnit.SECONDS)
        );

        host.session.sendChat("잠시만 기다려 주세요.");
        assertEquals(
                "잠시만 기다려 주세요.",
                client.messages.poll(5, TimeUnit.SECONDS)
        );

        assertFalse(host.session.isRoundActive());
        assertFalse(client.session.isRoundActive());
    }

    private static void closeQuietly(P2PGameSession session) {
        if (session == null) {
            return;
        }

        try {
            session.close();
        } catch (IOException ignored) {
            // 테스트 정리 중 상대가 먼저 연결을 닫았을 수 있다.
        }
    }

    // 실제 화면 대신 연결 결과와 채팅을 받아 검사한다.
    private static final class LobbyListener
            implements P2PGameRunner.ConnectListener {

        private final CountDownLatch connected = new CountDownLatch(1);
        private final CountDownLatch remoteReady = new CountDownLatch(1);

        private final BlockingQueue<String> messages =
                new LinkedBlockingQueue<>();

        private final AtomicBoolean allChatsOnEdt =
                new AtomicBoolean(true);

        private volatile P2PGameSession session;

        @Override
        public void onConnected(P2PGameSession connectedSession) {
            session = connectedSession;
            connected.countDown();
        }

        @Override
        public void onRemoteReady(boolean ready) {
            if (ready) {
                remoteReady.countDown();
            }
        }

        @Override
        public void onRemoteChat(String message) {
            if (!SwingUtilities.isEventDispatchThread()) {
                allChatsOnEdt.set(false);
            }

            messages.add(message);
        }

        @Override
        public void onGameStarting() {
        }

        @Override
        public void onLobbyDisconnected(IOException exception) {
        }

        @Override
        public void onFailure(IOException exception) {
        }
    }
}