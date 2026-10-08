package item;

import blocks.core.BlockType;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * 표시 정보만 보관한다. 실제 효과는 개별 아이템 클래스가 담당한다.
 * baseType: 배경 색상·패턴의 기준 타입, symbol: 표시 문자.
 * markerCell: 현재 모양에서 문자를 표시할 칸인지 판별하는 함수.
 */
public record ItemAppearance(BlockType baseType, char symbol,
                             BiPredicate<Integer, Integer> markerCell) {
    public ItemAppearance {
        Objects.requireNonNull(baseType, "baseType");
        Objects.requireNonNull(markerCell, "markerCell");
    }

    // 표시 칸이 아니면 문자 없음(널 문자)을 반환한다.
    public char symbolAt(int row, int col) {
        return markerCell.test(row, col) ? symbol : '\0';
    }
}