package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.ViewGroup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.debug.SquareDumbResourceView;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.views.AvatarView;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static junit.framework.Assert.assertNotNull;

public class ChatListCell extends ViewGroup {

    public static final int STATE_IC_UNREAD = 0;
    public static final int STATE_IC_CLOCK = 1;
    public static final int STATE_IC_NULL = 2;

    private final int dividerStart;
    private final Paint dividerPaint;
    public final AvatarView avatarView;
    private final int dip72;
    private final int avatarViewMargin;
    private final int avatarViewSize;
    private final Drawable icGroup;
    private final int displayWidth;
    private final int timeRightPadding;
    private final int timeTopPadding;

    public final SquareDumbResourceView iconTop;
    private final int iconTopSize;
    private final int iconTopTopPadding;
    private final int iconTopRightPadding;

    DpCalculator calc;
    private TextPaint timePaint;
    private StaticLayout timeLayout;
    private float timeLeft;
    private TextPaint titlePaint;

    public ChatListCell(Context context, AttributeSet a) {
        super(context, a);
        final MyApp app = MyApp.from(context);
        calc = app.calc;
        dip72 = calc.dp(72);
        this.dividerStart = dip72;
        displayWidth = app.displayWidth;
        dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(0xffd4d4d4);

        avatarViewMargin = calc.dp(10f);
        avatarViewSize = calc.dp(52f);
        avatarView = new AvatarView(context, avatarViewSize, app);
        addView(avatarView);

        final Resources res = getResources();
        icGroup = res.getDrawable(R.drawable.ic_group);
        assertNotNull(icGroup);
        layoutIconGroup();

        Drawable[] ds = new Drawable[3];
        iconTop = new SquareDumbResourceView(ds, this);
        iconTopSize = calc.dp(12f);
        ds[STATE_IC_UNREAD] = res.getDrawable(R.drawable.ic_unread);
        ds[STATE_IC_CLOCK] = res.getDrawable(R.drawable.ic_clock);
        ds[STATE_IC_NULL] = null;
        for (Drawable d : ds) {
            if (d != null) {
                d.setBounds(0, 0, iconTopSize, iconTopSize);
            }
        }
        iconTopTopPadding = calc.dp(18f);
        iconTopRightPadding = calc.dp(6f);

        timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setColor(0xFF999999);
        timePaint.setTextSize(calc.dpFloat(13));
        timeRightPadding = calc.dp(15);
        timeTopPadding = calc.dp(18f);

        titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        setWillNotDraw(false);
    }

    String time;

    public void setTime(String time) {
        this.time = time;
        timeLayout = new StaticLayout(time, timePaint, displayWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
    }

    String title;

    public void setTitle(String title) {
        this.title = title;

        //        int widthWithoutAvatar = displayWidth - avatarViewSize - avatarViewMargin * 2;
        //        int titleMaxWidth = widthWithoutAvatar - icGroup.getIntrinsicWidth() - timeLayout.getLineWidth(0);
        //        titleLayout = new StaticLayout(time, titlePaint, titleMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
    }

    private void layoutIconGroup() {
        int left = avatarViewSize + avatarViewMargin * 2;
        int right = left + icGroup.getIntrinsicWidth();
        int bottom = calc.dp(31f);//top + icGroup.getIntrinsicHeight();
        int top = bottom - icGroup.getIntrinsicHeight();
        icGroup.setBounds(left, top, right, bottom);
    }

    Rect rect = new Rect();

    private boolean drawGroupChatIcon;

    public void setDrawGroupChatIcon(boolean drawGroupChatIcon) {
        this.drawGroupChatIcon = drawGroupChatIcon;
        invalidate();
    }

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

        timeLeft = getWidth() - timeRightPadding - timeLayout.getLineWidth(0);

        iconTop.layout(iconTopTopPadding, (int) (timeLeft - iconTopSize - iconTopRightPadding));

        rect.set(dividerStart, getHeight() - 1, r, getHeight());
    }

    private void layoutAvatar() {
        int left = avatarViewMargin;
        int top = avatarViewMargin;
        int right = left + avatarViewSize;
        int bottom = top + avatarViewSize;
        avatarView.layout(left, top, right, bottom);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawRect(rect, dividerPaint);
        if (drawGroupChatIcon) {
            icGroup.draw(canvas);
        }

        //        float timeLeft = (int) (timeRightPadding - timeLayout.getLineWidth(0));

        canvas.save();
        canvas.translate(timeLeft, timeTopPadding);
        timeLayout.draw(canvas);
        canvas.restore();

        iconTop.draw(canvas);
    }
}
