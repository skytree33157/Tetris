package menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.GameMode;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import network.P2PGameSession;
import org.junit.jupiter.api.Test;

// 대전 메뉴의 방 만들기/참여 화면이 쓰는 연결 흐름 (게임 화면 대신 로비 리스너로 세션을 받는다)
class P2PGameRunnerTest {
    @Test // 방 만들기 → 참여: 양쪽 모두 연결되고, 참여한 쪽은 주소를 최근 접속에 저장
    void hostAndJoinConnectAndJoinerRemembersAddress() throws Exception {
        RecentingListener host = new RecentingListener();
        RecentingListener joiner = new RecentingListener();

        P2PGameRunner.HostHandle handle = P2PGameRunner.startServer(host);
        P2PGameRunner.joinServer("127.0.0.1", joiner);

        try {
            assertTrue(host.connected.await(5, TimeUnit.SECONDS), "host should connect");
            assertTrue(joiner.connected.await(5, TimeUnit.SECONDS), "joiner should connect");
            flushEdt();
            assertTrue(host.session.get().isServer());
            assertEquals(List.of(), host.failures);
            assertEquals(List.of(), joiner.failures);
        } finally {
            closeQuietly(joiner.session.get());
            closeQuietly(host.session.get());
            handle.cancel();
        }
    }

    @Test // 방 만들기 → 참여 → 호스트 모드 선택 → 참여자 준비: 호스트가 준비 상태를 받지만 아직 시작되지는 않음
    void joinerReadinessReachesHostWithoutStartingRound() throws Exception {
        RecentingListener host = new RecentingListener();
        RecentingListener joiner = new RecentingListener();

        P2PGameRunner.HostHandle handle = P2PGameRunner.startServer(host);
        P2PGameRunner.joinServer("127.0.0.1", joiner);

        try {
            assertTrue(host.connected.await(5, TimeUnit.SECONDS), "host should connect");
            assertTrue(joiner.connected.await(5, TimeUnit.SECONDS), "joiner should connect");
            P2PGameSession hostSession = host.session.get();
            P2PGameSession joinerSession = joiner.session.get();

            hostSession.setMode(GameMode.NORMAL);
            joinerSession.setReady();

            assertTrue(host.remoteReady.await(5, TimeUnit.SECONDS), "host should see joiner ready");
            flushEdt();
            // 게임 시작은 양쪽이 모두 준비된 뒤 호스트가 준비할 때 일어나며, 이 테스트에서는 게임 창을 띄우지 않도록 여기서 멈춘다.
            assertFalse(hostSession.isRoundActive());
            assertFalse(joinerSession.isRoundActive());
            assertEquals(List.of(), host.failures);
            assertEquals(List.of(), joiner.failures);
        } finally {
            closeQuietly(joiner.session.get());
            closeQuietly(host.session.get());
            handle.cancel();
        }
    }

    @Test // 대기 취소: 실패로 알리지 않고 포트를 바로 풀어 다시 방을 열 수 있음
    void cancellingHostDoesNotReportFailureAndReleasesPort() throws Exception {
        RecentingListener host = new RecentingListener();

        P2PGameRunner.HostHandle handle = P2PGameRunner.startServer(host);
        handle.cancel();

        Thread.sleep(200);
        flushEdt();
        assertTrue(handle.isCancelled());
        assertEquals(List.of(), host.failures);
        assertEquals(1, host.connected.getCount());
        // 같은 포트로 다시 열 수 있어야 한다
        P2PGameSession.HostInvitation.open(GameMode.NORMAL).close();
    }

    @Test // 이미 방을 열고 있으면 같은 포트로 또 열 수 없음
    void openingSecondRoomFailsWithBindException() throws Exception {
        P2PGameRunner.HostHandle first = P2PGameRunner.startServer(new RecentingListener());
        try {
            IOException failure = assertThrows(IOException.class,
                    () -> P2PGameRunner.startServer(new RecentingListener()));
            assertTrue(failure instanceof BindException);
            assertTrue(P2PGameRunner.describeFailure(failure).contains(
                    String.valueOf(P2PGameSession.DEFAULT_PORT)));
        } finally {
            first.cancel();
        }
    }

    @Test // 열린 방이 없으면 실패를 알림
    void joinWithoutHostReportsFailure() throws Exception {
        RecentingListener joiner = new RecentingListener();

        P2PGameRunner.joinServer("127.0.0.1", joiner);

        assertTrue(joiner.failed.await(10, TimeUnit.SECONDS), "join should fail");
        assertEquals(1, joiner.connected.getCount());
    }

    @Test // 실패 원인별 안내 문구
    void describesCommonFailures() {
        assertTrue(P2PGameRunner.describeFailure(new ConnectException("refused")).contains("거부"));
        assertTrue(P2PGameRunner.describeFailure(new java.net.UnknownHostException("x")).contains("주소"));
        assertTrue(P2PGameRunner.describeFailure(new java.net.SocketTimeoutException()).contains("응답"));
        assertTrue(P2PGameRunner.describeFailure(new IOException("boom")).contains("boom"));
    }

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    private static void closeQuietly(P2PGameSession session) {
        if (session == null) {
            return;
        }
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    private static final class RecentingListener implements P2PGameRunner.ConnectListener {
        final CountDownLatch connected = new CountDownLatch(1);
        final CountDownLatch failed = new CountDownLatch(1);
        final CountDownLatch remoteReady = new CountDownLatch(1);
        final List<IOException> failures = new CopyOnWriteArrayList<>();
        final AtomicReference<P2PGameSession> session = new AtomicReference<>();

        @Override
        public void onConnected(P2PGameSession connectedSession) {
            session.set(connectedSession);
            connected.countDown();
        }

        @Override
        public void onRemoteReady(boolean ready) {
            if (ready) {
                remoteReady.countDown();
            }
        }

        @Override
        public void onGameStarting() {
        }

        @Override
        public void onLobbyDisconnected(IOException exception) {
            failures.add(exception);
        }

        @Override
        public void onFailure(IOException exception) {
            failures.add(exception);
            failed.countDown();
        }
    }
}
