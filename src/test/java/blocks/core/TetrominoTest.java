package blocks.core;
import blocks.tetromino.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// FR-04·05: 7종의 모양·타입과 인스턴스 독립성 검증.
class TetrominoTest {
    @Test
    void sevenShapesHaveExpectedOccupiedCells() {
        Block[] blocks = {new IBlock(), new OBlock(), new TBlock(), new SBlock(), new ZBlock(), new JBlock(), new LBlock()};
        String[] expected = {"0000/1111/0000/0000", "11/11", "010/111/000", "011/110/000",
                "110/011/000", "111/001/000", "111/100/000"};
        for (int i = 0; i < blocks.length; i++) {
            assertEquals(i + 1, blocks[i].getType().getValue());
            StringJoiner rows = new StringJoiner("/");
            for (int[] row : blocks[i].getShape()) {
                StringBuilder cells = new StringBuilder();
                for (int cell : row) cells.append(cell);
                rows.add(cells);
            }
            assertEquals(expected[i], rows.toString());
        }
        IBlock first = new IBlock(), second = new IBlock();
        first.getShape()[1][0] = 0;
        assertEquals(1, second.getShape()[1][0]);
    }

    @Test
    void typeValuesKeepBoardCompatibilityAndRejectUnknownValues() {
        for (BlockType type : BlockType.values()) assertSame(type, BlockType.fromValue(type.getValue()));
        for (int value : new int[]{0, -1, 11, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> BlockType.fromValue(value));
        }
    }
}
