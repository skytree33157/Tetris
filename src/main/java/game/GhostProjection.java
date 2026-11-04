package game;

import java.util.Objects;

import blocks.core.BlockType;
import ui.ItemAppearanceResolver;

public final class GhostProjection {

    private GhostProjection() {
    }

    public static ActionController.BlockSnapshot project(
            int[][] board,
            ActionController.BlockSnapshot currentBlock) {

        Objects.requireNonNull(board, "board");

        if (currentBlock == null || board.length == 0) {
            return null;
        }

        int[][] shape = currentBlock.cells();

        if (!hasOccupiedCell(shape)
                || !isValidPosition(
                        board,
                        shape,
                        currentBlock.x(),
                        currentBlock.y(),
                        false)) {
            return null;
        }

        boolean ignoreFixedCells = isWeightShape(shape);
        int landingY = currentBlock.y();

        while (isValidPosition(
                board,
                shape,
                currentBlock.x(),
                landingY + 1,
                ignoreFixedCells)) {
            landingY++;
        }

        return new ActionController.BlockSnapshot(
                currentBlock.x(),
                landingY,
                shape
        );
    }

    private static boolean hasOccupiedCell(int[][] shape) {
        for (int[] row : shape) {
            for (int cell : row) {
                if (cell != 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isWeightShape(int[][] shape) {
        boolean foundCell = false;

        for (int[] row : shape) {
            for (int cell : row) {
                if (cell == 0) {
                    continue;
                }

                foundCell = true;

                if (ItemAppearanceResolver.fromBoardCell(cell).type()
                        != BlockType.WEIGHT) {
                    return false;
                }
            }
        }

        return foundCell;
    }

    private static boolean isValidPosition(
            int[][] board,
            int[][] shape,
            int targetX,
            int targetY,
            boolean ignoreFixedCells) {

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 0) {
                    continue;
                }

                int boardX = targetX + col;
                int boardY = targetY + row;

                if (boardX < 0
                        || boardX >= board[0].length
                        || boardY >= board.length) {
                    return false;
                }

                if (!ignoreFixedCells
                        && boardY >= 0
                        && board[boardY][boardX] != 0) {
                    return false;
                }
            }
        }

        return true;
    }
}