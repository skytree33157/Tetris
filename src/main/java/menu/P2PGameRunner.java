package menu;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import game.GameMode;
import network.P2PMatchSession;

public final class P2PGameRunner {
    private P2PGameRunner() {}

    // 방 생성 (호스트)
    public static void startServer(GameMode mode) {
        Thread serverThread = new Thread(() -> {
            try {
                P2PMatchSession session = P2PMatchSession.createRoom(mode);
                SwingUtilities.invokeLater(() -> new P2PMatchRunnerLoop(session).showLobby());
            } catch (IOException exception) {
                SwingUtilities.invokeLater(() -> showConnectionFailure(exception));
            }
        }, "p2p-server");
        serverThread.start();
    }

    // 방 참여 (클라이언트)
    public static void joinServer(String address) {
        Thread clientThread = new Thread(() -> {
            try {
                P2PMatchSession session = P2PMatchSession.connect(address);
                SwingUtilities.invokeLater(() -> new P2PMatchRunnerLoop(session).showLobby());
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

    // 대기실 → 대전 → 결과까지 한 매치를 관리
    private static final class P2PMatchRunnerLoop {
        private final P2PMatchSession session;
        private GameScreen gameScreen;
        private volatile P2PGameController p2pController;
        private P2PLobbyScreen lobby;
        private volatile boolean finished = false;
        private final LatencyStatusTracker latency = new LatencyStatusTracker();

        private P2PMatchRunnerLoop(P2PMatchSession session) {
            this.session = session;
        }

        private void showLobby() {
            // 게임 화면은 숨긴 채 미리 만들어 두고, 시작 신호에서만 표시
            gameScreen = new GameScreen(session.getMode());
            
            // P2P 게임 컨트롤러 생성 - 게임 화면과 P2P 세션 연결
            p2pController = new P2PGameController(
                    session,
                    gameScreen.getGameStateManager(),
                    gameScreen.getActionController(),
                    gameScreen.getScoreManager()
                );

            // 게임 화면에 P2P 컨트롤러 설정
            gameScreen.setP2PController(p2pController);
            p2pController.attach();

            lobby = new P2PLobbyScreen(session.isServer(), this::startMatch, this::leave);
            lobby.updateRoster(session.getRoster(), session.getSlot());
            lobby.setVisible(true);

            session.setListener(new P2PMatchSession.Listener() {
                @Override
                public void onRosterChanged(List<Integer> slots) {
                    SwingUtilities.invokeLater(() -> {
                        lobby.updateRoster(slots, session.getSlot());
                        // 나간 참가자는 지연 목록에서도 제거해야 라벨이 남지 않는다
                        latency.keepOnly(slots);
                        refreshLatencyStatus();
                        if (gameScreen != null) {
                            gameScreen.retainOpponents(slots);
                        }
                    });
                }

                @Override
                public void onStarted() {
                    SwingUtilities.invokeLater(() -> {
                        lobby.dispose();
                        gameScreen.setVisible(true);
                        gameScreen.allowGameStart();
                    });
                }

                @Override
                public void onRemoteStatus(int slot, network.GameStatus status) {
                    // 공격 판정은 세션/컨트롤러가 처리하고, 여기서는 상대 점수판만 갱신한다
                    if (slot == session.getSlot()) {
                        return;
                    }
                    SwingUtilities.invokeLater(() -> gameScreen.showOpponentStatus(slot, status));
                }

                @Override
                public void onAttack(List<int[]> rows) {
                    // 대기열 삽입은 stateLock으로 보호되므로 네트워크 스레드에서 직접 호출한다. 보드는 다음 블록 고정 시 밀린다.
                    gameScreen.getActionController().receiveAttackRows(rows);
                }

                @Override
                public void onResult(List<Integer> ranking) {
                    SwingUtilities.invokeLater(() -> {
                        finished = true;
                        gameScreen.showMatchResult(ranking, session.getSlot());
                        closeSilently();
                    });
                }

                @Override
                public void onConnectionLatency(int slot) {
                    SwingUtilities.invokeLater(() -> {
                        latency.markLagging(slot);
                        refreshLatencyStatus();
                    });
                }

                @Override
                public void onConnectionRecovered(int slot) {
                    SwingUtilities.invokeLater(() -> {
                        latency.markRecovered(slot);
                        refreshLatencyStatus();
                    });
                }

                @Override
                public void onDisconnected(IOException exception) {
                    // 연결 끊김 처리
                    handleDisconnect(exception);
                }
            });
        }

        // 호스트의 대전 시작 요청 (네트워크 쓰기이므로 EDT 밖에서 수행)
        private void startMatch() {
            Thread starter = new Thread(() -> {
                try {
                    session.start();
                } catch (IOException | IllegalStateException exception) {
                    SwingUtilities.invokeLater(() -> handleDisconnect(
                            exception instanceof IOException io ? io : new IOException(exception.getMessage())));
                }
            }, "p2p-start");
            starter.start();
        }

        private void leave() {
            closeSilently();
            lobby.dispose();
            new StartMenu().setVisible(true);
        }

        private void closeSilently() {
            session.close();
        }

        // 지연 중인 상대 문구를 표시하고, 없으면 경고 라벨을 숨긴다 (EDT에서 호출)
        private void refreshLatencyStatus() {
            if (gameScreen == null) {
                return;
            }
            latency.message().ifPresentOrElse(
                    text -> gameScreen.showConnectionStatus(text, true),
                    gameScreen::hideConnectionStatus);
        }

        // 대전 중 연결 끊김: 게임을 정리하고 초기 화면으로 복귀
        private void handleDisconnect(IOException exception) {
            if (finished) {
                return;
            }
            finished = true;
            session.close();

            SwingUtilities.invokeLater(() -> {
                if (lobby != null) {
                    lobby.dispose();
                }
                if (gameScreen != null) {
                    gameScreen.stopGame();
                    gameScreen.dispose();
                }

                String detail = exception.getMessage() == null ? "" : "\n" + exception.getMessage();
                JOptionPane.showMessageDialog(
                        null,
                        "대전 연결이 종료되었습니다." + detail + "\nP2P 초기 화면으로 돌아갑니다.",
                        "P2P 연결 오류",
                        JOptionPane.ERROR_MESSAGE);
                new StartMenu().setVisible(true);
            });
        }
    }
}
