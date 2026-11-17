package ui.components;

import ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.*;
import java.util.function.IntConsumer;

/** 여러 값 중 하나를 고르는 세그먼트 버튼 묶음 */
public class SegmentedControl extends JPanel {
    private static final long serialVersionUID = 1L;

    private final SegmentButton[] segments;
    private boolean highlighted;

    public SegmentedControl(String[] labels, IntConsumer onPick) {
        setOpaque(false);
        setLayout(new GridLayout(1, labels.length, 4, 0));
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        segments = new SegmentButton[labels.length];
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            segments[i] = new SegmentButton(labels[i]);
            segments[i].addActionListener(e -> onPick.accept(index));
            add(segments[i]);
        }
    }

    public void setSelectedIndex(int index) {
        for (int i = 0; i < segments.length; i++) {
            segments[i].setSelected(i == index);
            segments[i].setForeground(i == index ? Theme.TEXT : Theme.TEXT_MUTED);
        }
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
        for (SegmentButton segment : segments) {
            segment.setEnabled(enabled);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int r = Theme.RADIUS * 2;
        g2.setColor(Theme.BG);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
        if (highlighted) {
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Theme.ACCENT);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, r, r);
        }
        g2.dispose();
    }

    private static class SegmentButton extends JButton {
        private static final long serialVersionUID = 1L;

        SegmentButton(String text) {
            super(text);
            setFocusable(false);   // 키 입력은 창이 받도록
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setFont(Theme.body(13, Font.BOLD));
            setPreferredSize(new Dimension(60, 32));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
