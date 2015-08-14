package ru.korniltsev.telegram.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.BlurTransformation;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.AlbumCoverRequestHandler;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.DownloadView;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static ru.korniltsev.telegram.common.AppUtils.performerOf;
import static rx.Observable.timer;

public class AudioPlayerView extends LinearLayout {
    public static final String SHUFFLE = "shuffle";
    public static final String LOOP = "loop";
    private final DpCalculator calc;
    private final AudioPLayer audioPLayer;
    private final Paint trackPaint;
    private final Paint positionPaint;
    private final Drawable shuffleIcon;
    private final Drawable backIcon;
    private final Drawable repeatIcon;
    private final Drawable playlistIcon;
    @Inject AudioPlayerPresenter presenter;
    @Inject RxGlide glide;
    @Inject RxDownloadManager downloader;
    private View next;
    private View prev;
    private DownloadView play;
    private ImageButton shuffle;
    private ImageButton btn_loop;
    private ImageView cover;
    private BlurTransformation blur;
    private TextView performer;
    private TextView title;
    private View square;
    private Subscription timerSubscription;
    private int radius;
    private int trackHeight;
    private ToolbarUtils toolbar;
    private boolean hasCover;
    private TextView durationText;
    private TextView positionText;

    public AudioPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        final MyApp app = MyApp.from(context);
        calc = app.dpCalculator;
        audioPLayer = app.audioPLayer;

        blur = new BlurTransformation(getContext().getApplicationContext(), calc.dp(2));
        setWillNotDraw(false);
        int trackColor = 0xFFDDDDDD;
        int positionColor = 0xFF6BADDE;
        trackPaint = new Paint();
        trackPaint.setColor(trackColor);
        positionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        positionPaint.setColor(positionColor);

        final Resources res = getResources();
        shuffleIcon = res.getDrawable(R.drawable.ic_shuffle_white);
        repeatIcon = res.getDrawable(R.drawable.ic_repeat_white);
        backIcon = res.getDrawable(R.drawable.ic_back);
        playlistIcon = res.getDrawable(R.drawable.ic_playlist_white);


    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        square = findViewById(R.id.square);
        next = findViewById(R.id.btn_next);
        prev = findViewById(R.id.btn_prev);
        play = (DownloadView) findViewById(R.id.btn_play);
        shuffle = (ImageButton) findViewById(R.id.btn_shuffle);
        btn_loop = (ImageButton) findViewById(R.id.btn_loop);
        cover = ((ImageView) findViewById(R.id.cover));
        durationText = ((TextView) findViewById(R.id.duration));
        positionText = ((TextView) findViewById(R.id.position));
        shuffle.setImageDrawable(shuffleIcon);
        btn_loop.setImageDrawable(repeatIcon);

        performer = ((TextView) findViewById(R.id.performer));
        title = ((TextView) findViewById(R.id.title));
        toolbar = ToolbarUtils.initToolbar(this)
                .inflate(R.menu.player)
                .pop();
        toolbar.toolbar.setNavigationIcon(backIcon);
        toolbar.toolbar.getMenu().findItem(R.id.playlist).setIcon(playlistIcon);

        radius = calc.dp(6);
        trackHeight = calc.dp(4f);

        btn_loop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPLayer.toggleLoop();
                updateButtonsColors();
            }
        });

        shuffle.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                audioPLayer.toggleShuffle();
                updateButtonsColors();
            }
        });
    }

    float progress = 0;
    final Rect trackRect = new Rect();
    final Rect positionRect = new Rect();

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        int squareBottom = square.getBottom();
        int top = squareBottom;
        int left = l;
        int right = r;

        int bottom = top + trackHeight;
        trackRect.set(left, top, right, bottom);
    }

    final RectF oval = new RectF();

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        canvas.drawRect(trackRect, trackPaint);

        positionRect.set(trackRect);
        final int width = positionRect.width();
        int positionWidth = (int) (width * progress);
        positionRect.right = positionWidth;
        canvas.drawRect(positionRect, positionPaint);

        int ovalCenterX = positionWidth;
        int ovalCenterY = trackRect.top + trackRect.height() / 2;
        int left = ovalCenterX - radius;
        int right = ovalCenterX + radius;

        int top = ovalCenterY - radius;
        int bottom = ovalCenterY + radius;
        oval.set(left, top, right, bottom);
        canvas.drawOval(oval, positionPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
        final TdApi.Audio currentAudio = audioPLayer.getCurrentAudio();
        if (currentAudio != null) {
            bind(currentAudio);
        }
        timerSubscription = timer(0, 1, TimeUnit.SECONDS)
                .onBackpressureDrop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ObserverAdapter<Long>() {
                    @Override
                    public void onNext(Long response) {
                        updateProgress();
                    }
                });

        audioPLayer.currentState().subscribe(new ObserverAdapter<AudioPLayer.State>() {
            @Override
            public void onNext(AudioPLayer.State response) {
                if (response instanceof AudioPLayer.StateStopped){
                    play.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (response instanceof AudioPLayer.StatePlaying){
                    play.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else if (response instanceof AudioPLayer.StatePaused){
                    play.setLevel(DownloadView.LEVEL_PLAY, true);
                }
            }
        });
    }

    private static final PeriodFormatter DURATION_FORMATTER = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(1).appendMinutes()
            .appendSeparator(":")
            .minimumPrintedDigits(2).printZeroAlways()
            .appendSeconds()
            .toFormatter();;

    private void updateProgress() {
        progress = audioPLayer.getProgress();
        final long duration = audioPLayer.getDuration();
        final long position = (int) (progress * duration);
        final Period d = new Period(duration);
        final Period p = new Period(position);
        durationText.setText(DURATION_FORMATTER.print(d));
        positionText.setText(DURATION_FORMATTER.print(p));
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
        timerSubscription.unsubscribe();
    }

    public void bind(final TdApi.Audio currentAudio) {
        final TdApi.File thumb = currentAudio.albumCoverThumb.photo;
        hasCover = thumb.id != TdApi.File.NO_FILE_ID;
        if (hasCover) {
            if (!downloader.isDownloaded(currentAudio.audio)) {
                glide.loadPhoto(thumb, false)
                        .into(cover);
            }
        }
        updateButtonsColors();
        performer.setText(performerOf(currentAudio));
        title.setText(currentAudio.title);
        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, R.drawable.ic_pause, true, true, 48);
        play.bind(currentAudio.audio, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {
                super.onProgress(p);
            }

            @Override
            public void onFinished(TdApi.File fileLocal, boolean justDownloaded) {
                if (hasCover) {
                    loadCover(fileLocal);
                }
                if (audioPLayer.isPLaying()) {
                    play.setLevel(DownloadView.LEVEL_PAUSE, false);
                }
            }

            @Override
            public void play(TdApi.File fileLocal) {
                if (audioPLayer.isPLaying()) {
                    audioPLayer.pause();
                    play.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (audioPLayer.isPaused()) {
                    audioPLayer.resume();
                    play.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else {
                    audioPLayer.play(currentAudio, fileLocal);
                    play.setLevel(DownloadView.LEVEL_PAUSE, true);
                }
            }
        }, play);
    }

    private void updateButtonsColors() {

        final int white = Color.WHITE;
        final int gray = 0xff818181;
        final int blue = 0xFF69ABDB;
        if (hasCover) {
            //white
            setColor(white, backIcon);
            setColor(white, playlistIcon);
            updateShuffleRepeateButtons(white, blue);
        } else {
            setColor(gray, backIcon);
            setColor(gray, playlistIcon);
            updateShuffleRepeateButtons(gray, blue);
        }
    }

    private void updateShuffleRepeateButtons(int white, int blue) {
        if (audioPLayer.isLoopEnabled()) {
            setColor(blue, repeatIcon);
        } else {
            setColor(white, repeatIcon);
        }
        if (audioPLayer.isShuffleEnabled()) {
            setColor(blue, shuffleIcon);
        } else {
            setColor(white, shuffleIcon);
        }
    }

    private void setColor(int white, Drawable icon) {
        icon.setColorFilter(white, PorterDuff.Mode.MULTIPLY);
    }

    private void loadCover(TdApi.File fileLocal) {
        glide.getPicasso()
                .load(new AlbumCoverRequestHandler.Uri(fileLocal))
                .noPlaceholder()
                .stableKey("mp3_cover:" + fileLocal.id)
                .into(cover);
    }
}
