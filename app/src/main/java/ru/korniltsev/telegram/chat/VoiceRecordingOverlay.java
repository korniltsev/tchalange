package ru.korniltsev.telegram.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import java.util.concurrent.TimeUnit;

import static android.view.MotionEvent.*;

public class VoiceRecordingOverlay extends FrameLayout {

    public static final Interpolator INTERPOLATOR = new DecelerateInterpolator(1.5f);
    private View anchor;
    private TextView time;
    private View voicePanel;
    private float dp48;
    private View redDot;
    private Observable<Long> everySecond;
    private Subscription subscription = Subscriptions.empty();
    private ObjectAnimator redDotAnimation;
    private PeriodFormatter timeFormatter;

    public VoiceRecordingOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        anchor = findViewById(R.id.anchor);
        time = (TextView) findViewById(R.id.time);
        voicePanel = findViewById(R.id.voice_panel);
        voicePanel.setVisibility(View.INVISIBLE);
        anchor.setVisibility(View.INVISIBLE);
        redDot = findViewById(R.id.red_dot);
        dp48 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        everySecond = Observable.timer(1, 1, TimeUnit.SECONDS)
//                .startWith(0l)
                .observeOn(AndroidSchedulers.mainThread());


        redDotAnimation = ObjectAnimator.ofFloat(redDot, ALPHA, 1f, 0.2f);
//        redDotAnimation.setInterpolator(INTERPOLATOR);
        redDotAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        redDotAnimation.setRepeatMode(ValueAnimator.REVERSE);
        redDotAnimation.setDuration(500);


        timeFormatter = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(2) // gives the '01'
                .appendMinutes()
                .appendSeparator(":")
                .appendSeconds()
                .toFormatter();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        Log.d("VoiceRecordingOverlay", event.toString());
        final int actionMasked = event.getActionMasked();
        if (actionMasked == ACTION_DOWN) {
            final int right = getRight();
            final int bottom = getBottom();
            if (event.getX() >= right - dp48
                    && event.getY() >= bottom - dp48) {
                start();
                return true;
            }
        }
        if (actionMasked == ACTION_UP) {
            stop();
        }
        return false;
    }

    boolean started = false;
    boolean animating = false;
    private boolean stopOnEndOfAnimation;

    boolean stateEnabled = true;

    public void setStateEnabled(boolean stateEnabled) {
        this.stateEnabled = stateEnabled;
    }

    private void stop() {
        if (!started) {
            return;
        }
        if (animating) {
            stopOnEndOfAnimation = true;
        } else {
            animating = true;
            subscription.unsubscribe();
            redDotAnimation.cancel();
            voicePanel.animate().translationX(-voicePanel.getWidth())
                    .setInterpolator(INTERPOLATOR)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            started = false;
                            animating = false;
                        }
                    });
        }
    }

    private void start() {
        if (!stateEnabled) {
            return;
        }
        if (started || animating) {
            return;
        }
        started = true;
        animating = true;
        setTime(0l);
        subscription = everySecond.subscribe(new ObserverAdapter<Long>() {
            @Override
            public void onNext(Long response) {
                //todo take duration from recorder
                setTime(response);
            }
        });
        voicePanel.setVisibility(View.VISIBLE);
        voicePanel.setTranslationX(voicePanel.getWidth());
        voicePanel.animate()
                .translationX(0)
                .setInterpolator(INTERPOLATOR)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animating = false;
                        if (stopOnEndOfAnimation) {
                            stop();
                        }
                        redDotAnimation.start();
                        stopOnEndOfAnimation = false;
                    }
                });
    }

    private void setTime(Long response) {
        final Period p = new Duration(response * 1000)
                .toPeriod();
        time.setText(timeFormatter.print(p));
    }
}
