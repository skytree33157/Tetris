package item;

/** 생성할 아이템 종류. 블록 모양을 나타내는 BlockType과 별개. */
public enum ItemType {
    LINE_CLEAR("line-clear"),
    WEIGHT("weight"),
    BOMB("bomb"),
    BONUS("bonus");

    private final String id;

    ItemType(String id) {
        this.id = id;
    }

    // Registry에 등록된 아이템 id 반환.
    public String getId() {
        return id;
    }
}
