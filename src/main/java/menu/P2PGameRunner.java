package menu;

import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import game.GameMode;
import menu.screens.BattleMenuScreen;
import menu.screens.GameScreen;
import menu.screens.StartMenuScreen;
import network.GameStatus;
import network.P2PGameSession;
import network.RecentPeerStore;

public final class P2PGameRunner {
    private P2PGameRunner() {}

    // 연결 결과를 화면(방 만들기/참여 화면)에 알리는 리스너. 모두 EDT에서 호출된다.
    public interface ConnectListener {
        // 연결 성공 직후 호출. 로비는 이 세션으로 준비/시작을 요청한다.
        void onConnected(P2PGameSession session);
        // 상대의 준비 상태 변경. onConnected보다 먼저 올 수 있으므로 화면은 상태를 저장해 둔다.
        void onRemoteReady(boolean ready);
        // 게임 화면을 띄우기 직전에 호출 (로비 창 정리용)
        void onGameStarting();
        // 게임 시작 전에 연결이 끊긴 경우
        void onLobbyDisconnected(IOException exception);
        // 연결 실패. 사용자가 취소한 경우에는 호출되지 않는다.
        void onFailure(IOException exception);
    }

    // 방 만들기 대기를 취소할 수 있는 핸들
    public static final class HostHandle {
        private final P2PGameSession.HostInvitation invitation;
        private volatile boolean cancelled;

        private HostHandle(P2PGameSession.HostInvitation invitation) {
            this.invitation = invitation;
        }

        // 대기 중인 서버 소켓을 닫아 accept()를 끝낸다. 포트도 바로 해제된다.
        public void cancel() {
            cancelled = true;
            try {
                invitation.close();
            } catch (IOException ignored) {
                // 이미 닫혔으면 할 일 없음
            }
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    // 명령줄(--p2p-server)용: 모드를 정한 뒤 바로 준비한다. 실패하면 팝업을 띄우고 시작 메뉴로 돌아간다.
    public static void startServer(GameMode mode) {
        try {
            startServer(commandLineListener(mode));
        } catch (IOException exception) {
            showConnectionFailure(exception);
        }
    }

    // 명령줄(--p2p-client)용: 접속하면 바로 준비한다.
    public static void joinServer(String address) {
        joinServer(address, commandLineListener(null));
    }

    /**
     * 방을 열고 별도 스레드에서 상대를 기다린다. 포트를 열지 못하면 바로 IOException.
     * 게임 모드는 방 생성자가 로비에서 정한다.
     *
     * @return 대기를 취소할 수 있는 핸들
     */
    public static HostHandle startServer(ConnectListener listener) throws IOException {
        Objects.requireNonNull(listener, "listener");
        P2PGameSession.HostInvitation invitation = P2PGameSession.HostInvitation.open();
        HostHandle handle = new HostHandle(invitation);

        // 서버 연결을 기다리는 동안 UI가 멈추지 않도록 별도 스레드에서 실행
        Thread serverThread = new Thread(() -> {
            try (P2PGameSession.HostInvitation owned = invitation) {
                P2PGameSession session = owned.accept();
                if (handle.isCancelled()) {
                    // 취소와 접속이 거의 동시에 일어난 경우: 연결을 닫고 아무 화면도 띄우지 않는다
                    closeQuietly(session);
                    return;
                }
                attachLobby(session, listener);
            } catch (IOException exception) {
                if (!handle.isCancelled()) {
                    SwingUtilities.invokeLater(() -> listener.onFailure(exception));
                }
            }
        }, "p2p-server");
        serverThread.setDaemon(true);
        serverThread.start();
        return handle;
    }

    // 상대 주소로 접속한다. 성공하면 최근 접속 주소에 저장한다.
    public static void joinServer(String address, ConnectListener listener) {
        Objects.requireNonNull(listener, "listener");
        Thread clientThread = new Thread(() -> {
            try {
                P2PGameSession session = P2PGameSession.connect(address);
                rememberQuietly(new RecentPeerStore(), address);
                attachLobby(session, listener);
            } catch (IOException exception) {
                SwingUtilities.invokeLater(() -> listener.onFailure(exception));
            }
        }, "p2p-client");
        clientThread.setDaemon(true);
        clientThread.start();
    }

    // 세션을 로비 라우터에 연결한 뒤 화면에 연결 완료를 알린다. 백그라운드 스레드에서 호출한다.
    private static void attachLobby(P2PGameSession session, ConnectListener listener) {
        LobbyRouter router = new LobbyRouter(session, listener);
        session.setListener(router);
        SwingUtilities.invokeLater(router::connected);
    }

    // 사용자에게 보여줄 연결 실패 문구
    public static String describeFailure(IOException exception) {
        if (exception instanceof BindException) {
            return "포트 " + P2PGameSession.DEFAULT_PORT + "이(가) 이미 사용 중이에요. 다른 TFSE 창이 방을 열고 있지 않은지 확인해 주세요.";
        }
        if (exception instanceof UnknownHostException) {
            return "주소를 찾을 수 없어요. IP를 다시 확인해 주세요.";
        }
        if (exception instanceof SocketTimeoutException) {
            return "응답이 없어요. 상대가 방을 열었는지, 같은 네트워크인지 확인해 주세요.";
        }
        if (exception instanceof ConnectException) {
            return "연결이 거부됐어요. 상대가 방을 열어 둔 상태인지 확인해 주세요.";
        }
        String message = exception.getMessage();
        return "연결하지 못했어요." + (message == null || message.isBlank() ? "" : " (" + message + ")");
    }

    // 명령줄 실행용 리스너. 호스트면 모드를 정한 뒤, 클라이언트면 그대로 준비 신호를 보낸다.
    private static ConnectListener commandLineListener(GameMode hostMode) {
        return new ConnectListener() {
            @Override
            public void onConnected(P2PGameSession session) {
                try {
                    if (hostMode != null) {
                        session.setMode(hostMode);
                    }
                    session.setReady();
                } catch (IOException exception) {
                    showConnectionFailure(exception);
                }
            }

            @Override
            public void onRemoteReady(boolean ready) {
            }

            @Override
            public void onGameStarting() {
            }

            @Override
            public void onLobbyDisconnected(IOException exception) {
                showConnectionFailure(exception);
            }

            @Override
            public void onFailure(IOException exception) {
                showConnectionFailure(exception);
            }
        };
    }

    // 로비 단계의 세션 이벤트를 받아 화면 리스너로 전달하고, 게임 시작(START)이 오면 게임 화면으로 넘긴다.
    // 세션 리더 스레드에서 호출되므로 모든 처리는 EDT로 넘긴다. lobbyClosed/loop는 EDT에서만 접근한다.
    private static final class LobbyRouter implements P2PGameSession.Listener {
        private final P2PGameSession session;
        private final ConnectListener listener;
        private boolean lobbyClosed;
        private P2PGameRunnerLoop loop;

        private LobbyRouter(P2PGameSession session, ConnectListener listener) {
            this.session = session;
            this.listener = listener;
        }

        // EDT에서 실행
        private void connected() {
            if (lobbyClosed) {
                return;
            }
            if (!session.isConnected()) {
                handleDisconnect(new IOException("연결이 끊어졌습니다"));
                return;
            }
            listener.onConnected(session);
        }

        @Override
        public void onRemoteReady(boolean ready) {
            SwingUtilities.invokeLater(() -> {
                if (!lobbyClosed) {
                    listener.onRemoteReady(ready);
                }
            });
        }

        @Override
        public void onStarted() {
            SwingUtilities.invokeLater(this::enterGame);
        }

        @Override
        public void onDisconnected(IOException exception) {
            SwingUtilities.invokeLater(() -> handleDisconnect(exception));
        }

        // EDT에서 실행. 게임 시작 전에 연결이 끊기면 로비에서 처리하고, 이후에는 게임 루프가 처리한다.
        private void enterGame() {
            if (lobbyClosed || loop != null) {
                return;
            }
            listener.onGameStarting();
            loop = new P2PGameRunnerLoop(session);
            loop.start();
        }

        private void handleDisconnect(IOException exception) {
            if (loop != null) {
                loop.handleDisconnect(exception);
                return;
            }
            if (lobbyClosed) {
                return;
            }
            lobbyClosed = true;
            closeQuietly(session);
            listener.onLobbyDisconnected(exception);
        }
    }

    private static void rememberQuietly(RecentPeerStore recentPeers, String address) {
        try {
            recentPeers.remember(address);
        } catch (IOException | IllegalArgumentException ignored) {
            // 주소 저장 실패는 대전 진행에 영향 없음
        }
    }

    private static void closeQuietly(P2PGameSession session) {
        try {
            session.close();
        } catch (IOException ignored) {
        }
    }

    // 연결 실패 시 메시지 표시 (명령줄 실행용)
    private static void showConnectionFailure(IOException exception) {
        JOptionPane.showMessageDialog(
                null,
                "P2P 연결을 시작하지 못했습니다.\n" + describeFailure(exception),
                "P2P 연결 오류", JOptionPane.ERROR_MESSAGE);
        new StartMenuScreen().setVisible(true);
    }

    // P2P 게임 루프 클래스
    private static final class P2PGameRunnerLoop {
        private final P2PGameSession session;
        private volatile GameScreen gameScreen;
        private volatile P2PGameController p2pController;
        private volatile boolean monitoringDisconnection = false;
        private volatile boolean gameFinished = false;

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
            p2pController = new P2PGameController(
                    session,
                    gameScreen,
                    gameScreen.getGameStateManager(),
                    gameScreen.getActionController(),
                    gameScreen.getScoreManager()
            );

            // 게임 화면에 P2P 컨트롤러 설정
            gameScreen.setP2PController(p2pController);
            
            // 통합 리스너: Runner와 Controller 모두에 event 전달
            session.setListener(new P2PGameSession.Listener() {
                @Override
                public void onStarted() {
                    // Controller에 전달
                    p2pController.onSessionStarted();
                    // GameScreen에 게임 시작 허용
                    SwingUtilities.invokeLater(() -> gameScreen.allowGameStart());
                }
                
                @Override
                public void onRemoteStatus(network.GameStatus status) {
                    // Controller에 전달
                    p2pController.onRemoteStatus(status);
                }
                
                @Override
                public void onRoundFinished() {
                    // 승패 판정: 먼저 게임 오버된 쪽이 패배한다
                    boolean won = !p2pController.isLocalGameOver();
                    p2pController.onRoundFinished();
                    // 플래그를 먼저 세워 뒤따르는 연결 종료 이벤트를 무시하게 한다
                    gameFinished = true;
                    SwingUtilities.invokeLater(() -> showGameResult(won));
                }
                
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

            // 로비에서 이미 라운드가 시작됐다면(게임 화면 생성 전 START 수신) 현재 상태를 반영한다.
            if (session.isRoundActive()) {
                p2pController.onSessionStarted();
                gameScreen.allowGameStart();
                GameStatus remoteStatus = session.getRemoteStatus();
                if (remoteStatus != null) {
                    p2pController.onRemoteStatus(remoteStatus);
                }
            }
        }

        // 한 쪽이 게임 오버되면 승패 결과를 띄우고 P2P 초기 화면으로 복귀
        private void showGameResult(boolean won) {
            monitoringDisconnection = false;
            if (gameScreen != null) {
                gameScreen.stopGame();
                gameScreen.dispose();
            }

            String message = won ? "승리했습니다!" : "패배했습니다.";
            JOptionPane.showMessageDialog(
                    null,
                    message,
                    "대전 결과",
                    JOptionPane.INFORMATION_MESSAGE);

            try {
                session.close();
            } catch (IOException ignored) {
                // 상대가 이미 연결을 끊은 경우
            }
            new BattleMenuScreen().setVisible(true);
        }

        // 연결 종료 처리 - P2P 초기 화면으로 복귀
        private void handleDisconnect(IOException exception) {
            if (gameFinished) {
                // 승패가 이미 결정된 뒤의 연결 종료는 정상 흐름(상대가 결과 확인 후 닫음)
                try {
                    session.close();
                } catch (IOException ignored) {
                    // 이미 닫힘
                }
                monitoringDisconnection = false;
                return;
            }
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
                // P2P 초기 화면(대전 메뉴)으로 복귀
                new BattleMenuScreen().setVisible(true);
            });
        }
    }
}
