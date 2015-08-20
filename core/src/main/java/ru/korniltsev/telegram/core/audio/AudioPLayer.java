package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import com.crashlytics.android.core.CrashlyticsCore;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

import java.util.List;
import java.util.Random;

import static rx.Observable.just;
import static rx.Observable.zip;

public class AudioPLayer {
    public static final String PREF_LOOP = "pref_loop";
    public static final String PREF_SHUFFLE = "pref_shuffle";
    final Context ctx;
    private final PublishSubject<State> currentState = PublishSubject.create();
    private final SharedPreferences prefs;
    private TdApi.File currentFile;
    private TdApi.Audio currentAudio;
    private boolean paused;
    final RXClient client;
    private final RxDownloadManager downloader;
    private Subscription playlistCreation = Subscriptions.empty();
    private TdApi.Message currentMessage;
    private Subscription downloadSubscription = Subscriptions.empty();

    public AudioPLayer(Context ctx, RXClient client, RxDownloadManager downloader) {
        this.ctx = ctx;
        this.client = client;
        this.downloader = downloader;
        prefs = ctx.getSharedPreferences("AudioPlayerPrefernces", Context.MODE_PRIVATE);
    }

    @Nullable MediaPlayer current = null;

    public void play(TdApi.Audio audio, TdApi.File fileLocal, TdApi.Message msg) {
        cleanup();
        currentState.onNext(new StatePrepare(audio));
        startNewPlayer(audio, fileLocal, msg);
        createPlaylist(msg);
    }


    long playlistChatId = 0;
    @Nullable List<TdApi.Message> playList;//null до тех пор пока
    private void createPlaylist(TdApi.Message msg) {
        if (playlistChatId == msg.chatId){
            return;
        }
        playlistCreation.unsubscribe();
        playlistCreation =  RXClient.getAllMedia(client, msg.chatId)
                .subscribe(new ObserverAdapter<List<TdApi.Message>>() {
                    @Override
                    public void onNext(List<TdApi.Message> response) {
                        playList = response;
                    }
                });
    }

    private void startNewPlayer(final TdApi.Audio audio, final TdApi.File fileLocal, final TdApi.Message msg) {
        try {

            final MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(fileLocal.path);
            mp.prepare();
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (isLoopEnabled()){
                        cleanup();
                        startNewPlayer(audio, fileLocal, msg);
                    } else if (isShuffleEnabled()){
                        //shuffle
                        random();
                    } else {
//                        next();
                        next();
                    }
                }
            });

            mp.start();
            current = mp;
            currentFile = fileLocal;
            this.currentMessage = msg;
            currentAudio = audio;
            currentState.onNext(new StatePlaying(audio));
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    private void random() {
        final List<TdApi.Message> playList = this.playList;
        if (playList == null || playList.isEmpty()){
            return;
        }
        final TdApi.Message message = playList.get(rnd.nextInt(playList.size()));
        downloadAndPlay(message);
    }

    Random rnd = new Random();

    private void cleanup() {
        downloadSubscription.unsubscribe();
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
        playList = null;
        playlistChatId = 0;
    }

    public TdApi.Audio getCurrentAudio() {
        return currentAudio;
    }

    public float getProgress() {
        if (current == null) {
            return 0f;
        } else {
            try {
                final int currentPosition = current.getCurrentPosition();
                final int duration = current.getDuration();
                return  (float) currentPosition / duration;
            } catch (Exception e) {
                CrashlyticsCore.getInstance().logException(e);
                return 0f;
            }
        }
    }

    public boolean isLoopEnabled() {
        return prefs.getBoolean(PREF_LOOP, false);
    }

    public boolean isShuffleEnabled() {
        return prefs.getBoolean(PREF_SHUFFLE, false);
    }

    public void toggleLoop() {
        final boolean loopEnabled = isLoopEnabled();
        prefs.edit().putBoolean(PREF_LOOP, !loopEnabled).commit();
    }

    public void toggleShuffle() {
        final boolean shuffleEnabled = isShuffleEnabled();
        prefs.edit().putBoolean(PREF_SHUFFLE, !shuffleEnabled).commit();
    }

    public int getDuration() {
        if (current != null){
            try {
                return current.getDuration();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public void seekTo(float seekTo) {
        if (current == null){
            return;
        }
        try {
            final int duration = current.getDuration();
            final int targetSeek = (int) (duration * seekTo);
            current.seekTo(targetSeek);
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    public void prev() {
        final List<TdApi.Message> playListCopy = this.playList;
        if (playListCopy == null){
            return;
        }
        final TdApi.Message currentMessage = this.currentMessage;
        for (int i = 0, playListCopySize = playListCopy.size(); i < playListCopySize; i++) {
            TdApi.Message m = playListCopy.get(i);
            if (m.id == currentMessage.id) {
                if (i + 1 < playListCopy.size()) {
                    final TdApi.Message nextMessage = playListCopy.get(i + 1);
                    downloadAndPlay(nextMessage);
                } else {
                    downloadAndPlay(playListCopy.get(0));
                }
                break;
            }
        }
    }

    private void downloadAndPlay(final TdApi.Message nextMessage) {
        cleanup();
        final TdApi.MessageAudio a = (TdApi.MessageAudio) nextMessage.message;
        final TdApi.File audio = a.audio.audio;
        final TdApi.File downloadedFile = downloader.getDownloadedFile(audio);
        if (downloadedFile != null) {
            play(a.audio, downloadedFile, nextMessage);
        } else {
            downloadSubscription = downloader.downloadWithoutProgress(audio)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new ObserverAdapter<TdApi.File>() {
                        @Override
                        public void onNext(TdApi.File response) {
                            play(a.audio, response, nextMessage);
                        }
                    });
            currentState.onNext(new StatePrepare(a.audio));
        }
    }

    public void next() {
        final List<TdApi.Message> playListCopy = this.playList;
        if (playListCopy == null){
            return;
        }

        final TdApi.Message currentMessage = this.currentMessage;
        for (int i = 0, playListCopySize = playListCopy.size(); i < playListCopySize; i++) {
            TdApi.Message m = playListCopy.get(i);
            if (m.id == currentMessage.id) {
                if (i - 1 > 0) {
                    final TdApi.Message nextMessage = playListCopy.get(i - 1);
                    downloadAndPlay(nextMessage);
                } else {
                    downloadAndPlay(playListCopy.get(playListCopy.size() - 1));
                }
                break;
            }
        }
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

    public static class StatePrepare extends State {
        //going to play the music
        public StatePrepare(TdApi.Audio audio) {
            super(audio);
        }
    }

    public TdApi.Message getCurrentMessage() {
        return currentMessage;
    }

    //    @Nullable
//    public MediaPlayer getCurrent() {
//        return current;
//    }
}
