package ru.korniltsev.telegram.main.passcode.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import junit.framework.Assert;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import java.util.ArrayList;
import java.util.List;

public class GestureView extends View {

    private final DpCalculator calc;
    private final Paint paint;
    private final Path path;

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        calc = MyApp.from(context).calc;
        setWillNotDraw(true);
//        setBackgroundColor(Color.RED);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(calc.dpFloat(5) + 1f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        path = new Path();
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int actionMasked = event.getActionMasked();
        final float x = event.getX();
        final float y = event.getY();
        final Point p = new Point(x, y);
        if (actionMasked == MotionEvent.ACTION_DOWN){
            ps.clear();

            path.reset();
        }


        if (ps.isEmpty()){
            path.moveTo(x, y);
        } else {
            path.lineTo(x,y);
        }
        ps.add(p);
        if (actionMasked == MotionEvent.ACTION_UP){
//            visualizeGesture();
            cb.gestureSelected(new Gesture(getWidth(), getHeight(), new ArrayList<>(ps)));
        }

        invalidate();
        return true;
    }

    protected void costLeven(List<Point>a, List<Point> b){
//
//        // point
//        if (a[0]==-1){
//            return b.length==0 ? 0 : 100000;
//        }
//
//        // precalc difangles
//        var d:Array=fill2DTable(a.length+1,b.length+1,0);
//        var w:Array=d.slice();
//
//        for (var x:uint=1;x<=a.length;x++){
//            for (var y:uint=1;y<b.length;y++){
//                d[x][y]=difAngle(a[x-1],b[y-1]);
//            }
//        }
//
//        // max cost
//        for (y=1;y<=b.length;y++)w[0][y]=100000;
//        for (x=1;x<=a.length;x++)w[x][0]=100000;
//        w[0][0]=0;
//
//        // levensthein application
//        var cost:uint=0;
//        var pa:uint;
//        var pb:uint;
//        var pc:uint;
//
//        for (x=1;x<=a.length;x++){
//            for (y=1;y<b.length;y++){
//                cost=d[x][y];
//                pa=w[x-1][y]+cost;
//                pb=w[x][y-1]+cost;
//                pc=w[x-1][y-1]+cost;
//                w[x][y]=Math.min(Math.min(pa,pb),pc)
//            }
//        }
//
//        return w[x-1][y-1];
    }

//    private void visualizeGesture() {
//
//    }

    public class Gesture {
        public final int width;
        public final int height;
        public final List<Point> ps;

        public Gesture(int width, int height, List<Point> ps) {
            this.width = width;
            this.height = height;
            this.ps = ps;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        super.layout(l, t, r, b);
        Assert.assertEquals(getWidth(), getHeight());
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(path, paint);

    }

    final List<Point> ps = new ArrayList<>();

    public  class Point {
        final float x ;
        final float y ;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    public void clear() {
        ps.clear();
        path.reset();
        invalidate();
    }
    Callback cb ;

    public void setCb(Callback cb) {
        this.cb = cb;
    }

    public interface Callback {
        void gestureSelected(Gesture g);
    }
}
