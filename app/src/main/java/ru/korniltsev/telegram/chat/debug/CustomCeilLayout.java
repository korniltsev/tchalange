package ru.korniltsev.telegram.chat.debug;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import org.drinkless.td.libcore.telegram.TdApi;
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
    public final DpCalculator calc;
    public final StaticLayoutCache layoutCache;
    private final int screenWidth;
    private static int paddingTopBottom;
    private static int unspecifiedMeasureSpec;

    //avatarview
    public final AvatarView avatarView;
    private static int avatarMeasureSpec;
    private static int avatarSize;
    private static int avatarMarginLeft;
    private static int avatarMarginRight;

    //iconright
    private static int iconRightMarginRight;
    private static int iconRightSize;
    public final SquareDumbResourceView iconRight3;

    private StaticLayout timeLayout;
    private int timeWidth;
    private static int timePadding;

    //nick
    private Layout nickLayout;
    private int nickWidth;
    private int nickHeight;
    private int nickRight;
    private int nickLeft;
    //content
    private View contentView;
    private static int marginBetweenNickAndContentView;

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
        initPaints(calc);





        //avatar

        avatarView = new AvatarView(ctx, avatarSize, app.rxGlide);
        avatarView.setId(R.id.avatar);
        addView(avatarView);

        //iconRight


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




        setWillNotDraw(false);

    }

    public static synchronized void initPaints(DpCalculator calc) {
        if (timePaint == null) {
            timePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(calc.dpFloat(14));
            timePaint.setColor(0xff939494);

            nickPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            nickPaint.setTextSize(calc.dpFloat(14));
            nickPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            nickPaint.setColor(Colors.USER_NAME_COLOR);

            timePadding = calc.dp(8);

            avatarSize = calc.dp(41);
            avatarMeasureSpec = makeMeasureSpec(avatarSize, EXACTLY);
            avatarMarginLeft = calc.dp(9);
            avatarMarginRight = calc.dp(11);

            iconRightSize = calc.dp(12);
            iconRightMarginRight = calc.dp(15);
            paddingTopBottom = calc.dp(8);
            marginBetweenNickAndContentView = calc.dp(4);


            unspecifiedMeasureSpec = makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
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

    public void setTime(@NonNull TdApi.Message msg) {
        timeLayout = getStaticLayoutForTime(layoutCache, msg);
        timeWidth = getTimeWidth(timeLayout);
    }

    public static int getTimeWidth(StaticLayout timeLayout) {
        return (int) (timeLayout.getLineWidth(0) + timePadding * 2);
    }

    @NonNull
    public static StaticLayout getStaticLayoutForTime(StaticLayoutCache layoutCache, TdApi.Message msg) {
        final int width = 700;
        return layoutCache.getLayout(width, timePaint, msg.dateFormatted);
        //        final StaticLayoutCache.Key key = new StaticLayoutCache.Key(time, width);
        //        final StaticLayout check = layoutCache.check(key);
        //        if (check != null) {
        //            return check;
        //        }
        //        final StaticLayout res = new StaticLayout(time, timePaint, width, Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
        //        layoutCache.put(key, res);
        //        return res;
    }

    public void setNick(@Nullable TdApi.User user) {
        String nick;
        if (user == null){
            nick = "";
        } else {
            if (user.nullableUiName != null) {
                nick = user.nullableUiName;
            } else {
                nick = AppUtils.uiName(user, getContext());
            }
        }

        final int spaceLeftForNick = getSpaceLeftForNick(this.screenWidth, this.timeWidth);
        CharSequence str2 = getEllipsizedNick(nick, spaceLeftForNick);
        this.nickLayout = getStaticLayoutForNick(spaceLeftForNick, str2, layoutCache);
        nickLayoutFirstLineLeft = nickLayout.getLineLeft(0);
        nickWidth = (int) nickLayout.getLineWidth(0);
        nickHeight = nickLayout.getHeight();
    }

    public static CharSequence getEllipsizedNick(@NonNull String nick, int spaceLeftForNick) {
        return TextUtils.ellipsize(nick, nickPaint, spaceLeftForNick, TextUtils.TruncateAt.END);
    }

    public static  int getSpaceLeftForNick(int screenWidth, int timeWidth) {
        return screenWidth - avatarSize - avatarMarginLeft - avatarMarginRight
                - timeWidth - iconRightSize - iconRightMarginRight;
    }

    @NonNull
    public static StaticLayout getStaticLayoutForNick(int spaceLeftForNick, CharSequence str2, StaticLayoutCache layoutCache) {
        return layoutCache.getLayout(spaceLeftForNick, nickPaint, str2);
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

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

}
