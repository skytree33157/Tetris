package difficulty;

public enum Difficulty {
    EASY(12),
    NORMAL(10),
    HARD(8);

    private final int iBlockWeight;

    Difficulty(int iBlockWeight) {
        this.iBlockWeight = iBlockWeight;
    }

    public int getIBlockWeight() {
        return iBlockWeight;
    }
}
