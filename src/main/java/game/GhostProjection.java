package game;

import java.util.Arrays;
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
        int x = currentBlock.x();

        if (!hasOccupiedCell(shape)
                || !isValidPosition(
                        board, shape, x, currentBlock.y())) {
            return null;
        }

        int landingY = currentBlock.y();

        while (isValidPosition(board, shape, x, landingY + 1)) {
            landingY++;
        }

        if (isWeightShape(shape)) {
            landingY = projectWeightStep(
                    board, shape, x, landingY
            );
        }

        return new ActionController.BlockSnapshot(
                x,
                landingY,
                shape
        );
    }

    private static int projectWeightStep(
            int[][] board,
            int[][] shape,
            int x,
            int landingY) {

        int nextY = landingY + 1;

        if (!hasBlockCollision(board, shape, x, nextY)) {
            return landingY;
        }

        // WeightItem.clearBlocksBelow()가 지우는 행
        int rowToClear = landingY + shape.length;

        if (rowToClear < 0 || rowToClear >= board.length) {
            return landingY;
        }

        int[][] simulatedBoard = Arrays.stream(board)
                .map(int[]::clone)
                .toArray(int[][]::new);

        int firstColumn = Math.max(0, x);
        int lastColumn = Math.min(
                simulatedBoard[rowToClear].length,
                x + shape[0].length
        );

        Arrays.fill(
                simulatedBoard[rowToClear],
                firstColumn,
                lastColumn,
                0
        );

        return isValidPosition(simulatedBoard, shape, x, nextY)
                ? nextY
                : landingY;
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

    private static boolean hasBlockCollision(
            int[][] board,
            int[][] shape,
            int targetX,
            int targetY) {

        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 0) {
                    continue;
                }

                int boardX = targetX + col;
                int boardY = targetY + row;

                if (boardY >= 0
                        && boardY < board.length
                        && boardX >= 0
                        && boardX < board[boardY].length
                        && board[boardY][boardX] != 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isValidPosition(
            int[][] board,
            int[][] shape,
            int targetX,
            int targetY) {

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

                if (boardY >= 0 && board[boardY][boardX] != 0) {
                    return false;
                }
            }
        }

        return true;
    }
}