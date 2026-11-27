package menu.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 방장과 참가자 대기 화면이 함께 사용하는 채팅 패널.
// 화면을 변경하는 메서드는 Swing 화면 처리 스레드(EDT)에서 호출한다.
public class LobbyChatPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JTextArea conversation = new JTextArea();
    private final JTextField input = new JTextField();
    private final ThemedButton sendButton =
            new ThemedButton("전송", Kind.PRIMARY);
    private final JLabel hint = new JLabel();

    private final int maxLength;
    private final Consumer<String> onSend;

    private boolean chatEnabled;
    private boolean sending;

    public LobbyChatPanel(int maxLength, Consumer<String> onSend) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException(
                    "채팅 최대 길이는 0보다 커야 합니다."
            );
        }

        this.maxLength = maxLength;
        this.onSend = Objects.requireNonNull(onSend, "onSend");

        setLayout(new BorderLayout(0, 12));
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));
        setPreferredSize(new Dimension(320, 320));

        JLabel title = new JLabel("대기실 채팅");
        title.setFont(Theme.body(16, Font.BOLD));
        title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(Theme.body(14, Font.PLAIN));
        conversation.setForeground(Theme.TEXT);
        conversation.setBackground(Theme.BOARD_BG);
        conversation.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(conversation);
        scroll.setBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE)
        );
        scroll.getViewport().setBackground(Theme.BOARD_BG);
        add(scroll, BorderLayout.CENTER);

        input.setFont(Theme.body(14, Font.PLAIN));
        input.setForeground(Theme.TEXT);
        input.setCaretColor(Theme.TEXT);
        input.setBackground(Theme.BOARD_BG);
        input.setDisabledTextColor(Theme.TEXT_MUTED);
        input.setSelectionColor(Theme.ACCENT);
        input.setSelectedTextColor(Theme.ON_ACCENT);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        // 입력칸에서 Enter를 누르면 채팅만 전송한다.
        input.addActionListener(e -> submitMessage());

        sendButton.setFont(Theme.body(14, Font.BOLD));
        sendButton.setPreferredSize(new Dimension(72, 36));
        sendButton.setFocusable(false);
        sendButton.addActionListener(e -> submitMessage());

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);

        hint.setFont(Theme.body(12, Font.PLAIN));

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        bottom.add(inputRow, BorderLayout.CENTER);
        bottom.add(hint, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        setChatEnabled(false);
    }

    public void setChatEnabled(boolean enabled) {
        chatEnabled = enabled;
        refreshControls();

        if (!enabled) {
            showHint("연결 후 대기 중에 사용할 수 있어요.", false);
        } else if (!sending) {
            showHint("Enter 또는 전송 버튼을 눌러 주세요.", false);
        }
    }

    private void submitMessage() {
        if (!chatEnabled || sending) {
            return;
        }

        String message = input.getText();

        if (message.isBlank()) {
            showHint("메시지를 입력해 주세요.", true);
            return;
        }
        if (message.length() > maxLength) {
            showHint(maxLength + "자 이하로 입력해 주세요.", true);
            return;
        }

        sending = true;
        refreshControls();
        showHint("전송 중...", false);

        onSend.accept(message);
    }

    public void sendSucceeded(String message) {
        sending = false;
        appendMessage("나", message);
        input.setText("");

        setChatEnabled(chatEnabled);
        focusInput();
    }

    public void sendFailed(String reason) {
        sending = false;
        refreshControls();

        String text = reason == null || reason.isBlank()
                ? "전송하지 못했어요. 다시 시도해 주세요."
                : reason;

        showHint(text, true);
        focusInput();
    }

    public void appendRemoteMessage(String message) {
        appendMessage("상대", message);
    }

    private void appendMessage(String sender, String message) {
        conversation.append("[" + sender + "] " + message + "\n");
        conversation.setCaretPosition(
                conversation.getDocument().getLength()
        );
    }

    public void resetChat() {
        conversation.setText("");
        input.setText("");
        sending = false;
        setChatEnabled(false);
    }

    public void focusInput() {
        if (chatEnabled && !sending) {
            input.requestFocusInWindow();
        }
    }

    private void refreshControls() {
        boolean available = chatEnabled && !sending;
        input.setEnabled(available);
        sendButton.setEnabled(available);
    }

    private void showHint(String text, boolean warning) {
        hint.setText(text);
        hint.setForeground(warning ? Theme.BRONZE : Theme.TEXT_MUTED);
    }
}