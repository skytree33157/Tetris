package menu.views;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import network.RoomMember;
import network.RoomProtocol;
import ui.components.RoundedPanel;
import ui.components.ThemedButton;
import ui.theme.Theme;

// P2P 방 만들기 / 참여 화면이 같이 쓰는 레이아웃 조각
final class LobbyParts {
    static final int COLUMN_WIDTH = 560;
    // 참가자 목록 영역 높이. 인원이 많아도 아래 버튼이 밀리지 않도록 고정하고 스크롤한다
    static final int ROSTER_HEIGHT = 120;

    private LobbyParts() {}

    static JLabel label(String text, Font font, java.awt.Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    // 세로로 쌓는 카드. 너비는 화면 열 너비에 맞춘다
    static RoundedPanel card() {
        RoundedPanel card = new RoundedPanel(Theme.CARD, Theme.CARD_BORDER, Theme.RADIUS);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 18, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    // 카드 높이를 내용에 맞게 고정 (BoxLayout이 남는 높이를 나눠 주지 않도록)
    static <T extends JComponent> T fixHeight(T c) {
        Dimension pref = c.getPreferredSize();
        c.setMaximumSize(new Dimension(COLUMN_WIDTH, pref.height));
        return c;
    }

    static JPanel column() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setPreferredSize(null);
        return col;
    }

    static JPanel buttonRow(ThemedButton primary, ThemedButton secondary) {
        JPanel row = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (ThemedButton b : new ThemedButton[] {primary, secondary}) {
            b.setFocusable(false);
            b.setPreferredSize(new Dimension(0, 52));
            row.add(b);
        }
        row.setMaximumSize(new Dimension(COLUMN_WIDTH, 52));
        return row;
    }

    // 상태 문구: 일반 안내는 흐린 글자, 경고는 BRONZE
    static void setStatus(JLabel status, String text, boolean warning) {
        status.setText(text == null || text.isEmpty() ? " " : text);
        status.setForeground(warning ? Theme.BRONZE : Theme.TEXT_SOFT);
        status.setFont(Theme.body(14, warning ? Font.BOLD : Font.PLAIN));
    }

    // 참가자 목록을 고정 높이 스크롤 영역에 담는다
    static JScrollPane rosterScroll(JPanel list) {
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension size = new Dimension(COLUMN_WIDTH - 40, ROSTER_HEIGHT);
        scroll.setPreferredSize(size);
        scroll.setMaximumSize(size);
        return scroll;
    }

    // 참가자 목록을 다시 채운다. mySlot 위치에는 "(나)", 방장에는 "[방장]"을 붙인다.
    static void fillRoster(JPanel list, List<RoomMember> members, int mySlot) {
        list.removeAll();
        list.add(label(members.size() + " / " + RoomProtocol.MAX_PARTICIPANTS + "명",
                Theme.body(13, Font.PLAIN), Theme.TEXT_MUTED));
        for (int i = 0; i < members.size(); i++) {
            RoomMember member = members.get(i);
            StringBuilder text = new StringBuilder((i + 1) + ". " + member.name());
            if (i == mySlot) {
                text.append(" (나)");
            }
            if (member.host()) {
                text.append(" [방장]");
            }
            text.append(member.ready() ? "  준비 완료" : "  대기 중");
            list.add(label(text.toString(), Theme.body(14, Font.PLAIN), Theme.TEXT));
        }
        list.revalidate();
        list.repaint();
    }
}
