package ui.theme;

import java.awt.Color;
import java.awt.Font;

// 다크 그레이 4단계 + 포인트 색 1개(형광 라임 초록). 게임 보드는 검정
public final class Theme {
    private Theme() {}

    // 회색 단계: 배경 → 카드 → 버튼·테두리 순으로 밝아짐
    public static final Color BG          = new Color(0x1E1E1E);
    public static final Color CARD_DARK   = new Color(0x232323);
    public static final Color CARD        = new Color(0x2A2A2A);
    public static final Color CARD_BORDER = new Color(0x3A3A3A);
    public static final Color SURFACE     = new Color(0x3A3A3A);   // 보조 버튼
    public static final Color SURFACE_HOT = new Color(0x464646);   // 보조 버튼 선택·오버
    public static final Color OUTLINE     = new Color(0x4A4A4A);   // 버튼·키캡·보드 테두리

    // 포인트 색: 주요 버튼, 선택 표시, 슬라이더, 진행 막대, 강조 글자
    public static final Color ACCENT     = new Color(0x95D600);
    public static final Color ACCENT_HOT = new Color(0xACDF38);
    public static final Color ON_ACCENT  = new Color(0x0B0B0B);    // 포인트 색 위 글자

    // 로고 4칸 (회색 3단계 + 포인트)
    public static final Color SHADE_1 = new Color(0x3A3A3A);
    public static final Color SHADE_2 = new Color(0x5A5A5A);
    public static final Color SHADE_3 = new Color(0x8A8A8A);
    public static final Color SHADE_4 = ACCENT;

    // 게임 보드: 검정 배경 + 배경보다 살짝 밝은 격자 (블록이 도드라지도록 격자는 은은하게)
    public static final Color BOARD_BG  = Color.BLACK;
    public static final Color GRID_LINE = new Color(0x1E1E1E);

    // 텍스트
    public static final Color TEXT       = new Color(0xEDEDED);
    public static final Color TEXT_SOFT  = new Color(0xCFCFCF);
    public static final Color TEXT_MUTED = new Color(0xA0A0A0);

    // 진행바·슬라이더 빈 부분, 순위 메달·경고
    public static final Color TRACK  = new Color(0x333333);
    public static final Color GOLD   = new Color(0xFFD66B);
    public static final Color SILVER = new Color(0xC4C4C4);
    public static final Color BRONZE = new Color(0xF2A86B);

    public static final int RADIUS = 0;   // 카드·버튼 모두 직사각형

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
