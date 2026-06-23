package com.sokoban.engine.model;

public final class GameState {
    private final String id;
    private final String name;
    private final String author;
    private final int width;
    private final int height;
    private final GridCell[][] grid;
    private final Position playerPos;
    private final int movesCount;
    private final int pushesCount;
    private final GameState previousState;

    public GameState(String id, String name, String author, int width, int height, 
                     GridCell[][] grid, Position playerPos, int movesCount, int pushesCount, 
                     GameState previousState) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.width = width;
        this.height = height;
        this.grid = deepCopyGrid(grid);
        this.playerPos = playerPos;
        this.movesCount = movesCount;
        this.pushesCount = pushesCount;
        this.previousState = previousState;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public GridCell[][] getGrid() {
        return deepCopyGrid(grid);
    }

    public GridCell getCell(int row, int col) {
        if (row < 0 || row >= height || col < 0 || col >= width) {
            return GridCell.WALL; // Boundary acts as wall
        }
        return grid[row][col];
    }

    public Position getPlayerPos() {
        return playerPos;
    }

    public int getMovesCount() {
        return movesCount;
    }

    public int getPushesCount() {
        return pushesCount;
    }

    public GameState getPreviousState() {
        return previousState;
    }

    public boolean hasPreviousState() {
        return previousState != null;
    }

    private static GridCell[][] deepCopyGrid(GridCell[][] source) {
        GridCell[][] copy = new GridCell[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }
}
