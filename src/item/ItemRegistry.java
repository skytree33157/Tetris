package item;

import blocks.core.Block;
import blocks.core.BlockFactory;
import blocks.core.BlockType;
import difficulty.Difficulty;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.random.RandomGenerator;

/** 게임 시작 전에 구현된 아이템의 생성 함수와 표시 정보를 한 번 등록한다. */
public final class ItemRegistry {
    private record Definition(String id, Class<? extends Block> blockClass,
                              Function<Difficulty, ? extends Block> creator,
                              Function<Block, ItemAppearance> appearance) {}

    private static final Map<String, Definition> ITEMS = new LinkedHashMap<>();

    static {
        register("line-clear", LineClearItem.class, BlockFactory::createRandomLineClearItem,
                item -> new ItemAppearance(item.getSourceType(), 'L', item::isMarkerCell));
        // 일부 칸이 삭제되어도 구분할 수 있도록 Weight의 채워진 칸마다 W를 표시한다.
        register("weight", WeightItem.class, difficulty -> new WeightItem(),
                item -> new ItemAppearance(BlockType.WEIGHT, 'W', (row, col) -> true));
    }

    private ItemRegistry() {}

    /** 같은 id나 클래스를 중복 등록할 수 없다. 등록 즉시 랜덤 생성 후보에 포함된다. */
    public static synchronized <T extends Block> void register(
            String id, Class<T> blockClass, Function<Difficulty, T> creator,
            Function<T, ItemAppearance> appearance) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blockClass, "blockClass");
        Objects.requireNonNull(creator, "creator");
        Objects.requireNonNull(appearance, "appearance");
        if (id.isBlank() || ITEMS.containsKey(id)
                || ITEMS.values().stream().anyMatch(item -> item.blockClass() == blockClass)) {
            throw new IllegalArgumentException("Duplicate/blank item id or class: " + id);
        }
        ITEMS.put(id, new Definition(id, blockClass, creator,
                block -> appearance.apply(blockClass.cast(block))));
    }

    // 등록 목록을 복사한 뒤 후보를 균등 선택하고, 선택한 생성 함수에 난이도를 전달한다.
    public static Block createRandom(Difficulty difficulty, RandomGenerator random) {
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(random, "random");
        Definition[] candidates;
        synchronized (ItemRegistry.class) {
            candidates = ITEMS.values().toArray(Definition[]::new);
        }
        Definition selected = candidates[random.nextInt(candidates.length)];
        Block result = Objects.requireNonNull(selected.creator().apply(difficulty), "created item");
        if (result.getClass() != selected.blockClass()) {
            throw new IllegalStateException("Creator returned a different item class: " + selected.id());
        }
        return result;
    }

    // 등록된 정확한 클래스의 표시 정보를 조회한다. 일반 블록이면 null을 반환한다.
    public static ItemAppearance appearanceOf(Block block) {
        Definition definition;
        synchronized (ItemRegistry.class) {
            definition = ITEMS.values().stream()
                    .filter(item -> item.blockClass() == block.getClass())
                    .findFirst().orElse(null);
        }
        return definition == null ? null
                : Objects.requireNonNull(definition.appearance().apply(block), "item appearance");
    }
}