package ru.korniltsev.telegram.profile.media.controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.BlurTransformation;
import ru.korniltsev.telegram.chat.adapter.view.VideoView;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import ru.korniltsev.telegram.core.views.DownloadView;

import static ru.korniltsev.telegram.core.views.DownloadView.Config.FINAL_ICON_EMPTY;

public class SharedMediaItemView extends FrameLayout {

    public static final BlurTransformation BLUR = new BlurTransformation(6);
    public static final float SCALE_SELECTED = 0.83f;
    public static final int DURATION = 128;
    public static final AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator(1.5f);
    public static final DecelerateInterpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator(1.5f);
    private final RxGlide picasso;
    private final RxDownloadManager downloader;
    private final int dip100;

    private View content;
    public ImageView img;
    private View btnPlayVideo;
    private TextView videoDuration;
    private DownloadView downloadView;
    private View whiteCircle;
    private View greenCircle;

    public SharedMediaItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final MyApp app = MyApp.from(context);
        dip100 = app.calc.dp(100);
        picasso = app.rxGlide;
        downloader = app.downloadManager;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        content = findViewById(R.id.content);
        img = ((ImageView) findViewById(R.id.img));
        setBackgroundColor(0xFFF5F5F5);
        btnPlayVideo = findViewById(R.id.video_play_button);
        videoDuration = ((TextView) findViewById(R.id.duration));
        downloadView = ((DownloadView) findViewById(R.id.download_view));
        whiteCircle = findViewById(R.id.white_circle);
        greenCircle = findViewById(R.id.green_circle);

        whiteCircle.setVisibility(View.GONE);
        greenCircle.setVisibility(View.GONE);
    }

    private boolean ignoreWidth;

    public void setIgnoreWidth(boolean ignoreWidth) {
        this.ignoreWidth = ignoreWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (ignoreWidth) {
            super.onMeasure(heightMeasureSpec, heightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);
        }
    }

    public void bindSelection(boolean selected, boolean selectedAtLeastOne) {
        stopCircleAnimations();
        if (selectedAtLeastOne) {
            whiteCircle.setVisibility(View.VISIBLE);
            if (selected) {
                greenCircle.setVisibility(View.VISIBLE);
                content.setScaleX(SCALE_SELECTED);
                content.setScaleY(SCALE_SELECTED);
            } else {
                greenCircle.setVisibility(View.GONE);
                content.setScaleX(1f);
                content.setScaleY(1f);
            }
        } else {
            whiteCircle.setVisibility(View.GONE);
            greenCircle.setVisibility(View.GONE);
            content.setScaleX(1f);
            content.setScaleY(1f);
        }
    }

    private void stopCircleAnimations() {
        whiteCircle.animate()
                .cancel();
        greenCircle.animate()
                .cancel();
    }

    public void bindVideo(TdApi.Video v) {
        downloadView.setVisibility(View.VISIBLE);
        videoDuration.setVisibility(View.VISIBLE);
        btnPlayVideo.setVisibility(View.VISIBLE);

        if (v.thumb.photo.id == TdApi.File.NO_FILE_ID) {
            picasso.getPicasso().cancelRequest(img);
            img.setImageDrawable(null);
        } else {
            picasso.loadPhoto(v.thumb.photo, false)
                    .transform(BLUR)
                    .into(img);
        }

        DownloadView.Config cfg = new DownloadView.Config(FINAL_ICON_EMPTY, FINAL_ICON_EMPTY, false, false, 48);
        downloadView.bind(v.video, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {

            }

            @Override
            public void onFinished(TdApi.File fileLocal, boolean justDownloaded) {

            }

            @Override
            public void play(TdApi.File fileLocal) {
                VideoView.playVideo(getContext(), downloader, fileLocal);
            }
        }, this);
        videoDuration.setText(
                DURATION_FORMATTER.print(new Period(v.duration)));
    }

    public void bindPhoto(TdApi.Photo p) {
        downloadView.setVisibility(View.GONE);
        videoDuration.setVisibility(View.GONE);
        btnPlayVideo.setVisibility(View.GONE);

        final TdApi.File smallestBiggerThan = PhotoUtils.findSmallestBiggerThan(p, dip100, dip100);
        if (smallestBiggerThan.id == TdApi.File.NO_FILE_ID) {
            picasso.getPicasso().cancelRequest(img);
            img.setImageDrawable(null);
        } else {
            picasso.loadPhoto(smallestBiggerThan, false)
                    .into(img);
        }
    }

    private static final PeriodFormatter DURATION_FORMATTER = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(1).appendMinutes()
            .appendSeparator(":")
            .minimumPrintedDigits(2).printZeroAlways()
            .appendSeconds()
            .toFormatter();
    ;

    public void animateWhiteCircle(boolean in) {
        animateView(whiteCircle, in);
    }

    public void animateGreenCircle(boolean in) {
        animateView(greenCircle, in);
        content.animate()
                .cancel();
        if (in) {
            content.animate()
                    .setDuration(DURATION)
                    .scaleX(SCALE_SELECTED)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .scaleY(SCALE_SELECTED);
        } else {
            content.animate()
                    .setDuration(DURATION)
                    .setInterpolator(ACCELERATE_INTERPOLATOR)
                    .scaleX(1f)
                    .scaleY(1f);
        }
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
}
