package menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// 일시정지 화면
public class PauseScreen extends JPanel {

    private static final String[] MENU_ITEMS = {
        "계속하기",
        "시작 화면"
    };

    private final JTextPane menuLabel;
    private final Runnable onResume;
    private final Runnable onQuit;
    private int selectedIndex;

    public PauseScreen(Runnable onResume, Runnable onQuit) {
        this.onResume = onResume;
        this.onQuit = onQuit;

        setBackground(Color.BLACK);
        setLayout(new BorderLayout());
        setFocusable(true);

        menuLabel = new JTextPane();
        menuLabel.setForeground(Color.WHITE);
        menuLabel.setBackground(Color.BLACK);
        menuLabel.setFont(new Font("Courier", Font.BOLD, 20));
        menuLabel.setEditable(false);
        menuLabel.setFocusable(false);
        menuLabel.setOpaque(false);
        menuLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        JPanel menuContainer = new JPanel(new GridBagLayout());
        menuContainer.setBackground(Color.BLACK);
        menuContainer.add(menuLabel);
        add(menuContainer, BorderLayout.CENTER);

        addKeyListener(new MenuKeyListener());
        drawMenu();
    }

    private void drawMenu() {
        StringBuilder text = new StringBuilder("[ 일시정지 ]\n\n");
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            if (i == selectedIndex) {
                text.append("> ").append(MENU_ITEMS[i]).append(" <");
            } else {
                text.append(MENU_ITEMS[i]);
            }
            if (i < MENU_ITEMS.length - 1) {
                text.append("\n\n");
            }
        }
        menuLabel.setText(text.toString());
        StyledDocument document = menuLabel.getStyledDocument();
        SimpleAttributeSet centered = new SimpleAttributeSet();
        StyleConstants.setAlignment(centered, StyleConstants.ALIGN_CENTER);
        document.setParagraphAttributes(0, document.getLength(), centered, false);
    }

    private void selectMenuItem() {
        if (selectedIndex == 0) {
            onResume.run();
        } else {
            onQuit.run();
        }
    }

    private class MenuKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.VK_UP
                    || event.getKeyCode() == KeyEvent.VK_DOWN) {
                selectedIndex = (selectedIndex + 1) % MENU_ITEMS.length;
                drawMenu();
            } else if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                selectMenuItem();
            }
        }
    }
}
