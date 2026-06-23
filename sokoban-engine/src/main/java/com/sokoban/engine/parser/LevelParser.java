package com.sokoban.engine.parser;

import com.sokoban.engine.model.GameState;
import com.sokoban.engine.model.GridCell;
import com.sokoban.engine.model.Position;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public final class LevelParser {

    public static GameState parse(String xmlContent) throws Exception {
        return parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    public static GameState parse(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Allow parsing HTML-like or self-closing tags smoothly
        factory.setCoalescing(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(inputStream);
        doc.getDocumentElement().normalize();

        String id = getElementValue(doc, "Id");
        String name = getElementValue(doc, "Name");
        String author = getElementValue(doc, "Author");
        int height = Integer.parseInt(getElementValue(doc, "Height"));
        int width = Integer.parseInt(getElementValue(doc, "Width"));
        String gridRaw = getElementValue(doc, "GridWithBlankSpaces");

        // Parse grid cells
        GridCell[][] grid = new GridCell[height][width];
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                grid[r][c] = GridCell.EMPTY;
            }
        }

        // Split raw grid by /
        String[] lines = gridRaw.split("/");
        int targetRow = 0;
        Position playerPos = null;

        for (String line : lines) {
            // Skip empty lines (often resulting from leading/trailing slashes)
            if (line.isEmpty() && targetRow == 0) {
                continue;
            }
            if (targetRow >= height) {
                break;
            }

            for (int c = 0; c < line.length() && c < width; c++) {
                char symbol = line.charAt(c);
                GridCell cell = GridCell.fromChar(symbol);
                grid[targetRow][c] = cell;

                if (cell == GridCell.PLAYER || cell == GridCell.PLAYER_ON_GOAL) {
                    playerPos = new Position(targetRow, c);
                }
            }
            targetRow++;
        }

        if (playerPos == null) {
            // Default player position if not found (fallback)
            playerPos = new Position(0, 0);
        }

        return new GameState(id, name, author, width, height, grid, playerPos, 0, 0, null);
    }

    private static String getElementValue(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent().trim();
        }
        return "";
    }
}
