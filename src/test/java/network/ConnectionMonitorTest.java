package network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ConnectionMonitor 단위 테스트
 * 
 * FR-46: 지연 및 연결 끊김 판정 기준 검증
 */
public class ConnectionMonitorTest {
    private ConnectionMonitor monitor;
    private List<String> events;
    
    @BeforeEach
    public void setUp() {
        monitor = new ConnectionMonitor();
        events = new ArrayList<>();
        
        monitor.setCallback(new Callback() {
            @Override
            public void onLatency() {
                events.add("LATENCY");
            }
            
            @Override
            public void onDisconnected() {
                events.add("DISCONNECTED");
            }
        });
    }
    
    /**
     * 정상 연결 상태 - 메시지 수신 후 상태 확인
     */
    @Test
    public void testConnectedState() {
        monitor.recordMessageReceived();
        ConnectionState state = monitor.checkAndUpdate();
        
        assertEquals(ConnectionState.CONNECTED, state);
        assertTrue(events.isEmpty(), "메시지 수신 직후에는 콜백이 호출되지 않아야 함");
    }
    
    /**
     * 지연 상태 판정 - 3초 이상 메시지 없음
     */
    @Test
    public void testLatencyDetection() throws InterruptedException {
        monitor.recordMessageReceived();
        
        // 3.1초 대기 (지연 임계값: 3초)
        Thread.sleep(3100);
        
        ConnectionState state = monitor.checkAndUpdate();
        assertEquals(ConnectionState.LATENCY, state);
        assertTrue(events.contains("LATENCY"), "지연 상태로 전환되어야 함");
    }
    
    /**
     * 지연 상태에서 메시지 수신 시 복구
     */
    @Test
    public void testRecoveryFromLatency() throws InterruptedException {
        // 지연 상태로 전환
        monitor.recordMessageReceived();
        Thread.sleep(3100);
        monitor.checkAndUpdate();
        assertTrue(events.contains("LATENCY"));
        
        events.clear();
        
        // 메시지 수신
        monitor.recordMessageReceived();
        ConnectionState state = monitor.checkAndUpdate();
        
        assertEquals(ConnectionState.CONNECTED, state);
        assertTrue(events.isEmpty() || events.contains("LATENCY") == false);
    }
    
    /**
     * 연결 끊김 판정 - 10초 이상 메시지 없음
     */
    @Test
    public void testDisconnectionDetection() throws InterruptedException {
        monitor.recordMessageReceived();
        
        // 10.1초 대기 (연결 끊김 임계값: 10초)
        Thread.sleep(10100);
        
        ConnectionState state = monitor.checkAndUpdate();
        assertEquals(ConnectionState.DISCONNECTED, state);
        assertTrue(events.contains("DISCONNECTED"), "연결 끊김 상태로 전환되어야 함");
    }
    
    /**
     * 연결 끊김 상태에서는 콜백이 반복되지 않음
     */
    @Test
    public void testDisconnectedCallbackOnce() throws InterruptedException {
        monitor.recordMessageReceived();
        Thread.sleep(10100);
        
        // 첫 번째 체크
        monitor.checkAndUpdate();
        int firstEventCount = events.size();
        assertTrue(events.contains("DISCONNECTED"));
        
        events.clear();
        
        // 두 번째 체크
        monitor.checkAndUpdate();
        assertTrue(events.isEmpty(), "같은 상태 유지 시 콜백이 반복되지 않아야 함");
    }
    
    /**
     * 3초 미만 대기는 지연 상태가 아님
     */
    @Test
    public void testNoLatencyBeforeThreshold() throws InterruptedException {
        monitor.recordMessageReceived();
        
        // 2.9초 대기
        Thread.sleep(2900);
        
        ConnectionState state = monitor.checkAndUpdate();
        assertEquals(ConnectionState.CONNECTED, state);
        assertTrue(events.isEmpty());
    }
    
    /**
     * 10초 미만이면 연결 끊김이 아님
     */
    @Test
    public void testNoDisconnectionBeforeThreshold() throws InterruptedException {
        monitor.recordMessageReceived();
        
        // 9.9초 대기
        Thread.sleep(9900);
        
        ConnectionState state = monitor.checkAndUpdate();
        assertEquals(ConnectionState.LATENCY, state);
        assertTrue(events.contains("LATENCY"));
        assertFalse(events.contains("DISCONNECTED"));
    }
    
    /**
     * 모니터 리셋 - 상태 초기화
     */
    @Test
    public void testReset() throws InterruptedException {
        // 연결 끊김 상태로 만들기
        monitor.recordMessageReceived();
        Thread.sleep(10100);
        monitor.checkAndUpdate();
        
        events.clear();
        
        // 리셋
        monitor.reset();
        ConnectionState state = monitor.checkAndUpdate();
        
        assertEquals(ConnectionState.CONNECTED, state);
        assertTrue(events.isEmpty());
    }
    
    /**
     * 상태 변화 시나리오 - 정상 -> 지연 -> 정상 -> 끊김
     */
    @Test
    public void testStateTransitions() throws InterruptedException {
        // 1. 정상 상태
        monitor.recordMessageReceived();
        assertEquals(ConnectionState.CONNECTED, monitor.checkAndUpdate());
        
        // 2. 지연 상태로 전환
        Thread.sleep(3100);
        assertEquals(ConnectionState.LATENCY, monitor.checkAndUpdate());
        assertTrue(events.contains("LATENCY"));
        
        events.clear();
        
        // 3. 메시지 수신으로 복구
        monitor.recordMessageReceived();
        assertEquals(ConnectionState.CONNECTED, monitor.checkAndUpdate());
        
        events.clear();
        
        // 4. 연결 끊김으로 진행
        Thread.sleep(10100);
        assertEquals(ConnectionState.DISCONNECTED, monitor.checkAndUpdate());
        assertTrue(events.contains("DISCONNECTED"));
    }
}
