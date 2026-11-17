package ui.theme;

import java.awt.Color;
import java.awt.Font;

public final class Theme {
    private Theme() {}

    // 배경 / 카드
    public static final Color BG          = new Color(0x0B1530);
    public static final Color CARD        = new Color(0x12224A);
    public static final Color CARD_BORDER = new Color(0x24396E);
    public static final Color CARD_DARK   = new Color(0x0E1B3D);
    public static final Color BOARD_BG    = new Color(0x08112A);
    public static final Color GRID_LINE   = new Color(0x15254D);

    // 버튼 / 강조
    public static final Color PRIMARY        = new Color(0x3264C8);
    public static final Color PRIMARY_SHADOW = new Color(0x1E3C8C);
    public static final Color OUTLINE        = new Color(0x2E4A86);
    public static final Color ACCENT         = new Color(0x82BEF5);

    // 텍스트
    public static final Color TEXT       = Color.WHITE;
    public static final Color TEXT_SOFT  = new Color(0xDCE8FA);
    public static final Color TEXT_MUTED = new Color(0x9FB4D9);

    // 진행바 트랙, 순위 메달
    public static final Color TRACK  = new Color(0x1E3360);
    public static final Color GOLD   = new Color(0xFFD66B);
    public static final Color SILVER = new Color(0xB8C4D6);
    public static final Color BRONZE = new Color(0xF2A86B);

    public static final int RADIUS = 10;

    // 폰트: Orbitron/Noto Sans KR 번들 제외. 지금은 시스템 폰트로 폴백
    public static Font display(int size) {
        return new Font("Orbitron", Font.BOLD, size).getFamily().equals("Orbitron")
                ? new Font("Orbitron", Font.BOLD, size)
                : new Font(Font.MONOSPACED, Font.BOLD, size);
    }

    public static Font body(int size, int style) {
        return new Font("Malgun Gothic", style, size);   // Windows 기본 한글 폰트
    }
}
