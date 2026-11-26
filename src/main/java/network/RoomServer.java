package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import game.GameMode;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// 최대 20명이 참가하는 대전 방 서버. 호스트 PC가 직접 실행하며, 공격 대상 선택과 순위 계산을 담당한다.
public final class RoomServer implements AutoCloseable {
    private static final int MAX_NAME_LENGTH = 12;
    private static final String DEFAULT_NAME = "플레이어";
    private static final int END_CLOSE_DELAY_SECONDS = 3;

    // 서버 -> 클라이언트 메시지 본문 작성기
    private interface Body {
        void write(DataOutputStream output) throws IOException;
    }

    private static final class Member {
        final Socket socket;
        final DataOutputStream output;
        final String name;
        int slot;
        int score;
        boolean ready;
        boolean host;
        boolean connected = true;

        Member(Socket socket, DataOutputStream output, String name, int slot) {
            this.socket = socket;
            this.output = output;
            this.name = name;
            this.slot = slot;
        }
    }

    private final ServerSocket serverSocket;
    private final Random random;
    private final ScheduledExecutorService scheduler;
    private final List<Member> members = new ArrayList<>();
    private RoomRules rules;
    private boolean playing;
    private boolean ended;
    private boolean closed;
    // 호스트가 게임 시작을 누른 모드. null이면 아직 시작 전이다.
    private GameMode hostMode;
    private int hostTimeLimitSeconds;
    private ScheduledFuture<?> timeLimitFuture;
    // 슬롯별 공격 대상(-1이면 없음). 대상이 탈락하기 전까지 바뀌지 않는다.
    private int[] attackTargets = new int[0];

    private RoomServer(ServerSocket serverSocket, Random random) {
        this.serverSocket = serverSocket;
        this.random = random;
        this.scheduler = Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "room-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    // 지정한 포트에 방을 열고 접속을 받기 시작한다.
    public static RoomServer open(int port) throws IOException {
        return open(port, new Random());
    }

    static RoomServer open(int port, Random random) throws IOException {
        RoomServer server = new RoomServer(new ServerSocket(port), random);
        server.startThreads();
        return server;
    }

    private void startThreads() {
        Thread acceptThread = new Thread(this::acceptLoop, "room-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        scheduler.scheduleAtFixedRate(this::sendKeepAlive,
                RoomProtocol.KEEP_ALIVE_INTERVAL_MS, RoomProtocol.KEEP_ALIVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    // 게임을 시작한다. 참가자가 2명 이상일 때만 가능하며, timeLimitSeconds가 0이면 시간 제한이 없다.
    public synchronized boolean startGame(GameMode mode, int timeLimitSeconds) {
        if (timeLimitSeconds < 0) {
            throw new IllegalArgumentException("timeLimitSeconds must not be negative");
        }
        if (closed || playing || ended || members.size() < 2) {
            return false;
        }
        rules = new RoomRules(members.size());
        for (Member member : members) {
            member.score = 0;
        }
        playing = true;
        broadcast(output -> {
            output.writeByte(RoomProtocol.S_START);
            output.writeByte(mode.ordinal());
            output.writeInt(timeLimitSeconds);
        });
        attackTargets = new int[members.size()];
        Arrays.fill(attackTargets, -1);
        assignAttackTargets();
        if (timeLimitSeconds > 0) {
            timeLimitFuture = scheduler.schedule(this::onTimeLimit, timeLimitSeconds, TimeUnit.SECONDS);
        }
        return true;
    }

    public synchronized int participantCount() {
        return members.size();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        cancelTimeLimit();
        scheduler.shutdownNow();
        for (Member member : members) {
            member.connected = false;
            closeQuietly(member.socket);
        }
        closeQuietly(serverSocket);
    }

    private void acceptLoop() {
        while (true) {
            Socket socket;
            try {
                socket = serverSocket.accept();
            } catch (IOException exception) {
                return;
            }
            Thread thread = new Thread(() -> serve(socket), "room-client");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void serve(Socket socket) {
        Member member = null;
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(RoomProtocol.HANDSHAKE_TIMEOUT_MS);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            if (input.readInt() != RoomProtocol.MAGIC) {
                throw new IOException("Client uses an incompatible multiplayer protocol");
            }
            String name = sanitizeName(input.readUTF());
            member = join(socket, output, name);
            if (member == null) {
                return;
            }
            socket.setSoTimeout(RoomProtocol.READ_TIMEOUT_MS);
            while (true) {
                int type = input.readUnsignedByte();
                if (type == RoomProtocol.C_CLOSE) {
                    return;
                }
                handleMessage(member, type, input);
            }
        } catch (IOException | RuntimeException exception) {
            // 연결 오류는 아래 finally에서 참가자 제거로 처리한다.
        } finally {
            closeQuietly(socket);
            if (member != null) {
                dropMember(member);
            }
        }
    }

    private void handleMessage(Member member, int type, DataInputStream input) throws IOException {
        switch (type) {
            case RoomProtocol.C_STATUS -> onStatus(member, GameStatus.readFrom(input));
            case RoomProtocol.C_ATTACK -> onAttack(member, RoomProtocol.readRows(input));
            case RoomProtocol.C_OUT -> onOut(member);
            case RoomProtocol.C_READY -> onReady(member);
            case RoomProtocol.C_CHANGE_TARGET -> onChangeTarget(member);
            case RoomProtocol.C_KEEP_ALIVE -> {
            }
            default -> throw new IOException("Unknown room message: " + type);
        }
    }

    private synchronized Member join(Socket socket, DataOutputStream output, String name) throws IOException {
        if (closed) {
            return null;
        }
        if (playing || ended) {
            sendError(output, "게임이 진행 중인 방이에요");
            return null;
        }
        if (members.size() >= RoomProtocol.MAX_PARTICIPANTS) {
            sendError(output, "방이 가득 찼어요 (최대 " + RoomProtocol.MAX_PARTICIPANTS + "명)");
            return null;
        }
        Member member = new Member(socket, output, name, members.size());
        // 방장의 루프백 클라이언트는 같은 PC에서 접속하므로 루프백 주소로 구분한다.
        member.host = socket.getInetAddress().isLoopbackAddress();
        members.add(member);
        broadcastRoster();
        return member;
    }

    private synchronized void onStatus(Member member, GameStatus status) {
        if (!playing || ended) {
            return;
        }
        member.score = status.getScore();
        for (Member other : snapshot()) {
            if (other != member) {
                sendTo(other, output -> {
                    output.writeByte(RoomProtocol.S_STATUS);
                    output.writeInt(member.slot);
                    status.writeTo(output);
                });
            }
        }
    }

    private synchronized void onAttack(Member sender, List<int[]> rows) {
        if (!playing || ended || !rules.isAlive(sender.slot)) {
            return;
        }
        int target = attackTargets[sender.slot];
        if (target < 0 || !rules.isAlive(target)) {
            return;
        }
        sendTo(members.get(target), output -> {
            output.writeByte(RoomProtocol.S_ATTACK);
            RoomProtocol.writeRows(output, rows);
        });
        // 공격 후에는 다른 무작위 상대로 대상을 바꾸고 발신자에게 알린다.
        attackTargets[sender.slot] = rules.pickNextTarget(sender.slot, target, random);
        sendTargetTo(sender.slot);
    }

    // 참가자가 Tab으로 요청하면 직전 대상을 제외한 다른 무작위 상대로 바꾼다.
    private synchronized void onChangeTarget(Member sender) {
        if (!playing || ended || !rules.isAlive(sender.slot)) {
            return;
        }
        attackTargets[sender.slot] = rules.pickNextTarget(sender.slot, attackTargets[sender.slot], random);
        sendTargetTo(sender.slot);
    }

    // 바뀐 공격 대상만 해당 참가자에게 알린다.
    private void assignAttackTargets() {
        for (int slot : rules.refreshTargets(attackTargets, random)) {
            sendTargetTo(slot);
        }
    }

    private void sendTargetTo(int slot) {
        int target = attackTargets[slot];
        sendTo(members.get(slot), output -> {
            output.writeByte(RoomProtocol.S_TARGET);
            output.writeInt(target);
        });
    }

    private synchronized void onOut(Member member) {
        if (playing && !ended) {
            eliminate(member.slot);
        }
    }

    // 클라이언트의 준비 완료. 한 번 준비하면 취소되지 않는다.
    private synchronized void onReady(Member member) {
        if (playing || ended || member.ready) {
            return;
        }
        member.ready = true;
        broadcastRoster();
        tryStart();
    }

    // 호스트의 게임 시작 요청. 호스트 자신은 준비된 것으로 간주하며, 모드는 이 시점에 확정된다.
    public synchronized boolean markHostReady(GameMode mode, int timeLimitSeconds) {
        if (closed || playing || ended || members.size() < 2) {
            return false;
        }
        Member host = findHost();
        if (host == null) {
            return false;
        }
        hostMode = mode;
        hostTimeLimitSeconds = timeLimitSeconds;
        host.ready = true;
        broadcastRoster();
        tryStart();
        return true;
    }

    // 호스트가 시작을 누르고 모든 참가자가 준비되었을 때만 게임을 시작한다.
    private void tryStart() {
        if (closed || playing || ended || hostMode == null || members.size() < 2) {
            return;
        }
        for (Member member : members) {
            if (!member.ready) {
                return;
            }
        }
        startGame(hostMode, hostTimeLimitSeconds);
    }

    private Member findHost() {
        for (Member member : members) {
            if (member.host) {
                return member;
            }
        }
        return null;
    }

    // 탈락 처리. 마지막 생존자가 남으면 게임을 끝낸다.
    private void eliminate(int slot) {
        if (!rules.eliminate(slot)) {
            return;
        }
        broadcast(output -> {
            output.writeByte(RoomProtocol.S_PLAYER_OUT);
            output.writeInt(slot);
        });
        assignAttackTargets();
        if (rules.aliveCount() <= 1) {
            endGame(rules.rankingByElimination());
        }
    }

    private synchronized void onTimeLimit() {
        if (playing && !ended) {
            int[] scores = new int[members.size()];
            for (int i = 0; i < scores.length; i++) {
                scores[i] = members.get(i).score;
            }
            endGame(rules.rankingByScore(scores));
        }
    }

    private void endGame(List<Integer> ranking) {
        ended = true;
        playing = false;
        cancelTimeLimit();
        broadcast(output -> {
            output.writeByte(RoomProtocol.S_ROOM_END);
            output.writeInt(ranking.size());
            for (int slot : ranking) {
                output.writeInt(slot);
            }
        });
        // 결과 메시지가 전달될 시간을 준 뒤 방을 닫는다.
        scheduler.schedule(this::close, END_CLOSE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void dropMember(Member member) {
        if (!member.connected) {
            return;
        }
        member.connected = false;
        closeQuietly(member.socket);
        if (closed || ended) {
            return;
        }
        if (playing) {
            eliminate(member.slot);
            return;
        }
        members.remove(member);
        for (int i = 0; i < members.size(); i++) {
            members.get(i).slot = i;
        }
        broadcastRoster();
        // 나간 참가자를 기다리던 중이었다면 남은 참가자만으로 시작할 수 있다.
        tryStart();
    }

    private void broadcastRoster() {
        List<Member> roster = snapshot();
        for (Member member : roster) {
            sendTo(member, output -> {
                output.writeByte(RoomProtocol.S_ROSTER);
                output.writeInt(member.slot);
                output.writeInt(roster.size());
                for (Member entry : roster) {
                    output.writeUTF(entry.name);
                    output.writeBoolean(entry.ready);
                    output.writeBoolean(entry.host);
                }
            });
        }
    }

    private synchronized void sendKeepAlive() {
        broadcast(output -> output.writeByte(RoomProtocol.S_KEEP_ALIVE));
    }

    private void broadcast(Body body) {
        for (Member member : snapshot()) {
            sendTo(member, body);
        }
    }

    // 쓰기 실패는 해당 참가자의 연결 끊김으로 처리한다.
    private void sendTo(Member member, Body body) {
        if (!member.connected) {
            return;
        }
        try {
            synchronized (member.output) {
                body.write(member.output);
                member.output.flush();
            }
        } catch (IOException exception) {
            dropMember(member);
        }
    }

    private void sendError(DataOutputStream output, String message) {
        try {
            output.writeByte(RoomProtocol.S_ERROR);
            output.writeUTF(message);
            output.flush();
        } catch (IOException ignored) {
            // 거절 메시지 전송 실패는 연결을 어차피 닫으므로 무시한다.
        }
    }

    private void cancelTimeLimit() {
        if (timeLimitFuture != null) {
            timeLimitFuture.cancel(false);
            timeLimitFuture = null;
        }
    }

    // 브로드캐스트 중 참가자 목록이 바뀔 수 있으므로 복사본을 순회한다.
    private List<Member> snapshot() {
        return new ArrayList<>(members);
    }

    private static String sanitizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            return DEFAULT_NAME;
        }
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫힌 소켓은 무시한다.
        }
    }

    private static void closeQuietly(ServerSocket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 이미 닫힌 소켓은 무시한다.
        }
    }
}
