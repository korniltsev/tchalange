package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;
import rx.Observable;
import rx.subjects.PublishSubject;

public class AudioPLayer {
    final Context ctx;
    private final PublishSubject<State> currentState = PublishSubject.create();
    private TdApi.File currentFile;
    private TdApi.Audio currentAudio;
    private boolean paused;

    public AudioPLayer(Context ctx) {
        this.ctx = ctx;
    }

    @Nullable MediaPlayer current = null;

    public void play(TdApi.Audio audio, TdApi.File fileLocal) {
        cleanup();
        startNewPlayer(audio, fileLocal);
    }

    private void startNewPlayer(final TdApi.Audio audio, TdApi.File fileLocal) {
        try {

            final MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(fileLocal.path);
            mp.prepare();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    cleanup();
                }
            });

            mp.start();
            current = mp;
            currentFile = fileLocal;
            currentAudio = audio;
            currentState.onNext(new StatePlaying(audio));
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    private void cleanup() {
        if (current != null) {
            current.stop();
            current.release();
            final TdApi.Audio copy = this.currentAudio;
            currentFile = null;
            currentAudio = null;
            current = null;
            paused = false;
            currentState.onNext(new StateStopped(copy));
        }

    }

    public boolean isPLaying() {
        if (current == null) {
            return false;
        }
        return current.isPlaying();
    }

    public void pause() {
        if (current == null){
            return;
        }

        if (current.isPlaying()) {
            current.pause();
            paused = true;
            currentState.onNext(new StatePaused(currentAudio));
        }
    }

    public boolean isPaused() {
        if (current == null) {
            return false;
        }
        return paused;
    }

    public void resume() {
        if (current == null) {
            return;
        }
        if (!paused){
            return;
        }
        paused = false;
        current.start();
        currentState.onNext(new StatePlaying(currentAudio));
    }

    public Observable<State> currentState() {
        return currentState;
    }

    public void stop() {
        cleanup();
    }

    public TdApi.Audio getCurrentAudio() {
        return currentAudio;
    }

    public static abstract class State {
        public final TdApi.Audio audio;

        protected State(TdApi.Audio audio) {
            this.audio = audio;
        }
    }
    public static class StateStopped extends State {
        public StateStopped(TdApi.Audio audio) {
            super(audio);
        }
    }

    public static class StatePlaying extends State {

        public StatePlaying(TdApi.Audio audio) {
            super(audio);
        }
    }
    public static class StatePaused extends State {

        public StatePaused(TdApi.Audio audio) {
            super(audio);
        }
    }


}
