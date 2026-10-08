package item.types;
import blocks.core.BlockType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// FR-36: 폭탄 모양과 효과 담당자에게 전달할 좌표 검증. 폭발 로직 검증 X.
class BombItemTest {
    @Test
    void bombIsOneCellAndTargetFollowsPosition() {
        BombItem bomb = new BombItem();
        assertEquals(BlockType.BOMB, bomb.getType());
        assertEquals(1, bomb.getWidth()); assertEquals(1, bomb.getHeight());
        assertArrayEquals(new int[]{1}, bomb.getShape()[0]);
        bomb.setX(6); bomb.setY(-1);
        assertEquals(6, bomb.getTargetCol()); assertEquals(-1, bomb.getTargetRow());
        bomb.moveDown(); bomb.moveLeft();
        assertEquals(5, bomb.getTargetCol()); assertEquals(0, bomb.getTargetRow());
    }
}
