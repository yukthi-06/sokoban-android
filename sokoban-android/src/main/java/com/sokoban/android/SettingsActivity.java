package com.sokoban.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "SokobanPrefs";
    public static final String KEY_REPLAY_INTERVAL = "replay_interval";
    public static final int DEFAULT_REPLAY_INTERVAL = 300;

    private TextView intervalValueText;
    private SeekBar intervalSeekBar;
    private SharedPreferences prefs;
    private int selectedInterval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        intervalValueText = findViewById(R.id.intervalValueText);
        intervalSeekBar = findViewById(R.id.intervalSeekBar);

        selectedInterval = prefs.getInt(KEY_REPLAY_INTERVAL, DEFAULT_REPLAY_INTERVAL);
        
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
            prefs.edit().putInt(KEY_REPLAY_INTERVAL, selectedInterval).apply();
            finish();
        });
    }

    private void updateIntervalText(int ms) {
        intervalValueText.setText("Interval: " + ms + " ms");
    }
}
