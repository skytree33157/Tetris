package network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import blocks.core.BlockType;
import game.GameMode;

// 2~10명 네트워크 대전 세션. 호스트가 모든 메시지를 중계하는 스타 구조다.
public final class P2PMatchSession implements AutoCloseable {
    public static final int DEFAULT_PORT = 57823;
    public static final int MAX_PLAYERS = 10;
    public static final int MIN_PLAYERS_TO_START = 2;
    public static final long MATCH_TIME_LIMIT_MS = 180_000L;

    static final int HOST_SLOT = 0;
    private static final int PROTOCOL_MAGIC = 0x54465345;
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int KEEP_ALIVE_INTERVAL_MS = 2_000;
    private static final int MONITOR_INTERVAL_MS = 500;
    private static final int BOARD_WIDTH = 10;
    private static final int MAX_ATTACK_ROWS = 20;
    private static final int JOIN_REJECTED = -1;
    private static final int JOIN_ACCEPTED = 0;
    static final String CONNECTION_LOST_MESSAGE = "연결이 끊어졌습니다";

    private static final int MESSAGE_ROSTER = 1;
    private static final int MESSAGE_START = 2;
    private static final int MESSAGE_STATUS = 3;
    private static final int MESSAGE_KEEP_ALIVE = 5;
    private static final int MESSAGE_ATTACK_REQUEST = 6;
    private static final int MESSAGE_ATTACK = 7;
    private static final int MESSAGE_RESULT = 8;

    // 세션 이벤트 리스너. 모든 콜백은 네트워크 스레드에서 호출되므로 UI 작업은 invokeLater로 넘겨야 한다.
    public interface Listener {
        default void onRosterChanged(List<Integer> slots) {}
        default void onStarted() {}
        default void onRemoteStatus(int slot, GameStatus status) {}
        default void onAttack(List<int[]> rows) {}
        // 순위는 슬롯 번호 목록이며 1등부터 나열한다.
        default void onResult(List<Integer> ranking) {}
        default void onDisconnected(IOException exception) {}
        // 지연/복구된 상대의 슬롯 번호를 전달한다. 호스트 연결은 HOST_SLOT이다.
        default void onConnectionLatency(int slot) {}
        default void onConnectionRecovered(int slot) {}
    }

    private static final Listener NO_OP_LISTENER = new Listener() {};

    private interface Body {
        void write(DataOutputStream out) throws IOException;
    }

    private static final Body NO_BODY = out -> {};

    private static final class Peer {
        final int slot;
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;
        final ConnectionMonitor monitor = new ConnectionMonitor();
        final AtomicBoolean failed = new AtomicBoolean();

        Peer(int slot, Socket socket, DataInputStream in, DataOutputStream out) {
            this.slot = slot;
            this.socket = socket;
            this.in = in;
            this.out = out;
        }
    }

    private final GameMode mode;
    private final boolean server;
    private final ServerSocket serverSocket;
    private final int localSlot;
    private final long timeLimitMs;
    private final Object lock = new Object();
    private final Map<Integer, Peer> peers = new LinkedHashMap<>();
    private final Set<Integer> alive = new LinkedHashSet<>();
    private final List<Integer> eliminated = new ArrayList<>();
    private final Map<Integer, GameStatus> statuses = new HashMap<>();
    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "p2p-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private volatile Listener listener = NO_OP_LISTENER;
    private List<Integer> roster = List.of(HOST_SLOT);
    private int nextSlot = 1;
    private int pendingJoins = 0;
    private boolean started;
    private boolean matchRunning;
    private boolean closed;
    private ScheduledFuture<?> timeLimitTask;

    private P2PMatchSession(GameMode mode, boolean server, ServerSocket serverSocket, int localSlot, long timeLimitMs) {
        this.mode = mode;
        this.server = server;
        this.serverSocket = serverSocket;
        this.localSlot = localSlot;
        this.timeLimitMs = timeLimitMs;
    }

    // 호스트 방을 만들고 접속을 받기 시작한다.
    public static P2PMatchSession createRoom(GameMode mode) throws IOException {
        return createRoom(mode, DEFAULT_PORT, MATCH_TIME_LIMIT_MS);
    }

    static P2PMatchSession createRoom(GameMode mode, int port, long timeLimitMs) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        P2PMatchSession session = new P2PMatchSession(mode, true, serverSocket, HOST_SLOT, timeLimitMs);
        session.startHost();
        return session;
    }

    // "host" 또는 "host:port" 형식의 주소로 방에 참가한다.
    public static P2PMatchSession connect(String address) throws IOException {
        int colon = address.lastIndexOf(':');
        if (colon < 0) {
            return connect(address, DEFAULT_PORT);
        }
        try {
            return connect(address.substring(0, colon), Integer.parseInt(address.substring(colon + 1)));
        } catch (NumberFormatException exception) {
            throw new IOException("잘못된 포트 번호입니다: " + address, exception);
        }
    }

    static P2PMatchSession connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        DataInputStream in;
        DataOutputStream out;
        int slot;
        GameMode joinedMode;
        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            out.writeInt(PROTOCOL_MAGIC);
            out.flush();
            int result = in.readInt();
            if (result != JOIN_ACCEPTED) {
                throw new IOException("방이 가득 찼거나 이미 시작된 경기입니다.");
            }
            slot = in.readInt();
            int modeOrdinal = in.readInt();
            if (modeOrdinal < 0 || modeOrdinal >= GameMode.values().length || slot <= HOST_SLOT) {
                throw new IOException("잘못된 방 정보를 받았습니다.");
            }
            joinedMode = GameMode.values()[modeOrdinal];
            socket.setSoTimeout(0);
        } catch (IOException exception) {
            closeQuietly(socket);
            throw exception;
        }
        P2PMatchSession session = new P2PMatchSession(joinedMode, false, null, slot, MATCH_TIME_LIMIT_MS);
        session.startClient(new Peer(HOST_SLOT, socket, in, out));
        return session;
    }

    public GameMode getMode() {
        return mode;
    }

    public int getSlot() {
        return localSlot;
    }

    public boolean isServer() {
        return server;
    }

    public int getPort() {
        return server ? serverSocket.getLocalPort() : -1;
    }

    public void setListener(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    public List<Integer> getRoster() {
        synchronized (lock) {
            return List.copyOf(roster);
        }
    }

    public boolean isMatchRunning() {
        synchronized (lock) {
            return matchRunning;
        }
    }

    // 호스트만 호출한다. 2명 이상 모이면 경기를 시작하고 모든 참가자에게 알린다.
    public void start() throws IOException {
        if (!server) {
            throw new IllegalStateException("호스트만 경기를 시작할 수 있습니다.");
        }
        synchronized (lock) {
            if (closed) {
                throw new IOException("세션이 이미 종료되었습니다.");
            }
            if (started) {
                throw new IllegalStateException("이미 시작된 경기입니다.");
            }
            if (peers.size() + 1 < MIN_PLAYERS_TO_START) {
                throw new IllegalStateException("경기를 시작하려면 최소 " + MIN_PLAYERS_TO_START + "명이 필요합니다.");
            }
            started = true;
            matchRunning = true;
            alive.clear();
            alive.addAll(roster);
            eliminated.clear();
            statuses.clear();
            timeLimitTask = scheduler.schedule(this::onTimeLimit, timeLimitMs, TimeUnit.MILLISECONDS);
        }
        broadcast(frameOrThrow(MESSAGE_START, NO_BODY), -1);
        listener.onStarted();
    }

    // 자신의 현재 상태를 전송한다. 호스트는 직접 처리하고 클라이언트는 호스트에 보낸다.
    public void sendStatus(GameStatus status) {
        Objects.requireNonNull(status, "status");
        if (server) {
            acceptStatus(HOST_SLOT, status);
            return;
        }
        Peer host = peerOf(HOST_SLOT);
        if (host != null) {
            send(host, frameOrThrow(MESSAGE_STATUS, out -> {
                out.writeInt(localSlot);
                status.writeTo(out);
            }));
        }
    }

    // 줄 삭제 공격을 요청한다. 호스트는 무작위 생존 대상에게 바로 라우팅하고 클라이언트는 호스트에 요청한다.
    public void requestAttack(List<int[]> rows) {
        List<int[]> copies = copyRows(rows);
        if (server) {
            routeAttack(HOST_SLOT, copies);
            return;
        }
        Peer host = peerOf(HOST_SLOT);
        if (host != null) {
            send(host, frameOrThrow(MESSAGE_ATTACK_REQUEST, out -> writeRows(out, copies)));
        }
    }

    private void startHost() {
        Thread acceptThread = new Thread(this::acceptLoop, "p2p-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        scheduler.scheduleAtFixedRate(this::sendKeepAlives, KEEP_ALIVE_INTERVAL_MS, KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::checkMonitors, MONITOR_INTERVAL_MS, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void startClient(Peer host) {
        synchronized (lock) {
            peers.put(HOST_SLOT, host);
        }
        watch(host);
        startReader(host);
        scheduler.scheduleAtFixedRate(this::sendKeepAlives, KEEP_ALIVE_INTERVAL_MS, KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::checkMonitors, MONITOR_INTERVAL_MS, MONITOR_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void acceptLoop() {
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException exception) {
                // 세션이 닫히면 ServerSocket이 예외를 던지며 루프가 끝난다.
                return;
            }
            Thread joinThread = new Thread(() -> handleJoin(socket), "p2p-join");
            joinThread.setDaemon(true);
            joinThread.start();
        }
    }

    // 접속 요청을 처리한다. 응답을 보낸 뒤에만 방 명단에 올려 ROSTER가 핸드셰이크보다 먼저 가지 않게 한다.
    private void handleJoin(Socket socket) {
        DataInputStream in;
        DataOutputStream out;
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            if (in.readInt() != PROTOCOL_MAGIC) {
                closeQuietly(socket);
                return;
            }
        } catch (IOException exception) {
            closeQuietly(socket);
            return;
        }

        int slot;
        synchronized (lock) {
            if (closed || started || peers.size() + pendingJoins >= MAX_PLAYERS - 1) {
                slot = -1;
            } else {
                slot = nextSlot++;
                pendingJoins++;
            }
        }
        if (slot < 0) {
            try {
                out.writeInt(JOIN_REJECTED);
                out.flush();
            } catch (IOException ignored) {
                // 거절 응답을 보내지 못해도 연결은 닫으면 된다.
            }
            closeQuietly(socket);
            return;
        }

        Peer peer = new Peer(slot, socket, in, out);
        try {
            out.writeInt(JOIN_ACCEPTED);
            out.writeInt(slot);
            out.writeInt(mode.ordinal());
            out.flush();
            socket.setSoTimeout(0);
        } catch (IOException exception) {
            synchronized (lock) {
                pendingJoins--;
            }
            closeQuietly(socket);
            return;
        }

        List<Integer> snapshot;
        synchronized (lock) {
            pendingJoins--;
            if (closed || started) {
                closeQuietly(socket);
                return;
            }
            peers.put(slot, peer);
            updateRosterLocked();
            snapshot = roster;
        }
        watch(peer);
        startReader(peer);
        broadcastRoster(snapshot);
        listener.onRosterChanged(snapshot);
    }

    private void watch(Peer peer) {
        peer.monitor.setCallback(new Callback() {
            @Override
            public void onLatency() {
                listener.onConnectionLatency(peer.slot);
            }

            @Override
            public void onDisconnected() {
                failPeer(peer, new IOException(CONNECTION_LOST_MESSAGE));
            }

            @Override
            public void onConnectionRecovered() {
                listener.onConnectionRecovered(peer.slot);
            }
        });
    }

    private void startReader(Peer peer) {
        Thread reader = new Thread(() -> readLoop(peer), "p2p-reader-" + peer.slot);
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop(Peer peer) {
        try {
            while (true) {
                int type = peer.in.readUnsignedByte();
                peer.monitor.recordMessageReceived();
                handleMessage(peer, type);
            }
        } catch (IOException exception) {
            failPeer(peer, exception);
        } catch (RuntimeException exception) {
            failPeer(peer, new IOException("잘못된 메시지를 받았습니다.", exception));
        }
    }

    private void handleMessage(Peer peer, int type) throws IOException {
        if (type == MESSAGE_KEEP_ALIVE) {
            return;
        }
        if (server) {
            switch (type) {
                case MESSAGE_STATUS -> {
                    int slot = peer.in.readInt();
                    GameStatus status = GameStatus.readFrom(peer.in);
                    if (slot != peer.slot) {
                        throw new IOException("상태 발신자가 일치하지 않습니다.");
                    }
                    acceptStatus(peer.slot, status);
                }
                case MESSAGE_ATTACK_REQUEST -> routeAttack(peer.slot, readRows(peer.in));
                default -> throw new IOException("알 수 없는 메시지입니다: " + type);
            }
            return;
        }
        switch (type) {
            case MESSAGE_ROSTER -> {
                List<Integer> slots = readSlots(peer.in);
                synchronized (lock) {
                    roster = List.copyOf(slots);
                }
                listener.onRosterChanged(List.copyOf(slots));
            }
            case MESSAGE_START -> {
                synchronized (lock) {
                    started = true;
                    matchRunning = true;
                }
                listener.onStarted();
            }
            case MESSAGE_STATUS -> {
                int slot = peer.in.readInt();
                GameStatus status = GameStatus.readFrom(peer.in);
                if (slot != localSlot) {
                    listener.onRemoteStatus(slot, status);
                }
            }
            case MESSAGE_ATTACK -> listener.onAttack(readRows(peer.in));
            case MESSAGE_RESULT -> {
                List<Integer> ranking = readSlots(peer.in);
                synchronized (lock) {
                    matchRunning = false;
                }
                listener.onResult(ranking);
            }
            default -> throw new IOException("알 수 없는 메시지입니다: " + type);
        }
    }

    // 호스트: 상태를 기록하고, 탈락 처리 및 종료 조건을 확인한 뒤 다른 참가자에게 중계한다.
    private void acceptStatus(int slot, GameStatus status) {
        List<Peer> targets;
        List<Integer> ranking = null;
        synchronized (lock) {
            if (closed) {
                return;
            }
            statuses.put(slot, status);
            if (matchRunning && status.isGameOver() && alive.remove(slot)) {
                eliminated.add(slot);
            }
            if (matchRunning && alive.size() <= 1) {
                ranking = finishLocked();
            }
            targets = peersExcept(slot);
        }
        byte[] message = frameOrThrow(MESSAGE_STATUS, out -> {
            out.writeInt(slot);
            status.writeTo(out);
        });
        for (Peer target : targets) {
            send(target, message);
        }
        if (slot != HOST_SLOT) {
            listener.onRemoteStatus(slot, status);
        }
        if (ranking != null) {
            completeMatch(ranking);
        }
    }

    // 호스트: 생존자 중 송신자를 제외하고 무작위 한 명에게 공격을 전달한다.
    private void routeAttack(int sender, List<int[]> rows) {
        Peer target;
        synchronized (lock) {
            if (!matchRunning) {
                return;
            }
            List<Integer> candidates = new ArrayList<>(alive);
            candidates.remove(Integer.valueOf(sender));
            if (candidates.isEmpty()) {
                return;
            }
            int chosen = candidates.get(random.nextInt(candidates.size()));
            if (chosen == HOST_SLOT) {
                target = null;
            } else {
                target = peers.get(chosen);
                if (target == null) {
                    return;
                }
            }
        }
        if (target == null) {
            listener.onAttack(rows);
            return;
        }
        send(target, frameOrThrow(MESSAGE_ATTACK, out -> writeRows(out, rows)));
    }

    // 생존자를 점수 내림차순으로 정렬하고, 그 뒤에 탈락자를 나중에 탈락한 순서대로 붙인다. 호출 전 lock 보유 필요.
    private List<Integer> finishLocked() {
        matchRunning = false;
        if (timeLimitTask != null) {
            timeLimitTask.cancel(false);
            timeLimitTask = null;
        }
        List<Integer> ranking = new ArrayList<>(alive);
        ranking.sort(Comparator.<Integer>comparingInt(slot -> scoreOf(slot)).reversed()
                .thenComparingInt(slot -> slot));
        for (int i = eliminated.size() - 1; i >= 0; i--) {
            ranking.add(eliminated.get(i));
        }
        return ranking;
    }

    private int scoreOf(int slot) {
        GameStatus status = statuses.get(slot);
        return status == null ? 0 : status.getScore();
    }

    private void onTimeLimit() {
        List<Integer> ranking;
        synchronized (lock) {
            if (!matchRunning || closed) {
                return;
            }
            ranking = finishLocked();
        }
        completeMatch(ranking);
    }

    private void completeMatch(List<Integer> ranking) {
        broadcast(frameOrThrow(MESSAGE_RESULT, out -> writeSlots(out, ranking)), -1);
        listener.onResult(List.copyOf(ranking));
    }

    // 연결이 끊긴 참가자를 제거한다. 경기 중이면 마지막 점수로 탈락 처리한다.
    private void peerLost(int slot) {
        List<Integer> snapshot;
        GameStatus eliminatedStatus = null;
        synchronized (lock) {
            if (closed || peers.remove(slot) == null) {
                return;
            }
            updateRosterLocked();
            snapshot = roster;
            if (matchRunning && alive.contains(slot)) {
                GameStatus last = statuses.get(slot);
                eliminatedStatus = last == null
                        ? new GameStatus(0, 1, 0, true)
                        : new GameStatus(last.getScore(), last.getLevel(), last.getLines(), true);
            }
        }
        broadcastRoster(snapshot);
        listener.onRosterChanged(snapshot);
        if (eliminatedStatus != null) {
            acceptStatus(slot, eliminatedStatus);
        }
    }

    private void failPeer(Peer peer, IOException cause) {
        if (!peer.failed.compareAndSet(false, true)) {
            return;
        }
        closeQuietly(peer.socket);
        if (server) {
            peerLost(peer.slot);
            return;
        }
        boolean wasOpen;
        synchronized (lock) {
            wasOpen = !closed;
        }
        close();
        if (wasOpen) {
            listener.onDisconnected(cause);
        }
    }

    private void sendKeepAlives() {
        byte[] message = frameOrThrow(MESSAGE_KEEP_ALIVE, NO_BODY);
        for (Peer peer : allPeers()) {
            send(peer, message);
        }
    }

    private void checkMonitors() {
        for (Peer peer : allPeers()) {
            peer.monitor.checkAndUpdate();
        }
    }

    private void send(Peer peer, byte[] message) {
        try {
            synchronized (peer.out) {
                peer.out.write(message);
                peer.out.flush();
            }
        } catch (IOException exception) {
            failPeer(peer, exception);
        }
    }

    private void broadcast(byte[] message, int exceptSlot) {
        List<Peer> targets;
        synchronized (lock) {
            targets = peersExcept(exceptSlot);
        }
        for (Peer target : targets) {
            send(target, message);
        }
    }

    private void broadcastRoster(List<Integer> slots) {
        broadcast(frameOrThrow(MESSAGE_ROSTER, out -> writeSlots(out, slots)), -1);
    }

    private void updateRosterLocked() {
        List<Integer> slots = new ArrayList<>();
        slots.add(HOST_SLOT);
        slots.addAll(peers.keySet());
        roster = List.copyOf(slots);
    }

    private List<Peer> peersExcept(int slot) {
        List<Peer> result = new ArrayList<>();
        for (Peer peer : peers.values()) {
            if (peer.slot != slot) {
                result.add(peer);
            }
        }
        return result;
    }

    private List<Peer> allPeers() {
        synchronized (lock) {
            return new ArrayList<>(peers.values());
        }
    }

    private Peer peerOf(int slot) {
        synchronized (lock) {
            return peers.get(slot);
        }
    }

    private static byte[] frameOrThrow(int type, Body body) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeByte(type);
            body.write(out);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("메시지 인코딩에 실패했습니다.", exception);
        }
    }

    private static void writeSlots(DataOutputStream out, List<Integer> slots) throws IOException {
        out.writeInt(slots.size());
        for (int slot : slots) {
            out.writeInt(slot);
        }
    }

    private static List<Integer> readSlots(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_PLAYERS) {
            throw new IOException("잘못된 플레이어 수입니다.");
        }
        List<Integer> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(in.readInt());
        }
        return slots;
    }

    private static void writeRows(DataOutputStream out, List<int[]> rows) throws IOException {
        requireValidRows(rows);
        out.writeInt(rows.size());
        for (int[] row : rows) {
            for (int cell : row) {
                out.writeInt(cell);
            }
        }
    }

    private static List<int[]> readRows(DataInputStream in) throws IOException {
        int count = in.readInt();
        if (count < 0 || count > MAX_ATTACK_ROWS) {
            throw new IOException("잘못된 공격 행 수입니다.");
        }
        List<int[]> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int[] row = new int[BOARD_WIDTH];
            for (int col = 0; col < BOARD_WIDTH; col++) {
                row[col] = in.readInt();
            }
            rows.add(row);
        }
        try {
            requireValidRows(rows);
        } catch (IllegalArgumentException exception) {
            throw new IOException("잘못된 공격 행입니다.", exception);
        }
        return rows;
    }

    // 공격 칸은 빈칸(0) 또는 ATTACK 값만 허용한다. 아이템 문자나 블록 색상이 상대에게 복제되지 않게 한다.
    private static void requireValidRows(List<int[]> rows) {
        if (rows.size() > MAX_ATTACK_ROWS) {
            throw new IllegalArgumentException("공격 행이 너무 많습니다.");
        }
        for (int[] row : rows) {
            if (row == null || row.length != BOARD_WIDTH) {
                throw new IllegalArgumentException("공격 행의 너비가 맞지 않습니다.");
            }
            for (int cell : row) {
                if (cell != 0 && cell != BlockType.ATTACK.getValue()) {
                    throw new IllegalArgumentException("허용되지 않은 공격 칸입니다.");
                }
            }
        }
    }

    private static List<int[]> copyRows(List<int[]> rows) {
        List<int[]> copies = new ArrayList<>(rows.size());
        for (int[] row : rows) {
            copies.add(row.clone());
        }
        requireValidRows(copies);
        return copies;
    }

    @Override
    public void close() {
        List<Peer> snapshot;
        synchronized (lock) {
            if (closed) {
                return;
            }
            closed = true;
            matchRunning = false;
            if (timeLimitTask != null) {
                timeLimitTask.cancel(false);
                timeLimitTask = null;
            }
            snapshot = new ArrayList<>(peers.values());
            peers.clear();
        }
        for (Peer peer : snapshot) {
            closeQuietly(peer.socket);
        }
        scheduler.shutdownNow();
        if (serverSocket != null) {
            closeQuietly(serverSocket);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫힌 소켓은 정리할 필요가 없다.
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫힌 서버 소켓은 정리할 필요가 없다.
        }
    }
}
