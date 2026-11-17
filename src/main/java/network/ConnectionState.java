package network;

public enum ConnectionState {
    CONNECTED,      // 정상 상태
    LATENCY,        // 지연 발생
    DISCONNECTED    // 연결 오류
}
