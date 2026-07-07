package com.vypeensoft.sokoban.android;

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

import com.vypeensoft.sokoban.android.controller.AndroidGameController;
import com.vypeensoft.sokoban.android.repository.LevelRepository;
import com.vypeensoft.sokoban.android.view.GameView;
import com.vypeensoft.sokoban.engine.GameEngine;
import com.vypeensoft.sokoban.engine.model.Direction;
import com.vypeensoft.sokoban.engine.model.GameState;

import java.util.List;

public final class GameActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL_INDEX = "extra_level_index";

    private LevelRepository repository;
    private List<String> levelFiles;
    private int currentLevelIndex = 0;
    private GameState currentState;
    private long startTime;
    private String currentDisplayName;
    private String rawFileName;
    
    private static final String SOLUTIONS_DIR = "/sdcard/Vypeensoft/Sokoban/solutions/";

    private TextView levelTitleText;
    private TextView btnPrevLevel;
    private TextView btnNextLevel;
    private TextView movesText;
    private TextView pushesText;
    private TextView timeText;
    private GameView gameView;
    private Button btnReplay;
    private Button btnUndo;
    private TextView btnLike;
    private TextView btnDislike;
    
    private static final String LIKE_DISLIKE_DIR = "/sdcard/Vypeensoft/Sokoban/like_dislike/";
    private String currentLikeState = "neutral";

    private boolean isReplaying = false;
    private boolean isReplayPaused = false;
    private Handler replayHandler;
    private String replaySequence;
    private int replayIndex;
    private int currentReplayInterval = 300;

    private int bestMoves = -1;
    private int bestPushes = -1;
    private long bestTime = -1;

    private boolean isAutoMoving = false;
    private Handler autoMoveHandler;
    private List<Direction> autoMovePath;
    private int autoMoveIndex;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentState != null && !GameEngine.isWin(currentState) && !isReplaying) {
                if (currentState.getMovesCount() > 0) {
                    long timeTaken = (System.currentTimeMillis() - startTime) / 1000;
                    if (timeText != null) {
                        timeText.setText("Time: " + timeTaken + "s");
                    }
                } else if (bestMoves < 0) {
                    if (timeText != null) {
                        timeText.setText("Time: 0s");
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
        btnPrevLevel = findViewById(R.id.btnPrevLevel);
        btnNextLevel = findViewById(R.id.btnNextLevel);
        movesText = findViewById(R.id.movesText);
        pushesText = findViewById(R.id.pushesText);
        timeText = findViewById(R.id.timeText);
        gameView = findViewById(R.id.gameView);
        btnReplay = findViewById(R.id.btnReplay);
        btnUndo = findViewById(R.id.btnUndo);
        btnLike = findViewById(R.id.btnLike);
        btnDislike = findViewById(R.id.btnDislike);

        setupControls();
        loadLevel(currentLevelIndex);
    }

    private void setupControls() {
        AndroidGameController controller = new AndroidGameController(this, new AndroidGameController.OnMoveListener() {
            @Override
            public void onMove(Direction direction) {
                stopAutoMove();
                handleMove(direction);
            }
            @Override
            public void onTap(float x, float y) {
                if (!isReplaying) {
                    handleTap(x, y);
                }
            }
        });
        gameView.setOnTouchListener(controller);

        btnLike.setOnClickListener(v -> {
            if ("like".equals(currentLikeState)) {
                saveLikeDislike("neutral");
            } else {
                saveLikeDislike("like");
            }
        });
        btnDislike.setOnClickListener(v -> {
            if ("dislike".equals(currentLikeState)) {
                saveLikeDislike("neutral");
            } else {
                saveLikeDislike("dislike");
            }
        });

        btnUndo.setOnClickListener(v -> {
            stopAutoMove();
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
            stopAutoMove();
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
        
        btnPrevLevel.setOnClickListener(v -> {
            int prevIdx = findPrevValidIndex(currentLevelIndex);
            if (prevIdx != -1) {
                currentLevelIndex = prevIdx;
                loadLevel(currentLevelIndex);
            }
        });
        
        btnNextLevel.setOnClickListener(v -> {
            int nextIdx = findNextValidIndex(currentLevelIndex);
            if (nextIdx != -1) {
                currentLevelIndex = nextIdx;
                loadLevel(currentLevelIndex);
            }
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
        stopAutoMove();
        stopReplay();
        if (levelFiles == null || levelFiles.isEmpty()) return;
        
        btnPrevLevel.setVisibility(findPrevValidIndex(index) != -1 ? View.VISIBLE : View.INVISIBLE);
        btnNextLevel.setVisibility(findNextValidIndex(index) != -1 ? View.VISIBLE : View.INVISIBLE);
        
        String fileName = levelFiles.get(index);
        
        rawFileName = fileName.replace(".json", "");
        currentDisplayName = rawFileName
                                     .replaceAll("\\s+", "")
                                     .replaceFirst("^0+(?!$)", "");
        
        loadLikeDislikeState();
        
        bestMoves = -1;
        bestPushes = -1;
        bestTime = -1;

        File solutionFile = new File(SOLUTIONS_DIR, rawFileName + "_solution.json");
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

    private boolean isDisliked(int index) {
        String fileName = levelFiles.get(index);
        String rawName = fileName.replace(".json", "");
        File likeDislikeFile = new File(LIKE_DISLIKE_DIR, rawName + ".json");
        if (likeDislikeFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(likeDislikeFile.getAbsolutePath())));
                JSONObject json = new JSONObject(content);
                return "dislike".equals(json.optString("state", ""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean isCompleted(int index) {
        String fileName = levelFiles.get(index);
        String rawName = fileName.replace(".json", "");
        File solutionFile = new File(SOLUTIONS_DIR, rawName + "_solution.json");
        return solutionFile.exists();
    }

    private int findNextValidIndex(int currentIndex) {
        SharedPreferences prefs = getSharedPreferences("SokobanPrefs", Context.MODE_PRIVATE);
        boolean hideDisliked = prefs.getBoolean("hide_disliked", false);
        boolean showCompleted = prefs.getBoolean("show_completed", true);
        
        int nextIdx = currentIndex + 1;
        while (nextIdx < levelFiles.size()) {
            boolean shouldSkip = false;
            if (hideDisliked && isDisliked(nextIdx)) {
                shouldSkip = true;
            }
            if (!showCompleted && isCompleted(nextIdx)) {
                shouldSkip = true;
            }
            
            if (shouldSkip) {
                nextIdx++;
            } else {
                return nextIdx;
            }
        }
        return -1;
    }

    private int findPrevValidIndex(int currentIndex) {
        SharedPreferences prefs = getSharedPreferences("SokobanPrefs", Context.MODE_PRIVATE);
        boolean hideDisliked = prefs.getBoolean("hide_disliked", false);
        boolean showCompleted = prefs.getBoolean("show_completed", true);
        
        int prevIdx = currentIndex - 1;
        while (prevIdx >= 0) {
            boolean shouldSkip = false;
            if (hideDisliked && isDisliked(prevIdx)) {
                shouldSkip = true;
            }
            if (!showCompleted && isCompleted(prevIdx)) {
                shouldSkip = true;
            }
            
            if (shouldSkip) {
                prevIdx--;
            } else {
                return prevIdx;
            }
        }
        return -1;
    }

    private void loadLikeDislikeState() {
        btnLike.setAlpha(0.5f);
        btnDislike.setAlpha(0.5f);
        currentLikeState = "neutral";
        File dir = new File(LIKE_DISLIKE_DIR);
        if (!dir.exists()) dir.mkdirs();
        File f = new File(LIKE_DISLIKE_DIR, rawFileName + ".json");
        if (f.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
                JSONObject json = new JSONObject(content);
                String state = json.optString("state", "");
                if ("like".equals(state)) {
                    btnLike.setAlpha(1.0f);
                    currentLikeState = "like";
                } else if ("dislike".equals(state)) {
                    btnDislike.setAlpha(1.0f);
                    currentLikeState = "dislike";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveLikeDislike(String state) {
        currentLikeState = state;
        btnLike.setAlpha("like".equals(state) ? 1.0f : 0.5f);
        btnDislike.setAlpha("dislike".equals(state) ? 1.0f : 0.5f);
        try {
            File dir = new File(LIKE_DISLIKE_DIR);
            if (!dir.exists()) dir.mkdirs();
            File f = new File(LIKE_DISLIKE_DIR, rawFileName + ".json");
            
            if ("neutral".equals(state)) {
                if (f.exists()) f.delete();
                return;
            }
            
            JSONObject json = new JSONObject();
            json.put("state", state);
            if (currentState != null) {
                json.put("id", currentState.getId());
            }
            try (FileWriter file = new FileWriter(f)) {
                file.write(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    private void stopAutoMove() {
        isAutoMoving = false;
        if (autoMoveHandler != null) {
            autoMoveHandler.removeCallbacks(autoMoveRunnable);
        }
    }

    private void startAutoMove(List<Direction> path) {
        autoMovePath = path;
        autoMoveIndex = 0;
        isAutoMoving = true;
        if (autoMoveHandler == null) {
            autoMoveHandler = new Handler(Looper.getMainLooper());
        }
        autoMoveHandler.post(autoMoveRunnable);
    }

    private final Runnable autoMoveRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAutoMoving || currentState == null) return;
            if (autoMoveIndex < autoMovePath.size()) {
                Direction dir = autoMovePath.get(autoMoveIndex++);
                handleMoveInternal(dir);
                if (isAutoMoving) {
                    autoMoveHandler.postDelayed(this, 50);
                }
            } else {
                stopAutoMove();
            }
        }
    };

    private void handleTap(float x, float y) {
        if (currentState == null || GameEngine.isWin(currentState)) return;
        com.vypeensoft.sokoban.engine.model.Position target = gameView.getLogicalPosition(x, y);
        if (target != null) {
            List<Direction> path = GameEngine.findPath(currentState, target);
            if (path != null && !path.isEmpty()) {
                startAutoMove(path);
            }
        }
    }

    private Direction getMappedDirection(Direction visualDir) {
        if (currentState != null && currentState.getWidth() > currentState.getHeight()) {
            switch (visualDir) {
                case UP: return Direction.LEFT;
                case DOWN: return Direction.RIGHT;
                case LEFT: return Direction.DOWN;
                case RIGHT: return Direction.UP;
            }
        }
        return visualDir;
    }

    private void handleMove(Direction direction) {
        Direction logicalDir = getMappedDirection(direction);
        if (isReplaying) {
            promptInterruptReplay(() -> {
                stopReplay();
                handleMoveInternal(logicalDir);
            });
            return;
        }
        handleMoveInternal(logicalDir);
    }

    private void handleMoveInternal(Direction direction) {
        if (currentState == null || GameEngine.isWin(currentState)) return;

        if (currentState.getMovesCount() == 0) {
            startTime = System.currentTimeMillis();
        }

        currentState = GameEngine.move(currentState, direction);
        updateUI();

        if (GameEngine.isWin(currentState)) {
            saveSolution(rawFileName);
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
            
            if (timeText != null) {
                if (isReplaying && bestTime >= 0) {
                    timeText.setText("Time: " + bestTime + "s");
                } else if (currentState.getMovesCount() == 0 && bestMoves < 0) {
                    timeText.setText("Time: 0s");
                } else {
                    timeText.setText("Time: " + timeTaken + "s");
                }
            }
        }
        
        if (isReplaying || (bestMoves >= 0 && currentState.getMovesCount() == 0)) {
            btnUndo.setVisibility(View.GONE);
        } else {
            btnUndo.setVisibility(View.VISIBLE);
        }
    }

    private void showWinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.victory_title);
        builder.setMessage(getString(R.string.victory_message, 
            currentState.getMovesCount(), currentState.getPushesCount()));

        builder.setCancelable(false);
        builder.setPositiveButton(R.string.next_level_btn, (dialogInterface, which) -> {
            int nextIdx = findNextValidIndex(currentLevelIndex);
            if (nextIdx != -1) {
                currentLevelIndex = nextIdx;
                loadLevel(currentLevelIndex);
            } else {
                AlertDialog.Builder finishedBuilder = new AlertDialog.Builder(GameActivity.this);
                finishedBuilder.setTitle(R.string.congrats);
                finishedBuilder.setMessage("You have completed all available levels!");
                finishedBuilder.setPositiveButton("OK", (d, w) -> finish());
                AlertDialog finishedDialog = finishedBuilder.create();
                finishedDialog.show();
                styleDialogButtons(finishedDialog);
            }
        });

        builder.setNegativeButton("Close", (dialogInterface, which) -> finish());
        AlertDialog dialog = builder.create();
        dialog.show();
        styleDialogButtons(dialog);
    }

    private void styleDialogButtons(AlertDialog dialog) {
        android.widget.Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveBtn != null) {
            positiveBtn.setTextColor(Color.WHITE);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(Color.TRANSPARENT);
            gd.setCornerRadius(16);
            gd.setStroke(3, Color.WHITE);
            positiveBtn.setBackground(gd);
            
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) positiveBtn.getLayoutParams();
            if (params != null) {
                params.setMargins(16, 0, 16, 0);
                positiveBtn.setLayoutParams(params);
            }
        }
        
        if (negativeBtn != null) {
            negativeBtn.setTextColor(Color.WHITE);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(Color.TRANSPARENT);
            gd.setCornerRadius(16);
            gd.setStroke(3, Color.WHITE);
            negativeBtn.setBackground(gd);
            
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) negativeBtn.getLayoutParams();
            if (params != null) {
                params.setMargins(16, 0, 16, 0);
                negativeBtn.setLayoutParams(params);
            }
        }
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
            stopAutoMove();
            handleMove(dir);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
