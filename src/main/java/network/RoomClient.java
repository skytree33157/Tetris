package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import game.GameMode;

// 방 서버에 접속하는 클라이언트. 모든 콜백은 리더 스레드에서 호출되므로 UI에서는 EDT로 넘겨야 한다.
public final class RoomClient implements AutoCloseable {

    public interface Listener {
        void onRoster(int mySlot, List<RoomMember> members);

        void onStart(GameMode mode, int timeLimitSeconds);

        void onStatus(int slot, GameStatus status);

        void onAttack(List<int[]> rows);

        void onPlayerOut(int slot);

        // 순위는 1등부터 나열된 슬롯 목록이다.
        void onRoomEnd(List<Integer> rankingSlots);

        void onError(String message);

        void onDisconnected();
    }

    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    private final Listener listener;
    private final ScheduledExecutorService keepAlive;
    private final AtomicBoolean closed = new AtomicBoolean();

    private RoomClient(Socket socket, DataInputStream input, DataOutputStream output, Listener listener) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.listener = listener;
        this.keepAlive = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "room-client-keep-alive");
            thread.setDaemon(true);
            return thread;
        });
    }

    // 방에 접속하고 리더 스레드를 시작한다. 거절되면 리더가 onError를 호출한 뒤 연결을 닫는다.
    public static RoomClient connect(String host, int port, String name, Listener listener) throws IOException {
        Socket socket = new Socket();
        try {
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, port), RoomProtocol.HANDSHAKE_TIMEOUT_MS);
            socket.setSoTimeout(RoomProtocol.HANDSHAKE_TIMEOUT_MS);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(RoomProtocol.MAGIC);
            output.writeUTF(name);
            output.flush();
            RoomClient client = new RoomClient(socket, input, output, listener);
            client.start();
            return client;
        } catch (IOException | RuntimeException exception) {
            socket.close();
            throw exception;
        }
    }

    private void start() {
        Thread reader = new Thread(this::readLoop, "room-client-reader");
        reader.setDaemon(true);
        reader.start();
        keepAlive.scheduleAtFixedRate(this::sendKeepAlive,
                RoomProtocol.KEEP_ALIVE_INTERVAL_MS, RoomProtocol.KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void sendStatus(GameStatus status) {
        send(() -> {
            output.writeByte(RoomProtocol.C_STATUS);
            status.writeTo(output);
        });
    }

    public void sendAttack(List<int[]> rows) {
        send(() -> {
            output.writeByte(RoomProtocol.C_ATTACK);
            RoomProtocol.writeRows(output, rows);
        });
    }

    // 로컬 플레이어가 게임 시작 준비를 마쳤음을 서버에 알린다.
    public void sendReady() {
        send(() -> output.writeByte(RoomProtocol.C_READY));
    }

    // 로컬 플레이어가 탈락했음을 서버에 알린다.
    public void sendOut() {
        send(() -> output.writeByte(RoomProtocol.C_OUT));
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        keepAlive.shutdownNow();
        try {
            synchronized (output) {
                output.writeByte(RoomProtocol.C_CLOSE);
                output.flush();
            }
        } catch (IOException ignored) {
            // 연결이 이미 끊겼다면 종료 메시지를 보낼 필요가 없다.
        }
        closeSocket();
    }

    private void sendKeepAlive() {
        send(() -> output.writeByte(RoomProtocol.C_KEEP_ALIVE));
    }

    private interface Writer {
        void write() throws IOException;
    }

    private void send(Writer writer) {
        if (closed.get()) {
            return;
        }
        try {
            synchronized (output) {
                writer.write();
                output.flush();
            }
        } catch (IOException exception) {
            fail();
        }
    }

    private void readLoop() {
        try {
            socket.setSoTimeout(RoomProtocol.READ_TIMEOUT_MS);
            while (!closed.get()) {
                int type = input.readUnsignedByte();
                if (type == RoomProtocol.S_CLOSE) {
                    break;
                }
                handle(type);
            }
        } catch (IOException | RuntimeException exception) {
            // 아래에서 연결 끊김으로 처리한다.
        }
        boolean intentional = closed.get();
        closed.set(true);
        keepAlive.shutdownNow();
        closeSocket();
        if (!intentional) {
            listener.onDisconnected();
        }
    }

    private void handle(int type) throws IOException {
        switch (type) {
            case RoomProtocol.S_ROSTER -> {
                int mySlot = input.readInt();
                int count = input.readInt();
                if (count < 0 || count > RoomProtocol.MAX_PARTICIPANTS) {
                    throw new IOException("Invalid roster size");
                }
                List<RoomMember> members = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String name = input.readUTF();
                    boolean ready = input.readBoolean();
                    boolean host = input.readBoolean();
                    members.add(new RoomMember(name, ready, host));
                }
                listener.onRoster(mySlot, members);
            }
            case RoomProtocol.S_START -> {
                GameMode mode = GameMode.values()[input.readUnsignedByte() % GameMode.values().length];
                listener.onStart(mode, input.readInt());
            }
            case RoomProtocol.S_STATUS -> {
                int slot = input.readInt();
                listener.onStatus(slot, GameStatus.readFrom(input));
            }
            case RoomProtocol.S_ATTACK -> listener.onAttack(RoomProtocol.readRows(input));
            case RoomProtocol.S_PLAYER_OUT -> listener.onPlayerOut(input.readInt());
            case RoomProtocol.S_ROOM_END -> {
                int count = input.readInt();
                if (count < 0 || count > RoomProtocol.MAX_PARTICIPANTS) {
                    throw new IOException("Invalid ranking size");
                }
                List<Integer> ranking = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    ranking.add(input.readInt());
                }
                // 정상 종료이므로 이후 소켓 종료는 연결 끊김으로 보고하지 않는다.
                closed.set(true);
                listener.onRoomEnd(ranking);
            }
            case RoomProtocol.S_ERROR -> {
                String message = input.readUTF();
                closed.set(true);
                listener.onError(message);
            }
            case RoomProtocol.S_KEEP_ALIVE -> {
            }
            default -> throw new IOException("Unknown room message: " + type);
        }
    }

    private void fail() {
        if (closed.get()) {
            return;
        }
        closed.set(true);
        keepAlive.shutdownNow();
        closeSocket();
        listener.onDisconnected();
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫힌 소켓은 무시한다.
        }
    }
}
