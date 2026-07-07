package com.vypeensoft.sokoban.android.controller;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import com.vypeensoft.sokoban.engine.model.Direction;

public final class AndroidGameController implements View.OnTouchListener {

    public interface OnMoveListener {
        void onMove(Direction direction);
        void onTap(float x, float y);
    }

    private final GestureDetector gestureDetector;
    private final OnMoveListener moveListener;

    public AndroidGameController(Context context, OnMoveListener moveListener) {
        this.moveListener = moveListener;
        this.gestureDetector = new GestureDetector(context, new SwipeGestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        return gestureDetector.onTouchEvent(event);
    }

    private final class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 80;
        private static final int SWIPE_VELOCITY_THRESHOLD = 80;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            moveListener.onTap(e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1 == null || e2 == null) return false;
            
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        moveListener.onMove(Direction.RIGHT);
                    } else {
                        moveListener.onMove(Direction.LEFT);
                    }
                    return true;
                }
            } else {
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        moveListener.onMove(Direction.DOWN);
                    } else {
                        moveListener.onMove(Direction.UP);
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
