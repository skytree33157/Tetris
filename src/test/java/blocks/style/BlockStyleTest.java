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
}
