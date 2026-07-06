package com.sokoban.android;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.sokoban.android.controller.AndroidGameController;
import com.sokoban.android.repository.LevelRepository;
import com.sokoban.android.view.GameView;
import com.sokoban.engine.GameEngine;
import com.sokoban.engine.model.Direction;
import com.sokoban.engine.model.GameState;
import java.util.List;

public final class MainActivity extends AppCompatActivity {

    private LevelRepository repository;
    private List<String> levelFiles;
    private int currentLevelIndex = 0;
    private GameState currentState;

    private Spinner levelSpinner;
    private TextView movesText;
    private TextView pushesText;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new LevelRepository(this);

        levelSpinner = findViewById(R.id.levelSpinner);
        movesText = findViewById(R.id.movesText);
        pushesText = findViewById(R.id.pushesText);
        gameView = findViewById(R.id.gameView);

        setupControls();
        
        requestStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                if (levelFiles == null) {
                    initializeGame();
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
                initializeGame();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                initializeGame();
            }
        }
    }

    private void initializeGame() {
        levelFiles = repository.getLevelFiles();
        setupLevelSpinner();
        loadLevel(currentLevelIndex);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeGame();
            }
        }
    }

    private void setupLevelSpinner() {
        if (levelFiles == null || levelFiles.isEmpty()) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this, android.R.layout.simple_spinner_item, levelFiles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        levelSpinner.setAdapter(adapter);

        levelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != currentLevelIndex) {
                    currentLevelIndex = position;
                    loadLevel(currentLevelIndex);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupControls() {
        // Setup Swipe Controls via Controller
        AndroidGameController controller = new AndroidGameController(this, this::handleMove);
        gameView.setOnTouchListener(controller);

        // Setup Buttons
        findViewById(R.id.btnUndo).setOnClickListener(v -> {
            if (currentState != null) {
                currentState = GameEngine.undo(currentState);
                updateUI();
            }
        });

        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            if (currentState != null) {
                currentState = GameEngine.restart(currentState);
                updateUI();
            }
        });
    }

    private void loadLevel(int index) {
        if (levelFiles == null || levelFiles.isEmpty()) return;
        String fileName = levelFiles.get(index);
        currentState = repository.loadLevel(fileName);
        updateUI();
    }

    private void handleMove(Direction direction) {
        if (currentState == null || GameEngine.isWin(currentState)) return;

        currentState = GameEngine.move(currentState, direction);
        updateUI();

        if (GameEngine.isWin(currentState)) {
            showWinDialog();
        }
    }

    private void updateUI() {
        if (currentState == null) return;

        gameView.setGameState(currentState);
        movesText.setText(getString(R.string.moves_label, currentState.getMovesCount()));
        pushesText.setText(getString(R.string.pushes_label, currentState.getPushesCount()));
    }

    private void showWinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.victory_title);
        builder.setMessage(getString(R.string.victory_message, 
            currentState.getMovesCount(), currentState.getPushesCount()));

        builder.setCancelable(false);
        builder.setPositiveButton(R.string.next_level_btn, (dialog, which) -> {
            if (currentLevelIndex + 1 < levelFiles.size()) {
                currentLevelIndex++;
                levelSpinner.setSelection(currentLevelIndex);
                loadLevel(currentLevelIndex);
            } else {
                // Completed all levels
                AlertDialog.Builder finishedBuilder = new AlertDialog.Builder(MainActivity.this);
                finishedBuilder.setTitle(R.string.congrats);
                finishedBuilder.setMessage("You have completed all available levels!");
                finishedBuilder.setPositiveButton("OK", null);
                finishedBuilder.show();
            }
        });

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Direction dir = null;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
                dir = Direction.UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
                dir = Direction.DOWN;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
                dir = Direction.LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
                dir = Direction.RIGHT;
                break;
        }

        if (dir != null) {
            handleMove(dir);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
