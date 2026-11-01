package ui.components;

import ui.theme.Theme;

import javax.swing.JButton;
import java.awt.*;

public class ThemedButton extends JButton {
    public enum Kind { PRIMARY, SECONDARY }

    private final Kind kind;

    public ThemedButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(kind == Kind.PRIMARY ? Theme.TEXT : Theme.TEXT_SOFT);
        setFont(Theme.body(kind == Kind.PRIMARY ? 18 : 17, kind == Kind.PRIMARY ? Font.BOLD : Font.PLAIN));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), r = Theme.RADIUS * 2;
        boolean hot = getModel().isRollover() || isSelected();

        if (kind == Kind.PRIMARY) {
            int h = getHeight() - 4;
            g2.setColor(Theme.PRIMARY_SHADOW);
            g2.fillRoundRect(0, 4, w, h, r, r);
            g2.setColor(hot ? Theme.PRIMARY.brighter() : Theme.PRIMARY);
            g2.fillRoundRect(0, getModel().isPressed() ? 3 : 0, w, h, r, r);
        } else {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(hot ? Theme.ACCENT : Theme.OUTLINE);
            g2.drawRoundRect(1, 1, w - 3, getHeight() - 3, r, r);
        }
        g2.dispose();
        super.paintComponent(g);   // 글자는 JButton이 가운데 정렬로 그림
    }
}
