package com.sokoban.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

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
    private TextView timeText;
    private GameView gameView;
    private Button btnReplay;

    private boolean isReplaying = false;
    private boolean isReplayPaused = false;
    private Handler replayHandler;
    private String replaySequence;
    private int replayIndex;
    private int currentReplayInterval = 300;

    private int bestMoves = -1;
    private int bestPushes = -1;
    private long bestTime = -1;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentState != null && !GameEngine.isWin(currentState) && !isReplaying) {
                if (bestMoves < 0 || currentState.getMovesCount() > 0) {
                    long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
                    if (timeText != null) {
                        timeText.setText("Time: " + timeTaken + "s");
                    }
                }
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

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
        timeText = findViewById(R.id.timeText);
        gameView = findViewById(R.id.gameView);
        btnReplay = findViewById(R.id.btnReplay);

        setupControls();
        loadLevel(currentLevelIndex);
    }

    private void setupControls() {
        AndroidGameController controller = new AndroidGameController(this, this::handleMove);
        gameView.setOnTouchListener(controller);

        findViewById(R.id.btnUndo).setOnClickListener(v -> {
            if (isReplaying) {
                promptInterruptReplay(() -> {
                    stopReplay();
                    doUndo();
                });
            } else {
                doUndo();
            }
        });

        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            if (isReplaying) {
                promptInterruptReplay(() -> {
                    stopReplay();
                    doRestart();
                });
            } else {
                doRestart();
            }
        });

        btnReplay.setOnClickListener(v -> {
            startReplay();
        });
    }

    private void doUndo() {
        if (currentState != null) {
            currentState = GameEngine.undo(currentState);
            updateUI();
        }
    }

    private void doRestart() {
        if (currentState != null) {
            currentState = GameEngine.restart(currentState);
            startTime = System.currentTimeMillis();
            timerHandler.removeCallbacks(timerRunnable);
            timerHandler.post(timerRunnable);
            updateUI();
        }
    }

    private void loadLevel(int index) {
        stopReplay();
        if (levelFiles == null || levelFiles.isEmpty()) return;
        
        String fileName = levelFiles.get(index);
        
        currentDisplayName = fileName.replace(".json", "")
                                     .replaceAll("\\s+", "")
                                     .replaceFirst("^0+(?!$)", "");
        
        bestMoves = -1;
        bestPushes = -1;
        bestTime = -1;

        File solutionFile = new File(SOLUTIONS_DIR, currentDisplayName + "_solution.json");
        if (solutionFile.exists()) {
            levelTitleText.setText("Level " + currentDisplayName + " (Solved)");
            levelTitleText.setTextColor(Color.parseColor("#4CAF50")); // Green
            try {
                String content = new String(Files.readAllBytes(Paths.get(solutionFile.getAbsolutePath())));
                JSONObject json = new JSONObject(content);
                bestMoves = json.optInt("moves count", 0);
                bestPushes = json.optInt("pushes count", 0);
                bestTime = json.optLong("timetaken", 0);
                replaySequence = json.optString("sequence of moves", "");
                
                if (!replaySequence.isEmpty()) {
                    btnReplay.setVisibility(View.VISIBLE);
                } else {
                    btnReplay.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                btnReplay.setVisibility(View.GONE);
            }
        } else {
            levelTitleText.setText("Level " + currentDisplayName);
            levelTitleText.setTextColor(ContextCompat.getColor(this, R.color.accent));
            btnReplay.setVisibility(View.GONE);
        }

        currentState = repository.loadLevel(fileName);
        startTime = System.currentTimeMillis();
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
        updateUI();
    }

    private void startReplay() {
        if (replaySequence == null || replaySequence.isEmpty()) return;
        
        SharedPreferences prefs = getSharedPreferences("SokobanPrefs", Context.MODE_PRIVATE);
        currentReplayInterval = prefs.getInt("replay_interval", 300);
        
        isReplaying = true;
        isReplayPaused = false;
        currentState = GameEngine.restart(currentState);
        updateUI();
        replayIndex = 0;
        
        if (replayHandler == null) {
            replayHandler = new Handler(Looper.getMainLooper());
        }
        replayHandler.postDelayed(replayRunnable, currentReplayInterval);
    }

    private void stopReplay() {
        isReplaying = false;
        isReplayPaused = false;
        if (replayHandler != null) {
            replayHandler.removeCallbacks(replayRunnable);
        }
    }

    private final Runnable replayRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isReplaying) return;
            if (isReplayPaused) {
                replayHandler.postDelayed(this, currentReplayInterval);
                return;
            }
            if (replayIndex < replaySequence.length()) {
                char c = replaySequence.charAt(replayIndex++);
                Direction dir = getDirectionFromChar(c);
                if (dir != null) {
                    currentState = GameEngine.move(currentState, dir);
                    updateUI();
                }
                replayHandler.postDelayed(this, currentReplayInterval);
            } else {
                stopReplay();
            }
        }
    };

    private Direction getDirectionFromChar(char c) {
        c = Character.toLowerCase(c);
        if (c == 'u') return Direction.UP;
        if (c == 'd') return Direction.DOWN;
        if (c == 'l') return Direction.LEFT;
        if (c == 'r') return Direction.RIGHT;
        return null;
    }

    private void promptInterruptReplay(Runnable onConfirm) {
        if (isReplayPaused) return; // Already showing dialog
        isReplayPaused = true;
        new AlertDialog.Builder(this)
            .setTitle("Stop Replay?")
            .setMessage("Do you want to stop the replay and take over?")
            .setPositiveButton("Yes", (d, w) -> {
                isReplayPaused = false;
                onConfirm.run();
            })
            .setNegativeButton("No", (d, w) -> {
                isReplayPaused = false;
            })
            .setOnCancelListener(d -> isReplayPaused = false)
            .show();
    }

    private void handleMove(Direction direction) {
        if (isReplaying) {
            promptInterruptReplay(() -> {
                stopReplay();
                handleMoveInternal(direction);
            });
            return;
        }
        handleMoveInternal(direction);
    }

    private void handleMoveInternal(Direction direction) {
        if (currentState == null || GameEngine.isWin(currentState)) return;

        if (currentState.getMovesCount() == 0) {
            startTime = System.currentTimeMillis();
        }

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
            
            // Reload to update best stats
            if (!isReplaying) {
                // Read fresh json for bestStatsText updates if desired, but we can just wait for next load
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateUI() {
        if (currentState == null) return;
        gameView.setGameState(currentState);
        
        long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
        
        if (bestMoves >= 0 && currentState.getMovesCount() == 0 && !isReplaying) {
            // Show best stats when viewing a solved level before making a move
            movesText.setText(getString(R.string.moves_label, bestMoves));
            pushesText.setText(getString(R.string.pushes_label, bestPushes));
            if (timeText != null) timeText.setText("Time: " + bestTime + "s");
        } else {
            // Show current running stats (or replay stats)
            movesText.setText(getString(R.string.moves_label, currentState.getMovesCount()));
            pushesText.setText(getString(R.string.pushes_label, currentState.getPushesCount()));
            if (timeText != null) timeText.setText("Time: " + timeTaken + "s");
        }
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
