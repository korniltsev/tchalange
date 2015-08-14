package ru.korniltsev.telegram.audio;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import mortar.ViewPresenter;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.audio.AudioPLayer;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Inject;
import javax.inject.Singleton;

import static junit.framework.Assert.assertTrue;

@Singleton
public class AudioPlayerPresenter extends ViewPresenter<ru.korniltsev.telegram.audio.AudioPlayerView> {

    final AudioPlayerPath path;
//    final AudioPLayer player;
    final ActivityOwner activity;
    private CompositeSubscription subs;
    private int requestedOrientation;

    @Inject
    public AudioPlayerPresenter(Context ctx, AudioPlayerPath path, ActivityOwner activity) {
        this.path = path;
        this.activity = activity;
//        player = MyApp.from(ctx).audioPLayer;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
//        requestedOrientation = activity.expose().getRequestedOrientation();
//        activity.expose().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        subs = new CompositeSubscription();
//        final TdApi.Audio currentAudio = player.getCurrentAudio();
//        getView().bind(currentAudio);
    }



    @Override
    public void dropView(AudioPlayerView view) {
        super.dropView(view);
        subs.unsubscribe();
//        final Activity expose = activity.expose();
//        expose.setRequestedOrientation(requestedOrientation);
    }


}
