package menu;

import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import game.GameMode;
import network.P2PGameSession;

public final class P2PGameRunner {
    private P2PGameRunner() {}
    
    // 서버 시작
    public static void startServer(GameMode mode) {
        // 서버 연결을 기다리는 동안 UI가 멈추지 않도록 별도 스레드에서 실행
        Thread serverThread = new Thread(() -> {
            try (P2PGameSession.HostInvitation invitation =
                            P2PGameSession.HostInvitation.open(mode)) {
                P2PGameSession session = invitation.accept();
                SwingUtilities.invokeLater(() -> new P2PGameRunnerLoop(session).start());
            } catch (IOException exception) {
                SwingUtilities.invokeLater(() -> showConnectionFailure(exception));
            }
        }, "p2p-server");
        serverThread.start();
    }

    // 서버 참여
    public static void joinServer(String address) {
        Thread clientThread = new Thread(() -> {
            try {
                P2PGameSession session = P2PGameSession.connect(address);
                SwingUtilities.invokeLater(() -> new P2PGameRunnerLoop(session).start());
            } catch (IOException exception) {
                SwingUtilities.invokeLater(() -> showConnectionFailure(exception));
            }
        }, "p2p-client");
        clientThread.start();
    }

    // 연결 실패 시 메시지 표시
    private static void showConnectionFailure(IOException exception) {
        JOptionPane.showMessageDialog(
                null,
                "P2P 연결을 시작하지 못했습니다.\n" + exception.getMessage(),
                "P2P 연결 오류", JOptionPane.ERROR_MESSAGE);
        new StartMenu().setVisible(true);
    }

    // P2P 게임 루프 클래스
    private static final class P2PGameRunnerLoop {
        private final P2PGameSession session;
        private volatile GameScreen gameScreen;
        private volatile boolean monitoringDisconnection = false;

        private P2PGameRunnerLoop(P2PGameSession session) {
            this.session = session;
        }

        private void start() {
            showGame();
        }

        private void showGame() {
            // 게임 화면 생성
            gameScreen = new GameScreen(session.getMode());
            
            // P2P 게임 컨트롤러 생성 - 게임 화면과 P2P 세션 연결
            P2PGameController p2pController = new P2PGameController(
                    session,
                    gameScreen,
                    gameScreen.getGameStateManager(),
                    gameScreen.getActionController(),
                    gameScreen.getScoreManager()
            );

            // 게임 화면에 P2P 컨트롤러 설정
            gameScreen.setP2PController(p2pController);
            
            // P2P 세션 이벤트 리스너 추가 설정 - 지연 및 연결 상태
            session.setListener(new P2PGameSession.Listener() {
                @Override
                public void onConnectionLatency() {
                    // 지연 상태를 UI에 표시
                    SwingUtilities.invokeLater(() -> {
                        gameScreen.showConnectionStatus("연결 지연 중...", true);
                    });
                }
                
                @Override
                public void onConnectionRecovered() {
                    // 연결 정상화 시 상태 메시지 제거
                    SwingUtilities.invokeLater(() -> {
                        gameScreen.hideConnectionStatus();
                    });
                }
                
                @Override
                public void onDisconnected(IOException exception) {
                    // 연결 끊김 처리
                    handleDisconnect(exception);
                }
            });
            
            gameScreen.setVisible(true);
            monitoringDisconnection = true;

            // 게임 화면이 표시된 후 준비 완료 통지
            try {
                p2pController.notifyPlayerReady();
            } catch (IOException exception) {
                handleDisconnect(exception);
            }
        }

        // 연결 종료 처리 - P2P 초기 화면으로 복귀
        private void handleDisconnect(IOException exception) {
            try {
                session.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            
            monitoringDisconnection = false;
            
            SwingUtilities.invokeLater(() -> {
                // 게임 화면 정리 - 렌더링 타이머와 게임 루프 스레드 중지
                if (gameScreen != null) {
                    gameScreen.stopGame();
                    gameScreen.dispose();
                }
                
                // 연결 끊김 상황에 따른 메시지
                String message;
                if ("연결이 끊어졌습니다".equals(exception.getMessage())) {
                    message = "네트워크 연결이 끊어졌습니다.\n P2P 초기 화면으로 돌아갑니다.";
                } else {
                    message = "대전 연결이 종료되었습니다.\n" + exception.getMessage() + "\nP2P 초기 화면으로 돌아갑니다.";
                }
                
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "P2P 연결 오류",
                        JOptionPane.ERROR_MESSAGE);
                // P2P 초기 화면으로 복귀
                new StartMenu().setVisible(true);
            });
        }
    }
}
