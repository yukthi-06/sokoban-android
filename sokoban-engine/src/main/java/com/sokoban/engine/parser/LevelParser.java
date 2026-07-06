package com.sokoban.engine.parser;

import com.sokoban.engine.model.GameState;
import com.sokoban.engine.model.GridCell;
import com.sokoban.engine.model.Position;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class LevelParser {

    public static GameState parse(String jsonContent) throws Exception {
        JSONObject json = new JSONObject(jsonContent);

        String id = String.valueOf(json.optInt("id", 0));
        String name = json.optString("title", "");
        String author = json.optString("author", "");
        int height = json.optInt("height", 0);
        int width = json.optInt("width", 0);
        String gridRaw = json.optString("map", "");

        GridCell[][] grid = new GridCell[height][width];
        Position playerPos = null;

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c] = GridCell.EMPTY;
            }
        }

        int index = 0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (index < gridRaw.length()) {
                    char symbol = gridRaw.charAt(index++);
                    GridCell cell = parseCell(symbol);
                    grid[r][c] = cell;

                    if (cell == GridCell.PLAYER || cell == GridCell.PLAYER_ON_GOAL) {
                        playerPos = new Position(r, c);
                    }
                }
            }
        }

        if (playerPos == null) {
            playerPos = new Position(0, 0);
        }

        return new GameState(id, name, author, width, height, grid, playerPos, 0, 0, null);
    }

    public static GameState parse(InputStream inputStream) throws Exception {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            String content = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            return parse(content);
        }
    }

    private static GridCell parseCell(char c) {
        switch (c) {
            case '0':
            case '7':
                return GridCell.EMPTY;
            case '1':
                return GridCell.WALL;
            case '2':
                return GridCell.PLAYER;
            case '3':
                return GridCell.BOX;
            case '4':
                return GridCell.GOAL;
            case '5':
                return GridCell.BOX_ON_GOAL;
            case '6':
                return GridCell.PLAYER_ON_GOAL;
            default:
                return GridCell.EMPTY;
        }
    }
}
