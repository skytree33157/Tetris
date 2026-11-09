package network;

public interface Callback{
        // 지연 상태로 전환 시 호출
        void onLatency();

        // 연결 오류로 전환 시 호출
        void onDisconnected();
        
        // 연결 정상 복구 시 호출
        void onConnectionRecovered();
    }