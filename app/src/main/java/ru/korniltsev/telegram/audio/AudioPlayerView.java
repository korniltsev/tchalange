package ru.korniltsev.telegram.audio;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.chat.adapter.view.BlurTransformation;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.AlbumCoverRequestHandler;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.toolbar.ToolbarUtils;
import ru.korniltsev.telegram.core.views.DownloadView;

import javax.inject.Inject;

import static ru.korniltsev.telegram.common.AppUtils.performerOf;

public class AudioPlayerView extends LinearLayout {
    private final DpCalculator calc;
    private final AudioPLayer audioPLayer;
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

    public AudioPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        final MyApp app = MyApp.from(context);
        calc = app.dpCalculator;
        audioPLayer = app.audioPLayer;

        blur = new BlurTransformation(getContext().getApplicationContext(), calc.dp(2));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        next = findViewById(R.id.btn_next);
        prev = findViewById(R.id.btn_prev);
        play = (DownloadView) findViewById(R.id.btn_play);
        shuffle = (ImageButton) findViewById(R.id.btn_shuffle);
        btn_loop = (ImageButton) findViewById(R.id.btn_loop);
        cover = ((ImageView) findViewById(R.id.cover));
        //        ToolbarUtils.initToolbar(this)
        //                .inflate(R.menu.photo_view)
        //                .pop();

        performer = ((TextView) findViewById(R.id.performer));
        title = ((TextView) findViewById(R.id.title));
        ToolbarUtils.initToolbar(this)
                .pop();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        presenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

    public void bind(final TdApi.Audio currentAudio) {
        final TdApi.File thumb = currentAudio.albumCoverThumb.photo;
        final boolean hasCover = thumb.id != TdApi.File.NO_FILE_ID;
        if (hasCover) {
            if (!downloader.isDownloaded(currentAudio.audio)) {
                glide.loadPhoto(thumb, false)
                        .into(cover);
            }
        }
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
                if (hasCover){
                    loadCover(fileLocal);
                }
                if (audioPLayer.isPLaying()){
                    play.setLevel(DownloadView.LEVEL_PAUSE, false);
                }
            }

            @Override
            public void play(TdApi.File fileLocal) {
                if (audioPLayer.isPLaying()) {
                    audioPLayer.pause();
                    play.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (audioPLayer.isPaused()){
                    audioPLayer.resume();
                    play.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else {
                    audioPLayer.play(currentAudio, fileLocal);
                    play.setLevel(DownloadView.LEVEL_PAUSE, true);

                }
            }
        }, play);
    }

    private void loadCover(TdApi.File fileLocal) {
        glide.getPicasso()
                .load(new AlbumCoverRequestHandler.Uri(fileLocal))
                .noPlaceholder()
                .stableKey("mp3_cover:" + fileLocal.id)
                .into(cover);
    }
}
