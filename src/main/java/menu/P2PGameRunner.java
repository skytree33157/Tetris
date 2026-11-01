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

        private P2PGameRunnerLoop(P2PGameSession session) {
            this.session = session;
        }

        private void start() {
            showGame();
        }

        private void showGame() {
            // 새로운 게임 화면을 생성하여 표시. 이전 연결을 재사용.
            GameScreen screen = new GameScreen(
                    session.getMode(),
                    session,
                    this::showGame,
                    this::handleDisconnect);
            screen.setVisible(true);
        }

        // 연결 종료
        private void handleDisconnect(IOException exception) {
            try {
                session.close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            JOptionPane.showMessageDialog(
                    null,
                    "대전 연결이 종료되었습니다.\n" + exception.getMessage(),
                    "P2P 연결 종료",
                    JOptionPane.ERROR_MESSAGE);
            new StartMenu().setVisible(true);
        }
    }
}
