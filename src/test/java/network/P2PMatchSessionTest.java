package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import blocks.core.BlockType;
import game.GameMode;

class P2PMatchSessionTest {
    private static final int TIMEOUT_SECONDS = 5;

    @Test
    void rankingPlacesLastEliminatedNearTheTopAndHostAliveFirst() throws Exception {
        P2PMatchSession host = P2PMatchSession.createRoom(GameMode.NORMAL, 0, 180_000L);
        P2PMatchSession c1 = null;
        P2PMatchSession c2 = null;
        try {
            c1 = P2PMatchSession.connect("127.0.0.1", host.getPort());
            c2 = P2PMatchSession.connect("127.0.0.1", host.getPort());
            waitUntil(() -> host.getRoster().size() == 3);

            CompletableFuture<List<Integer>> result = new CompletableFuture<>();
            CompletableFuture<Integer> firstEliminated = new CompletableFuture<>();
            host.setListener(new P2PMatchSession.Listener() {
                @Override
                public void onRemoteStatus(int slot, GameStatus status) {
                    if (slot == 1 && status.isGameOver()) {
                        firstEliminated.complete(slot);
                    }
                }
            });
            c1.setListener(new P2PMatchSession.Listener() {
                @Override
                public void onResult(List<Integer> ranking) {
                    result.complete(ranking);
                }
            });

            host.start();
            waitUntil(c1::isMatchRunning);
            waitUntil(c2::isMatchRunning);

            // 탈락 순서가 소켓 간 도착 순서에 따라 바뀌지 않도록 c1의 탈락을 host가 확인한 뒤 c2를 보낸다
            c1.sendStatus(new GameStatus(10, 1, 0, true));
            firstEliminated.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            c2.sendStatus(new GameStatus(5, 1, 0, true));

            List<Integer> ranking = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(List.of(0, 2, 1), ranking);
        } finally {
            closeAll(c1, c2, host);
        }
    }

    @Test
    void attackSkipsEliminatedPlayersAndReachesAliveTarget() throws Exception {
        P2PMatchSession host = P2PMatchSession.createRoom(GameMode.NORMAL, 0, 180_000L);
        P2PMatchSession c1 = null;
        P2PMatchSession c2 = null;
        try {
            c1 = P2PMatchSession.connect("127.0.0.1", host.getPort());
            c2 = P2PMatchSession.connect("127.0.0.1", host.getPort());
            waitUntil(() -> host.getRoster().size() == 3);

            CompletableFuture<List<int[]>> hostAttack = new CompletableFuture<>();
            CompletableFuture<Integer> eliminated = new CompletableFuture<>();
            host.setListener(new P2PMatchSession.Listener() {
                @Override
                public void onAttack(List<int[]> rows) {
                    hostAttack.complete(rows);
                }

                @Override
                public void onRemoteStatus(int slot, GameStatus status) {
                    if (slot == 2 && status.isGameOver()) {
                        eliminated.complete(slot);
                    }
                }
            });

            host.start();
            waitUntil(c1::isMatchRunning);
            waitUntil(c2::isMatchRunning);

            c2.sendStatus(new GameStatus(0, 1, 0, true));
            eliminated.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            int[] row = new int[10];
            row[0] = BlockType.ATTACK.getValue();
            c1.requestAttack(List.of(row));

            List<int[]> received = hostAttack.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(1, received.size());
            assertEquals(BlockType.ATTACK.getValue(), received.get(0)[0]);
        } finally {
            closeAll(c1, c2, host);
        }
    }

    @Test
    void timeLimitEndsMatchWithScoreRanking() throws Exception {
        P2PMatchSession host = P2PMatchSession.createRoom(GameMode.NORMAL, 0, 300L);
        P2PMatchSession c1 = null;
        try {
            c1 = P2PMatchSession.connect("127.0.0.1", host.getPort());
            waitUntil(() -> host.getRoster().size() == 2);

            CompletableFuture<List<Integer>> result = new CompletableFuture<>();
            host.setListener(new P2PMatchSession.Listener() {
                @Override
                public void onResult(List<Integer> ranking) {
                    result.complete(ranking);
                }
            });

            host.start();
            List<Integer> ranking = result.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(2, ranking.size());
            assertTrue(ranking.contains(0) && ranking.contains(1));
        } finally {
            closeAll(c1, host);
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("condition not met within timeout");
            }
            Thread.sleep(10);
        }
    }

    private static void closeAll(P2PMatchSession... sessions) {
        List<P2PMatchSession> list = new ArrayList<>();
        for (P2PMatchSession session : sessions) {
            if (session != null) {
                list.add(session);
            }
        }
        for (P2PMatchSession session : list) {
            session.close();
        }
    }
}
