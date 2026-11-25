package menu;

import game.BattleMode;
import game.BattleSettings;
import menu.screens.GameHud;

/**
 * 대전 방식 선택지 (로컬 대전·온라인 방 만들기에서 같이 쓴다).
 * 일반 / 아이템 / 시간 제한 순서. 시간 제한은 일반 규칙으로 하고 끝나면 점수로 승패를 정한다.
 */
public final class BattleModes {

    /** 시간 제한 대전의 제한 시간 (3분) */
    public static final int TIME_LIMIT_SECONDS = 180;

    public static final int NORMAL_INDEX = 0;
    public static final int ITEM_INDEX = 1;
    public static final int TIME_LIMIT_INDEX = 2;

    private static final String[] LABELS = {"일반", "아이템", "시간 제한"};

    private BattleModes() {
    }

    public static String[] labels() {
        return LABELS.clone();
    }

    public static int count() {
        return LABELS.length;
    }

    public static BattleSettings settings(int index) {
        return switch (index) {
            case ITEM_INDEX -> new BattleSettings(BattleMode.ITEM, 0);
            case TIME_LIMIT_INDEX -> new BattleSettings(BattleMode.TIME_LIMIT, TIME_LIMIT_SECONDS);
            default -> new BattleSettings(BattleMode.NORMAL, 0);
        };
    }

    /** 선택지 아래에 보여줄 한 줄 규칙 설명 */
    public static String description(int index) {
        return switch (index) {
            case ITEM_INDEX -> "아이템이 나오는 규칙. 블록이 먼저 천장에 닿으면 패배";
            case TIME_LIMIT_INDEX -> GameHud.formatTime(TIME_LIMIT_SECONDS * 1000L)
                    + " 동안 겨뤄 점수가 높은 쪽이 승리 (먼저 천장에 닿으면 패배)";
            default -> "블록이 먼저 천장에 닿으면 패배";
        };
    }

    /** 결과 화면 등에 쓰는 이름 */
    public static String modeLabel(BattleSettings settings) {
        return switch (settings.getMode()) {
            case ITEM -> "아이템 모드";
            case TIME_LIMIT -> "시간 제한 모드";
            default -> "일반 모드";
        };
    }
}
