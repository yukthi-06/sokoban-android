package com.sokoban.engine;

import com.sokoban.engine.model.Direction;
import com.sokoban.engine.model.GameState;
import com.sokoban.engine.model.GridCell;
import com.sokoban.engine.model.Position;

public final class GameEngine {

    private static String getMoveChar(Direction dir, boolean isPush) {
        switch (dir) {
            case UP: return isPush ? "U" : "u";
            case DOWN: return isPush ? "D" : "d";
            case LEFT: return isPush ? "L" : "l";
            case RIGHT: return isPush ? "R" : "r";
            default: return "";
        }
    }

    public static GameState move(GameState state, Direction dir) {
        Position currentPos = state.getPlayerPos();
        Position targetPos = currentPos.translate(dir);

        GridCell targetCell = state.getCell(targetPos.getRow(), targetPos.getCol());

        // 1. Move to empty or goal
        if (targetCell == GridCell.EMPTY || targetCell == GridCell.GOAL) {
            GridCell[][] newGrid = state.getGrid();

            // Clear current player pos
            newGrid[currentPos.getRow()][currentPos.getCol()] = 
                (state.getCell(currentPos.getRow(), currentPos.getCol()) == GridCell.PLAYER_ON_GOAL) 
                ? GridCell.GOAL : GridCell.EMPTY;

            // Set new player pos
            newGrid[targetPos.getRow()][targetPos.getCol()] = 
                (targetCell == GridCell.GOAL) ? GridCell.PLAYER_ON_GOAL : GridCell.PLAYER;

            return new GameState(
                state.getId(),
                state.getName(),
                state.getAuthor(),
                state.getWidth(),
                state.getHeight(),
                newGrid,
                targetPos,
                state.getMovesCount() + 1,
                state.getPushesCount(),
                state.getMoveSequence() + getMoveChar(dir, false),
                state
            );
        }

        // 2. Move & push box
        if (targetCell == GridCell.BOX || targetCell == GridCell.BOX_ON_GOAL) {
            Position behindBoxPos = targetPos.translate(dir);
            GridCell behindBoxCell = state.getCell(behindBoxPos.getRow(), behindBoxPos.getCol());

            if (behindBoxCell == GridCell.EMPTY || behindBoxCell == GridCell.GOAL) {
                GridCell[][] newGrid = state.getGrid();

                // Clear current player pos
                newGrid[currentPos.getRow()][currentPos.getCol()] = 
                    (state.getCell(currentPos.getRow(), currentPos.getCol()) == GridCell.PLAYER_ON_GOAL) 
                    ? GridCell.GOAL : GridCell.EMPTY;

                // Move player to target pos (where box was)
                newGrid[targetPos.getRow()][targetPos.getCol()] = 
                    (targetCell == GridCell.BOX_ON_GOAL) ? GridCell.PLAYER_ON_GOAL : GridCell.PLAYER;

                // Push box to behindBoxPos
                newGrid[behindBoxPos.getRow()][behindBoxPos.getCol()] = 
                    (behindBoxCell == GridCell.GOAL) ? GridCell.BOX_ON_GOAL : GridCell.BOX;

                return new GameState(
                    state.getId(),
                    state.getName(),
                    state.getAuthor(),
                    state.getWidth(),
                    state.getHeight(),
                    newGrid,
                    targetPos,
                    state.getMovesCount() + 1,
                    state.getPushesCount() + 1,
                    state.getMoveSequence() + getMoveChar(dir, true),
                    state
                );
            }
        }

        // 3. Wall or blocked move: return same state
        return state;
    }

    public static boolean isWin(GameState state) {
        GridCell[][] grid = state.getGrid();
        for (int r = 0; r < state.getHeight(); r++) {
            for (int c = 0; c < state.getWidth(); c++) {
                GridCell cell = grid[r][c];
                // If there's any goal that is NOT covered by a box, game is not won
                if (cell == GridCell.GOAL || cell == GridCell.PLAYER_ON_GOAL) {
                    return false;
                }
            }
        }
        return true;
    }

    public static GameState undo(GameState state) {
        if (state.hasPreviousState()) {
            return state.getPreviousState();
        }
        return state;
    }

    public static GameState restart(GameState state) {
        GameState temp = state;
        while (temp.hasPreviousState()) {
            temp = temp.getPreviousState();
        }
        return temp;
    }
}
