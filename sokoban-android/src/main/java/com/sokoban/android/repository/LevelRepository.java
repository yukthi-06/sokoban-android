package com.vypeensoft.sokoban.android.repository;

import android.content.Context;
import com.vypeensoft.sokoban.engine.model.GameState;
import com.vypeensoft.sokoban.engine.parser.LevelParser;

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
            File[] packDirs = dir.listFiles();
            if (packDirs != null) {
                for (File packDir : packDirs) {
                    if (packDir.isDirectory()) {
                        File[] files = packDir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                if (file.isFile() && file.getName().endsWith(".json")) {
                                    levels.add(packDir.getName() + "/" + file.getName());
                                }
                            }
                        }
                    }
                }
            }
        }
        Collections.sort(levels);
        return levels;
    }

    public java.util.Map<String, Integer> getPacksWithCounts() {
        java.util.Map<String, Integer> packs = new java.util.TreeMap<>();
        List<String> files = getLevelFiles();
        for (String f : files) {
            String[] parts = f.split("/");
            if (parts.length >= 2) {
                String packName = parts[0];
                packs.put(packName, packs.getOrDefault(packName, 0) + 1);
            }
        }
        return packs;
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
