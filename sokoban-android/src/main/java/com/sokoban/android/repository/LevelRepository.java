package com.sokoban.android.repository;

import android.content.Context;
import android.content.res.AssetManager;
import com.sokoban.engine.model.GameState;
import com.sokoban.engine.parser.LevelParser;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LevelRepository {
    private final Context context;

    public LevelRepository(Context context) {
        this.context = context;
    }

    public List<String> getLevelFiles() {
        AssetManager assetManager = context.getAssets();
        List<String> levels = new ArrayList<>();
        try {
            String[] files = assetManager.list("levels");
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".json")) {
                        levels.add(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collections.sort(levels);
        return levels;
    }

    public GameState loadLevel(String fileName) {
        try (InputStream is = context.getAssets().open("levels/" + fileName)) {
            return LevelParser.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
