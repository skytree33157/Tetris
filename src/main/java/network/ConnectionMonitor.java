package network;

import java.util.Objects;

public final class ConnectionMonitor{
    private static final long LATENCY_THRESHOLD=3000;
    private static final long DISCONNECTION_THRESHOLD=10000;

    private volatile Callback callback=null;
    private volatile long lastMessageTime = System.currentTimeMillis();
    private volatile ConnectionState state=ConnectionState.CONNECTED;

    // 모니터 콜백 설정
    public void setCallback(Callback callback){
        this.callback=Objects.requireNonNull(callback, "callback");
        
    }

    // 상대로부터 메시지를 받았음을 기록 및 복구
    public synchronized void recordMessageReceived(){
        lastMessageTime=System.currentTimeMillis();

        //이전 상태가 지연 or 오류 상태였다면 정상으로 복구
        if(state!=ConnectionState.CONNECTED){
            state=ConnectionState.CONNECTED;
        }
    }

    // 현재 연결 상태 확인 및 업데이트
    public synchronized ConnectionState checkAndUpdate(){
        // 마지막 수신 시간으로부터 경과한 시간
        long elapsedTime = System.currentTimeMillis() - lastMessageTime;

        if(elapsedTime>=DISCONNECTION_THRESHOLD){
            if(state!=ConnectionState.DISCONNECTED ){
                state=ConnectionState.DISCONNECTED;
                if(callback!=null){
                    callback.onDisconnected();
                }
            }
        }
        else if(elapsedTime>=LATENCY_THRESHOLD){
            if(state!=ConnectionState.LATENCY){
                state=ConnectionState.LATENCY;
                if(callback!=null){
                    callback.onLatency();
                }
            }
        }
        else{
            if(state!=ConnectionState.CONNECTED){
                state=ConnectionState.CONNECTED;
            }
        }
        return state;
    }

    // 현재 연결 상태 반환
    public synchronized ConnectionState getState(){
        return state;
    }

    // 초기화
    public synchronized void reset(){
        lastMessageTime=System.currentTimeMillis();
        state=ConnectionState.CONNECTED;
    }

}