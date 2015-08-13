//package ru.korniltsev.telegram.common.view;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Rect;
//import android.graphics.drawable.Drawable;
//import android.util.AttributeSet;
//import android.widget.LinearLayout;
//
//public class LinearLayoutWithShadow extends LinearLayout {
//
//    private final Drawable shadow;
//    private final int shadowHeight;
//
//    public LinearLayoutWithShadow(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        shadow = getResources().getDrawable(R.drawable.bottom_shadow);
//        shadowHeight = shadow.getIntrinsicHeight();
//        setWillNotDraw(false);
//
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//
//    }
//
//    int shadowPadding = 0;
//
//    public void setShadowPadding(int shadowPadding) {
//        this.shadowPadding = shadowPadding;
//        invalidate();
//    }
//
//    final Rect rect = new Rect();
//
//    @Override
//    public void draw(Canvas canvas) {
//        super.draw(canvas);
//        final int top = getTop() + shadowPadding;
//        final int bottom = top + shadowHeight;
//        rect.set(getLeft(), top, getRight(), bottom);
//        shadow.setBounds(rect);
//        shadow.draw(canvas);
//
//    }
//}
