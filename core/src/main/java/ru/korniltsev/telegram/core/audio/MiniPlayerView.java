package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.views.RobotoMediumTextView;
import ru.korniltsev.telegram.utils.R;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

import static android.text.TextUtils.isEmpty;
import static rx.Observable.timer;

public class MiniPlayerView extends LinearLayout {

    private final AudioPLayer audioPLayer;
    private final DpCalculator calc;
    private final int dp1point5;
    private Subscription subscription;
    private ImageButton btnPlay;
    private ImageButton btnStop;
    private TextView title;
    @Nullable private View shadow;
    private float progress;
    private Paint paint;

    public MiniPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final MyApp from = MyApp.from(context);
        audioPLayer = from.audioPLayer;
        calc = from.dpCalculator;
        setWillNotDraw(false);
        dp1point5 = calc.dp(1.5f);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        title = (TextView) findViewById(R.id.text);
        btnPlay = ((ImageButton) findViewById(R.id.btn_play));
        btnStop = ((ImageButton) findViewById(R.id.btn_stop));
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
    }

    final Rect r = new Rect();
    @Override
    protected void onDraw(Canvas canvas) {
        int bottom = getBottom();
        int top = bottom - dp1point5;
        int left = 0;
        int right = (int) (getWidth() * progress);
        r.set(left, top, right, bottom);
        canvas.drawRect(r, paint);
    }

    Subscription timerSubscription = Subscriptions.empty();



    public void setState(boolean playing, boolean paused, TdApi.Audio currentAudio) {
        timerSubscription.unsubscribe();
        updateProgress();
        if (playing || paused) {
            setVisibility(View.VISIBLE);
            title.setText(getTitle(currentAudio));
            if (playing) {
                btnPlay.setImageResource(R.drawable.ic_pausepl);
                timerSubscription = timer(0, 1, TimeUnit.SECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new ObserverAdapter<Long>() {
                            @Override
                            public void onNext(Long response) {
                                updateProgress();
                            }
                        });
            } else {
                btnPlay.setImageResource(R.drawable.ic_playpl);
            }
            updateShadowState(true);
        } else {
            setVisibility(View.GONE);
            updateShadowState(false);
        }
    }

    private void updateProgress() {
        this.progress = 0f;
        final MediaPlayer current = audioPLayer.current;
        if (current == null) {
            progress = 0f;
        } else {
            final int currentPosition = current.getCurrentPosition();
            final int duration = current.getDuration();
            progress = (float) currentPosition / duration;
        }
        invalidate();
    }

    @NonNull
    private CharSequence getTitle(TdApi.Audio currentAudio) {
        final String performer = currentAudio.performer;
        final String title = currentAudio.title;
        final boolean performerEmpty = isEmpty(performer);
        final boolean titleEmpty = isEmpty(title);
        if (performerEmpty && titleEmpty){
            if (isEmpty(currentAudio.fileName)) {
                return "Unknown audio";
            } else {
                return currentAudio.fileName;
            }
        } else {
            if (performerEmpty){
                return title;
            } else if (titleEmpty){
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

    public void setShadow(View toolbarShadow) {
        this.shadow = toolbarShadow;
        updateShadowState(getVisibility() == View.VISIBLE);

    }

    private void updateShadowState(boolean playerVisible) {
        if (shadow == null){
            return;
        }
        final FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) shadow.getLayoutParams();
        if (playerVisible){
            lp.topMargin = calc.dp(35f);
        } else {
            lp.topMargin = 0;
        }
        shadow.setLayoutParams(lp);
    }
}
