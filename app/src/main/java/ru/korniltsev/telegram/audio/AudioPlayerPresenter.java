package ru.korniltsev.telegram.audio;

import android.content.Context;
import android.os.Bundle;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import static junit.framework.Assert.assertTrue;

@Singleton
public class AudioPlayerPresenter extends ViewPresenter<ru.korniltsev.telegram.audio.AudioPlayerView> {

    final AudioPlayerPath path;
    final AudioPLayer player;
    private CompositeSubscription subs;

    @Inject
    public AudioPlayerPresenter(Context ctx, AudioPlayerPath path) {
        this.path = path;
        player = MyApp.from(ctx).audioPLayer;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {

        subs = new CompositeSubscription();
        final TdApi.Audio currentAudio = player.getCurrentAudio();
        getView().bind(currentAudio);
    }



    @Override
    public void dropView(AudioPlayerView view) {
        super.dropView(view);
        subs.unsubscribe();
    }


}
