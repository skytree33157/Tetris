package menu.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import ui.components.ThemedButton;
import ui.components.ThemedButton.Kind;
import ui.theme.Theme;

// 게임 중 채팅 표시와 입력 UI.
// 화면을 변경하는 메서드는 EDT에서 호출한다.
public class GameChatPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final int MAX_MESSAGES = 100;

    private final JTextArea conversation = new JTextArea();
    private final JTextField input = new JTextField();
    private final JLabel hint = new JLabel();
    private final JPanel inputRow =
            new JPanel(new BorderLayout(6, 0));
    private final ThemedButton sendButton =
            new ThemedButton("전송", Kind.PRIMARY);

    private final Deque<String> messages = new ArrayDeque<>();

    private final int maxLength;
    private final int openKeyCode;
    private final String openKeyText;
    private final Consumer<String> onSend;
    private final Runnable onClose;

    private boolean chatEnabled;
    private boolean editing;
    private boolean sending;
    private boolean ignoreOpeningEnter;

    public GameChatPanel(
            int maxLength,
            int openKeyCode,
            Consumer<String> onSend,
            Runnable onClose) {

        if (maxLength <= 0 || openKeyCode <= 0) {
            throw new IllegalArgumentException(
                    "채팅 길이와 열기 키는 올바른 값이어야 합니다."
            );
        }

        this.maxLength = maxLength;
        this.openKeyCode = openKeyCode;
        this.openKeyText = KeyEvent.getKeyText(openKeyCode);
        this.onSend = Objects.requireNonNull(onSend, "onSend");
        this.onClose = Objects.requireNonNull(onClose, "onClose");

        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.CARD);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.CARD_BORDER),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setPreferredSize(new Dimension(210, 150));

        JLabel title = new JLabel("게임 채팅");
        title.setFont(Theme.body(13, Font.BOLD));
        title.setForeground(Theme.TEXT);
        add(title, BorderLayout.NORTH);

        conversation.setEditable(false);
        conversation.setFocusable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(Theme.body(12, Font.PLAIN));
        conversation.setForeground(Theme.TEXT);
        conversation.setBackground(Theme.BOARD_BG);
        conversation.setMargin(new Insets(4, 6, 4, 6));
        conversation.getAccessibleContext()
                .setAccessibleName("게임 채팅 기록");

        JScrollPane scroll = new JScrollPane(conversation);
        scroll.setBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE)
        );
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.getVerticalScrollBar().setFocusable(false);
        scroll.getViewport().setBackground(Theme.BOARD_BG);
        add(scroll, BorderLayout.CENTER);

        input.setFont(Theme.body(13, Font.PLAIN));
        input.setForeground(Theme.TEXT);
        input.setCaretColor(Theme.TEXT);
        input.setBackground(Theme.BOARD_BG);
        input.setDisabledTextColor(Theme.TEXT_MUTED);
        input.setSelectionColor(Theme.ACCENT);
        input.setSelectedTextColor(Theme.ON_ACCENT);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.OUTLINE),
                BorderFactory.createEmptyBorder(5, 6, 5, 6)
        ));
        input.getAccessibleContext()
                .setAccessibleName("게임 채팅 입력");

        input.addActionListener(e -> submitMessage());

        input.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "close-game-chat"
        );
        input.getActionMap().put(
                "close-game-chat",
                new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        closeInput();
                    }
                }
        );

        sendButton.setFont(Theme.body(12, Font.BOLD));
        sendButton.setPreferredSize(new Dimension(48, 30));
        sendButton.setFocusable(false);
        sendButton.addActionListener(e -> submitMessage());

        inputRow.setOpaque(false);
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(sendButton, BorderLayout.EAST);
        inputRow.setVisible(false);

        hint.setFont(Theme.body(11, Font.PLAIN));

        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        bottom.add(inputRow, BorderLayout.CENTER);
        bottom.add(hint, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        setChatEnabled(false);
    }

    public void setChatEnabled(boolean enabled) {
        chatEnabled = enabled;

        if (!enabled) {
            closeInput();
        }

        refreshControls();

        if (!editing) {
            showIdleHint();
        }
    }

    /**
     * 입력칸을 연다. 성공하면 true를 반환한다.
     * 호출자가 게임 입력을 차단한 다음 입력칸에 포커스를 준다.
     */
    public boolean openInput() {
        if (!chatEnabled || editing || sending) {
            return false;
        }

        editing = true;
        ignoreOpeningEnter = openKeyCode == KeyEvent.VK_ENTER;
        inputRow.setVisible(true);
        refreshControls();
        showHint("Enter 전송 · Esc 취소", false);

        revalidate();
        repaint();
        return true;
    }

    // 열기 키를 떼면 호출한다.
    // 최초 Enter의 반복 입력이 전송으로 처리되는 것을 막는다.
    public void openingKeyReleased() {
        ignoreOpeningEnter = false;
    }

    public void closeInput() {
        if (!editing) {
            return;
        }

        editing = false;
        ignoreOpeningEnter = false;
        inputRow.setVisible(false);
        refreshControls();
        showIdleHint();

        revalidate();
        repaint();

        onClose.run();
    }

    private void submitMessage() {
        if (!chatEnabled || !editing || sending || ignoreOpeningEnter) {
            return;
        }

        String message = input.getText();

        // 빈 입력에서 Enter를 누르면 전송 없이 닫는다.
        if (message.isBlank()) {
            input.setText("");
            closeInput();
            return;
        }

        if (message.length() > maxLength) {
            showHint("최대 " + maxLength + "자까지 입력해 주세요.", true);
            return;
        }

        sending = true;

        // 전송 완료를 기다리는 동안에도 게임 조작으로 복귀한다.
        closeInput();
        onSend.accept(message);
    }

    public void sendSucceeded(String message) {
        sending = false;
        input.setText("");
        appendMessage("나", message);
        refreshControls();
        showIdleHint();
    }

    public void sendFailed(String reason) {
        sending = false;
        refreshControls();

        showHint(
                chatEnabled
                        ? "전송 실패 · " + openKeyText + "로 재입력"
                        : "연결 상태를 확인해 주세요.",
                true
        );
        hint.setToolTipText(reason);
    }

    public void appendRemoteMessage(String message) {
        appendMessage("상대", message);
    }

    private void appendMessage(String sender, String message) {
        messages.addLast("[" + sender + "] " + message);

        while (messages.size() > MAX_MESSAGES) {
            messages.removeFirst();
        }

        conversation.setText(String.join("\n", messages) + "\n");
        conversation.setCaretPosition(
                conversation.getDocument().getLength()
        );
    }

    public boolean isInputOpen() {
        return editing;
    }

    public int getOpenKeyCode() {
        return openKeyCode;
    }

    // 게임 화면에서 키 누름·뗌 상태 처리를 연결할 때 사용한다.
    public JTextField getInputField() {
        return input;
    }

    private void refreshControls() {
        boolean available = chatEnabled && editing && !sending;
        input.setEnabled(available);
        sendButton.setEnabled(available);
    }

    private void showIdleHint() {
        if (!chatEnabled) {
            showHint("채팅을 사용할 수 없어요.", false);
        } else if (sending) {
            showHint("전송 중...", false);
        } else {
            showHint(openKeyText + " 채팅 입력", false);
        }
    }

    private void showHint(String text, boolean warning) {
        hint.setText(text);
        hint.setForeground(warning ? Theme.BRONZE : Theme.TEXT_MUTED);
        hint.setToolTipText(null);
    }
}