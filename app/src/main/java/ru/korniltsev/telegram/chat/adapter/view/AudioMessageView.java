package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.audio.AudioPlayer;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.views.DownloadView;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

import static junit.framework.Assert.assertTrue;

public class AudioMessageView extends LinearLayout {

    public static final Subscription EMPTY_SUBSCRIPTION = Subscriptions.empty();
//    private ImageView btnPlay;
    private TextView duration;
    private SeekBar progress;

    @Inject AudioPlayer player;
    @Inject RXClient client;
    @Inject RxDownloadManager downloader;

    private TdApi.Voice audio;
    private DownloadView download_view;
    private static final PeriodFormatter DURATION_FORMATTER = new PeriodFormatterBuilder()
            .printZeroAlways()
            .minimumPrintedDigits(1).appendMinutes()
            .appendSeparator(":")
            .minimumPrintedDigits(2).printZeroAlways()
            .appendSeconds()
            .toFormatter();;
    private Subscription subscription = Subscriptions.empty();
    private final Action1<TdApi.UpdateFile> decodeAction = new Action1<TdApi.UpdateFile>() {
        @Override
        public void call(TdApi.UpdateFile updateFile) {
            player.decode(updateFile);
        }
    };

    public AudioMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);


//        PeriodFormatter minutesAndSeconds =
//        minutesAndSeconds = minutesAndSeconds.printTo(period);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        download_view = ((DownloadView) findViewById(R.id.download_view));
        progress = (SeekBar) findViewById(R.id.progress);
        duration = ((TextView) findViewById(R.id.duration));
        progress.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //do not support seek
                return true;
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        subscription.unsubscribe();
    }

    public void setAudio(TdApi.Voice a) {
        this.audio = a;
        progress.setProgress(0);
        long secs = a.duration;
        Period p = new Duration(secs * 1000)
                .toPeriod();

        this.duration.setText(DURATION_FORMATTER.print(p));
        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, R.drawable.ic_pause, true, true, 38);
        downloader.hook(a.voice, decodeAction);
        download_view.bind(a.voice, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {

            }

            @Override
            public void onFinished(TdApi.File e, boolean b) {
                assertTrue(e.isLocal());
                if (player.isPLaying(e.path)) {
                    download_view.setLevel(DownloadView.LEVEL_PAUSE, false);
                    //show pause
                    //subscribe
                    player.current()
                            .subscribe(updateProgress());
                }
            }

            @Override
            public void play(TdApi.File e) {
                assertTrue(e.isLocal());
                if (player.isPLaying(e.path)) {
                    player.pause(e.path);
                    download_view.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (player.isPaused(e.path)) {
                    player.resume(e.path);
                    download_view.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else{
                    download_view.setLevel(DownloadView.LEVEL_PAUSE, true);
                    subscription = player.play(e)
                            .subscribe(updateProgress());
                }

            }
        }, download_view);
    }

    private ObserverAdapter<AudioPlayer.TrackState> updateProgress() {
        return new ObserverAdapter<AudioPlayer.TrackState>() {
            @Override
            public void onNext(AudioPlayer.TrackState trackState) {
                updateProgress(trackState);
            }
        };
    }

    private void updateProgress(AudioPlayer.TrackState trackState) {

        progress.setMax(trackState.duration);
        if (!trackState.playing || trackState.duration == trackState.head){
            progress.setProgress(0);
            download_view.setLevel(DownloadView.LEVEL_PLAY, true);
        } else {
            progress.setProgress(trackState.head);
        }

    }
}
