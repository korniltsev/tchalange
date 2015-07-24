package ru.korniltsev.telegram.core.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.core.CrashlyticsCore;
import junit.framework.Assert;
import opus.OpusSupport;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.adapters.ObserverAdapter;
import ru.korniltsev.telegram.core.rx.RXAuthState;
import rx.Observable;
import rx.subjects.BehaviorSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;

@Singleton
public class AudioPlayer {
    public static final int SAMPLE_RATE_IN_HZ = 48000;
    private final int[] mOutArgs = new int[3];
//    private final MediaPlayer mPlayer;
    private final Context ctx;
    private final File decodeCacheDir;
    private int playerBufferSize;
    //guarde by ui thread
    @Nullable private Track currentTrack;
    private final ExecutorService service = Executors.newCachedThreadPool();

    @Inject
    public AudioPlayer(Context ctx, RXAuthState auth) {
        this.ctx = ctx;
        decodeCacheDir = createAudioCacheDir(ctx);

        playerBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }

        auth.listen()
                .subscribe(new ObserverAdapter<RXAuthState.AuthState>() {
                    @Override
                    public void onNext(RXAuthState.AuthState authState) {
                        if (authState instanceof RXAuthState.StateLogout){
                            cleanDecodeCache();
                        }
                    }
                });

    }

    private void cleanDecodeCache() {
        File[] files = decodeCacheDir.listFiles();
        if (files != null){
            for (File file : files) {
                file.delete();
            }
        }
    }

    private File createAudioCacheDir(Context ctx) {
        File decodeCacheDir = new File(ctx.getFilesDir(), "decodeCacheDir");//todo move tod DownloadManger
        decodeCacheDir.mkdirs();
        File[] files = decodeCacheDir.listFiles();
        if (files != null){
            for (File f : files) {
                f.delete();
            }
        }
        return decodeCacheDir;
    }

    public Observable<TrackState> play(TdApi.File file){

        if (OpusSupport.nativeIsOpusFile(file.path)){
            return playOpus(file.path);
        } else {
            CrashlyticsCore.getInstance()
                    .logException(new IllegalStateException("unsupported"));
            return Observable.empty();

        }

    }

    public boolean isPaused(String path) {
        return currentTrack != null
                && currentTrack.filePath.equals(path)
                && currentTrack.track.getPlayState() == AudioTrack.PLAYSTATE_PAUSED;
    }

    public void resume(String path) {
        assertNotNull(currentTrack);
        assertTrue(path.equals(currentTrack.filePath));
        currentTrack.track.play();
//        currentTrack.write(currentTrack.pcm16File, currentTrack.track.getPlaybackHeadPosition() * 2);
    }

    public void decode(TdApi.UpdateFile updateFile) {
        DecodeOpusFile(updateFile.file.path);
    }

    class Track {
        final AudioTrack track;
        final String filePath;
        private final int frameCount;
        private final File pcm16File;

        public BehaviorSubject<TrackState> state;

        public Track(  String filePath) {
            checkMainThread();
            this.filePath = filePath;
            pcm16File = DecodeOpusFile(filePath);

            int length = (int) pcm16File.length();
            this.track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE_IN_HZ,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    playerBufferSize,
                    AudioTrack.MODE_STREAM);

            write(pcm16File);

            track.setPositionNotificationPeriod(SAMPLE_RATE_IN_HZ / 8);
            frameCount = length / 2;
            track.setNotificationMarkerPosition(frameCount);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override
                public void onMarkerReached(AudioTrack track) {
                    log("marker reached " );
                    state.onNext(new TrackState(false, frameCount, frameCount));
                    trackPlayed();
                }

                @Override
                public void onPeriodicNotification(AudioTrack track) {

//                    log("period " + track.getPlaybackHeadPosition() + " " +  frameCount);
                    state.onNext(new TrackState(true, track.getPlaybackHeadPosition(), frameCount));
                }
            });
            track.play();
            state = BehaviorSubject.create(new TrackState(true, 0, frameCount));
        }


        private void write(final File arr) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    BufferedInputStream source = null;
                    try {
                        source = new BufferedInputStream(new FileInputStream(arr));
                        byte[] buffer = new byte[playerBufferSize];
                        int read;
                        while (((read = source.read(buffer)) != -1)) {
                            int writeInIteration = 0;
                            int write;

                            do {
                                write = track.write(buffer, writeInIteration, read - writeInIteration);
                                writeInIteration += write;

                                if (write < 0 ){
                                    log("break " + write);
                                    break;
                                }
                                if (writeInIteration != read) {
                                    SystemClock.sleep(64);//todo use semaphore
                                }
                            } while (writeInIteration != read);

                            if (write < 0) {
                                log("break " + write);
                                break;
                            }
                        }
                    } catch (IOException ignore) {
                        CrashlyticsCore.getInstance()
                                .logException(ignore);
                    } finally {
                        if (source != null) {
                            try {
                                source.close();
                            } catch (IOException ignore) {
                            }
                        }
                    }

                    log("finish " + arr);
//                    log("wrote " + bytesWrote);
                }
            });

        }




        public void stop() {
            log("stop");
            state.onNext(new TrackState(false, 0, frameCount));
            track.pause();
            track.stop();
        }
    }

    private int log(String msg) {
        return Log.d("AudioPlayer", msg);
    }

    private void trackPlayed() {
        currentTrack = null;
    }

    public class TrackState{
        //false if finished or stopped by another track
        public final boolean playing;
        public final int head;
        public final int duration;

        public TrackState(boolean playing, int head, int duration) {
            this.playing = playing;
            this.head = head;
            this.duration = duration;


        }
    }



    private Observable<TrackState> playOpus(final String path) {
        if (currentTrack != null){
            currentTrack.stop();
        }
        currentTrack = new Track(path);
        return currentTrack.state;
    }

    public Observable<TrackState> current() {
        assertNotNull(currentTrack);
        return currentTrack.state;
    }

    public boolean isPLaying(String path) {
        return currentTrack != null
                && currentTrack.filePath.equals(path)
                && AudioTrack.PLAYSTATE_PLAYING == currentTrack.track.getPlayState();
    }


    public void pause(String path) {
        log("pause");
        assertNotNull(currentTrack);
        assertTrue(path.equals(currentTrack.filePath));
        assertTrue(currentTrack.track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING);
        currentTrack.track.pause();
    }


    @NonNull
    private File DecodeOpusFile(String filePath)  {
        try {
            return decodeOpusFileUnsafe(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    private File decodeOpusFileUnsafe(String filePath) throws IOException {
        File src = new File(filePath);
        File dst = new File(decodeCacheDir, src.getName());
        if (dst.exists()){
            return dst;
        }
        boolean opened = OpusSupport.nativeOpenOpusFile(filePath);
        Assert.assertTrue(opened);
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);


        FileOutputStream out = new FileOutputStream(dst);
        while (true){
            OpusSupport.nativeReadOpusFile(buffer, buffer.capacity(), mOutArgs);
            int size = mOutArgs[0];
            int pcmOffset = mOutArgs[1];
            int finished = mOutArgs[2];
            out.write(buffer.array(), 0, size);

            if (finished == 1){
                break;
            }
        }
        out.flush();
        out.close();
        return dst;
    }


}
