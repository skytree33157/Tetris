package menu.settings;

// 게임 보드 렌더링 크기 프리셋 
public enum ScreenSize {
    SMALL(20),
    MEDIUM(30),
    LARGE(40);

    private final int cellSize;

    ScreenSize(int cellSize) {
        this.cellSize = cellSize;
    }

    public int getCellSize() {
        return cellSize;
    }

}
