package com.sokoban.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.sokoban.engine.model.GameState;
import com.sokoban.engine.model.GridCell;

public final class GameView extends View {

    private GameState gameState;
    private float tileSize;
    private float offsetX;
    private float offsetY;

    private final Paint paintWall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintFloor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBox = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBoxOnGoal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGoal = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPlayer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGridLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintInnerDetail = new Paint(Paint.ANTI_ALIAS_FLAG);

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
        // Curated, premium color palette
        paintWall.setColor(0xFF2B2D42);        // Dark Charcoal / Slate
        paintFloor.setColor(0xFF3D3F58);       // Soft Slate
        paintBox.setColor(0xFFD88C51);         // Warm Wood Brown
        paintBoxOnGoal.setColor(0xFF4FAD6E);   // Emerald Green
        paintGoal.setColor(0xFFF4A261);        // Soft Amber/Orange
        paintPlayer.setColor(0xFF4CC9F0);      // Neon Blue
        
        paintGridLine.setColor(0xFF242636);
        paintGridLine.setStyle(Paint.Style.STROKE);
        paintGridLine.setStrokeWidth(2f);

        paintInnerDetail.setColor(0xFFFFFFFF);
        paintInnerDetail.setStyle(Paint.Style.STROKE);
        paintInnerDetail.setStrokeWidth(4f);
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
                if (cell != GridCell.WALL) {
                    canvas.drawRect(rect, paintFloor);
                    canvas.drawRect(rect, paintGridLine);
                }

                // 2. Draw specific cell graphics
                switch (cell) {
                    case WALL:
                        // Rounded corners for walls to look modern and premium
                        float cornerRadius = tileSize * 0.15f;
                        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paintWall);
                        break;

                    case GOAL:
                        drawGoal(canvas, left, top);
                        break;

                    case BOX:
                        drawBox(canvas, rect, paintBox);
                        break;

                    case BOX_ON_GOAL:
                        drawBox(canvas, rect, paintBoxOnGoal);
                        break;

                    case PLAYER:
                        drawPlayer(canvas, left, top);
                        break;

                    case PLAYER_ON_GOAL:
                        drawGoal(canvas, left, top);
                        drawPlayer(canvas, left, top);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private void drawGoal(Canvas canvas, float left, float top) {
        float cx = left + tileSize / 2f;
        float cy = top + tileSize / 2f;
        
        // Target outer ring
        paintGoal.setStyle(Paint.Style.STROKE);
        paintGoal.setStrokeWidth(tileSize * 0.05f);
        canvas.drawCircle(cx, cy, tileSize * 0.25f, paintGoal);

        // Center dot
        paintGoal.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, tileSize * 0.08f, paintGoal);
    }

    private void drawBox(Canvas canvas, RectF rect, Paint paint) {
        float padding = tileSize * 0.08f;
        RectF boxRect = new RectF(rect.left + padding, rect.top + padding, rect.right - padding, rect.bottom - padding);
        float boxRadius = tileSize * 0.12f;
        canvas.drawRoundRect(boxRect, boxRadius, boxRadius, paint);

        // Draw inner accent cross to signify a box
        float strokePadding = padding * 2;
        paintInnerDetail.setColor(0x55FFFFFF);
        paintInnerDetail.setStrokeWidth(tileSize * 0.06f);
        canvas.drawLine(rect.left + strokePadding, rect.top + strokePadding, rect.right - strokePadding, rect.bottom - strokePadding, paintInnerDetail);
        canvas.drawLine(rect.right - strokePadding, rect.top + strokePadding, rect.left + strokePadding, rect.bottom - strokePadding, paintInnerDetail);
    }

    private void drawPlayer(Canvas canvas, float left, float top) {
        float cx = left + tileSize / 2f;
        float cy = top + tileSize / 2f;
        float radius = tileSize * 0.35f;

        // Player body
        canvas.drawCircle(cx, cy, radius, paintPlayer);

        // Eyes for expression
        Paint paintEye = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintEye.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.1f, radius * 0.18f, paintEye);
        canvas.drawCircle(cx + radius * 0.3f, cy - radius * 0.1f, radius * 0.18f, paintEye);

        paintEye.setColor(0xFF1D1E2C);
        canvas.drawCircle(cx - radius * 0.3f, cy - radius * 0.1f, radius * 0.08f, paintEye);
        canvas.drawCircle(cx + radius * 0.3f, cy - radius * 0.1f, radius * 0.08f, paintEye);
    }
}
