package menu;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import game.GameMode;
import network.RecentPeerStore;
import network.RoomClient;
import network.RoomProtocol;
import network.RoomServer;

// 방 생성(호스트)과 참가(클라이언트) 연결을 맡는다.
public final class RoomGameRunner {

    // 호스트 한 명이 열어 둔 방. 호스트 자신도 loopback 클라이언트로 참가한다.
    public static final class HostHandle {
        private final RoomServer server;
        private RoomClient selfClient;
        private boolean cancelled;

        private HostHandle(RoomServer server) {
            this.server = server;
        }

        public synchronized boolean startGame(GameMode mode, int timeLimitSeconds) {
            return !cancelled && server.markHostReady(mode, timeLimitSeconds);
        }

        public synchronized void cancel() {
            cancelled = true;
            closeQuietly(selfClient);
            closeQuietly(server);
        }

        // 연결이 끝난 뒤 호출된다. 이미 취소됐다면 바로 닫는다.
        private synchronized boolean adopt(RoomClient client) {
            if (cancelled) {
                closeQuietly(client);
                return false;
            }
            selfClient = client;
            return true;
        }
    }

    private RoomGameRunner() {
    }

    // 참가자 이름: OS 사용자 이름을 쓰고, 없으면 기본값을 쓴다 (길이 제한은 서버에서 적용)
    public static String defaultPlayerName() {
        String name = System.getProperty("user.name");
        return name == null || name.isBlank() ? "플레이어" : name.trim();
    }

    // 방을 연다. 포트 충돌은 BindException으로 바로 전달한다.
    public static HostHandle openRoom(String name, RoomGameController controller) throws IOException {
        RoomServer server = RoomServer.open(RoomProtocol.DEFAULT_PORT);
        HostHandle handle = new HostHandle(server);
        Thread starter = new Thread(() -> {
            try {
                RoomClient selfClient = RoomClient.connect("127.0.0.1", RoomProtocol.DEFAULT_PORT, name, controller);
                if (handle.adopt(selfClient)) {
                    controller.attach(selfClient);
                }
            } catch (IOException exception) {
                controller.onError(describeFailure(exception));
            }
        }, "room-host-client");
        starter.setDaemon(true);
        starter.start();
        return handle;
    }

    // 주소로 방에 참가한다. 연결 결과는 controller 콜백으로 전달된다.
    public static void join(String address, String name, RoomGameController controller, RecentPeerStore recentPeers) {
        Thread joiner = new Thread(() -> {
            try {
                RoomClient client = RoomClient.connect(address, RoomProtocol.DEFAULT_PORT, name, controller);
                controller.attach(client);
                rememberQuietly(recentPeers, address);
            } catch (IOException exception) {
                controller.onError(describeFailure(exception));
            }
        }, "room-join");
        joiner.setDaemon(true);
        joiner.start();
    }

    // 사용자에게 보여줄 연결 실패 문구
    public static String describeFailure(IOException exception) {
        if (exception instanceof BindException) {
            return "포트 " + RoomProtocol.DEFAULT_PORT + "이(가) 이미 사용 중이에요. 다른 TFSE 창이 방을 열고 있지 않은지 확인해 주세요.";
        }
        if (exception instanceof UnknownHostException) {
            return "주소를 찾을 수 없어요. IP를 다시 확인해 주세요.";
        }
        if (exception instanceof SocketTimeoutException) {
            return "응답이 없어요. 상대가 방을 열었는지, 같은 네트워크인지 확인해 주세요.";
        }
        if (exception instanceof ConnectException) {
            return "연결이 거부됐어요. 상대가 방을 열어 둔 상태인지 확인해 주세요.";
        }
        String message = exception.getMessage();
        return "연결하지 못했어요." + (message == null || message.isBlank() ? "" : " (" + message + ")");
    }

    private static void rememberQuietly(RecentPeerStore recentPeers, String address) {
        try {
            recentPeers.remember(address);
        } catch (IOException | IllegalArgumentException ignored) {
            // 주소 저장 실패는 대전 진행에 영향 없음
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
