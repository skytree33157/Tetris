package blocks.core;

import blocks.tetromino.*;
import difficulty.Difficulty;
import item.core.*;
import item.types.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// FR-33·36: 생성 API와 부착 위치만 검증. 아이템 효과 검증 X.
class BlockFactoryItemTest {
    @Test
    void specifiedAndLegacyApisCreateRequestedItems() {
        Class<?>[] expected = {LineClearItem.class, WeightItem.class, BombItem.class, BonusItem.class};
        for (Difficulty difficulty : Difficulty.values()) {
            for (int i = 0; i < ItemType.values().length; i++) {
                ItemType type = ItemType.values()[i];
                assertEquals(expected[i], BlockFactory.createItem(type, difficulty).getClass());
                assertEquals(expected[i], BlockFactory.createItem(type).getClass());
            }
            assertInstanceOf(LineClearItem.class, BlockFactory.createRandomLineClearItem(difficulty));
            assertInstanceOf(BonusItem.class, BlockFactory.createRandomBonusItem(difficulty));
        }
        assertInstanceOf(BombItem.class, BlockFactory.createBombItem());
    }

    @Test
    void randomItemApisReturnRegisteredItems() {
        Set<Class<?>> allowed = Set.of(LineClearItem.class, WeightItem.class, BombItem.class, BonusItem.class);
        for (Difficulty difficulty : Difficulty.values()) {
            for (int i = 0; i < 200; i++) {
                assertTrue(allowed.contains(BlockFactory.createRandomItem(difficulty).getClass()));
                assertTrue(allowed.contains(BlockFactory.createRandomItem().getClass()));
            }
        }
    }

    @Test
    void lineAndBonusHaveExactlyOneMarkerOnOccupiedCells() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int i = 0; i < 300; i++) {
                for (Block block : List.of(BlockFactory.createRandomLineClearItem(difficulty),
                        BlockFactory.createRandomBonusItem(difficulty))) {
                    int occupied = 0, markers = 0;
                    ItemAppearance appearance = ItemRegistry.appearanceOf(block);
                    for (int row = 0; row < block.getHeight(); row++) {
                        for (int col = 0; col < block.getShape()[row].length; col++) {
                            if (block.getShape()[row][col] != 0) occupied++;
                            if (appearance.symbolAt(row, col) != '\0') {
                                markers++;
                                assertNotEquals(0, block.getShape()[row][col]);
                            }
                        }
                    }
                    assertEquals(4, occupied);
                    assertEquals(1, markers);
                    assertTrue(appearance.baseType().getValue() <= 7);
                }
            }
        }
    }

    @Test
    void occupiedCellSelectionNeverPicksHolesAndReachesEveryCell() {
        for (Block block : List.of(new IBlock(), new OBlock(), new TBlock(), new SBlock(),
                new ZBlock(), new JBlock(), new LBlock())) {
            Set<String> selected = new HashSet<>();
            for (int i = 0; i < 1000; i++) {
                int[] cell = BlockFactory.selectRandomOccupiedCell(block);
                assertNotEquals(0, block.getShape()[cell[0]][cell[1]]);
                selected.add(cell[0] + ":" + cell[1]);
            }
            assertEquals(4, selected.size());
        }
    }

    @Test
    void rejectsMissingArgumentsAndEmptyShape() {
        assertThrows(NullPointerException.class, () -> BlockFactory.createItem(null));
        assertThrows(NullPointerException.class, () -> BlockFactory.createItem(ItemType.BOMB, null));
        assertThrows(NullPointerException.class, () -> BlockFactory.createRandomItem(null));
        assertThrows(NullPointerException.class, () -> BlockFactory.selectRandomOccupiedCell(null));
        Block empty = new Block() {{ shape = new int[][]{{0}}; type = BlockType.I; }};
        assertThrows(IllegalArgumentException.class, () -> BlockFactory.selectRandomOccupiedCell(empty));
    }
}
