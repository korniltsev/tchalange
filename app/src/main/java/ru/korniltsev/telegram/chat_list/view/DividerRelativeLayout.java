//package ru.korniltsev.telegram.chat_list.view;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Paint;
//import android.graphics.Rect;
//import android.util.AttributeSet;
//import android.widget.RelativeLayout;
//import mortar.dagger1support.ObjectGraphService;
//import ru.korniltsev.telegram.core.emoji.DpCalculator;
//
//import javax.inject.Inject;
//
//@Deprecated//todo remove with itemDecorator
//public class DividerRelativeLayout extends RelativeLayout {
//    private final int from;
//    private final Paint p;
//    @Inject DpCalculator calc;
//
//    public DividerRelativeLayout(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        ObjectGraphService.inject(context, this);
//        from = calc.dp(72);
//        setWillNotDraw(false);
//        p = new Paint(Paint.ANTI_ALIAS_FLAG);
//        p.setColor(0xffd4d4d4);
//    }
//
//    Rect rect = new Rect();
//
//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        super.onLayout(changed, l, t, r, b);
//        rect.set(from, b - 1, r, b);
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        canvas.drawRect(rect, p );
//    }
//}
