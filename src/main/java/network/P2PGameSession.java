package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import blocks.core.BlockType;
import game.GameMode;
import blocks.core.BlockType;

// TCP 연결, 대전 관리
public final class P2PGameSession implements AutoCloseable {
    // TCP 포트
    public static final int DEFAULT_PORT = 57823;
    // handshake용 프로토콜 식별값
    // 버전 2("TFS2"): 상태 메시지에 상대 보드 추가
    // 버전 3("TFS3"): 핸드셰이크에 제한 시간, 공격·시간 종료 메시지 추가. 이전 버전과는 연결을 거부한다.
    private static final int PROTOCOL_MAGIC = 0x54465333;
    // 제한 시간 상한 (초). 잘못된 값이 와도 비정상적으로 긴 경기가 되지 않게 한다.
    public static final int MAX_TIME_LIMIT_SECONDS = 3600;
    // 공격 메시지 한 번에 담을 수 있는 줄 수 상한 (보드 높이)
    private static final int MAX_ATTACK_ROWS = 23;
    private static final int BOARD_COLS = 10;
    // handshake 완료를 기다리는 최대 시간
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    // 메시지 종류별 식별자. 각 메시지의 실제 필드 형식은 writeMessage/readMessages에서 처리한다.
    private static final int MESSAGE_READY = 1;
    private static final int MESSAGE_START = 2;
    private static final int MESSAGE_STATUS = 3;
    private static final int MESSAGE_CLOSE = 4;
    private static final int MESSAGE_KEEP_ALIVE = 5;
    private static final int MESSAGE_ATTACK = 6;
    private static final int MESSAGE_TIME_UP = 7;
    // 항복: STATUS와 같은 필드(게임 오버 상태)를 보내되, 패배 원인이 항복이라는 점을 함께 전한다
    private static final int MESSAGE_SURRENDER = 8;
    private static final int BOARD_WIDTH = 10;


    // 세션 이벤트 리스너 인터페이스
    public interface Listener {
        // 상대 플레이어가 준비 시 호출
        default void onRemoteReady(boolean ready) {}
        // 새 라운드 시작 시 호출
        default void onStarted() {}
        // 상대 플레이어 상태 수신 시 호출
        default void onRemoteStatus(GameStatus status) {}

        default void onRemoteAttack(java.util.List<int[]> rows) {}
        // 라운드 종료 시 호출
        default void onRoundFinished() {}
        // 게임 오버(어느 한쪽이 탑아웃)로 승패가 정해졌을 때 호출. 시간 제한 종료에는 호출되지 않는다
        default void onGameDecided() {}
        // 연결 끊김 또는 프로토콜 오류 시 호출
        default void onDisconnected(IOException exception) {}
        // 연결 지연 상태로 전환 시 호출
        default void onConnectionLatency(){}
        // 연결 정상 복구 시 호출
        default void onConnectionRecovered(){}
        // 상대가 보낸 공격 줄 수신 시 호출 (네트워크 스레드)
        default void onAttack(List<int[]> rows) {}
        // 시간 제한 대전이 끝났을 때 호출. 점수는 호스트가 확정한 값
        default void onTimeUp(int hostScore, int clientScore) {}
    }

    // 호스트 초대 클래스
    public static final class HostInvitation implements AutoCloseable {
        private final GameMode mode;
        private final int timeLimitSeconds;
        // 클라이언트 연결을 기다리는 서버 소켓
        private final ServerSocket serverSocket;
        // accept()가 이미 호출되어 초대를 소비했는지 나타낸다. -> accept()는 한 번만 호출 가능
        private boolean consumed;

        private HostInvitation(GameMode mode, int timeLimitSeconds, ServerSocket serverSocket) {
            this.mode = mode;
            this.timeLimitSeconds = timeLimitSeconds;
            this.serverSocket = serverSocket;
        }

        /**
         * 기본 포트에서 클라이언트 연결을 기다릴 호스트 초대를 만든다.
         * 게임 모드는 연결 후 {@link P2PGameSession#setMode}로 정한다.
         *
         * @return 연결을 수락할 수 있는 호스트 초대
         * @throws IOException 포트를 열지 못한 경우
         */
        public static HostInvitation open(GameMode mode) throws IOException {
            return open(mode, 0);
        }

        /**
         * @param timeLimitSeconds 시간 제한 대전의 제한 시간(초). 0이면 시간 제한 없음
         */
        public static HostInvitation open(GameMode mode, int timeLimitSeconds) throws IOException {
            Objects.requireNonNull(mode, "mode");
            validateTimeLimit(timeLimitSeconds);
            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            return new HostInvitation(mode, timeLimitSeconds, serverSocket);
        }

        // 클라이언트 한 명을 수락하고 초기 프로토콜 핸드셰이크(프로토콜 식별값 확인)를 수행한다.
        public P2PGameSession accept() throws IOException {
            synchronized (this) {
                if (consumed) {
                    throw new IllegalStateException("Host invitation has already been used");
                }
                consumed = true;
            }
            try (ServerSocket server = serverSocket) {
                Socket socket = server.accept();
                try {
                    // 작은 상태 메시지가 지연되지 않도록 Nagle 알고리즘을 끈다.
                    socket.setTcpNoDelay(true);
                    // 연결 직후의 핸드셰이크가 응답 없이 무한 대기하지 않도록 제한한다.
                    socket.setSoTimeout(CONNECT_TIMEOUT_MS);
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    if (input.readInt() != PROTOCOL_MAGIC) {
                        throw new IOException("Client uses an incompatible multiplayer protocol");
                    }
                    // 클라이언트가 호스트와 동일한 규칙으로 플레이하도록 호스트 모드를 알린다.
                    output.writeInt(mode.ordinal());
                    output.writeInt(timeLimitSeconds);
                    output.flush();
                    // 핸드셰이크가 끝난 뒤에는 세션 종료까지 계속 메시지를 수신한다.
                    socket.setSoTimeout(0);
                    return new P2PGameSession(socket, input, output, mode, timeLimitSeconds, true);
                } catch (IOException | RuntimeException exception) {
                    // 세션 생성에 실패한 소켓도 누수되지 않도록 닫고, 닫기 실패는 원래 오류에 첨부한다.
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        exception.addSuppressed(closeException);
                    }
                    throw exception;
                }
            }
        }

        // 연결 대기 소켓을 닫아 accept()를 종료시킨다.
        @Override
        public synchronized void close() throws IOException {
            serverSocket.close();
        }
    }

    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    // 호스트는 시작 전까지 바꿀 수 있고, 클라이언트는 시작 메시지로 받은 값을 적용한다.
    private volatile int timeLimitSeconds;
    // 게임 모드. 호스트는 시작 전에 정하고, 클라이언트는 시작 메시지로 받는다. 시작 전에는 null.
    private volatile GameMode mode;
    private final boolean server;
    private volatile Listener listener = new Listener() {};
    private boolean closed;
    private boolean localReady;
    private boolean remoteReady;
    private boolean roundActive;
    private boolean localGameOver;
    // 이번 라운드에서 내가 항복했는지 / 상대가 항복했는지 (결과 화면의 종료 사유용)
    private volatile boolean localSurrendered;
    private volatile boolean remoteSurrendered;
    private GameStatus remoteStatus;
    private ConnectionMonitor connectionMonitor=new ConnectionMonitor();
    private volatile Thread monitorThread;

    private P2PGameSession(
            Socket socket,
            DataInputStream input,
            DataOutputStream output,
            GameMode mode,
            int timeLimitSeconds,
            boolean server) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.mode = mode;
        this.timeLimitSeconds = timeLimitSeconds;
        this.server = server;

        // 연결 모니터 콜백 설정
        connectionMonitor.setCallback(new Callback() {
            @Override
            public void onLatency() {
                listener.onConnectionLatency();
            }

            @Override
            public void onDisconnected() {
                try {
                    synchronized (P2PGameSession.this) {
                        if (!closed) {
                            notifyDisconnected(new IOException("연결이 끊어졌습니다"));
                        }
                    }
                } catch (Exception e) {
                    // 콜백에서 예외 발생 시 무시
                }
            }
            
            @Override
            public void onConnectionRecovered() {
                listener.onConnectionRecovered();
            }
        });


        // 읽기 전용 데몬 스레드가 수신을 담당해 게임/UI 스레드의 입력을 막지 않는다.
        Thread reader = new Thread(this::readMessages, "p2p-reader");
        reader.setDaemon(true);
        reader.start();

        // Keep-alive 스레드 시작 - 연결이 유휴 상태일 때도 정기적으로 신호 전송
        Thread keepAliveThread = new Thread(this::sendKeepAlives, "p2p-keepalive");
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();

        // 지연 감시 스레드 시작
        monitorThread = new Thread(this::monitorConnection, "p2p-monitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * 서버 주소의 기본 포트에 접속한다. 게임 모드는 호스트가 시작할 때 전달된다.
     * 초기 핸드셰이크에 실패하면 연결 소켓을 닫은 뒤 원래 오류를 호출자에게 전달한다.
     *
     * @param host 접속할 호스트 이름 또는 IP 주소
     * @return 연결 및 핸드셰이크가 완료된 클라이언트 세션
     */
    public static P2PGameSession connect(String host) throws IOException {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Server address is required");
        }
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host.trim(), DEFAULT_PORT), CONNECT_TIMEOUT_MS);
            socket.setTcpNoDelay(true);
            // 핸드셰이크 응답이 오지 않는 경우 연결 설정 단계에서 빠져나오도록 제한한다.
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            output.writeInt(PROTOCOL_MAGIC);
            output.flush();
            int modeOrdinal = input.readInt();
            GameMode[] modes = GameMode.values();
            if (modeOrdinal < 0 || modeOrdinal >= modes.length) {
                throw new IOException("Server selected an unsupported game mode");
            }
            int timeLimitSeconds = input.readInt();
            if (timeLimitSeconds < 0 || timeLimitSeconds > MAX_TIME_LIMIT_SECONDS) {
                throw new IOException("Server selected an unsupported time limit");
            }
            // 핸드셰이크 이후에는 지속적인 상태 수신을 위해 읽기 제한 시간을 해제한다.
            socket.setSoTimeout(0);
            return new P2PGameSession(socket, input, output, modes[modeOrdinal], timeLimitSeconds, false);
        } catch (IOException | RuntimeException exception) {
            // 연결 중 오류가 발생해도 열린 소켓을 남기지 않는다.
            try {
                socket.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    // 시작 전에는 null일 수 있다.
    public GameMode getMode() {
        return mode;
    }

    /** 시간 제한 대전의 제한 시간(초). 0이면 시간 제한 없음 */
    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public boolean isTimeLimited() {
        return timeLimitSeconds > 0;
    }

    static void validateTimeLimit(int seconds) {
        if (seconds < 0 || seconds > MAX_TIME_LIMIT_SECONDS) {
            throw new IllegalArgumentException("제한 시간은 0~" + MAX_TIME_LIMIT_SECONDS + "초여야 합니다.");
        }
    }

    /**
     * 호스트가 게임 모드를 정한다. 첫 라운드가 시작되기 전까지만 바꿀 수 있다.
     *
     * @param mode 선택한 게임 모드
     */
    public synchronized void setMode(GameMode mode) {
        setMatch(mode, timeLimitSeconds);
    }

    /**
     * 호스트가 게임 모드와 제한 시간을 함께 정한다. 방을 연 뒤에도 첫 라운드 시작 전까지 바꿀 수 있다.
     *
     * @param mode 선택한 게임 모드
     * @param timeLimitSeconds 제한 시간(초). 0이면 시간 제한 없음
     */
    public synchronized void setMatch(GameMode mode, int timeLimitSeconds) {
        Objects.requireNonNull(mode, "mode");
        validateTimeLimit(timeLimitSeconds);
        if (!server) {
            throw new IllegalStateException("Only the host can choose the game mode");
        }
        if (roundActive) {
            throw new IllegalStateException("Game mode cannot change during an active round");
        }
        this.mode = mode;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public boolean isServer() {
        return server;
    }

    public synchronized boolean isConnected() {
        return !closed && socket.isConnected() && !socket.isClosed();
    }

    public synchronized boolean isRoundActive() {
        return roundActive;
    }

    public synchronized GameStatus getRemoteStatus() {
        return remoteStatus;
    }

    // 이번 라운드에서 내가 항복했는지 여부
    public synchronized boolean isLocalSurrendered() {
        return localSurrendered;
    }

    // 이번 라운드에서 상대가 항복했는지 여부
    public synchronized boolean isRemoteSurrendered() {
        return remoteSurrendered;
    }

    public void setListener(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    //로컬 플레이어의 준비 상태 알림. 호스트는 모드를 정한 뒤에만 준비할 수 있다.
    public void setReady() throws IOException {
        synchronized (this) {
            ensureConnected();
            if (roundActive || localReady) {
                return;
            }
            if (server && mode == null) {
                throw new IllegalStateException("Host must choose a game mode before starting");
            }
            localReady = true;
            writeMessage(MESSAGE_READY, true, null);
            if (server && remoteReady) {
                startRound();
            }
        }
    }

    // 현재 라운드의 로컬 플레이어 상태를 전송한다.
    // 라운드가 활성화되지 않은 상태에서는 상태 메시지를 보내지 않는다.
    public void sendStatus(GameStatus status) throws IOException {
        Objects.requireNonNull(status, "status");
        synchronized (this) {
            ensureConnected();
            if (!roundActive) {
                return;
            }
            writeMessage(MESSAGE_STATUS, false, status);
            localGameOver = status.isGameOver();
            finishRoundIfComplete();
        }
    }

    /**
     * 내가 항복했음을 알린다. status는 게임 오버 상태여야 하며 라운드를 바로 끝낸다.
     */
    public void sendSurrender(GameStatus status) throws IOException {
        Objects.requireNonNull(status, "status");
        synchronized (this) {
            ensureConnected();
            if (!roundActive) {
                return;
            }
            localSurrendered = true;
            writeMessage(MESSAGE_SURRENDER, false, status);
            localGameOver = true;
            finishRoundIfComplete();
        }
    }

    /**
     * 내가 지운 줄로 만든 공격을 상대에게 보낸다. 라운드 중이 아니면 보내지 않는다.
     * 각 줄은 10칸이며 칸 값은 0(구멍) 또는 BlockType.ATTACK이다.
     */
    public void sendAttack(List<int[]> rows) throws IOException {
        Objects.requireNonNull(rows, "rows");
        if (rows.isEmpty()) {
            return;
        }
        if (rows.size() > MAX_ATTACK_ROWS) {
            throw new IllegalArgumentException("Too many attack rows");
        }
        for (int[] row : rows) {
            if (row == null || row.length != BOARD_COLS) {
                throw new IllegalArgumentException("Attack row must have 10 cells");
            }
        }
        synchronized (this) {
            ensureConnected();
            if (!roundActive) {
                return;
            }
            output.writeByte(MESSAGE_ATTACK);
            output.writeByte(rows.size());
            for (int[] row : rows) {
                for (int cell : row) {
                    output.writeBoolean(cell != 0);
                }
            }
            output.flush();
        }
    }

    /**
     * 호스트 전용. 제한 시간이 끝났음을 알리고 확정 점수를 보낸다. 라운드는 여기서 끝난다.
     */
    public void sendTimeUp(int hostScore, int clientScore) throws IOException {
        if (!server) {
            throw new IllegalStateException("Only the host decides a time-limit result");
        }
        synchronized (this) {
            ensureConnected();
            if (!roundActive) {
                return;
            }
            output.writeByte(MESSAGE_TIME_UP);
            output.writeInt(Math.max(0, hostScore));
            output.writeInt(Math.max(0, clientScore));
            output.flush();
            endRound();
        }
    }

    // 소켓 입력을 계속 읽어 메시지별 처리기로 전달한다.
    // 상대의 정상 종료 통지, 예기치 않은 연결 종료, 알 수 없는 메시지를 구분해 리스너에 알린다.
    private void readMessages() {
        try {
            while (true) {
                int message = input.readUnsignedByte();

                // 메시지 수신할 때마다 연결 모니터 업데이트
                connectionMonitor.recordMessageReceived();
                // 메시지 식별자에 따라 필요한 데이터만 읽고 해당 상태 전이 처리기에 전달한다.
                switch (message) {
                    case MESSAGE_READY -> receiveReady(input.readBoolean());
                    case MESSAGE_START -> receiveStart(input.readUnsignedByte(), input.readInt());
                    case MESSAGE_STATUS -> receiveStatus(GameStatus.readFrom(input), false);
                    case MESSAGE_SURRENDER -> receiveStatus(GameStatus.readFrom(input), true);
                    case MESSAGE_ATTACK -> receiveAttack(readAttackRows());
                    case MESSAGE_TIME_UP -> receiveTimeUp(input.readInt(), input.readInt());
                    case MESSAGE_KEEP_ALIVE -> {
                        // Keep-alive 메시지는 처리 없음 - recordMessageReceived()에서 활동으로 기록됨
                    }
                    case MESSAGE_CLOSE -> {
                        // 상대가 명시적으로 종료한 경우에는 종료 사유를 리스너에 한 번 알린다.
                        notifyDisconnected(new IOException("상대 플레이어가 연결을 종료했습니다."));
                        return;
                    }
                    default -> throw new IOException("Unknown multiplayer message: " + message);
                }
            }
        } catch (EOFException | SocketException exception) {
            notifyIfUnexpected(exception);
        } catch (IOException exception) {
            notifyIfUnexpected(exception);
        } finally {
            // 수신 루프가 어떤 이유로 끝나더라도 실제 전송 자원은 정리한다.
            try {
                closeTransport();
            } catch (IOException exception) {
                notifyDisconnected(exception);
            }
        }
    }

    // 상대 준비 상태를 저장하고, 호스트라면 양쪽 준비 여부를 확인해 라운드를 시작한다.
    private void receiveReady(boolean ready) throws IOException {
        synchronized (this) {
            if (roundActive) {
                throw new IOException("Ready message received during an active round");
            }
            remoteReady = ready;
            listener.onRemoteReady(ready);
            if (server && ready && localReady) {
                startRound();
            }
        }
    }

    // 클라이언트가 호스트의 시작 메시지를 받았을 때 유효한 상태인지 확인하고, 호스트가 정한 모드를 적용한 뒤 라운드를 연다.
    private void receiveStart(int modeOrdinal, int startTimeLimitSeconds) throws IOException {
        synchronized (this) {
            GameMode[] modes = GameMode.values();
            if (server || roundActive || !localReady || !remoteReady
                    || modeOrdinal < 0 || modeOrdinal >= modes.length
                    || startTimeLimitSeconds < 0 || startTimeLimitSeconds > MAX_TIME_LIMIT_SECONDS) {
                throw new IOException("Unexpected multiplayer start message");
            }
            GameMode startMode = modes[modeOrdinal];
            mode = startMode;
            timeLimitSeconds = startTimeLimitSeconds;
            beginRound();
        }
    }

    // 상대 상태를 저장해 전달하고, 게임 오버 여부에 따라 라운드 종료를 검사한다.
    private void receiveStatus(GameStatus status, boolean surrendered) throws IOException {
        synchronized (this) {
            if (!roundActive) {
                throw new IOException("Player status received outside an active round");
            }
            // 리스너가 결과를 그리기 전에 항복 여부가 보이도록 먼저 기록한다
            remoteSurrendered = surrendered;
            remoteStatus = status;
            listener.onRemoteStatus(status);
            finishRoundIfComplete();
        }
    }

    private List<int[]> readAttackRows() throws IOException {
        int count = input.readUnsignedByte();
        if (count < 1 || count > MAX_ATTACK_ROWS) {
            throw new IOException("Invalid attack row count: " + count);
        }
        List<int[]> rows = new ArrayList<>(count);
        for (int r = 0; r < count; r++) {
            int[] row = new int[BOARD_COLS];
            for (int c = 0; c < BOARD_COLS; c++) {
                row[c] = input.readBoolean() ? BlockType.ATTACK.getValue() : 0;
            }
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    // 라운드가 끝난 뒤 늦게 도착한 공격은 버린다 (상대는 이미 결과를 보고 있음)
    private void receiveAttack(List<int[]> rows) {
        synchronized (this) {
            if (!roundActive) {
                return;
            }
        }
        listener.onAttack(rows);
    }

    private void receiveTimeUp(int hostScore, int clientScore) throws IOException {
        synchronized (this) {
            if (server) {
                throw new IOException("Only the host can end a time-limit match");
            }
            if (!roundActive) {
                return;
            }
            if (hostScore < 0 || clientScore < 0) {
                throw new IOException("Invalid time-up scores");
            }
            endRound();
        }
        listener.onTimeUp(hostScore, clientScore);
    }

    // 양쪽 준비를 확인한 호스트가 클라이언트에 시작을 알리고 자신의 라운드도 연다.
    private void startRound() throws IOException {
        if (!server || roundActive || !localReady || !remoteReady) {
            return;
        }
        writeMessage(MESSAGE_START, false, null);
        beginRound();
    }

    private void beginRound() {
        // 연결은 유지하고, 이전 라운드에 한정된 준비/게임 오버/상대 상태만 초기화한다.
        localReady = false;
        remoteReady = false;
        localGameOver = false;
        localSurrendered = false;
        remoteStatus = null;
        remoteSurrendered = false;
        roundActive = true;
        connectionMonitor.reset();
        listener.onStarted();
    }

    // 어느 한쪽의 게임 오버가 확인되면 라운드 종료
    // 종료 후에는 다음 준비 입력을 받을 수 있도록 라운드별 준비 상태를 초기화
    private void finishRoundIfComplete() {
        if (!roundActive
                || (!localGameOver && (remoteStatus == null || !remoteStatus.isGameOver()))) {
            return;
        }
        listener.onGameDecided();
        endRound();
    }

    private void endRound() {
        roundActive = false;
        localReady = false;
        remoteReady = false;
        listener.onRoundFinished();
    }

    // 메시지를 프로토콜 형식에 맞춰 직렬화하고 즉시 전송한다.
    // READY는 boolean 하나를, START는 게임 모드 번호 하나를, STATUS는 GameStatus 필드들을 추가로 기록한다.
    private void writeMessage(int message, boolean ready, GameStatus status) throws IOException {
        output.writeByte(message);
        if (message == MESSAGE_READY) {
            output.writeBoolean(ready);
        } else if (message == MESSAGE_START) {
            output.writeByte(mode.ordinal());
            output.writeInt(timeLimitSeconds);
        } else if (message == MESSAGE_STATUS || message == MESSAGE_SURRENDER) {
            status.writeTo(output);
        }
        output.flush();
    }

    // 송신이나 상태 변경 전에 소켓이 사용 가능한지 확인
    private void ensureConnected() throws IOException {
        if (!isConnected()) {
            throw new IOException("Multiplayer connection is closed");
        }
    }

    // 세션이 로컬에서 이미 닫힌 것이 아닌 경우에만 연결 끊김을 알림
    private void notifyIfUnexpected(IOException exception) {
        synchronized (this) {
            if (!closed) {
                notifyDisconnected(exception);
            }
        }
    }

    // 현재 리스너에 연결 종료 또는 통신 오류를 전달
    private void notifyDisconnected(IOException exception) {
        listener.onDisconnected(exception);
    }

    // 상대에게 종료 메시지를 보내고 소켓을 닫는다.
    // 메시지 전송과 소켓 정리 중 오류가 모두 발생하면 첫 오류를 주 오류로 유지하고
    // 추가 오류는 suppressed exception으로 보존한다.
    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            // 원격 피어가 단순한 EOF와 명시적 종료를 구분할 수 있도록 먼저 종료 메시지를 보낸다.
            output.writeByte(MESSAGE_CLOSE);
            output.flush();
        } catch (IOException exception) {
            failure = exception;
        }
        try {
            socket.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    // 수신 스레드 종료 시 연결을 정리하며, 이미 닫힌 세션은 다시 닫지 않는다.
    private synchronized void closeTransport() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        socket.close();
    }

    // Keep-alive 메시지 정기 전송 - 연결이 유휴 상태일 때도 활동을 신호
    private void sendKeepAlives(){
        try{
            // 2초마다 keep-alive 메시지 전송 (3초 지연 threshold보다 작음)
            while(!Thread.currentThread().isInterrupted()){
                Thread.sleep(2000);
                synchronized(this){
                    if(closed){
                        return;
                    }
                    try {
                        output.writeByte(MESSAGE_KEEP_ALIVE);
                        output.flush();
                    } catch (IOException e) {
                        // 송신 실패는 연결 모니터에서 감지
                    }
                }
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }

    // 연결 상태 모니터링 스레드
    private void monitorConnection(){
        try{
            while(!Thread.currentThread().isInterrupted()){
                synchronized(this){
                    if(closed){
                        return;
                    }
                    connectionMonitor.checkAndUpdate();
                }
                Thread.sleep(500);
            }
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        }
    }
}
