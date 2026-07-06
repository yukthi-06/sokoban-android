package com.sokoban.android.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.sokoban.android.R;
import com.sokoban.engine.model.GameState;
import com.sokoban.engine.model.GridCell;

public final class GameView extends View {

    private GameState gameState;
    private float tileSize;
    private float offsetX;
    private float offsetY;

    private Bitmap bmpWall;
    private Bitmap bmpPath;
    private Bitmap bmpBox;
    private Bitmap bmpBoxOnGoal;
    private Bitmap bmpGoal;
    private Bitmap bmpPlayer;
    private Bitmap bmpPlayerOnGoal;
    private Bitmap bmpBlank;
    private final Paint paintBitmap = new Paint(Paint.FILTER_BITMAP_FLAG);

    public GameView(Context context) {
        super(context);
        init();
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        bmpWall = BitmapFactory.decodeResource(getResources(), R.drawable.wall);
        bmpPath = BitmapFactory.decodeResource(getResources(), R.drawable.path);
        bmpBox = BitmapFactory.decodeResource(getResources(), R.drawable.box);
        bmpBoxOnGoal = BitmapFactory.decodeResource(getResources(), R.drawable.box_in_destination);
        bmpGoal = BitmapFactory.decodeResource(getResources(), R.drawable.destination_unfilled);
        bmpPlayer = BitmapFactory.decodeResource(getResources(), R.drawable.pusher);
        bmpPlayerOnGoal = BitmapFactory.decodeResource(getResources(), R.drawable.pusher_on_goal);
        bmpBlank = BitmapFactory.decodeResource(getResources(), R.drawable.blank);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gameState == null) return;

        int cols = gameState.getWidth();
        int rows = gameState.getHeight();

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Calculate tile size to fit layout perfectly
        float scaleX = viewWidth / cols;
        float scaleY = viewHeight / rows;
        tileSize = Math.min(scaleX, scaleY);

        // Center the grid on the view
        offsetX = (viewWidth - (cols * tileSize)) / 2f;
        offsetY = (viewHeight - (rows * tileSize)) / 2f;

        RectF rect = new RectF();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float left = offsetX + c * tileSize;
                float top = offsetY + r * tileSize;
                float right = left + tileSize;
                float bottom = top + tileSize;
                rect.set(left, top, right, bottom);

                GridCell cell = gameState.getCell(r, c);

                // 1. Draw floor as base
                canvas.drawBitmap(bmpPath, null, rect, paintBitmap);

                // 2. Draw specific cell graphics
                switch (cell) {
                    case WALL:
                        canvas.drawBitmap(bmpWall, null, rect, paintBitmap);
                        break;
                    case GOAL:
                        canvas.drawBitmap(bmpGoal, null, rect, paintBitmap);
                        break;
                    case BOX:
                        canvas.drawBitmap(bmpBox, null, rect, paintBitmap);
                        break;
                    case BOX_ON_GOAL:
                        canvas.drawBitmap(bmpBoxOnGoal, null, rect, paintBitmap);
                        break;
                    case PLAYER:
                        canvas.drawBitmap(bmpPlayer, null, rect, paintBitmap);
                        break;
                    case PLAYER_ON_GOAL:
                        canvas.drawBitmap(bmpPlayerOnGoal, null, rect, paintBitmap);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
