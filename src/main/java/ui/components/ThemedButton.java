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
        setForeground(kind == Kind.PRIMARY ? Theme.ON_ACCENT : Theme.TEXT);
        setFont(Theme.body(kind == Kind.PRIMARY ? 18 : 17, kind == Kind.PRIMARY ? Font.BOLD : Font.PLAIN));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), r = 0;   // 직사각형 버튼
        boolean hot = isEnabled() && (getModel().isRollover() || isSelected());

        // 비활성: 점선 느낌의 흐린 테두리 + 흐린 글자 (Look and Feel의 음각 글자 대신 직접 그림)
        if (!isEnabled()) {
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[] {6f, 4f}, 0f));
            g2.setColor(Theme.OUTLINE);
            g2.drawRoundRect(1, 1, w - 3, getHeight() - 3, r, r);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(Theme.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            String label = getText();
            g2.drawString(label, (w - fm.stringWidth(label)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
            return;
        }

        int h = getHeight();
        if (kind == Kind.PRIMARY) {
            // 포인트 색 평면 버튼. 선택·오버 시 밝게 + 흰 테두리
            g2.setColor(hot ? Theme.ACCENT_HOT : Theme.ACCENT);
            g2.fillRect(0, 0, w, h);
            if (hot) {
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(Color.WHITE);
                g2.drawRect(1, 1, w - 3, h - 3);
            }
        } else {
            // 회색 평면 버튼. 선택·오버 시 한 단계 밝게 + 포인트 색 테두리
            g2.setColor(hot ? Theme.SURFACE_HOT : Theme.SURFACE);
            g2.fillRect(0, 0, w, h);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(hot ? Theme.ACCENT : Theme.OUTLINE);
            g2.drawRect(1, 1, w - 3, h - 3);
        }
        g2.dispose();
        super.paintComponent(g);   // 글자는 JButton이 가운데 정렬로 그림
    }
}
