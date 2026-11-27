package menu.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JTextArea;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import network.P2PGameSession;

class GameChatPanelTest {

    private GameChatPanel panel;

    private final List<String> sentMessages = new ArrayList<>();
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = new GameChatPanel(
                    P2PGameSession.MAX_CHAT_LENGTH,
                    KeyEvent.VK_ENTER,
                    message -> {
                        sentMessages.add(message);
                        events.add("send");
                    },
                    () -> events.add("close")
            );
        });
    }

    @Test
    void openingEnterDoesNotImmediatelySend() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.setChatEnabled(true);
            assertTrue(panel.openInput());

            panel.getInputField().setText("안녕하세요");

            // 채팅을 연 Enter를 아직 떼지 않은 상태
            panel.getInputField().postActionEvent();

            assertTrue(panel.isInputOpen());
            assertTrue(sentMessages.isEmpty());
            assertTrue(events.isEmpty());

            // Enter를 뗀 다음 다시 누르면 전송된다.
            panel.openingKeyReleased();
            panel.getInputField().postActionEvent();

            assertEquals(List.of("안녕하세요"), sentMessages);
            assertFalse(panel.isInputOpen());
        });
    }

    @Test
    void sendingClosesInputAndSuccessAllowsOpeningAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("전송 확인");

            panel.getInputField().postActionEvent();

            assertEquals(List.of("전송 확인"), sentMessages);

            // 게임 조작으로 복귀한 다음 전송을 요청해야 한다.
            assertEquals(List.of("close", "send"), events);
            assertFalse(panel.isInputOpen());

            // 전송 중에는 다시 열리지 않는다.
            assertFalse(panel.openInput());

            panel.sendSucceeded("전송 확인");

            // 완료되어도 입력칸이 자동으로 열리지는 않는다.
            assertFalse(panel.isInputOpen());
            assertEquals("", panel.getInputField().getText());
            assertTrue(panel.openInput());
        });
    }

    @Test
    void escapeCancelsWithoutSendingAndKeepsDraft() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("작성 중인 메시지");

            Object actionKey = panel.getInputField()
                    .getInputMap(JComponent.WHEN_FOCUSED)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

            assertNotNull(actionKey);

            Action escapeAction = panel.getInputField()
                    .getActionMap()
                    .get(actionKey);

            assertNotNull(escapeAction);

            escapeAction.actionPerformed(new ActionEvent(
                    panel.getInputField(),
                    ActionEvent.ACTION_PERFORMED,
                    "test-escape"
            ));

            assertFalse(panel.isInputOpen());
            assertTrue(sentMessages.isEmpty());
            assertEquals(List.of("close"), events);

            assertTrue(panel.openInput());
            assertEquals(
                    "작성 중인 메시지",
                    panel.getInputField().getText()
            );
        });
    }

    @Test
    void blankMessageClosesWithoutSending() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("   ");

            panel.getInputField().postActionEvent();

            assertFalse(panel.isInputOpen());
            assertTrue(sentMessages.isEmpty());
            assertEquals("", panel.getInputField().getText());
            assertEquals(List.of("close"), events);
        });
    }

    @Test
    void lengthLimitRejectsOverflowAndAcceptsBoundary() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();

            String boundary =
                    "가".repeat(P2PGameSession.MAX_CHAT_LENGTH);

            // 제한 길이보다 한 글자 많으면 입력칸을 유지한다.
            panel.getInputField().setText(boundary + "나");
            panel.getInputField().postActionEvent();

            assertTrue(panel.isInputOpen());
            assertTrue(sentMessages.isEmpty());
            assertTrue(events.isEmpty());

            // 정확히 제한 길이면 전송할 수 있다.
            panel.getInputField().setText(boundary);
            panel.getInputField().postActionEvent();

            assertEquals(List.of(boundary), sentMessages);
            assertFalse(panel.isInputOpen());
        });
    }

    @Test
    void failedSendKeepsDraftAndAllowsRetry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("다시 보낼 메시지");

            panel.getInputField().postActionEvent();
            panel.sendFailed("연결 오류");

            assertFalse(panel.isInputOpen());
            assertEquals(
                    "다시 보낼 메시지",
                    panel.getInputField().getText()
            );

            assertTrue(panel.openInput());
            panel.openingKeyReleased();
            panel.getInputField().postActionEvent();

            assertEquals(
                    List.of("다시 보낼 메시지", "다시 보낼 메시지"),
                    sentMessages
            );
            assertEquals(
                    List.of("close", "send", "close", "send"),
                    events
            );
        });
    }

    @Test
    void receivedMessagesAppearInOrderWithoutOpeningInput() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.setChatEnabled(true);

            panel.appendRemoteMessage("안녕하세요");
            panel.appendRemoteMessage("반갑습니다");

            JTextArea history = findConversation(panel);
            assertNotNull(history);

            assertEquals(
                    "[상대] 안녕하세요\n[상대] 반갑습니다\n",
                    history.getText()
            );

            // 수신만으로 입력창이 열리거나 전송이 발생하면 안 된다.
            assertFalse(panel.isInputOpen());
            assertTrue(sentMessages.isEmpty());
            assertTrue(events.isEmpty());
        });
    }

    @Test
    void receivingMessageWhileEditingKeepsDraft() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("작성 중인 메시지");

            panel.appendRemoteMessage("상대 메시지");

            assertTrue(panel.isInputOpen());
            assertTrue(panel.getInputField().isEnabled());
            assertEquals(
                    "작성 중인 메시지",
                    panel.getInputField().getText()
            );

            JTextArea history = findConversation(panel);
            assertNotNull(history);
            assertEquals("[상대] 상대 메시지\n", history.getText());

            assertTrue(sentMessages.isEmpty());
            assertTrue(events.isEmpty());
        });
    }

    @Test
    void disablingClosesInputOnceAndBlocksSending() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("작성 중인 메시지");

            panel.setChatEnabled(false);
            panel.setChatEnabled(false);

            // 비활성화 후 전송 동작을 호출해도 전송되지 않아야 한다.
            panel.getInputField().postActionEvent();

            assertFalse(panel.isInputOpen());
            assertFalse(panel.getInputField().isEnabled());
            assertFalse(panel.openInput());

            assertTrue(sentMessages.isEmpty());
            assertEquals(List.of("close"), events);
            assertEquals(
                    "작성 중인 메시지",
                    panel.getInputField().getText()
            );
        });
    }

    @Test
    void lateSendSuccessDoesNotReenableDisabledChat() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            openInputAfterKeyRelease();
            panel.getInputField().setText("전송 중인 메시지");
            panel.getInputField().postActionEvent();

            // 전송 중에 라운드 종료 등으로 채팅이 비활성화된 상황
            panel.setChatEnabled(false);

            // 뒤늦게 전송 성공 결과가 도착한다.
            panel.sendSucceeded("전송 중인 메시지");

            assertFalse(panel.isInputOpen());
            assertFalse(panel.getInputField().isEnabled());
            assertFalse(panel.openInput());

            assertEquals(List.of("전송 중인 메시지"), sentMessages);
            assertEquals(List.of("close", "send"), events);

            // 화면에서 명시적으로 활성화해야 다시 열 수 있다.
            panel.setChatEnabled(true);
            assertTrue(panel.openInput());
        });
    }

    // 화면 구성 요소에서 채팅 기록 영역을 찾는다.
    private static JTextArea findConversation(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTextArea area) {
                return area;
            }

            if (child instanceof Container container) {
                JTextArea found = findConversation(container);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    // 각 테스트의 EDT 안에서 호출한다.
    private void openInputAfterKeyRelease() {
        panel.setChatEnabled(true);
        assertTrue(panel.openInput());
        panel.openingKeyReleased();
    }
}