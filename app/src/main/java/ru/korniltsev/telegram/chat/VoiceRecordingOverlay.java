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
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import java.io.File;
import java.util.concurrent.TimeUnit;

import static android.view.MotionEvent.*;
import static junit.framework.Assert.assertNotNull;

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
    @Inject Presenter presenter;
    @Inject DpCalculator calc;
    @Inject VoiceRecorder recorder;
    private int redDotInitRightPadding;
    private int redDotRightPadding;
    private ObjectAnimator redButtonAnimation;
    private int redButtonFinalRadius;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int redDotBottomPadding;
    private Drawable microphone;



    public VoiceRecordingOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        setWillNotDraw(false);
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
        everySecond = Observable.timer(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread());


        redDotAnimation = ObjectAnimator.ofFloat(redDot, ALPHA, 1f, 0.2f);
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
        if (actionMasked == ACTION_MOVE) {
            positionRedButton(event);
        }
        return false;
    }

    private void positionRedButton(MotionEvent event) {
        if (!started)
            return;
        if (animating){
            return;
        }
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
            stopImpl();
            animating = true;
            subscription.unsubscribe();
            redDotAnimation.cancel();
            slideOut();
            scaleOutRedButton();
        }
    }

    private void stopImpl() {
        //todo cancel
        final Observable<VoiceRecorder.Record> stop = recorder.stop();
        presenter.sendVoice(stop);
    }

    private void slideOut() {
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
        setTime(0l);
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
        recorder.record();
    }

    private void scaleInRedButton() {
        if (redButtonAnimation != null){
            redButtonAnimation.cancel();
        }

        redButtonAnimation = ObjectAnimator.ofFloat(this, RADIUS, getRedButtonRadius(),  1f);
        redButtonAnimation.setInterpolator(new DecelerateInterpolator());
        redButtonAnimation.start();
    }
    private void scaleOutRedButton() {
        if (redButtonAnimation != null){
            redButtonAnimation.cancel();
        }
        redButtonAnimation = ObjectAnimator.ofFloat(this, RADIUS, getRedButtonRadius(),  0f);
        redButtonAnimation.start();
    }

    private void slideInPanel() {
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
    final RectF rectF = new RectF();
//    final Rect rect = new Rect();


    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        if (redDotRadius != 0f){
            final float radius = getRedButtonRadius() * redButtonFinalRadius;
            int centerX = getRight() - redDotRightPadding;
            int centerY = getBottom() - redDotBottomPadding;

            float l = centerX - radius;
            float r = centerX + radius;

            float t = centerY - radius;
            float b = centerY + radius;
            rectF.set(l, t, r, b);
            canvas.drawOval(rectF, paint);

            l = centerX - microphone.getIntrinsicWidth()/2;
            t = centerY - microphone.getIntrinsicHeight()/2;
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

    float redDotRadius; // [0;1]

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
        subscription.unsubscribe();
    }
}
