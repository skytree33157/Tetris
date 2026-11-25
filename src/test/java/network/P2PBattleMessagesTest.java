package network;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import blocks.core.BlockType;
import game.GameMode;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

// 대전용 메시지: 제한 시간 핸드셰이크, 공격 줄 전송, 호스트의 시간 종료 판정 (실제 로컬 TCP 연결)
class P2PBattleMessagesTest {
    private static final int A = BlockType.ATTACK.getValue();

    @Test
    void timeLimitAttackRowsAndTimeUpReachTheOtherSide() throws Exception {
        CountDownLatch hostStarted = new CountDownLatch(1);
        CountDownLatch clientStarted = new CountDownLatch(1);
        CountDownLatch hostGotAttack = new CountDownLatch(1);
        CountDownLatch clientGotTimeUp = new CountDownLatch(1);
        CountDownLatch clientRoundFinished = new CountDownLatch(1);
        AtomicReference<List<int[]>> received = new AtomicReference<>();
        AtomicReference<int[]> scores = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try (P2PGameSession.HostInvitation invitation =
                        P2PGameSession.HostInvitation.open(GameMode.NORMAL, 180)) {
            var accepted = executor.submit(invitation::accept);
            try (P2PGameSession client = P2PGameSession.connect("127.0.0.1");
                    P2PGameSession host = accepted.get(5, TimeUnit.SECONDS)) {
                // 클라이언트는 호스트가 고른 제한 시간을 받는다
                assertEquals(180, client.getTimeLimitSeconds());
                assertTrue(client.isTimeLimited());
                assertEquals(180, host.getTimeLimitSeconds());

                host.setListener(new P2PGameSession.Listener() {
                    @Override
                    public void onStarted() {
                        hostStarted.countDown();
                    }

                    @Override
                    public void onAttack(List<int[]> rows) {
                        received.set(rows);
                        hostGotAttack.countDown();
                    }
                });
                client.setListener(new P2PGameSession.Listener() {
                    @Override
                    public void onStarted() {
                        clientStarted.countDown();
                    }

                    @Override
                    public void onRoundFinished() {
                        clientRoundFinished.countDown();
                    }

                    @Override
                    public void onTimeUp(int hostScore, int clientScore) {
                        scores.set(new int[] {hostScore, clientScore});
                        clientGotTimeUp.countDown();
                    }
                });

                client.setReady();
                host.setReady();
                assertTrue(hostStarted.await(5, TimeUnit.SECONDS));
                assertTrue(clientStarted.await(5, TimeUnit.SECONDS));

                // 구멍 위치까지 그대로 전달된다
                int[] first = {A, A, A, 0, A, A, A, A, A, A};
                int[] second = {A, A, A, A, A, A, A, A, 0, A};
                client.sendAttack(List.of(first, second));
                assertTrue(hostGotAttack.await(5, TimeUnit.SECONDS));
                assertEquals(2, received.get().size());
                assertArrayEquals(first, received.get().get(0));
                assertArrayEquals(second, received.get().get(1));

                // 시간 종료는 호스트만 보낼 수 있고, 보내면 양쪽 라운드가 끝난다
                assertThrows(IllegalStateException.class, () -> client.sendTimeUp(1, 2));
                host.sendTimeUp(1500, 900);
                assertFalse(host.isRoundActive());
                assertTrue(clientGotTimeUp.await(5, TimeUnit.SECONDS));
                assertTrue(clientRoundFinished.await(5, TimeUnit.SECONDS));
                assertArrayEquals(new int[] {1500, 900}, scores.get());
                assertFalse(client.isRoundActive());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void timeLimitDefaultsToNoneAndRejectsInvalidValues() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> P2PGameSession.HostInvitation.open(GameMode.NORMAL, -1));
        assertThrows(IllegalArgumentException.class,
                () -> P2PGameSession.HostInvitation.open(GameMode.NORMAL,
                        P2PGameSession.MAX_TIME_LIMIT_SECONDS + 1));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (P2PGameSession.HostInvitation invitation = P2PGameSession.HostInvitation.open(GameMode.ITEM)) {
            var accepted = executor.submit(invitation::accept);
            try (P2PGameSession client = P2PGameSession.connect("127.0.0.1");
                    P2PGameSession host = accepted.get(5, TimeUnit.SECONDS)) {
                assertEquals(0, client.getTimeLimitSeconds());
                assertFalse(client.isTimeLimited());
                assertFalse(host.isTimeLimited());
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
