package blocks.style;
import blocks.core.BlockType;
import java.awt.Color;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// FR-06·16·33: 모드별 스타일 구분. 실제 색각 이상 사용자 접근성 평가는 별도.
class BlockStyleTest {
    @Test
    void sevenBlocksHaveDistinctColorsAndPatternsInEveryMode() {
        for (ColorMode mode : ColorMode.values()) {
            Set<Color> colors = new HashSet<>();
            Set<BlockPattern> patterns = new HashSet<>();
            for (BlockType type : BlockType.values()) {
                BlockStyle style = BlockStyle.of(type, mode);
                assertNotNull(style.getColor());
                assertNotNull(style.getPattern());
                if (type.getValue() <= 7) {
                    colors.add(style.getColor()); patterns.add(style.getPattern());
                    assertEquals(BlockStyle.of(type, ColorMode.NORMAL).getPattern(), style.getPattern());
                }
            }
            assertEquals(7, colors.size()); assertEquals(7, patterns.size());
        }
    }

    @Test // TFSE 테마는 디자인 시안의 블루 팔레트 사용
    void tfseModeUsesDesignPalette() {
        assertEquals(new Color(0x82BEF5), BlockStyle.of(BlockType.I, ColorMode.TFSE).getColor());
        assertEquals(new Color(0x5096E6), BlockStyle.of(BlockType.O, ColorMode.TFSE).getColor());
        assertEquals(new Color(0x3264C8), BlockStyle.of(BlockType.T, ColorMode.TFSE).getColor());
        assertEquals(new Color(0x6FA8F0), BlockStyle.of(BlockType.S, ColorMode.TFSE).getColor());
        assertEquals(new Color(0x2A4FA8), BlockStyle.of(BlockType.Z, ColorMode.TFSE).getColor());
        assertEquals(new Color(0x1E3C8C), BlockStyle.of(BlockType.J, ColorMode.TFSE).getColor());
        assertEquals(new Color(0xA9D4FA), BlockStyle.of(BlockType.L, ColorMode.TFSE).getColor());
    }

    @Test
    void specialItemsKeepDedicatedStylesAcrossModes() {
        for (ColorMode mode : ColorMode.values()) {
            BlockStyle weight = BlockStyle.of(BlockType.WEIGHT, mode);
            assertEquals(new Color(0xC8CDD3), weight.getColor());
            assertEquals(BlockPattern.BRICK, weight.getPattern());
            assertNotEquals(BlockStyle.of(BlockType.J, mode).getPattern(), weight.getPattern());
            assertEquals(new Color(0x454B54), BlockStyle.of(BlockType.BOMB, mode).getColor());
        }
    }

    @Test
    void attackCellsAreGreyAndHaveNoItemSymbolInEveryMode() {
        for (ColorMode mode:ColorMode.values()) {
            BlockStyle style=BlockStyle.of(BlockType.ATTACK,mode);
            assertEquals(new Color(0x808080),style.getColor());
            assertEquals(BlockPattern.GRID,style.getPattern());
        }
        assertEquals('\0',ui.ItemAppearanceResolver.fromBoardCell(BlockType.ATTACK.getValue()).symbol());
    }
}
