package com.vypeensoft.sokoban.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vypeensoft.sokoban.android.repository.LevelRepository;

import java.util.List;

public final class MainActivity extends AppCompatActivity {

    private LevelRepository repository;
    private List<String> levelFiles;
    private RecyclerView recyclerViewLevels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new LevelRepository(this);
        recyclerViewLevels = findViewById(R.id.recyclerViewLevels);
        
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btnIndex).setOnClickListener(v -> {
            Toast.makeText(this, "Indexing levels...", Toast.LENGTH_SHORT).show();
            buildIndexAsync();
        });
        
        findViewById(R.id.btnHelp).setOnClickListener(v -> {
            Toast.makeText(this, "Help coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.btnAbout).setOnClickListener(v -> {
            Toast.makeText(this, "About coming soon!", Toast.LENGTH_SHORT).show();
        });

        requestStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean hasPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasPermission = Environment.isExternalStorageManager();
        } else {
            hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        if (hasPermission) {
            initializeDashboard();
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app requires file access permission to read levels and save settings. Please grant 'All files access' in the next screen.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.addCategory("android.intent.category.DEFAULT");
                            intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
                            startActivity(intent);
                        } catch (Exception e) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(false)
                    .show();
            } else {
                initializeDashboard();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app requires file access permission to read levels and save settings.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    })
                    .setCancelable(false)
                    .show();
            } else {
                initializeDashboard();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeDashboard();
            }
        }
    }

    private void loadSettingsFromJson() {
        java.io.File file = new java.io.File("/sdcard/Vypeensoft/Sokoban/settings/settings.json");
        if (file.exists()) {
            try {
                String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file.getAbsolutePath())));
                org.json.JSONObject json = new org.json.JSONObject(content);
                android.content.SharedPreferences prefs = getSharedPreferences("SokobanPrefs", android.content.Context.MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                if (json.has("replay_interval")) {
                    editor.putInt("replay_interval", json.getInt("replay_interval"));
                }
                if (json.has("show_disliked")) {
                    editor.putBoolean("show_disliked", json.getBoolean("show_disliked"));
                }
                if (json.has("show_completed")) {
                    editor.putBoolean("show_completed", json.getBoolean("show_completed"));
                }
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void buildIndexAsync() {
        new Thread(() -> {
            try {
                org.json.JSONObject indexJson = new org.json.JSONObject();
                java.io.File solDir = new java.io.File("/sdcard/Vypeensoft/Sokoban/solutions/");
                java.io.File ldDir = new java.io.File("/sdcard/Vypeensoft/Sokoban/like_dislike/");
                
                if (levelFiles == null) {
                    levelFiles = repository.getLevelFiles();
                }
                
                if (levelFiles != null) {
                    for (String f : levelFiles) {
                        String rawName = f.replace(".json", "");
                        String[] parts = rawName.split("/");
                        if (parts.length < 2) continue;
                        
                        String packName = parts[0];
                        String levelName = parts[1];
                        
                        org.json.JSONObject packObj;
                        if (indexJson.has(packName)) {
                            packObj = indexJson.getJSONObject(packName);
                        } else {
                            packObj = new org.json.JSONObject();
                            indexJson.put(packName, packObj);
                        }
                        
                        org.json.JSONObject levelObj = new org.json.JSONObject();
                        
                        // Check completion
                        java.io.File solFile = new java.io.File(solDir, rawName + "_solution.json");
                        levelObj.put("completed", solFile.exists());
                        
                        // Check like/dislike
                        java.io.File ldFile = new java.io.File(ldDir, rawName + ".json");
                        String likedState = "neutral";
                        if (ldFile.exists()) {
                            try {
                                String content = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(ldFile.getAbsolutePath())));
                                org.json.JSONObject ldJson = new org.json.JSONObject(content);
                                likedState = ldJson.optString("state", "neutral");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        levelObj.put("liked", likedState);
                        
                        packObj.put(levelName, levelObj);
                    }
                }
                
                java.io.File indexFile = new java.io.File("/sdcard/Vypeensoft/Sokoban/level_index.json");
                try (java.io.FileWriter writer = new java.io.FileWriter(indexFile)) {
                    writer.write(indexJson.toString(4));
                }
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "Indexing complete!", Toast.LENGTH_SHORT).show();
                    initializeDashboard();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Indexing failed.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void initializeDashboard() {
        loadSettingsFromJson();
        java.util.Map<String, Integer> packs = repository.getPacksWithCounts();
        if (packs == null || packs.isEmpty()) {
            Toast.makeText(this, "No packs found in /sdcard/Vypeensoft/Sokoban/levels/", Toast.LENGTH_LONG).show();
            return;
        }

        java.util.List<String> packNames = new java.util.ArrayList<>(packs.keySet());
        Toast.makeText(this, "Found " + packNames.size() + " packs!", Toast.LENGTH_SHORT).show();

        PackAdapter adapter = new PackAdapter(packNames, packs, packName -> {
            Intent intent = new Intent(MainActivity.this, LevelSelectionActivity.class);
            intent.putExtra(LevelSelectionActivity.EXTRA_PACK_NAME, packName);
            startActivity(intent);
        });
        
        recyclerViewLevels.setAdapter(adapter);
    }
}
