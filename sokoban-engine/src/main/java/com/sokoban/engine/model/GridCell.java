package com.vypeensoft.sokoban.engine.model;

public enum GridCell {
    WALL('#'),
    BOX('$'),
    PLAYER('@'),
    GOAL('.'),
    EMPTY(' '),
    BOX_ON_GOAL('*'),
    PLAYER_ON_GOAL('+');

    private final char symbol;

    GridCell(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public static GridCell fromChar(char c) {
        for (GridCell cell : values()) {
            if (cell.symbol == c) {
                return cell;
            }
        }
        return EMPTY;
    }
}
