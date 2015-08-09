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
    private boolean paused;

    public AudioPLayer(Context ctx) {
        this.ctx = ctx;
    }

    @Nullable MediaPlayer current = null;

    public void play(TdApi.File fileLocal) {
        cleanup();
        startNewPlayer(fileLocal);
    }

    private void startNewPlayer(final TdApi.File fileLocal) {
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
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    private void cleanup() {
        if (current != null) {


            current.stop();
            current.release();
            currentState.onNext(new StateCompleted(currentFile));
            currentFile = null;
            current = null;
            paused = false;
        }

    }

    public boolean isPLaying(TdApi.File fileLocal) {
        if (current == null) {
            return false;
        }
        if (!currentFile.path.equals(fileLocal.path)){
            return false;
        }
        return current.isPlaying();
    }

    public void pause(TdApi.File fileLocal) {
        if (current == null){
            return;
        }
        if (!currentFile.path.equals(fileLocal.path)){
            return;
        }
        if (current.isPlaying()) {
            current.pause();
            paused = true;
        }
    }

    public boolean isPaused(TdApi.File fileLocal) {
        if (current == null) {
            return false;
        }
        if (!currentFile.path.equals(fileLocal.path)){
            return false;
        }
        return paused;
//        current.pause();
//        return current.isPlaying();

    }

    public void resume(TdApi.File fileLocal) {
        if (current == null) {
            return;
        }
        if (!currentFile.path.equals(fileLocal.path)){
            return ;
        }
        if (!paused){
            return;
        }
        paused = false;
        current.start();
    }

    public Observable<State> currentState() {
        return currentState;
    }




    public static abstract class State {
        public final TdApi.File file;

        protected State(TdApi.File  file) {
            this.file = file;
        }
    }
    public static class StateCompleted extends State{

        public StateCompleted(TdApi.File  file) {
            super(file);
        }
    }

    public static class StateStarted extends State {

        public StateStarted(TdApi.File  file) {
            super(file);
        }
    }


}
