package ru.korniltsev.telegram.audio;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import flow.Flow;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.audio.helper.SimpleImageButtonView;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.views.RobotoMediumTextView;
import ru.korniltsev.telegram.utils.R;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

import static android.text.TextUtils.ellipsize;
import static android.text.TextUtils.isEmpty;
import static android.view.View.MeasureSpec.makeMeasureSpec;
import static rx.Observable.timer;

public class MiniPlayerView extends ViewGroup {

    public static final int PAUSE = 0;
    public static final int PLAY = 1;
    private final AudioPLayer audioPLayer;
    private final DpCalculator calc;
    private final int dp1point5;
    private final int spaceForSongName;
    private final TextPaint textPaint;
    private final int leftRightButtonWidth;
    private final int textpadding;
    private Subscription subscription;
    private SimpleImageButtonView btnPlay;
    private SimpleImageButtonView btnStop;
    //    private TextView title;
    @Nullable private LinearLayoutWithShadow shadow;
    private float progress;
    private Paint paint;
    private int dp;
    private StaticLayout songNameLayout;
    private int songNameLayoutHeight;

    public MiniPlayerView(Context ctx) {
        super(ctx);
        final MyApp from = MyApp.from(ctx);
        audioPLayer = from.audioPLayer;
        calc = from.calc;
        setWillNotDraw(false);
        dp1point5 = calc.dp(1.5f);

        btnPlay = new SimpleImageButtonView(ctx);
        btnPlay.setBackgroundResource(R.drawable.bg_keyboard_tab);
        addView(btnPlay);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(calc.dp(14f));
        textPaint.setColor(0xFF333333);

        btnStop = new SimpleImageButtonView(ctx);
        btnStop.setBackgroundResource(R.drawable.bg_keyboard_tab);
        setBackgroundResource(R.drawable.bg_keyboard_tab);
        addView(btnStop);

        leftRightButtonWidth = calc.dp(61);
        textpadding = calc.dp(4);
        spaceForSongName = from.displayWidth - leftRightButtonWidth * 2 - textpadding * 2;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        final Resources res = getResources();
        //        title = (TextView) findViewById(R.id.text);
        btnPlay.setDs(new Drawable[]{
                res.getDrawable(R.drawable.ic_pausepl),
                res.getDrawable(R.drawable.ic_playpl)
        });
        btnStop.setDs(new Drawable[]{res.getDrawable(R.drawable.ic_closeplayer)});
        btnStop.setCurrent(0);

        btnStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPLayer.stop();
            }
        });
        btnPlay.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (audioPLayer.isPLaying()) {
                    audioPLayer.pause();
                } else {
                    audioPLayer.resume();
                }
            }
        });
        final boolean pLaying = audioPLayer.isPLaying();
        final boolean paused = audioPLayer.isPaused();
        final TdApi.Audio currentAudio = audioPLayer.getCurrentAudio();
        setState(pLaying, paused, currentAudio);

        paint = new Paint();
        paint.setColor(0xFF66ACDF);
        dp = calc.dp(16);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Flow.get(getContext())
                        .set(new AudioPlayerPath());
            }
        });
    }

    final Rect r = new Rect();

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        int bottom = getBottom();
        int top2 = getBottom() - getTop() - dp1point5;

        //        int top = getHeight()/2 + dp;
        int left = 0;
        int right = (int) (getWidth() * progress);
        r.set(left, top2, right, bottom);
        canvas.drawRect(r, paint);
        Log.d("MiniPlayerView", "draw " + progress);

        if (songNameLayout != null) {
            canvas.save();
            int ty = (getHeight() - this.songNameLayoutHeight) / 2;
            final int tx = leftRightButtonWidth + textpadding;
            canvas.translate(tx, ty);
            songNameLayout.draw(canvas);
            canvas.restore();
        }
    }

    Subscription timerSubscription = Subscriptions.empty();

    public void setState(boolean playing, boolean paused, TdApi.Audio currentAudio) {
        Log.d("MiniPlayerView", "progress");
        timerSubscription.unsubscribe();
        updateProgress();
        if (playing || paused) {
            setVisibility(View.VISIBLE);
            setText(getTitle(currentAudio));
            if (playing) {
                btnPlay.setCurrent(PAUSE);
                Log.d("MiniPlayerView", "subscribe");
                timerSubscription = timer(0, 1, TimeUnit.SECONDS)
                        .onBackpressureDrop()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new ObserverAdapter<Long>() {
                            @Override
                            public void onNext(Long response) {
                                updateProgress();
                            }
                        });
            } else {
                btnPlay.setCurrent(PLAY);
            }
            updateShadowState(true);
        } else {
            setVisibility(View.GONE);
            updateShadowState(false);
        }
    }

    private void setText(CharSequence title) {
        final CharSequence ellipsized = ellipsize(title, textPaint, spaceForSongName, TextUtils.TruncateAt.END);
        songNameLayout = new StaticLayout(ellipsized, textPaint, spaceForSongName, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        songNameLayoutHeight = songNameLayout.getHeight();
    }

    private void updateProgress() {
        progress = audioPLayer.getProgress();
        invalidate();
    }

    @NonNull
    private CharSequence getTitle(TdApi.Audio currentAudio) {
        final String performer = currentAudio.performer;
        final String title = currentAudio.title;
        final boolean performerEmpty = isEmpty(performer);
        final boolean titleEmpty = isEmpty(title);
        if (performerEmpty && titleEmpty) {
            if (isEmpty(currentAudio.fileName)) {
                return "Unknown audio";
            } else {
                return currentAudio.fileName;
            }
        } else {
            if (performerEmpty) {
                return title;
            } else if (titleEmpty) {
                return performer;
            } else {
                final SpannableStringBuilder sb = new SpannableStringBuilder();
                final SpannableString performerMedium = new SpannableString(performer);
                final Typeface typeface = RobotoMediumTextView.sGetTypeface(getContext());
                final MyTypefaceSpan myTypefaceSpan = new MyTypefaceSpan(typeface);
                performerMedium.setSpan(myTypefaceSpan, 0, performerMedium.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.append(performerMedium).append(" - ").append(currentAudio.title);
                return sb;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        subscription = audioPLayer.currentState()
                .subscribe(new ObserverAdapter<AudioPLayer.State>() {
                    @Override
                    public void onNext(AudioPLayer.State response) {
                        final boolean paused = response instanceof AudioPLayer.StatePaused;
                        final boolean playing = response instanceof AudioPLayer.StatePlaying;
                        setState(playing, paused, response.audio);
                    }
                });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        final int btnWidth = makeMeasureSpec(leftRightButtonWidth, MeasureSpec.EXACTLY);
        btnPlay.measure(btnWidth, heightMeasureSpec);
        btnStop.measure(btnWidth, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        btnPlay.layout(0, 0, leftRightButtonWidth, getHeight());
        int left = getWidth() - leftRightButtonWidth;
        btnStop.layout(left, 0, getWidth(), getHeight());
    }

    public void setShadow(LinearLayoutWithShadow toolbarShadow) {
        this.shadow = toolbarShadow;
        updateShadowState(getVisibility() == View.VISIBLE);
    }

    private void updateShadowState(boolean playerVisible) {
        if (shadow == null) {
            return;
        }
        if (playerVisible) {
            shadow.setShadowPadding(calc.dp(35f));
        } else {
            shadow.setShadowPadding(0);
        }
    }
}
