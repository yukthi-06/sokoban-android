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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        intervalValueText = findViewById(R.id.intervalValueText);
        intervalSeekBar = findViewById(R.id.intervalSeekBar);

        int currentInterval = prefs.getInt(KEY_REPLAY_INTERVAL, DEFAULT_REPLAY_INTERVAL);
        
        // SeekBar range: 0-900 maps to 100-1000ms
        intervalSeekBar.setProgress(currentInterval - 100);
        updateIntervalText(currentInterval);

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
                int ms = seekBar.getProgress() + 100;
                prefs.edit().putInt(KEY_REPLAY_INTERVAL, ms).apply();
            }
        });
    }

    private void updateIntervalText(int ms) {
        intervalValueText.setText("Interval: " + ms + " ms");
    }
}
