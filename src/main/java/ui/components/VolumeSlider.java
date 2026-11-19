package ui.components;

import ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

/** 0~100 볼륨을 고르는 슬라이더. 값은 step 단위로 맞춰진다 */
public class VolumeSlider extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int INSET = 14;
    private static final int TRACK_HEIGHT = 6;
    private static final int KNOB = 14;

    private final int step;
    private final IntConsumer onChange;
    private int value;
    private boolean highlighted;

    public VolumeSlider(int step, IntConsumer onChange) {
        this.step = step;
        this.onChange = onChange;
        setOpaque(false);
        setFocusable(false);   // 키 입력은 창이 받도록
        setPreferredSize(new Dimension(200, 32));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { pick(e.getX()); }

            @Override
            public void mouseDragged(MouseEvent e) { pick(e.getX()); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private void pick(int x) {
        if (!isEnabled()) return;
        int width = Math.max(1, getWidth() - INSET * 2);
        double ratio = (x - INSET) / (double) width;
        int picked = (int) Math.round(ratio * 100 / step) * step;
        picked = Math.max(0, Math.min(100, picked));
        if (picked != value) {
            onChange.accept(picked);
        }
    }

    public void setValue(int value) {
        this.value = Math.max(0, Math.min(100, value));
        repaint();
    }

    // 키보드로 이 항목을 고른 상태 표시
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        repaint();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int r = 0;   // 직사각형

        g2.setColor(Theme.BG);
        g2.fillRoundRect(0, 0, w - 1, h - 1, r, r);
        if (highlighted) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Theme.ACCENT);
            g2.drawRoundRect(1, 1, w - 3, h - 3, r, r);
        }

        int trackWidth = w - INSET * 2;
        int trackY = (h - TRACK_HEIGHT) / 2;
        g2.setColor(Theme.TRACK);
        g2.fillRect(INSET, trackY, trackWidth, TRACK_HEIGHT);

        int filled = trackWidth * value / 100;
        g2.setColor(isEnabled() ? Theme.ACCENT : Theme.OUTLINE);
        g2.fillRect(INSET, trackY, filled, TRACK_HEIGHT);

        g2.setColor(isEnabled() ? Theme.TEXT : Theme.TEXT_MUTED);
        g2.fillOval(INSET + filled - KNOB / 2, (h - KNOB) / 2, KNOB, KNOB);
        g2.dispose();
    }
}
