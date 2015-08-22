package ru.korniltsev.telegram.chat_list.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewGroup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.debug.SquareDumbResourceView;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.views.AvatarView;
import ru.korniltsev.telegram.core.views.RobotoMediumTextView;

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
    private final int cellPaddingRight;
    private final int timeTopPadding;

    public final SquareDumbResourceView iconTop;
    private final int iconTopSize;
    private final int iconTopTopPadding;
    private final int iconTopRightPadding;
    private final int titlePaddingLeftRight;
    private final int titlePaddingTop;
    private final TextPaint messagePaint;
    private final Drawable icUnreadBadge;

    DpCalculator calc;
    private TextPaint timePaint;
    private StaticLayout timeLayout;
    private float timeLeft;
    private TextPaint titlePaint;
    private StaticLayout titleLayout;
    private int unreadCount;
    private TextPaint unreadPaint;
    private StaticLayout unreadLayout;
    private float unreadTx;
    private float unreadTy;

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
        cellPaddingRight = calc.dp(15);
        timeTopPadding = calc.dp(18f);

        titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(calc.dpFloat(17));
        final Typeface typeface = RobotoMediumTextView.sGetTypeface(getContext());
        titlePaint.setTypeface(typeface);
        titlePaddingLeftRight = calc.dp(6f);
        titlePaddingTop = calc.dp(14f);

        messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        messagePaint.setColor(0xFF8A8A8A);

        icUnreadBadge = res.getDrawable(R.drawable.ic_badge);
        assertNotNull(icUnreadBadge);
        int right = displayWidth - cellPaddingRight;
        int left = right- icUnreadBadge.getIntrinsicWidth();
        int top = calc.dp(39);
        int bottom = top + icUnreadBadge.getIntrinsicHeight();
        icUnreadBadge.setBounds(left, top, right, bottom);


        unreadPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        unreadPaint.setTextSize(calc.dpFloat(14));
        unreadPaint.setColor(Color.WHITE);


        setWillNotDraw(false);
    }

    String time;


    // time
    // nick
    // unread
    // message
    //

    public void setTime(String time) {
        this.time = time;
        timeLayout = new StaticLayout(time, timePaint, displayWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        timeLeft = displayWidth - cellPaddingRight - timeLayout.getLineWidth(0);
        iconTop.layout(iconTopTopPadding, (int) (timeLeft - iconTopSize - iconTopRightPadding));
    }

    String title;

    public void setTitle(String title) {
        this.title = title;

        
        int spaceLeftForNick = (int) (displayWidth 
                        - avatarViewSize - avatarViewMargin * 2 -
                        titlePaddingLeftRight *2
                        - (displayWidth - timeLeft)
                        - icGroup.getIntrinsicWidth());
        final CharSequence ellipsized = TextUtils.ellipsize(title, titlePaint, spaceLeftForNick, TextUtils.TruncateAt.END);
        titleLayout = new StaticLayout(ellipsized, titlePaint, spaceLeftForNick, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
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



        canvas.save();
        canvas.translate(timeLeft, timeTopPadding);
        timeLayout.draw(canvas);
        canvas.restore();

        iconTop.draw(canvas);

        canvas.save();
        int dx = avatarViewSize + avatarViewMargin * 2 ;
        if (drawGroupChatIcon) {
            dx += icGroup.getIntrinsicWidth() + titlePaddingLeftRight;
        }

        canvas.translate(dx, titlePaddingTop);
        titleLayout.draw(canvas);
        canvas.restore();

        if (unreadCount > 0){
            icUnreadBadge.draw(canvas);

            canvas.save();
            canvas.translate(unreadTx, unreadTy);
            unreadLayout.draw(canvas);
            canvas.restore();
        }

    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
        if (unreadCount > 0){
            unreadLayout = new StaticLayout(String.valueOf(unreadCount), unreadPaint, icUnreadBadge.getIntrinsicWidth(), Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
            float px = (icUnreadBadge.getIntrinsicWidth() - unreadLayout.getLineWidth(0))/2;
            float py = (icUnreadBadge.getIntrinsicHeight() - unreadLayout.getHeight())/2;
            final Rect bounds = icUnreadBadge.getBounds();
            unreadTx = bounds.left + px;
            unreadTy = bounds.top + py;
        }
    }
}
