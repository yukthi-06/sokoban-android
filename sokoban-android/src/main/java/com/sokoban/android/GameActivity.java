package com.sokoban.android;

import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;

import com.sokoban.android.controller.AndroidGameController;
import com.sokoban.android.repository.LevelRepository;
import com.sokoban.android.view.GameView;
import com.sokoban.engine.GameEngine;
import com.sokoban.engine.model.Direction;
import com.sokoban.engine.model.GameState;

import java.util.List;

public final class GameActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL_INDEX = "extra_level_index";

    private LevelRepository repository;
    private List<String> levelFiles;
    private int currentLevelIndex = 0;
    private GameState currentState;
    private long startTime;
    private String currentDisplayName;
    
    private static final String SOLUTIONS_DIR = "/sdcard/Vypeensoft/Sokoban/solutions/";

    private TextView levelTitleText;
    private TextView movesText;
    private TextView pushesText;
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        currentLevelIndex = getIntent().getIntExtra(EXTRA_LEVEL_INDEX, 0);

        repository = new LevelRepository(this);
        levelFiles = repository.getLevelFiles();

        levelTitleText = findViewById(R.id.levelTitleText);
        movesText = findViewById(R.id.movesText);
        pushesText = findViewById(R.id.pushesText);
        gameView = findViewById(R.id.gameView);

        setupControls();
        loadLevel(currentLevelIndex);
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
                startTime = System.currentTimeMillis(); // Reset timer
                updateUI();
            }
        });
    }

    private void loadLevel(int index) {
        if (levelFiles == null || levelFiles.isEmpty()) return;
        
        String fileName = levelFiles.get(index);
        
        // Format display name
        currentDisplayName = fileName.replace(".json", "")
                                     .replaceAll("\\s+", "")
                                     .replaceFirst("^0+(?!$)", "");
        levelTitleText.setText("Level " + currentDisplayName);

        currentState = repository.loadLevel(fileName);
        startTime = System.currentTimeMillis(); // Start timer
        updateUI();
    }

    private void handleMove(Direction direction) {
        if (currentState == null || GameEngine.isWin(currentState)) return;

        currentState = GameEngine.move(currentState, direction);
        updateUI();

        if (GameEngine.isWin(currentState)) {
            saveSolution(currentDisplayName);
            showWinDialog();
        }
    }

    private void saveSolution(String displayName) {
        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
        
        File dir = new File(SOLUTIONS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        File solutionFile = new File(dir, displayName + "_solution.json");
        try {
            JSONObject json = new JSONObject();
            json.put("moves count", currentState.getMovesCount());
            json.put("pushes count", currentState.getPushesCount());
            json.put("timetaken", timeTaken);
            json.put("sequence of moves", currentState.getMoveSequence());
            
            FileWriter writer = new FileWriter(solutionFile);
            writer.write(json.toString(4));
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
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
                loadLevel(currentLevelIndex);
            } else {
                // Completed all levels
                AlertDialog.Builder finishedBuilder = new AlertDialog.Builder(GameActivity.this);
                finishedBuilder.setTitle(R.string.congrats);
                finishedBuilder.setMessage("You have completed all available levels!");
                finishedBuilder.setPositiveButton("OK", (d, w) -> finish());
                finishedBuilder.show();
            }
        });

        builder.setNegativeButton("Close", (dialog, which) -> finish());
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
