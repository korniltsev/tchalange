package ru.korniltsev.telegram.main.passcode.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import junit.framework.Assert;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import java.util.ArrayList;
import java.util.List;

public class PatternView extends View {
    public static final int DOT_COUNT = 3;
    private final DpCalculator calc;
    private final int radius;
    private final Paint dotWhitePaint;
    private final int touchRadius;
    private final Paint lineWhitePaint;
//    private RectF rectF = new RectF();
//    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float lastX;
    private float lastY;

    public PatternView(Context context, AttributeSet attrs) {
        super(context, attrs);
        calc = MyApp.from(context).calc;
//        paint.setColor(Color.BLACK);
        radius = calc.dp(6);

        dotWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotWhitePaint.setColor(Color.WHITE);

        lineWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineWhitePaint.setColor(Color.WHITE);
        lineWhitePaint.setStrokeWidth(calc.dpFloat(3) + 1f);
        lineWhitePaint.setStrokeCap(Paint.Cap.ROUND);

        for (int i =0; i < DOT_COUNT * DOT_COUNT; ++i) {
            points.add(new Point(i));
        }
        touchRadius = calc.dp(20f);
    }

    boolean pointerDown = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int actionMasked = event.getActionMasked();
        lastX = event.getX();
        lastY = event.getY();

        if (actionMasked == MotionEvent.ACTION_DOWN){
            for (int i = 0, pointsSize = points.size(); i < pointsSize; i++) {
                Point point = points.get(i);
                if (isIntersect(point)) {
                    selectedPoints.clear();
                    selectedPoints.add(point);
                    pointerDown = true;

                    invalidate();
                    return true;
                }
            }
        }
        if (actionMasked == MotionEvent.ACTION_MOVE){
            if (pointerDown){
                for (int i = 0, pointsSize = points.size(); i < pointsSize; i++) {
                    Point point = points.get(i);
                    if (isIntersect(point) && !(selectedPoints.contains(point))){
                        selectedPoints.add(point);
                    }
                    if (selectedPoints.size() == points.size()) {
                        pointerDown = false;
                        callback.selected(selectedPoints);
                    }
                }
                invalidate();
            }
        }
        if (actionMasked == MotionEvent.ACTION_UP){
            if (selectedPoints.size() > 1){
                callback.selected(selectedPoints);
            }
            pointerDown = false;
            invalidate();
            return true;
        }
        return false;
    }

    private boolean isIntersect(Point point) {
        return point.touchRect.intersects(lastX, lastY, lastX, lastY);
    }
    CallBack callback;

    public void setCallback(CallBack callback) {
        this.callback = callback;
    }

    public void clear() {
        selectedPoints.clear();
        invalidate();
    }

    public interface CallBack{
        void selected(List<Point> points);
    }

    final List<Point> selectedPoints = new ArrayList<>();

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        Assert.assertEquals(getWidth(), getHeight());

        int size = getWidth();
        int rowSize = size / DOT_COUNT;
        int pointNumber = 0;
        for (int i = 0; i < DOT_COUNT; ++i){
            for (int j = 0; j < DOT_COUNT; ++j) {
                final Point point = points.get(pointNumber);
                int centerY = rowSize * i + rowSize / 2;
                int centerX = rowSize * j + rowSize / 2;
                point.set(centerX, centerY);

                pointNumber++;
            }
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < selectedPoints.size()-1; i++) {
            final Point current = selectedPoints.get(i);
            final Point next = selectedPoints.get(i + 1);
            canvas.drawLine(current.cx, current.cy, next.cx, next.cy, lineWhitePaint);
        }

        if (!selectedPoints.isEmpty() && pointerDown){
            final Point lastPoint = selectedPoints.get(selectedPoints.size() - 1);
            canvas.drawLine(lastPoint.cx, lastPoint.cy, lastX, lastY, lineWhitePaint);
        }

        for (int i = 0, pointsSize = points.size(); i < pointsSize; i++) {
            Point point = points.get(i);
//            canvas.drawRect(point.touchRect, paint);
            canvas.drawRoundRect(point.radiusRect, radius, radius, dotWhitePaint);
        }
    }

    final List<Point> points = new ArrayList<>();

    public class Point {
        final int position;
        int cx;
        int cy;
        final RectF radiusRect = new RectF();
        final RectF touchRect = new RectF();

        Point(int position) {
            this.position = position;
        }

        public void set(int centerX, int centerY) {
            this.cx = centerX;
            this.cy = centerY;
            radiusRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
            touchRect.set(centerX - touchRadius, centerY - touchRadius, centerX + touchRadius, centerY + touchRadius);
        }
    }

    public List<Point> getSelectedPoints() {
        return selectedPoints;
    }
}
