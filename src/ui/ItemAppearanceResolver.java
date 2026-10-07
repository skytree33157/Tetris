package ui;

import blocks.core.Block;
import blocks.core.BlockType;
import item.ItemAppearance;
import item.ItemRegistry;
import java.util.Objects;

/** 기존 아이템의 효과 코드를 수정하지 않고 객체를 렌더링 정보로 변환한다. */
public final class ItemAppearanceResolver {
    public record CellAppearance(BlockType type, char symbol) {}

    private ItemAppearanceResolver() {}

    // 일반 블록은 기존 타입을 사용하고, 아이템은 등록된 배경 스타일과 칸별 문자를 사용한다.
    public static CellAppearance resolve(Block block, int row, int col) {
        Objects.requireNonNull(block, "block");
        ItemAppearance appearance = ItemRegistry.appearanceOf(block);
        return appearance == null ? new CellAppearance(block.getType(), '\0')
                : new CellAppearance(appearance.baseType(), appearance.symbolAt(row, col));
    }

    /**
     * 향후 아이템 고정 시 원본 스타일과 문자를 보드의 정수 값 하나에 보존한다.
     * 일반 셀 값은 유지하고, 문자가 있는 셀은 구분 비트·문자·타입을 음수로 저장한다.
     * 모양 배열의 표시 값과는 별개이며, 고정 로직에서 호출해야 적용된다.
     */
    public static int toBoardCell(Block block, int row, int col) {
        if (block.getShape()[row][col] == 0) return 0;
        CellAppearance cell = resolve(block, row, col);
        return cell.symbol() == '\0' ? cell.type().getValue()
                : -(0x1000000 | (cell.symbol() << 8) | cell.type().getValue());
    }

    // 보드에 저장된 일반 타입 또는 표시 정보를 읽어 배경 타입과 문자를 복원한다.
    public static CellAppearance fromBoardCell(int value) {
        // PR33의 기존 -1 값에는 원본 타입이 없다. L 효과로 해당 행이 삭제될 때까지 호환 표시한다.
        if (value == -1) return new CellAppearance(BlockType.LINE_CLEAR, 'L');
        if (value >= 0) {
            BlockType type = BlockType.fromValue(value);
            return new CellAppearance(type, type == BlockType.WEIGHT ? 'W' : '\0');
        }
        long packed = -(long) value;
        if ((packed & 0xFF000000L) != 0x1000000L) {
            throw new IllegalArgumentException("Invalid item board cell: " + value);
        }
        return new CellAppearance(BlockType.fromValue((int) (packed & 0xFF)),
                (char) ((packed >>> 8) & 0xFFFF));
    }
}