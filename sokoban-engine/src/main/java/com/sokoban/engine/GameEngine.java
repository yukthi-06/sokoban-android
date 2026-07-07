package com.vypeensoft.sokoban.engine;

import com.vypeensoft.sokoban.engine.model.Direction;
import com.vypeensoft.sokoban.engine.model.GameState;
import com.vypeensoft.sokoban.engine.model.GridCell;
import com.vypeensoft.sokoban.engine.model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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

    public static List<Direction> findPath(GameState state, Position target) {
        Position start = state.getPlayerPos();
        if (start.equals(target)) return null;
        
        GridCell targetCell = state.getCell(target.getRow(), target.getCol());
        if (targetCell != GridCell.EMPTY && targetCell != GridCell.GOAL) {
            return null; // Can only walk to empty or goal
        }

        Queue<Position> queue = new LinkedList<>();
        Map<Position, Position> cameFrom = new HashMap<>();
        Map<Position, Direction> moveFrom = new HashMap<>();
        
        queue.add(start);
        cameFrom.put(start, null);
        
        while (!queue.isEmpty()) {
            Position curr = queue.poll();
            
            if (curr.equals(target)) {
                List<Direction> path = new ArrayList<>();
                Position p = target;
                while (cameFrom.get(p) != null) {
                    path.add(moveFrom.get(p));
                    p = cameFrom.get(p);
                }
                Collections.reverse(path);
                return path;
            }
            
            for (Direction dir : Direction.values()) {
                Position next = curr.translate(dir);
                if (next.getRow() >= 0 && next.getRow() < state.getHeight() && 
                    next.getCol() >= 0 && next.getCol() < state.getWidth()) {
                    
                    if (!cameFrom.containsKey(next)) {
                        GridCell cell = state.getCell(next.getRow(), next.getCol());
                        if (cell == GridCell.EMPTY || cell == GridCell.GOAL || cell == GridCell.PLAYER || cell == GridCell.PLAYER_ON_GOAL) {
                            cameFrom.put(next, curr);
                            moveFrom.put(next, dir);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return null;
    }
}
