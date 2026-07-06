package com.sokoban.android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sokoban.android.repository.LevelRepository;

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

        requestStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (levelFiles == null) {
                    initializeDashboard();
                }
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
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
            } else {
                initializeDashboard();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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

    private void initializeDashboard() {
        levelFiles = repository.getLevelFiles();
        if (levelFiles == null || levelFiles.isEmpty()) return;

        LevelAdapter adapter = new LevelAdapter(levelFiles, position -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            intent.putExtra(GameActivity.EXTRA_LEVEL_INDEX, position);
            startActivity(intent);
        });
        
        recyclerViewLevels.setAdapter(adapter);
    }
}
