package com.vypeensoft.sokoban.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.vypeensoft.sokoban.android.repository.LevelRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LevelSelectionActivity extends AppCompatActivity {

    public static final String EXTRA_PACK_NAME = "extra_pack_name";

    private LevelRepository repository;
    private List<String> levelFiles;
    private RecyclerView recyclerViewLevels;
    private String packName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_selection);

        packName = getIntent().getStringExtra(EXTRA_PACK_NAME);
        if (packName != null) {
            TextView subtitle = findViewById(R.id.selectLevelSubtitle);
            subtitle.setText("Select Level - " + packName);
        }

        repository = new LevelRepository(this);
        recyclerViewLevels = findViewById(R.id.recyclerViewLevels);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeLevels();
    }

    private void initializeLevels() {
        levelFiles = repository.getLevelFiles();
        if (levelFiles == null || levelFiles.isEmpty()) return;

        Set<String> completedLevels = new HashSet<>();
        Set<String> dislikedLevels = new HashSet<>();

        File indexFile = new File("/sdcard/Vypeensoft/Sokoban/level_index.json");
        if (indexFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(indexFile.getAbsolutePath())));
                org.json.JSONObject indexJson = new org.json.JSONObject(content);
                
                if (packName != null && indexJson.has(packName)) {
                    org.json.JSONObject packObj = indexJson.getJSONObject(packName);
                    java.util.Iterator<String> keys = packObj.keys();
                    while (keys.hasNext()) {
                        String levelName = keys.next();
                        org.json.JSONObject levelObj = packObj.getJSONObject(levelName);
                        // The key used in completedLevels is the rawName "packName/levelName"
                        String rawName = packName + "/" + levelName;
                        if (levelObj.optBoolean("completed", false)) {
                            completedLevels.add(rawName);
                        }
                        if ("dislike".equals(levelObj.optString("liked", "neutral"))) {
                            dislikedLevels.add(rawName);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Fallback to legacy crawling
            File solDir = new File("/sdcard/Vypeensoft/Sokoban/solutions/" + (packName != null ? packName : ""));
            if (solDir.exists() && solDir.isDirectory()) {
                File[] files = solDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith("_solution.json")) {
                            String levelName = f.getName().replace("_solution.json", "");
                            completedLevels.add(packName != null ? packName + "/" + levelName : levelName);
                        }
                    }
                }
            }

            File ldDir = new File("/sdcard/Vypeensoft/Sokoban/like_dislike/" + (packName != null ? packName : ""));
            if (ldDir.exists() && ldDir.isDirectory()) {
                File[] files = ldDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".json")) {
                            try {
                                String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
                                org.json.JSONObject ldJson = new org.json.JSONObject(content);
                                if ("dislike".equals(ldJson.optString("state", ""))) {
                                    String levelName = f.getName().replace(".json", "");
                                    dislikedLevels.add(packName != null ? packName + "/" + levelName : levelName);
                                }
                            } catch (Exception e) {}
                        }
                    }
                }
            }
        }

        SharedPreferences prefs = getSharedPreferences("SokobanPrefs", Context.MODE_PRIVATE);
        boolean showCompleted = prefs.getBoolean("show_completed", true);
        boolean showDisliked = prefs.getBoolean("show_disliked", true);

        List<String> displayFiles = new ArrayList<>();
        List<Integer> displayIndices = new ArrayList<>();

        for (int i = 0; i < levelFiles.size(); i++) {
            String f = levelFiles.get(i);
            
            // Only include files from the selected pack
            if (packName != null && !f.startsWith(packName + "/")) {
                continue;
            }
            
            String rawName = f.replace(".json", "");
            if (!showCompleted && completedLevels.contains(rawName)) {
                continue;
            }
            if (!showDisliked && dislikedLevels.contains(rawName)) {
                continue;
            }
            displayFiles.add(f);
            displayIndices.add(i); // Keep track of global index
        }

        LevelAdapter adapter = new LevelAdapter(displayFiles, completedLevels, position -> {
            int originalIndex = displayIndices.get(position);
            Intent intent = new Intent(LevelSelectionActivity.this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_LEVEL_INDEX, originalIndex);
            startActivity(intent);
        });

        recyclerViewLevels.setAdapter(adapter);
    }
}
