package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import blocks.core.Block;
import blocks.core.BlockType;
import blocks.style.ColorMode;
import blocks.tetromino.OBlock;
import board.Board;
import difficulty.Difficulty;
import item.types.WeightItem;
import menu.settings.AppSettings;
import menu.settings.ScreenSize;
import score.ScoreManager;

class GamePanelGhostTest {

    private final AppSettings settings = AppSettings.getInstance();

    private ScreenSize originalSize;
    private ColorMode originalColor;

    private Board board;
    private GameStateManager state;
    private ActionController controller;
    private GamePanel panel;

    @BeforeEach
    void setUp() {
        originalSize = settings.getScreenSize();
        originalColor = settings.getColorMode();

        settings.setScreenSize(ScreenSize.MEDIUM);
        settings.setColorMode(ColorMode.NORMAL);

        board = new Board();
        state = new GameStateManager();
    }

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.shutdown();
        }

        settings.setScreenSize(originalSize);
        settings.setColorMode(originalColor);
    }

    private void createGame(Block block, GameMode mode)
            throws Exception {
        controller = new ActionController(
                board,
                block,
                state,
                new ScoreManager(),
                Difficulty.NORMAL,
                mode,
                0
        );

        SwingUtilities.invokeAndWait(() -> {
            panel = new GamePanel(board);
        });
    }

    @ParameterizedTest
    @EnumSource(ScreenSize.class)
    void showsOutlineAtLandingInEveryScreenSize(ScreenSize size)
            throws Exception {
        settings.setScreenSize(size);
        createGame(new OBlock(), GameMode.NORMAL);

        BufferedImage image = renderSnapshot();
        int cellSize = size.getCellSize();

        assertTrue(hasInteriorDrawing(image, 3, 21, cellSize));

        assertEquals(
                Color.BLACK.getRGB(),
                image.getRGB(
                        3 * cellSize + cellSize / 2,
                        21 * cellSize + cellSize / 2
                )
        );

        assertTrue(hasInteriorDrawing(image, 3, 0, cellSize));
    }

    @ParameterizedTest
    @EnumSource(ColorMode.class)
    void showsGhostInEveryColorMode(ColorMode mode)
            throws Exception {
        settings.setColorMode(mode);
        createGame(new OBlock(), GameMode.NORMAL);

        assertTrue(
                hasInteriorDrawing(renderSnapshot(), 3, 21, 30)
        );
    }

    @Test
    void movingBlockUpdatesGhostAndClearsPreviousPosition()
            throws Exception {
        createGame(new OBlock(), GameMode.NORMAL);

        BufferedImage before = renderSnapshot();
        assertTrue(hasInteriorDrawing(before, 3, 21, 30));

        controller.moveRightAction();

        BufferedImage after = renderSnapshot();

        assertFalse(hasInteriorDrawing(after, 3, 21, 30));
        assertTrue(hasInteriorDrawing(after, 5, 21, 30));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hidesGhostAfterGameOverOrShutdown(boolean gameOver)
            throws Exception {
        createGame(new OBlock(), GameMode.NORMAL);

        assertTrue(
                hasInteriorDrawing(renderSnapshot(), 3, 21, 30)
        );

        if (gameOver) {
            state.setGameOver(true);
        } else {
            controller.shutdown();
        }

        assertFalse(
                hasInteriorDrawing(renderSnapshot(), 3, 21, 30)
        );
    }

    @Test
    void weightGhostAppearsBelowObstaclesWithoutRemovingThem()
            throws Exception {
        board.getBoard()[15][3] = BlockType.I.getValue();

        createGame(new WeightItem(), GameMode.ITEM);

        BufferedImage image = renderSnapshot();

        assertTrue(hasInteriorDrawing(image, 3, 22, 30));
        assertEquals(
                BlockType.I.getValue(),
                board.getBoard()[15][3]
        );
        assertEquals(0, controller.getCurrentBlock().getY());
    }

    @Test
    void legacyCurrentBlockSetterAlsoShowsGhost()
            throws Exception {
        OBlock block = new OBlock();
        block.setX(3);
        block.setY(0);

        AtomicReference<BufferedImage> result =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            panel = new GamePanel(board);
            panel.setCurrentBlock(block);
            result.set(paint(panel));
        });

        assertTrue(
                hasInteriorDrawing(result.get(), 3, 21, 30)
        );
    }

    private BufferedImage renderSnapshot() throws Exception {
        ActionController.ViewSnapshot frame =
                controller.getViewSnapshot();

        AtomicReference<BufferedImage> result =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            panel.setSnapshot(frame);
            result.set(paint(panel));
        });

        return result.get();
    }

    private static BufferedImage paint(GamePanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);

        BufferedImage image = new BufferedImage(
                size.width,
                size.height,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = image.createGraphics();

        try {
            panel.paint(graphics);
        } finally {
            graphics.dispose();
        }

        return image;
    }

    private static boolean hasInteriorDrawing(
            BufferedImage image,
            int column,
            int row,
            int cellSize) {
        int startX = column * cellSize;
        int startY = row * cellSize;

        for (int y = startY + 1; y < startY + cellSize - 1; y++) {
            for (int x = startX + 1; x < startX + cellSize - 1; x++) {
                if (image.getRGB(x, y) != Color.BLACK.getRGB()) {
                    return true;
                }
            }
        }

        return false;
    }
}