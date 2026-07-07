package com.sokoban.android.repository;

import android.content.Context;
import com.sokoban.engine.model.GameState;
import com.sokoban.engine.parser.LevelParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LevelRepository {
    private static final String LEVELS_DIR = "/sdcard/Vypeensoft/Sokoban/levels/";
    private final Context context;

    public LevelRepository(Context context) {
        this.context = context;
    }

    public List<String> getLevelFiles() {
        List<String> levels = new ArrayList<>();
        File dir = new File(LEVELS_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        levels.add(file.getName());
                    }
                }
            }
        }
        Collections.sort(levels);
        return levels;
    }

    public GameState loadLevel(String fileName) {
        File file = new File(LEVELS_DIR, fileName);
        try (InputStream is = new FileInputStream(file)) {
            return LevelParser.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
