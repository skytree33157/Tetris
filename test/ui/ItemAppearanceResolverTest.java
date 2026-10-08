package ui;
import blocks.core.*;
import blocks.tetromino.*;
import difficulty.Difficulty;
import item.core.*;
import item.types.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// FR-33·36: 보드 저장 변환 API만 검증. 실제 보드 고정 연결 검증 X.
class ItemAppearanceResolverTest {
    @Test
    void occupiedCellsRoundTripWithoutLosingTypeOrSymbolAfterRotation() {
        for (ItemType type : ItemType.values()) {
            Block block = BlockFactory.createItem(type, Difficulty.NORMAL);
            for (int i = 0; i < 4; i++) {
                for (int r = 0; r < block.getHeight(); r++) for (int c = 0; c < block.getWidth(); c++) {
                    int value = ItemAppearanceResolver.toBoardCell(block, r, c);
                    if (block.getShape()[r][c] == 0) { assertEquals(0, value); continue; }
                    assertEquals(ItemAppearanceResolver.resolve(block,r,c), ItemAppearanceResolver.fromBoardCell(value));
                }
                block.rotate();
            }
        }
    }
    @Test
    void normalCellsKeepExistingValuesAndAttachedMarkersKeepSourceStyle() {
        for (BlockType type : BlockType.values()) {
            var cell = ItemAppearanceResolver.fromBoardCell(type.getValue());
            assertSame(type, cell.type());
            assertEquals(type == BlockType.WEIGHT ? 'W' : type == BlockType.BOMB ? 'B' : '\0', cell.symbol());
        }
        BonusItem bonus = new BonusItem(new TBlock(),0,1);
        assertEquals(0, ItemAppearanceResolver.toBoardCell(bonus,0,0));
        assertEquals(BlockType.T.getValue(), ItemAppearanceResolver.toBoardCell(bonus,1,0));
        var marker = ItemAppearanceResolver.fromBoardCell(ItemAppearanceResolver.toBoardCell(bonus,0,1));
        assertEquals(BlockType.T, marker.type()); assertEquals('P', marker.symbol());
        assertEquals(BlockType.O.getValue(), ItemAppearanceResolver.toBoardCell(new OBlock(),0,0));
    }
    @Test
    void legacyMarkerIsReadableAndInvalidPackedValuesAreRejected() {
        assertEquals('L', ItemAppearanceResolver.fromBoardCell(-1).symbol());
        assertEquals(BlockType.LINE_CLEAR, ItemAppearanceResolver.fromBoardCell(-1).type());
        for (int value : new int[]{-2, Integer.MIN_VALUE, 999, -0x1000000})
            assertThrows(IllegalArgumentException.class, () -> ItemAppearanceResolver.fromBoardCell(value));
        assertThrows(NullPointerException.class, () -> ItemAppearanceResolver.resolve(null,0,0));
    }
}
