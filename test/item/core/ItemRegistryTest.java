package item.core;
import blocks.core.*;
import blocks.tetromino.OBlock;
import difficulty.Difficulty;
import item.types.*;
import java.util.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Isolated;
import static org.junit.jupiter.api.Assertions.*;

// FR-33·36: 생성·표시 등록 검증. 전역 등록 목록은 테스트 후 원상 복구.
@Isolated
class ItemRegistryTest {
    private Map<Object, Object> registry;
    private Map<Object, Object> original;
    @SuppressWarnings("unchecked")
    @BeforeEach
    void saveRegistrations() throws Exception {
        Field field = ItemRegistry.class.getDeclaredField("ITEMS");
        field.setAccessible(true);
        registry = (Map<Object, Object>) field.get(null);
        original = new LinkedHashMap<>(registry);
    }
    @AfterEach
    void restoreRegistrations() {
        registry.clear(); registry.putAll(original);
    }

    @Test
    void builtInSymbolsAreDistinctAndSourceStyleIsPreserved() {
        char[] symbols = {'L', 'W', 'B', 'P'};
        for (int i = 0; i < ItemType.values().length; i++) {
            ItemType type = ItemType.values()[i];
            assertFalse(type.getId().isBlank());
            Block block = ItemRegistry.create(type, Difficulty.HARD);
            ItemAppearance appearance = ItemRegistry.appearanceOf(block);
            assertNotNull(appearance);
            boolean found = false;
            for (int r = 0; r < block.getHeight(); r++) for (int c = 0; c < block.getWidth(); c++) {
                if (appearance.symbolAt(r, c) == symbols[i]) found = true;
            }
            assertTrue(found);
            if (block instanceof BonusItem bonus) assertSame(bonus.getSourceType(), appearance.baseType());
            if (block instanceof LineClearItem line) assertSame(line.getSourceType(), appearance.baseType());
        }
        assertNull(ItemRegistry.appearanceOf(new OBlock()));
    }

    @Test
    void seededRandomCreationUsesAllFourRegisteredKindsEqually() {
        Random random = new Random(20261008L);
        Map<Class<?>, Integer> counts = new HashMap<>();
        for (int i = 0; i < 20000; i++) {
            Block block = ItemRegistry.createRandom(Difficulty.NORMAL, random);
            counts.merge(block.getClass(), 1, Integer::sum);
        }
        assertEquals(4, counts.size());
        for (int count : counts.values()) assertEquals(5000, count, 250);
    }

    @Test
    void newRegistrationPassesDifficultyAndImmediatelyJoinsRandomCandidates() {
        ItemRegistry.register("test-custom", CustomItem.class, difficulty -> {
            assertEquals(Difficulty.HARD, difficulty); return new CustomItem();
        }, item -> new ItemAppearance(BlockType.O, 'Q', (r,c) -> r == 0 && c == 0));
        Block custom = ItemRegistry.createRandom(Difficulty.HARD, new Random() {
            @Override public int nextInt(int bound) { return bound - 1; }
        });
        assertInstanceOf(CustomItem.class, custom);
        assertEquals('Q', ItemRegistry.appearanceOf(custom).symbolAt(0,0));
        assertEquals('\0', ItemRegistry.appearanceOf(custom).symbolAt(0,1));
    }

    @Test
    void rejectsDuplicateAndInvalidRegistrationsWithoutChangingCandidates() {
        assertThrows(IllegalArgumentException.class, () -> ItemRegistry.register("bomb", CustomItem.class,
                d -> new CustomItem(), b -> new ItemAppearance(BlockType.O, 'Q', (r,c) -> true)));
        assertThrows(IllegalArgumentException.class, () -> ItemRegistry.register("test-duplicate-class", BombItem.class,
                d -> new BombItem(), b -> new ItemAppearance(BlockType.BOMB, 'B', (r,c) -> true)));
        assertThrows(IllegalArgumentException.class, () -> ItemRegistry.register(" ", CustomItem.class,
                d -> new CustomItem(), b -> new ItemAppearance(BlockType.O, 'Q', (r,c) -> true)));
        assertEquals(original, registry);
        assertThrows(NullPointerException.class, () -> ItemRegistry.create(null, Difficulty.NORMAL));
        assertThrows(NullPointerException.class, () -> ItemRegistry.create(ItemType.BOMB, null));
        assertThrows(NullPointerException.class, () -> ItemRegistry.createRandom(null, new Random()));
        assertThrows(NullPointerException.class, () -> ItemRegistry.createRandom(Difficulty.NORMAL, null));
        assertThrows(NullPointerException.class, () -> new ItemAppearance(null, 'Q', (r,c) -> true));
        assertThrows(NullPointerException.class, () -> new ItemAppearance(BlockType.O, 'Q', null));
    }

    private static class CustomItem extends Block {
        CustomItem() { shape = new int[][]{{1}}; type = BlockType.O; }
    }
}
