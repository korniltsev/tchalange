package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.text.TextUtils;
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

import static android.text.TextUtils.isEmpty;
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
        final String performer = audio.audio.performer;
        if (isEmpty(performer)){

        }
        songSinger.setText(performer);

        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, R.drawable.ic_pause, true, true, 38);
        downloadView.bind(audio.audio.audio, cfg, new DownloadView.CallBack() {
            @Override
            public void onProgress(TdApi.UpdateFileProgress p) {
                songName.setText(getResources().getString(R.string.downloading_kb, kb(p.ready), kb(p.size)));
            }

            @Override
            public void onFinished(TdApi.File fileLocal, boolean justDownloaded) {
                songName.setText(audio.audio.title);
                if (player.isPLaying()) {
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, false);
                }
            }

            @Override
            public void play(TdApi.File fileLocal) {
                final TdApi.Audio currentAudio = player.getCurrentAudio();
                if (player.isPLaying() && currentAudio.audio.id == fileLocal.id) {
                    player.pause();
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (player.isPaused()&& currentAudio.audio.id == fileLocal.id) {
                    player.resume();
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else{
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                    player.play(audio.audio, fileLocal);
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
                if (audio.audio.audio.id != response.audio.audio.id){
                    return;
                }

                if (response instanceof AudioPLayer.StateStopped){
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                } else if (response instanceof AudioPLayer.StatePlaying){
                    downloadView.setLevel(DownloadView.LEVEL_PAUSE, true);
                } else if (response instanceof AudioPLayer.StatePaused){
                    downloadView.setLevel(DownloadView.LEVEL_PLAY, true);
                }
            }
        };
    }
}
