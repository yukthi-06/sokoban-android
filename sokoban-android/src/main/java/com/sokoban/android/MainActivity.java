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
                if (json.has("hide_disliked")) {
                    editor.putBoolean("hide_disliked", json.getBoolean("hide_disliked"));
                }
                editor.apply();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeDashboard() {
        loadSettingsFromJson();
        levelFiles = repository.getLevelFiles();
        if (levelFiles == null || levelFiles.isEmpty()) return;

        java.util.Set<String> completedLevels = new java.util.HashSet<>();
        java.io.File solDir = new java.io.File("/sdcard/Vypeensoft/Sokoban/solutions/");
        if (solDir.exists() && solDir.isDirectory()) {
            java.io.File[] files = solDir.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.getName().endsWith("_solution.json")) {
                        completedLevels.add(f.getName().replace("_solution.json", ""));
                    }
                }
            }
        }

        LevelAdapter adapter = new LevelAdapter(levelFiles, completedLevels, position -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_LEVEL_INDEX, position);
            startActivity(intent);
        });
        
        recyclerViewLevels.setAdapter(adapter);
    }
}
