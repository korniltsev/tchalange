package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

public class ChatListCell extends RelativeLayout {
    private final int from;
    private final Paint p;
    DpCalculator calc;

    public ChatListCell(Context context, AttributeSet attrs) {
        super(context, attrs);
        calc = MyApp.from(context).calc;
        from = calc.dp(72);
        setWillNotDraw(false);
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xffd4d4d4);
    }

    Rect rect = new Rect();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        rect.set(from, getHeight()-1, r, getHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawRect(rect, p );
    }


}
