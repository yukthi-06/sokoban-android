package com.vypeensoft.sokoban.engine;

import static org.junit.jupiter.api.Assertions.*;

import com.vypeensoft.sokoban.engine.model.*;
import com.vypeensoft.sokoban.engine.parser.LevelParser;
import org.junit.jupiter.api.Test;

public class GameEngineTest {

    private static final String SAMPLE_LEVEL_JSON = 
        "{\n" +
        "    \"id\": 9999,\n" +
        "    \"title\": \"Test Level\",\n" +
        "    \"author\": \"Test Author\",\n" +
        "    \"height\": 3,\n" +
        "    \"width\": 5,\n" +
        "    \"map\": \"111111234110001\"\n" +
        "}";

    @Test
    public void testParserAndMovement() throws Exception {
        GameState state = LevelParser.parse(SAMPLE_LEVEL_JSON);
        
        assertEquals("9999", state.getId());
        assertEquals("Test Level", state.getName());
        assertEquals("Test Author", state.getAuthor());
        assertEquals(3, state.getHeight());
        assertEquals(5, state.getWidth());
        
        // Player is at row 1, col 1
        assertEquals(new Position(1, 1), state.getPlayerPos());
        assertEquals(GridCell.PLAYER, state.getCell(1, 1));
        assertEquals(GridCell.BOX, state.getCell(1, 2));
        assertEquals(GridCell.GOAL, state.getCell(1, 3));
        
        // Move right (pushing box onto goal)
        state = GameEngine.move(state, Direction.RIGHT);
        
        assertEquals(new Position(1, 2), state.getPlayerPos());
        assertEquals(GridCell.EMPTY, state.getCell(1, 1));
        assertEquals(GridCell.PLAYER, state.getCell(1, 2));
        assertEquals(GridCell.BOX_ON_GOAL, state.getCell(1, 3));
        assertEquals(1, state.getMovesCount());
        assertEquals(1, state.getPushesCount());
        
        // Verify win
        assertTrue(GameEngine.isWin(state));
        
        // Undo move
        state = GameEngine.undo(state);
        assertEquals(new Position(1, 1), state.getPlayerPos());
        assertEquals(GridCell.PLAYER, state.getCell(1, 1));
        assertEquals(GridCell.BOX, state.getCell(1, 2));
        assertEquals(GridCell.GOAL, state.getCell(1, 3));
        assertEquals(0, state.getMovesCount());
        assertFalse(GameEngine.isWin(state));
    }
}
