package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.views.AvatarView;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

public class ChatListCell extends ViewGroup {
    private final int dividerStart;
    private final Paint p;
    public final AvatarView avatarView;
    private final int dip72;
    private final int avatarViewMargin;
    private final int avatarViewSize;
    DpCalculator calc;

    public ChatListCell(Context context, AttributeSet a) {
        super(context, a);
        final MyApp app = MyApp.from(context);
        calc = app.calc;
        dip72 = calc.dp(72);
        this.dividerStart = dip72;
        setWillNotDraw(false);
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xffd4d4d4);

        avatarViewMargin = calc.dp(10f);
        avatarViewSize = calc.dp(52f);
        avatarView = new AvatarView(context, avatarViewSize, app);
        addView(avatarView);
    }

    Rect rect = new Rect();

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int avatarSpec = makeMeasureSpec(avatarViewSize, EXACTLY);
        avatarView.measure(avatarSpec, avatarSpec);

        final int w = getSize(widthMeasureSpec);
        setMeasuredDimension(w, dip72);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutAvatar();
        rect.set(dividerStart, getHeight() - 1, r, getHeight());
    }

    private void layoutAvatar() {
        int left = avatarViewMargin;
        int top = avatarViewMargin;
        int right = left + avatarViewSize;
        int bottom = top + avatarViewSize;
        avatarView.layout(left, top, right,  bottom);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawRect(rect, p );
    }


}
