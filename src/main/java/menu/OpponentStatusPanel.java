package menu;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import network.GameStatus;

// 사이드 패널 하단에 상대 참가자의 점수/레벨/라인을 표시한다. EDT에서만 사용한다.
final class OpponentStatusPanel extends JPanel {
    private final Map<Integer, GameStatus> statuses = new TreeMap<>();

    OpponentStatusPanel() {
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    void update(int slot, GameStatus status) {
        statuses.put(slot, status);
        rebuild();
    }

    // 나간 참가자를 제거한다
    void retainOnly(List<Integer> slots) {
        if (statuses.keySet().retainAll(slots)) {
            rebuild();
        }
    }

    private void rebuild() {
        removeAll();
        for (Map.Entry<Integer, GameStatus> entry : statuses.entrySet()) {
            GameStatus status = entry.getValue();
            Color color = status.isGameOver() ? Color.GRAY : Color.WHITE;
            for (String text : formatLines(entry.getKey(), status)) {
                JLabel label = new JLabel(text);
                label.setFont(new Font("Courier", Font.PLAIN, 12));
                label.setForeground(color);
                label.setAlignmentX(Component.LEFT_ALIGNMENT);
                add(label);
            }
            add(Box.createVerticalStrut(6));
        }
        revalidate();
        repaint();
    }

    // 사이드 패널 폭(170px)에 맞추려고 상대당 두 줄로 나눈다
    static List<String> formatLines(int slot, GameStatus status) {
        List<String> lines = new ArrayList<>();
        String header = "P" + (slot + 1) + (status.isGameOver() ? " (탈락)" : "");
        lines.add(header);
        lines.add("점수 " + status.getScore() + " / Lv " + status.getLevel() + " / " + status.getLines() + "줄");
        return lines;
    }
}
