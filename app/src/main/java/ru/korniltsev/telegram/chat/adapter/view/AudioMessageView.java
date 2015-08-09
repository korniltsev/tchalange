package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.views.DownloadView;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import static ru.korniltsev.telegram.common.AppUtils.kb;

public class AudioMessageView extends LinearLayout {
    private final AudioPLayer player;
    private TextView songName;
    private TextView songSinger;
    private DownloadView downloadView;

    private TdApi.MessageAudio audio;
    private Subscription subscription = Subscriptions.empty();

    public AudioMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        player = MyApp.from(context).audioPLayer;

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        downloadView = ((DownloadView) findViewById(R.id.download_view));
        songName = ((TextView) findViewById(R.id.song_name));
        songSinger = ((TextView) findViewById(R.id.song_singer));
    }

    public void bind(final TdApi.MessageAudio audio) {
        if (this.audio == audio){
            return;
        }
        this.audio = audio;
        subscription.unsubscribe();
        subscribe();
        songName.setText(audio.audio.title);
        songSinger.setText(audio.audio.performer);

        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, R.drawable.ic_pause, true, true, 38);
        downloadView.bind(audio.audio.audio, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {
                songName.setText(getResources().getString(R.string.downloading_kb, kb(p.ready), kb(p.size)));
            }

            @Override
            public void onFinished(TdApi.File fileLocal, boolean justDownloaded) {
                songName.setText(audio.audio.title);
                if (player.isPLaying(fileLocal)) {
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, false);
                }
            }

            @Override
            public void play(TdApi.File fileLocal) {
                if (player.isPLaying(fileLocal)) {
                    player.pause(fileLocal);
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (player.isPaused(fileLocal)) {
                    player.resume(fileLocal);
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else{
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                    player.play(fileLocal);
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
        return new ObserverAdapter<AudioPLayer.State>(){
            @Override
            public void onNext(AudioPLayer.State response) {
                if (audio.audio.audio.id != response.file.id){
                    return;
                }
                if (response instanceof AudioPLayer.StateCompleted){
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (response instanceof AudioPLayer.StateStarted){
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                }
            }
        };
    }
}
