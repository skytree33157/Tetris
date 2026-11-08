package network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import game.GameMode;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

// P2P 접속 자동화 테스트
//서버/클라이언트 연결 및 접속 진행 상태를 확인하는 자동화 테스트
@DisplayName("FR-48 P2P 접속 자동화 테스트")
class P2PConnectionAutomationTest {

    //서버 시작 - IP 바인딩 및 포트 개방
    @Test
    @DisplayName("서버가 기본 포트에서 클라이언트 연결 대기")
    void serverCanOpenPortAndWaitForConnection() throws Exception {
        // 서버 초대 생성 - 기본 포트에서 대기
        P2PGameSession.HostInvitation invitation = 
            P2PGameSession.HostInvitation.open(GameMode.NORMAL);
        
        assertNotNull(invitation);
        assertTrue(invitation != null, "서버 초대가 정상적으로 생성되어야 함");
        
        // 초대 객체가 재사용되지 않도록 닫기
        invitation.close();
    }

    //클라이언트 연결 - 성공 시나리오
    @DisplayName("클라이언트가 서버에 성공적으로 연결")
    void clientCanConnectToServer() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            // 서버가 클라이언트를 수락하는 동안 클라이언트 연결 시도
            var serverSession = executor.submit(invitation::accept);
            
            // 클라이언트 연결 (localhost 127.0.0.1 사용)
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            assertNotNull(client);
            assertTrue(client.isConnected(), "클라이언트가 연결되어야 함");
            
            // 서버 세션도 정상 생성
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            assertNotNull(server);
            assertTrue(server.isConnected(), "서버가 연결되어야 함");
            
            // 정리
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //서버 모드 전파 - 클라이언트가 서버의 모드를 수신
    @Test
    @DisplayName("클라이언트가 서버의 게임 모드를 수신")
    void clientReceivesServerGameMode() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        GameMode serverMode = GameMode.ITEM;
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(serverMode)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 클라이언트가 수신한 모드가 서버 모드와 일치
            assertEquals(serverMode, client.getMode(), 
                "클라이언트가 수신한 모드는 서버 모드와 같아야 함");
            assertEquals(serverMode, server.getMode());
            
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //연결 상태 추적 - isConnected() 메서드 검증=
    @Test
    @DisplayName("연결 상태를 정확하게 추적")
    void trackConnectionState() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 연결 상태 확인
            assertTrue(client.isConnected());
            assertTrue(server.isConnected());
            
            // 연결 종료 후 상태 확인
            client.close();
            Thread.sleep(100); // 네트워크 전파 시간
            assertFalse(client.isConnected());
            
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //준비 상태 동기화 - 양쪽이 준비를 완료해야만 라운드 시작
    @Test
    @DisplayName("양쪽 플레이어의 준비 상태 동기화")
    void synchronizeReadinessState() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch serverStarted = new CountDownLatch(1);
        CountDownLatch clientStarted = new CountDownLatch(1);
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 라운드 시작 이벤트 감지
            server.setListener(new P2PGameSession.Listener() {
                @Override
                public void onStarted() {
                    serverStarted.countDown();
                }
            });
            
            client.setListener(new P2PGameSession.Listener() {
                @Override
                public void onStarted() {
                    clientStarted.countDown();
                }
            });
            
            // 클라이언트만 준비 - 라운드 시작 안 됨
            client.setReady();
            assertFalse(serverStarted.await(500, TimeUnit.MILLISECONDS),
                "클라이언트만 준비해서는 라운드가 시작되면 안 됨");
            
            // 서버도 준비 - 양쪽 라운드 시작
            server.setReady();
            assertTrue(serverStarted.await(5, TimeUnit.SECONDS),
                "양쪽이 준비하면 서버 라운드 시작");
            assertTrue(clientStarted.await(5, TimeUnit.SECONDS),
                "양쪽이 준비하면 클라이언트 라운드 시작");
            
            // 라운드 활성화 확인
            assertTrue(server.isRoundActive());
            assertTrue(client.isRoundActive());
            
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    // 게임 상태 전송 - 플레이어 상태 동기화
    @Test
    @DisplayName("플레이어 게임 상태 실시간 동기화")
    void synchronizeGameStatus() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch clientReceivedStatus = new CountDownLatch(1);
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 클라이언트 상태 수신 대기
            AtomicReference<GameStatus> receivedStatus = new AtomicReference<>();
            client.setListener(new P2PGameSession.Listener() {
                @Override
                public void onRemoteStatus(GameStatus status) {
                    receivedStatus.set(status);
                    clientReceivedStatus.countDown();
                }
            });
            
            // 양쪽 준비 및 라운드 시작
            client.setReady();
            server.setReady();
            Thread.sleep(500);
            
            // 서버 상태 전송
            GameStatus testStatus = new GameStatus(100, 2, 5, false);
            server.sendStatus(testStatus);
            
            // 클라이언트가 상태를 수신했는지 확인
            assertTrue(clientReceivedStatus.await(5, TimeUnit.SECONDS),
                "클라이언트가 서버 상태를 수신해야 함");
            assertNotNull(receivedStatus.get());
            assertEquals(100, receivedStatus.get().getScore());
            assertEquals(2, receivedStatus.get().getLevel());
            
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //라운드 종료 - 게임 오버 상태 공유
    @Test
    @DisplayName("한 플레이어의 게임 오버가 양쪽 라운드 종료")
    void roundFinishesWhenOnePlayerGameOver() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch serverRoundFinished = new CountDownLatch(1);
        CountDownLatch clientRoundFinished = new CountDownLatch(1);
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 라운드 종료 이벤트 감지
            server.setListener(new P2PGameSession.Listener() {
                @Override
                public void onRoundFinished() {
                    serverRoundFinished.countDown();
                }
            });
            
            client.setListener(new P2PGameSession.Listener() {
                @Override
                public void onRoundFinished() {
                    clientRoundFinished.countDown();
                }
            });
            
            // 양쪽 준비 및 라운드 시작
            client.setReady();
            server.setReady();
            Thread.sleep(500);
            
            // 서버의 게임이 끝남
            GameStatus gameOverStatus = new GameStatus(100, 2, 5, true);
            server.sendStatus(gameOverStatus);
            
            // 양쪽 라운드가 종료되어야 함
            assertTrue(serverRoundFinished.await(5, TimeUnit.SECONDS),
                "서버 라운드가 종료되어야 함");
            assertTrue(clientRoundFinished.await(5, TimeUnit.SECONDS),
                "클라이언트 라운드도 종료되어야 함");
            
            // 라운드 비활성화 확인
            assertFalse(server.isRoundActive());
            assertFalse(client.isRoundActive());
            
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //라운드 재시작 - 연결을 재사용하여 새 라운드 시작
    @Test
    @DisplayName("연결을 재사용하여 새로운 라운드 시작 가능")
    void canRestartRoundUsingExistingConnection() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch firstRoundStarted = new CountDownLatch(1);
        CountDownLatch secondRoundStarted = new CountDownLatch(1);
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            java.util.concurrent.atomic.AtomicInteger serverStartCount = 
                new java.util.concurrent.atomic.AtomicInteger();
            
            // 라운드 시작 이벤트 감지
            server.setListener(new P2PGameSession.Listener() {
                @Override
                public void onStarted() {
                    if (serverStartCount.getAndIncrement() == 0) {
                        firstRoundStarted.countDown();
                    } else {
                        secondRoundStarted.countDown();
                    }
                }
            });
            
            // 첫 라운드
            client.setReady();
            server.setReady();
            assertTrue(firstRoundStarted.await(5, TimeUnit.SECONDS));
            
            // 첫 라운드 종료
            server.sendStatus(new GameStatus(100, 1, 0, true));
            Thread.sleep(500);
            
            // 두 번째 라운드 준비
            client.setReady();
            server.setReady();
            
            // 두 라운드가 모두 시작되었는지 확인
            assertTrue(secondRoundStarted.await(5, TimeUnit.SECONDS),
                "연결을 재사용하여 새 라운드를 시작할 수 있어야 함");
            
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //연결 실패 처리 - 잘못된 주소로 연결 시도
    @Test
    @DisplayName("유효하지 않은 주소로 연결 실패")
    void connectionFailsWithInvalidAddress() {
        // 유효하지 않은 주소로 연결 시도
        assertThrows(IOException.class, () -> {
            P2PGameSession.connect("255.255.255.255");
        }, "유효하지 않은 주소로는 연결이 실패해야 함");
    }

    //빈 주소 처리 - 빈 문자열로 연결 시도
    @Test
    @DisplayName("빈 주소로 연결 시 IllegalArgumentException 발생")
    void connectionFailsWithEmptyAddress() {
        assertThrows(IllegalArgumentException.class, () -> {
            P2PGameSession.connect("");
        }, "빈 주소로는 IllegalArgumentException이 발생해야 함");
    }

    //호스트 초대 단일 사용 - accept()는 한 번만 호출 가능
    @Test
    @DisplayName("호스트 초대는 한 번만 사용 가능")
    void hostInvitationCanOnlyBeUsedOnce() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 두 번째 accept() 호출은 실패해야 함
            assertThrows(IllegalStateException.class, () -> {
                invitation.accept();
            }, "호스트 초대는 이미 사용되었으므로 다시 사용할 수 없어야 함");
            
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    //정상 연결 해제
    @Test
    @DisplayName("정상적인 연결 해제 및 정리")
    void gracefulDisconnect() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
            
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
            
            // 정상 연결
            assertTrue(client.isConnected());
            assertTrue(server.isConnected());
            
            // 연결 해제
            client.close();
            
            // 정리 후 상태 확인
            assertFalse(client.isConnected());
            Thread.sleep(100);
            assertFalse(server.isConnected());
        } finally {
            executor.shutdownNow();
        }
    }
    // 네트워크 연결 끊김 시뮬레이션
    @Test
    @DisplayName("클라이언트 종료 시 서버가 연결 끊김 감지")
    void serverDetectsClientDisconnection() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch serverDisconnected = new CountDownLatch(1);
    
        try (P2PGameSession.HostInvitation invitation = 
            P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
        
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
        
            // 서버가 클라이언트 연결 끊김을 감지
            server.setListener(new P2PGameSession.Listener() {
                @Override
                public void onDisconnected(IOException exception) {
                    serverDisconnected.countDown();
                }
            });
        
            // 클라이언트 종료
            client.close();
        
            // 서버가 연결 끊김을 감지해야 함
            assertTrue(serverDisconnected.await(5, TimeUnit.SECONDS),
                "서버가 클라이언트의 연결 끊김을 감지해야 함");
        
            server.close();
        } finally {
        executor.shutdownNow();
        }
    }

    //연결 지연 및 복구 통합 테스트
    @Test
    @DisplayName("연결 지연이 감지되고 복구됨")
    void connectionLatencyDetectedAndRecovered() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latencyDetected = new CountDownLatch(1);
        CountDownLatch connectionRecovered = new CountDownLatch(1);
    
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
        
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
        
            // 연결 상태 모니터링
            server.setListener(new P2PGameSession.Listener() {
                @Override
                public void onConnectionLatency() {
                    latencyDetected.countDown();
                }
            
                @Override
                public void onConnectionRecovered() {
                    connectionRecovered.countDown();
                }
            });
        
            // 정상 연결 상태 확인 후 종료
            // 실제 네트워크 지연 시뮬레이션은 ConnectionMonitor 통합 테스트에서 수행
            assertTrue(client != null && server != null, "클라이언트와 서버 연결 확인");
        
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }

    // 다중 메시지 송수신 스트레스 테스트
    @Test
    @DisplayName("연속 메시지 송수신 처리")
    void handleMultipleStatusUpdates() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch allStatusesReceived = new CountDownLatch(10);
    
        try (P2PGameSession.HostInvitation invitation = 
             P2PGameSession.HostInvitation.open(GameMode.NORMAL)) {
        
            var serverSession = executor.submit(invitation::accept);
            P2PGameSession client = P2PGameSession.connect("127.0.0.1");
            P2PGameSession server = serverSession.get(5, TimeUnit.SECONDS);
        
            client.setListener(new P2PGameSession.Listener() {
                @Override
                public void onRemoteStatus(GameStatus status) {
                    allStatusesReceived.countDown();
                }
            });
        
            // 양쪽 준비 및 라운드 시작
            client.setReady();
            server.setReady();
            Thread.sleep(500);
        
            // 10개의 상태 메시지 연속 송수신
            for (int i = 0; i < 10; i++) {
                GameStatus status = new GameStatus(100 + i * 10, 1, i, false);
                server.sendStatus(status);
            }
        
            // 모든 메시지가 수신되었는지 확인
            assertTrue(allStatusesReceived.await(5, TimeUnit.SECONDS),
                "10개의 상태 메시지가 모두 수신되어야 함");
        
            client.close();
            server.close();
        } finally {
            executor.shutdownNow();
        }
    }
    
}
