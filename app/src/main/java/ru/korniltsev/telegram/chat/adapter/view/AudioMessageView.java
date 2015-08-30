package ru.korniltsev.telegram.chat.adapter.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.views.ArgbEvaluator;
import ru.korniltsev.telegram.core.views.DownloadView;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import static ru.korniltsev.telegram.common.AppUtils.kb;
import static ru.korniltsev.telegram.common.AppUtils.performerOf;

public class AudioMessageView extends LinearLayout {
    public static final int BG_SELECTED_COLOR = 0xFFF5F5F5;

    public static final int DURATION = 128;
    public static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    private final AudioPLayer player;
    private TextView songName;
    private TextView songSinger;
    private DownloadView downloadView;
    //    @Inject Presenter presenter;
    private TdApi.MessageAudio audio;
    private Subscription subscription = Subscriptions.empty();
    private ImageView greenCircle;
    public ImageView whiteCircle;

    final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ObjectAnimator currentBackgroundColorAnimation;
    private Integer currentBgColor = Color.TRANSPARENT;

    {
        bgPaint.setColor(Color.TRANSPARENT);

    }


    public AudioMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        player = MyApp.from(context).audioPLayer;
        setWillNotDraw(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        downloadView = ((DownloadView) findViewById(R.id.download_view));
        songName = ((TextView) findViewById(R.id.song_name));
        songSinger = ((TextView) findViewById(R.id.song_singer));
        greenCircle = (ImageView) findViewById(R.id.green_circle);
        whiteCircle = (ImageView) findViewById(R.id.white_circle);
    }

    public void bind(final TdApi.MessageAudio audio, final TdApi.Message msg) {
        if (this.audio == audio) {
            return;
        }
        this.audio = audio;
        subscription.unsubscribe();
        subscribe();
        songName.setText(audio.audio.title);
        songSinger.setText(performerOf(audio.audio));

        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, R.drawable.ic_pause, true, true, 38);
        downloadView.bind(audio.audio.audio, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {
                songName.setText(getResources().getString(R.string.downloading_kb, kb(p.ready), kb(p.size)));
            }

            @Override
            public void onFinished(TdApi.File fileLocal, boolean justDownloaded) {
                songName.setText(audio.audio.title);
                final TdApi.Audio currentAudio = player.getCurrentAudio();
                if (player.isPLaying() && currentAudio.audio.id == fileLocal.id) {
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, false);
                }
            }

            @Override
            public void play(TdApi.File fileLocal) {
                final TdApi.Audio currentAudio = player.getCurrentAudio();
                if (player.isPLaying() && currentAudio.audio.id == fileLocal.id) {
                    player.pause();
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (player.isPaused() && currentAudio.audio.id == fileLocal.id) {
                    player.resume();
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else {
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                    player.play(audio.audio, fileLocal, msg);
                }
            }
        }, downloadView);
    }

    private void subscribe() {
        subscription.unsubscribe();
        subscription = player.currentState()
                .subscribe(updateProgress());
    }

    private Observer<AudioPLayer.State> updateProgress() {
        return new ObserverAdapter<AudioPLayer.State>() {
            @Override
            public void onNext(AudioPLayer.State response) {
                if (audio.audio.audio.id != response.audio.audio.id) {
                    return;
                }

                if (response instanceof AudioPLayer.StateStopped) {
                    //todo looks like it is going to try animate twice if clicked
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (response instanceof AudioPLayer.StatePlaying) {
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else if (response instanceof AudioPLayer.StatePaused) {
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                }
            }
        };
    }

    static final class BgColorProperty extends Property<AudioMessageView, Integer> {
        public BgColorProperty() {
            super(Integer.class, "bg selection color");
        }

        @Override
        public void set(AudioMessageView object, Integer value) {
            object.setBgColor(value);
        }

        @Override
        public Integer get(AudioMessageView object) {
            return object.getBgColor();
        }
    }

    private Integer getBgColor() {
        return bgPaint.getColor();
    }

    private void setBgColor(Integer value) {
        this.currentBgColor = value;
        bgPaint.setColor(value);
        invalidate();
    }

    public static final BgColorProperty BG_SELECTION = new BgColorProperty();

    public void animateWhiteCircle(boolean in) {
        animateView(whiteCircle, in);

    }

    public void animateGreenCircle(boolean in) {
        animateView(greenCircle, in);
        if (currentBackgroundColorAnimation != null) {
            currentBackgroundColorAnimation.cancel();
        }
        if (in) {
            currentBackgroundColorAnimation = ObjectAnimator.ofInt(this, BG_SELECTION, BG_SELECTED_COLOR)

                    .setDuration(DURATION);
        } else {
            currentBackgroundColorAnimation = ObjectAnimator.ofInt(this, BG_SELECTION, Color.TRANSPARENT)
                    .setDuration(DURATION);
        }
        currentBackgroundColorAnimation.setEvaluator(ARGB_EVALUATOR);
        currentBackgroundColorAnimation.start();
    }

    private void animateView(final View v, boolean in) {
        v.animate()
                .cancel();
        if (in) {
            v.setVisibility(View.VISIBLE);
            v.setScaleX(0);
            v.setScaleY(0);
            v.animate()
                    .scaleX(1)
                    .setDuration(DURATION)
                    .scaleY(1)
                    .setListener(null);
        } else {
            v.animate()
                    .scaleX(0)
                    .setDuration(DURATION)
                    .scaleY(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            v.setVisibility(View.GONE);
                        }
                    });
        }
    }

    public void bindSelection(boolean selected, boolean selectedAtLeastOne) {
        stopCircleAnimations();
        if (currentBackgroundColorAnimation != null) {
            currentBackgroundColorAnimation.cancel();
        }
        if (selectedAtLeastOne) {
            whiteCircle.setVisibility(View.VISIBLE);
            if (selected) {
                greenCircle.setVisibility(View.VISIBLE);
                setBgColor(BG_SELECTED_COLOR);
            } else {
                greenCircle.setVisibility(View.GONE);
                setBgColor(Color.TRANSPARENT);
            }
        } else {
            whiteCircle.setVisibility(View.GONE);
            greenCircle.setVisibility(View.GONE);
        }
    }

    private void stopCircleAnimations() {
        whiteCircle.animate()
                .cancel();
        greenCircle.animate()
                .cancel();
    }

    final RectF bgRect = new RectF();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        bgRect.set(0, 0, r, b);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (currentBgColor != Color.TRANSPARENT) {
            canvas.drawRect(bgRect, bgPaint);
        }
    }
}
