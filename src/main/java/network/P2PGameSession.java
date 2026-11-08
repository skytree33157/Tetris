package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;

import game.GameMode;

// TCP 연결, 대전 관리
public final class P2PGameSession implements AutoCloseable {
    // TCP 포트
    public static final int DEFAULT_PORT = 57823;
    // handshake용 프로토콜 식별값
    private static final int PROTOCOL_MAGIC = 0x54465345;
    // handshake 완료를 기다리는 최대 시간
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    // 메시지 종류별 식별자. 각 메시지의 실제 필드 형식은 writeMessage/readMessages에서 처리한다.
    private static final int MESSAGE_READY = 1;
    private static final int MESSAGE_START = 2;
    private static final int MESSAGE_STATUS = 3;
    private static final int MESSAGE_CLOSE = 4;
    private static final int MESSAGE_KEEP_ALIVE = 5;


    // 세션 이벤트 리스너 인터페이스
    public interface Listener {
        // 상대 플레이어가 준비 시 호출
        default void onRemoteReady(boolean ready) {}
        // 새 라운드 시작 시 호출
        default void onStarted() {}
        // 상대 플레이어 상태 수신 시 호출
        default void onRemoteStatus(GameStatus status) {}
        // 라운드 종료 시 호출
        default void onRoundFinished() {}
        // 연결 끊김 또는 프로토콜 오류 시 호출
        default void onDisconnected(IOException exception) {}
        // 연결 지연 상태로 전환 시 호출
        default void onConnectionLatency(){}
        // 연결 정상 복구 시 호출
        default void onConnectionRecovered(){}
    }

    // 호스트 초대 클래스
    public static final class HostInvitation implements AutoCloseable {
        private final GameMode mode;
        // 클라이언트 연결을 기다리는 서버 소켓
        private final ServerSocket serverSocket;
        // accept()가 이미 호출되어 초대를 소비했는지 나타낸다. -> accept()는 한 번만 호출 가능
        private boolean consumed;

        private HostInvitation(GameMode mode, ServerSocket serverSocket) {
            this.mode = mode;
            this.serverSocket = serverSocket;
        }

        /**
         * 기본 포트에서 클라이언트 연결을 기다릴 호스트 초대를 만든다.
         *
         * @param mode 호스트가 선택한 게임 모드
         * @return 연결을 수락할 수 있는 호스트 초대
         * @throws IOException 포트를 열지 못한 경우
         */
        public static HostInvitation open(GameMode mode) throws IOException {
            Objects.requireNonNull(mode, "mode");
            ServerSocket serverSocket = new ServerSocket(DEFAULT_PORT);
            return new HostInvitation(mode, serverSocket);
        }

        
        // 클라이언트 한 명을 수락하고 초기 프로토콜 핸드셰이크를 수행한다.
        // 핸드셰이크에서 프로토콜 식별값을 확인한 뒤 호스트의 게임 모드를 전송한다.
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
                    output.flush();
                    // 핸드셰이크가 끝난 뒤에는 세션 종료까지 계속 메시지를 수신한다.
                    socket.setSoTimeout(0);
                    return new P2PGameSession(socket, input, output, mode, true);
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
    private final GameMode mode;
    private final boolean server;
    private volatile Listener listener = new Listener() {};
    private boolean closed;
    private boolean localReady;
    private boolean remoteReady;
    private boolean roundActive;
    private boolean localGameOver;
    private GameStatus remoteStatus;
    private ConnectionMonitor connectionMonitor=new ConnectionMonitor();
    private volatile Thread monitorThread;

    private P2PGameSession(
            Socket socket,
            DataInputStream input,
            DataOutputStream output,
            GameMode mode,
            boolean server) {
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.mode = mode;
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
     * 서버 주소의 기본 포트에 접속하고 호스트로부터 게임 모드를 받는다.
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
            // 초기 모드 응답이 오지 않는 경우 연결 설정 단계에서 빠져나오도록 제한한다.
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
            // 핸드셰이크 이후에는 지속적인 상태 수신을 위해 읽기 제한 시간을 해제한다.
            socket.setSoTimeout(0);
            return new P2PGameSession(socket, input, output, modes[modeOrdinal], false);
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

    public GameMode getMode() {
        return mode;
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

    public void setListener(Listener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    //로컬 플레이어의 준비 상태 알림
    public void setReady() throws IOException {
        synchronized (this) {
            ensureConnected();
            if (roundActive || localReady) {
                return;
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
                    case MESSAGE_START -> receiveStart();
                    case MESSAGE_STATUS -> receiveStatus(GameStatus.readFrom(input));
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

    // 클라이언트가 호스트의 시작 메시지를 받았을 때 유효한 상태인지 확인하고 라운드를 연다.
    private void receiveStart() throws IOException {
        synchronized (this) {
            if (server || roundActive || !localReady || !remoteReady) {
                throw new IOException("Unexpected multiplayer start message");
            }
            beginRound();
        }
    }

    // 상대 상태를 저장해 전달하고, 게임 오버 여부에 따라 라운드 종료를 검사한다.
    private void receiveStatus(GameStatus status) throws IOException {
        synchronized (this) {
            if (!roundActive) {
                throw new IOException("Player status received outside an active round");
            }
            remoteStatus = status;
            listener.onRemoteStatus(status);
            finishRoundIfComplete();
        }
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
        remoteStatus = null;
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
        roundActive = false;
        localReady = false;
        remoteReady = false;
        listener.onRoundFinished();
    }

    // 메시지를 프로토콜 형식에 맞춰 직렬화하고 즉시 전송한다.
    // READY는 boolean 하나를, STATUS는 GameStatus 필드들을 추가로 기록한다.
    private void writeMessage(int message, boolean ready, GameStatus status) throws IOException {
        output.writeByte(message);
        if (message == MESSAGE_READY) {
            output.writeBoolean(ready);
        } else if (message == MESSAGE_STATUS) {
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
