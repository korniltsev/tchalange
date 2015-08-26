package ru.korniltsev.telegram.chat.debug;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.StaticLayoutCache;
import ru.korniltsev.telegram.core.utils.Colors;
import ru.korniltsev.telegram.core.views.AvatarView;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

public class CustomCeilLayout extends ViewGroup {
    public static final int STATE_IC_UNREAD = 0;
    public static final int STATE_IC_CLOCK = 1;
    public static final int STATE_IC_NULL = 2;

    //staff
    public/* @Inject */ DpCalculator calc;
    public/* @Inject */ StaticLayoutCache layoutCache;
    private final int screenWidth;
    private final int paddingTopBottom;
    private final int unspecifiedMeasureSpec;

    //avatarview
    public final AvatarView avatarView;
    private final int avatarSize;
    private final int avatarMeasureSpec;
    private final int avatarMarginLeft;
    private final int avatarMarginRight;

    //iconright
    private final int iconRightMarginRight;
    private final int iconRightSize;
    public final SquareDumbResourceView iconRight3;

    private String time;
    private StaticLayout timeLayout;
    private int timeWidth;
    private final int timePadding;

    //nick
    private String nick;
    private Layout nickLayout;
    private int nickWidth;
    private int nickHeight;
    private int nickRight;
    private int nickLeft;
    //content
    private View contentView;
    private int marginBetweenNickAndContentView;


    private static TextPaint nickPaint;
    private static TextPaint timePaint;


    boolean bottomMarginEnabled = true;
    private float nickLayoutFirstLineLeft;

    public CustomCeilLayout(Context ctx) {
        this(ctx, null);
    }

    public CustomCeilLayout(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        //todo all dp lazy
        //        ObjectGraphService.inject(ctx, this);

        final MyApp app = MyApp.from(ctx);
        screenWidth = app.displayWidth;
        calc = app.calc;
        layoutCache = app.staticLayoutCache;

        paddingTopBottom = calc.dp(8);
        unspecifiedMeasureSpec = makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        //avatar
        avatarSize = calc.dp(41);
        avatarMeasureSpec = makeMeasureSpec(avatarSize, EXACTLY);
        avatarView = new AvatarView(ctx, avatarSize, app);
        avatarView.setId(R.id.avatar);
        avatarMarginLeft = calc.dp(9);
        avatarMarginRight = calc.dp(11);
        addView(avatarView);

        //iconRight
        iconRightSize = calc.dp(12);
        iconRightMarginRight = calc.dp(15);

        final Drawable[] ds = new Drawable[3];
        final Resources res = getContext().getResources();
        iconRight3 = new SquareDumbResourceView(ds, this);
        ds[STATE_IC_UNREAD] = res.getDrawable(R.drawable.ic_unread);
        ds[STATE_IC_CLOCK] = res.getDrawable(R.drawable.ic_clock);
        ds[STATE_IC_NULL] = null;
        for (Drawable d : ds) {
            if (d != null) {
                d.setBounds(0, 0, iconRightSize, iconRightSize);
            }
        }

        //time
        timePadding = calc.dp(8);
        if (timePaint == null){
            timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(calc.dpFloat(14));
            timePaint.setColor(0xff939494);


            nickPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            nickPaint.setTextSize(calc.dpFloat(14));
            nickPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            nickPaint.setColor(Colors.USER_NAME_COLOR);
        }


        marginBetweenNickAndContentView = calc.dp(4);
        setWillNotDraw(false);
        AppUtils.rtlPerformanceFix(this);
    }

    public void addContentView(View v) {
        this.contentView = v;
        addView(contentView);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, int heightMeasureSpec) {
        //        long start = System.nanoTime();
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);

        avatarView.measure(avatarMeasureSpec, avatarMeasureSpec);

        int spaceLeftForContent = availableWidth - avatarSize - avatarMarginLeft - avatarMarginRight;
        int contentWidthSpec = makeMeasureSpec(spaceLeftForContent, EXACTLY);
        contentView.measure(contentWidthSpec, unspecifiedMeasureSpec);

        final int min = avatarSize + (bottomMarginEnabled ? paddingTopBottom * 2 : paddingTopBottom);
        int real = (bottomMarginEnabled ? paddingTopBottom * 2 : paddingTopBottom)
                + nickHeight
                + contentView.getMeasuredHeight()
                + marginBetweenNickAndContentView;
        setMeasuredDimension(availableWidth, Math.max(min, real));


    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        //        long start = System.nanoTime();
        final int avaLeft = l + avatarMarginLeft;
        final int avaTop = paddingTopBottom;
        final int avaRight = avaLeft + avatarSize;
        final int avaBottom = avaTop + avatarSize;
        avatarView.layout(avaLeft, avaTop, avaRight, avaBottom);

        nickLeft = avaRight + avatarMarginRight;
        nickRight = nickLeft + nickWidth;

        final int iconRightRight = r - iconRightMarginRight;
        final int iconRightTop = avaTop;
        final int iconRightLeft = iconRightRight - iconRightSize;
        iconRight3.layout(iconRightTop, iconRightLeft);

        final int contentLeft = nickLeft;
        final int contentTop = avaTop + nickHeight + marginBetweenNickAndContentView;
        final int contentRight = contentLeft + contentView.getMeasuredWidth();
        final int contentBottom = contentTop + contentView.getMeasuredHeight();
        contentView.layout(contentLeft, contentTop, contentRight, contentBottom);

        //        long end = System.nanoTime();
        //        DebugRelativeLayout.log(start, end, "total дфнщге");
    }

    public void setTime(@NonNull String time) {
        if (!time.equals(this.time)) {
            this.time = time;
            timeLayout = getStaticLayoutForTime(time);
            timeWidth = (int) (timeLayout.getLineWidth(0) + timePadding * 2);
        }
    }

    @NonNull
    private StaticLayout getStaticLayoutForTime(@NonNull String time) {
        final int width = 700;
        final StaticLayoutCache.Key key = new StaticLayoutCache.Key(time, width);
        final StaticLayout check = layoutCache.check(key);
        if (check != null) {
            return check;
        }
        final StaticLayout res = new StaticLayout(time, timePaint, width, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        layoutCache.put(key, res);
        return res;
    }

    public void setNick(@NonNull String nick) {
        if (!nick.equals(this.nick)) {
            this.nick = nick;
            final int spaceLeftForNick = screenWidth - avatarSize - avatarMarginLeft - avatarMarginRight
                    - timeWidth - iconRightSize - iconRightMarginRight;
            CharSequence str2 = TextUtils.ellipsize(nick, nickPaint, spaceLeftForNick, TextUtils.TruncateAt.END);
            this.nickLayout = getStaticLayoutForNick(spaceLeftForNick, str2);
            nickLayoutFirstLineLeft = nickLayout.getLineLeft(0);
            nickWidth = (int) nickLayout.getLineWidth(0);
            nickHeight = nickLayout.getHeight();
        }
    }

    @NonNull
    private StaticLayout getStaticLayoutForNick(int spaceLeftForNick, CharSequence str2) {
        final StaticLayoutCache.Key key = new StaticLayoutCache.Key(str2.toString(), spaceLeftForNick);

        StaticLayout fromCache = layoutCache.check(key);
        if (fromCache != null) {
            return fromCache;
        }
        final StaticLayout staticLayout = new StaticLayout(str2, nickPaint, spaceLeftForNick, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        layoutCache.put(key, staticLayout);
        return staticLayout;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(nickRight + timePadding, paddingTopBottom);
        timeLayout.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.translate(nickLeft - nickLayoutFirstLineLeft, paddingTopBottom);
        nickLayout.draw(canvas);
        canvas.restore();

        iconRight3.draw(canvas);
    }

    public void setBottomMarginEnabled(boolean bottomMarginEnabled) {
        this.bottomMarginEnabled = bottomMarginEnabled;
    }

    @Override
    public boolean canResolveLayoutDirection() {
        return false;
    }

    @Override
    public boolean isLayoutDirectionResolved() {
        return super.isLayoutDirectionResolved();
    }


}
