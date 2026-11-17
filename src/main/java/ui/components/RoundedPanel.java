package ui.components;

import ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.*;

// 카드 패널
public class RoundedPanel extends JPanel {
    private final Color fill;
    private final Color border;
    private final int radius;

    public RoundedPanel() { this(Theme.CARD, Theme.CARD_BORDER, 12); }

    public RoundedPanel(Color fill, Color border, int radius) {
        this.fill = fill;
        this.border = border;
        this.radius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2);
        g2.dispose();
        super.paintComponent(g);
    }
}
