package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.views.DownloadView;

import static ru.korniltsev.telegram.common.AppUtils.kb;

public class AudioMessageView extends LinearLayout {
    private TextView songName;
    private TextView songSinger;
    private DownloadView downloadView;

    private TdApi.MessageAudio audio;

    public AudioMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
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
            }

            @Override
            public void play(TdApi.File fileLocal) {
                super.play(fileLocal);
            }
        }, downloadView);
    }
}
