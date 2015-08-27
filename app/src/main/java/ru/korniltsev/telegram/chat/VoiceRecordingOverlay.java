package ru.korniltsev.telegram.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Property;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.rx.VoiceRecorder;
import rx.Observable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static android.view.MotionEvent.*;
import static junit.framework.Assert.assertNotNull;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

public class VoiceRecordingOverlay extends FrameLayout {

    public static final Interpolator INTERPOLATOR = new DecelerateInterpolator(1.5f);
    public static final int SLIDE_DURATION = 256;
    public static final DecelerateInterpolator VALUE = new DecelerateInterpolator(1.5f);
    public static final int AMPLITUDE_ANIMATION_DURATION = 300;
    private final int dip2;
    private View anchor;
    private TextView time;
    private View voicePanel;
    private float dp48;
    private View redDot;
    private Observable<Long> everySecond;
    private Subscription subscription = Subscriptions.empty();
    private ObjectAnimator redDotAnimation;
    private PeriodFormatter timeFormatter;
    @Inject Presenter presenter;
    @Inject DpCalculator calc;
    @Inject VoiceRecorder recorder;
    private int redDotInitRightPadding;
    private int redDotRightPadding;
    private ObjectAnimator redButtonAnimation;
    private int redButtonFinalRadius;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintAmplitude = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int redDotBottomPadding;
    private Drawable microphone;
    private boolean stopCancelled;
    private boolean ignoreUpAndMove;
    private View slideToCanel;
    private Subscription amplitudeSubscriptions = Subscriptions.empty();
    private ObjectAnimator amplitudeAnimation;
    private int amplitudeMaxRadiusAddition ;

    public VoiceRecordingOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        setWillNotDraw(false);
        amplitudeMaxRadiusAddition = calc.dp(24f);
        paintAmplitude.setColor(0x10000000);
        dip2 = calc.dp(2);
        //        setLayerType(LAYER_TYPE_SOFTWARE, null);
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
        slideToCanel = findViewById(R.id.slide_to_cancel);
        dp48 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
        everySecond = Observable.timer(0, 1, TimeUnit.SECONDS)
                .onBackpressureDrop()
                .observeOn(mainThread());

        redDotAnimation = ObjectAnimator.ofFloat(redDot, ALPHA, 1f, 0.2f);
        redDotAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        redDotAnimation.setRepeatMode(ValueAnimator.REVERSE);
        redDotAnimation.setDuration(250);

        timeFormatter = new PeriodFormatterBuilder()
                .printZeroAlways()
                .minimumPrintedDigits(2) // gives the '01'
                .appendMinutes()
                .appendSeparator(":")
                .appendSeconds()
                .toFormatter();

        redDotInitRightPadding = calc.dp(26);
        redDotRightPadding = redDotInitRightPadding;
        redDotBottomPadding = calc.dp(24);
        redButtonFinalRadius = calc.dp(42);
        paint.setColor(Color.RED);
        microphone = getResources().getDrawable(R.drawable.ic_mic_white);
        assertNotNull(microphone);
        final int intrinsicWidth = microphone.getIntrinsicWidth();
        final int intrinsicHeight = microphone.getIntrinsicHeight();
        microphone.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if(!stateEnabled){
            return false;
        }
//        Log.d("VoiceRecordingOverlay", event.toString());
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
        if (actionMasked == ACTION_UP
                && !ignoreUpAndMove) {
            stop(false);
        }
        if (actionMasked == ACTION_MOVE
                & !ignoreUpAndMove) {
            positionRedButton(event);
        }
        return false;
    }

    private void positionRedButton(MotionEvent event) {
        if (!started) {
            return;
        }
        if (animating) {
            return;
        }
        final int pointerCount = event.getPointerCount();
        if (pointerCount != 1) {
            return;
        }

        int newPadding = (int) (getRight() - event.getX());
        if (newPadding > getWidth() / 2) {
            stop(true);
        } else {
            setRedDotRightPadding(
                    Math.max(redDotInitRightPadding, newPadding));

            int tx = getRedDotRightPadding() - redDotInitRightPadding;
            slideToCanel.setTranslationX(-tx);

            float alpha = 1f - (float) tx / (getWidth() / 2);
            slideToCanel.setAlpha(alpha);

        }
    }

    boolean started = false;
    boolean animating = false;
    private boolean stopOnEndOfAnimation;

    boolean stateEnabled = true;

    public void setStateEnabled(boolean stateEnabled) {
        this.stateEnabled = stateEnabled;
    }

    private void stop(boolean cancel) {
        if (!started) {
            return;
        }
        if (animating) {
            stopOnEndOfAnimation = true;
            stopCancelled = cancel;
        } else {
            ignoreUpAndMove = true;
            stopImpl(cancel);
            animating = true;
            release();
            slideOut();
//            scaleOutRedButton();
            animateRedButtonPadding();
        }
    }

    private void release() {
        subscription.unsubscribe();
        amplitudeSubscriptions.unsubscribe();
        redDotAnimation.cancel();
    }

    private void animateRedButtonPadding() {
        int diff = Math.abs(getRedDotRightPadding() - redDotInitRightPadding);
        final int maxDiff = getWidth() / 2 - redDotInitRightPadding;
        float ready = diff / maxDiff;

        final ObjectAnimator slideRedbutton = ObjectAnimator.ofInt(this, RED_DOT_RIGHT_PADDING,
                getRedDotRightPadding(), redDotInitRightPadding);
        slideRedbutton.setDuration((long) (SLIDE_DURATION * ready));
        slideRedbutton.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scaleOutRedButton();
            }
        });
        slideRedbutton.start();
    }

    private void stopImpl(boolean cancel) {
        final Observable<VoiceRecorder.Record> stop = recorder.stop();
        if (cancel) {

        } else {
            presenter.sendVoice(stop);
        }
    }

    private void slideOut() {
        voicePanel.animate().translationX(voicePanel.getWidth())
                .setDuration(SLIDE_DURATION)
                .setInterpolator(INTERPOLATOR)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        started = false;
                        animating = false;
                    }
                });
    }

    private void start() {
        if (!stateEnabled) {
            return;
        }
        if (started || animating) {
            return;
        }
        startImpl();
        started = true;
        animating = true;
        ignoreUpAndMove = false;
        setTime(0l);
        slideToCanel.setTranslationX(0);
        slideToCanel.setAlpha(1f);
        redDotRightPadding = redDotInitRightPadding;
        subscription = everySecond.subscribe(new ObserverAdapter<Long>() {
            @Override
            public void onNext(Long response) {
                //todo take duration from recorder
                setTime(response);
            }
        });
        scaleInRedButton();
        slideInPanel();
    }

    private void startImpl() {
        amplitudeSubscriptions = recorder.record()
                .sample(AMPLITUDE_ANIMATION_DURATION, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .observeOn(mainThread())
                .subscribe(new ObserverAdapter<Double>() {
                    @Override
                    public void onNext(Double response) {
                        animateAmplitude(response);
                    }
                });
    }

    final float max = 500f;
    private void animateAmplitude(Double response) {
        float normalized = (float) (Math.min(response, max) / max);
        if (amplitudeAnimation != null){
            amplitudeAnimation.cancel();
        }
        amplitudeAnimation = ObjectAnimator.ofFloat(this, AMPLITUDE, amplitude, normalized);
        amplitudeAnimation.setDuration(AMPLITUDE_ANIMATION_DURATION);
        amplitudeAnimation.setInterpolator(VALUE);
        amplitudeAnimation.start();
    }

    private void scaleInRedButton() {
        if (redButtonAnimation != null) {
            redButtonAnimation.cancel();
        }

        redButtonAnimation = ObjectAnimator.ofFloat(this, RADIUS, getRedButtonRadius(), 1f);
        redButtonAnimation.setInterpolator(INTERPOLATOR);
        redButtonAnimation.setDuration(SLIDE_DURATION/2);
        redButtonAnimation.start();
    }

    private void scaleOutRedButton() {
        if (redButtonAnimation != null) {
            redButtonAnimation.cancel();
        }
        redButtonAnimation = ObjectAnimator.ofFloat(this, RADIUS, getRedButtonRadius(), 0f);
        redButtonAnimation.setInterpolator(INTERPOLATOR);
        redButtonAnimation.setDuration(SLIDE_DURATION/2);
        redButtonAnimation.start();
    }

    private void slideInPanel() {
        voicePanel.setVisibility(View.VISIBLE);
        voicePanel.setTranslationX(voicePanel.getWidth());
        voicePanel.animate()
                .translationX(0)
                .setDuration(SLIDE_DURATION)
                .setInterpolator(INTERPOLATOR)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animating = false;
                        if (stopOnEndOfAnimation) {
                            stop(stopCancelled);
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

    final RectF rectF = new RectF();
    //    final Rect rect = new Rect();

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        int centerX = getRight() - redDotRightPadding;
        int centerY = getBottom() - redDotBottomPadding;

        if (amplitude != 0f && !(animating && started)) {
            float radius = redButtonFinalRadius + dip2 + amplitude * amplitudeMaxRadiusAddition;
            float l = centerX - radius;
            float r = centerX + radius;

            float t = centerY - radius;
            float b = centerY + radius;
            rectF.set(l, t, r, b);
            canvas.drawOval(rectF, paintAmplitude);
        }

        if (redDotRadius != 0f) {
            final float radius = getRedButtonRadius() * redButtonFinalRadius;


            float l = centerX - radius;
            float r = centerX + radius;

            float t = centerY - radius;
            float b = centerY + radius;
            rectF.set(l, t, r, b);
            canvas.drawOval(rectF, paint);

            l = centerX - microphone.getIntrinsicWidth() / 2;
            t = centerY - microphone.getIntrinsicHeight() / 2;
            canvas.save();
            canvas.translate(l, t);
            microphone.draw(canvas);
            canvas.restore();
        }

    }

    public static Property<VoiceRecordingOverlay, Float> RADIUS = new Property<VoiceRecordingOverlay, Float>(Float.class, "RADIUS") {
        @Override
        public Float get(VoiceRecordingOverlay object) {
            return object.getRedButtonRadius();
        }

        @Override
        public void set(VoiceRecordingOverlay object, Float value) {
            object.setRedDotRadius(value);
        }
    };

    float redDotRadius;

    public float getRedButtonRadius() {
        return redDotRadius;
    }

    public void setRedDotRadius(float redDotRadius) {
        this.redDotRadius = redDotRadius;
        microphone.setAlpha((int) (redDotRadius * 255));
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    public int getRedDotRightPadding() {
        return redDotRightPadding;
    }

    public void setRedDotRightPadding(int redDotRightPadding) {
        this.redDotRightPadding = redDotRightPadding;
        invalidate();
    }

    private static Property<VoiceRecordingOverlay, Integer> RED_DOT_RIGHT_PADDING = new Property<VoiceRecordingOverlay, Integer>(Integer.class, "RED_DOT_RIGHT_PADDING") {
        @Override
        public Integer get(VoiceRecordingOverlay object) {
            return object.getRedDotRightPadding();
        }

        @Override
        public void set(VoiceRecordingOverlay object, Integer value) {
            object.setRedDotRightPadding(value);
        }
    };

    private float amplitude = 0f;
    private static Property<VoiceRecordingOverlay, Float> AMPLITUDE = new Property<VoiceRecordingOverlay, Float>(Float.class, "VOICE_AMPLITUDE") {

        @Override
        public Float get(VoiceRecordingOverlay object) {
            return object.amplitude;
        }

        @Override
        public void set(VoiceRecordingOverlay object, Float value) {
            object.amplitude = value;
            object.invalidate();
        }
    };


}
