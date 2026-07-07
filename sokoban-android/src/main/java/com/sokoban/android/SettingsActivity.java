package com.vypeensoft.sokoban.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "SokobanPrefs";
    public static final String KEY_REPLAY_INTERVAL = "replay_interval";
    public static final String KEY_HIDE_DISLIKED = "hide_disliked";
    public static final String KEY_SHOW_COMPLETED = "show_completed";
    public static final int DEFAULT_REPLAY_INTERVAL = 300;

    private TextView intervalValueText;
    private SeekBar intervalSeekBar;
    private SharedPreferences prefs;
    private int selectedInterval;
    private CheckBox hideDislikedCheckbox;
    private boolean hideDisliked;
    private CheckBox showCompletedCheckbox;
    private boolean showCompleted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        intervalValueText = findViewById(R.id.intervalValueText);
        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        hideDislikedCheckbox = findViewById(R.id.hideDislikedCheckbox);
        showCompletedCheckbox = findViewById(R.id.showCompletedCheckbox);

        selectedInterval = prefs.getInt(KEY_REPLAY_INTERVAL, DEFAULT_REPLAY_INTERVAL);
        hideDisliked = prefs.getBoolean(KEY_HIDE_DISLIKED, false);
        hideDislikedCheckbox.setChecked(hideDisliked);
        
        showCompleted = prefs.getBoolean(KEY_SHOW_COMPLETED, true);
        showCompletedCheckbox.setChecked(showCompleted);

        hideDislikedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hideDisliked = isChecked;
        });
        
        showCompletedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showCompleted = isChecked;
        });
        
        // SeekBar range: 0-900 maps to 100-1000ms
        intervalSeekBar.setProgress(selectedInterval - 100);
        updateIntervalText(selectedInterval);

        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int ms = progress + 100;
                updateIntervalText(ms);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                selectedInterval = seekBar.getProgress() + 100;
            }
        });

        findViewById(R.id.btnSaveConfig).setOnClickListener(v -> {
            prefs.edit()
                 .putInt(KEY_REPLAY_INTERVAL, selectedInterval)
                 .putBoolean(KEY_HIDE_DISLIKED, hideDisliked)
                 .putBoolean(KEY_SHOW_COMPLETED, showCompleted)
                 .apply();
                 
            try {
                java.io.File dir = new java.io.File("/sdcard/Vypeensoft/Sokoban/settings");
                if (!dir.exists()) dir.mkdirs();
                java.io.File f = new java.io.File(dir, "settings.json");
                org.json.JSONObject json = new org.json.JSONObject();
                json.put("replay_interval", selectedInterval);
                json.put("hide_disliked", hideDisliked);
                json.put("show_completed", showCompleted);
                try (java.io.FileWriter file = new java.io.FileWriter(f)) {
                    file.write(json.toString(4));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            finish();
        });
    }

    private void updateIntervalText(int ms) {
        intervalValueText.setText("Interval: " + ms + " ms");
    }
}
