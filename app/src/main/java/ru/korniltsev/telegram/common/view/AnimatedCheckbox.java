package ru.korniltsev.telegram.common.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import mortar.dagger1support.ObjectGraphService;
import ru.korniltsev.telegram.core.emoji.DpCalculator;

import javax.inject.Inject;

//todo make animations
public class AnimatedCheckbox extends View {
    static final int COLOR_ENABLED = 0xff9ebeda;
    static final int COLOR_DISABLED = 0xffbab8b8;

    static final int OVAL_COLOR_ENABLED = 0xff4a82b5;
    static final int OVAL_COLOR_DISABLED = 0xffededed;
    static final int RECT_HEIGHT = 14;//dp
    private final int rectHeight;
    private final int dip1;

    private int color;
    private float pos;//0 - disabled, 1 - enabled
    private boolean checked;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mOvalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mOvalShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();

    @Inject DpCalculator calc;
    private int colorForOval;
    private int colorForShadow;

    public AnimatedCheckbox(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        setWillNotDraw(false);

        rectHeight = calc.dp(RECT_HEIGHT);
        dip1 = calc.dp(1);

//        mOvalShadowPaint.setColor(0x40000000);
        mOvalShadowPaint.setColor(Color.BLACK);

        check(false, false);
    }

    public void check(boolean checked, boolean animated) {
//        if (!animated) {
            this.checked = checked;
            this.color = colorFor(checked);
            this.colorForOval = colorForOval(checked);
            this.colorForShadow = darken(colorForOval);
            mOvalPaint.setColor(colorForOval);
            mPaint.setColor(color);
            mOvalShadowPaint.setColor(colorForShadow);
            pos = checked ? 1f : 0f;
            invalidate();
//        } todo animate
    }
    float [] tmpRect = new float[3];
    private int darken(int colorForOval) {
        Color.colorToHSV(colorForOval, tmpRect);
        tmpRect[2] *= 0.5f;
        final int res = Color.HSVToColor(tmpRect);
        return 0x40ffffff & res;
    }

    private int colorForOval(boolean checked) {
        return checked ? OVAL_COLOR_ENABLED : OVAL_COLOR_DISABLED;
    }

    private int colorFor(boolean checked) {
        return checked ? COLOR_ENABLED : COLOR_DISABLED;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int height = getHeight();
        final int width = getWidth();
        int rectPadding = (height - rectHeight) / 2;
        mRect.set(0, rectPadding, width, height - rectPadding);
        mRect.left += dip1;
        mRect.right -= dip1;
        final int radius = height / 2;
        canvas.drawRoundRect(mRect, radius, radius, mPaint);



        int dx = width - height;
        int translationX = (int) (pos * dx);
        mRect.set(translationX, 0, translationX + height, height);//set square
        mRect.left += dip1/2;
        mRect.right -= dip1/2;
        mRect.top += dip1/2;
        canvas.drawOval(mRect, mOvalShadowPaint);

        mRect.set(translationX, 0, translationX + height, height);//set square
        mRect.left += dip1;
        mRect.right -= dip1;
        mRect.top += dip1;
        mRect.bottom -= dip1;
        canvas.drawOval(mRect, mOvalPaint);
    }

    public void toggle() {
        check(!checked, false);
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
