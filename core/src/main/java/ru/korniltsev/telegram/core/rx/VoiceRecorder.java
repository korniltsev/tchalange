package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.util.Log;
import com.crashlytics.android.core.CrashlyticsCore;
import ru.korniltsev.OpusToolsWrapper;
import ru.korniltsev.telegram.core.audio.AudioPlayer;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@Singleton
public class VoiceRecorder {
    private final Context ctx;
    private final File tmpFilesDir;
    private final Vibrator vibrator;
    private int playerBufferSize;

    @Nullable private AudioRecord audioRecord;
    @Nullable private Reader reader;

    @Inject
    public VoiceRecorder(Context ctx) {
        this.ctx = ctx;
        tmpFilesDir = new File(ctx.getFilesDir(), "VoiceRecorder");
        tmpFilesDir.mkdir();
        playerBufferSize = AudioTrack.getMinBufferSize(AudioPlayer.SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }
        vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void record() {
        if (audioRecord != null){
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, AudioPlayer.SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, playerBufferSize);
        audioRecord.startRecording();
        reader = new Reader(audioRecord, getTemporaryFile(), playerBufferSize);
        new Thread(reader)
                .start();
        vibrate();
    }

    private void vibrate() {
        try {
            vibrator.vibrate(32);
        } catch (Exception e) {
        }
    }

    int counter;

    private File getTemporaryFile() {
        String fileName = "tmp" + counter++;
        return new File(tmpFilesDir, fileName);
    }

    public Observable<Record> stop() {
        vibrate();
        try {
            if (audioRecord == null || reader == null) {
                return Observable.empty();
            } else {
                audioRecord.stop();
                audioRecord.release();
                final PublishSubject<Record> result = reader.recordedAndEncodedFile;
                audioRecord = null;
                reader = null;
                return result;
            }
        } catch (IllegalStateException e) {
            CrashlyticsCore.getInstance().logException(e);
            return Observable.empty();
        }

    }

    static class Reader implements Runnable{
        final AudioRecord record;
        final File targetFile;
        final int bufferSize ;
        private final PublishSubject<Record> recordedAndEncodedFile = PublishSubject.create();
//        volatile boolean stopped = false;
        public Reader(AudioRecord record, File targetFile, int bufferSize) {
            this.record = record;
            this.targetFile = targetFile;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            log("msg");
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(targetFile);
                final byte[] bytes = new byte[bufferSize];
                int readTotal = 0;
                while (true) {
                    final int read = record.read(bytes, 0, bufferSize);
                    if (read == 0) {
                        log("breal");
                        break;
                    } else if (read > 0) {
                        readTotal += read;
                        fos.write(bytes, 0, read);
                        log("write");

                    } else {
                        log("break because " + read );
                        break;
                    }
                }
                fos.close();

                int samples = readTotal/2;
                float duration = (float)samples / AudioPlayer.SAMPLE_RATE_IN_HZ;

                final File ogg = new File(targetFile.getParent(), "encoded_" + targetFile.getName() + ".ogg");
                ogg.delete();
                String[] opusEncArgs = new String[]{
                        "--raw", "--raw-chan", "1",
                        targetFile.getAbsolutePath(), ogg.getAbsolutePath()};
                final boolean opusenc = OpusToolsWrapper.encode(targetFile.getAbsolutePath(), ogg.getAbsolutePath());
                log("opusenc = " + opusenc);
                if (opusenc) {
                    recordedAndEncodedFile.onNext(new Record(ogg, duration));
                    recordedAndEncodedFile.onCompleted();
                } else {
                    recordedAndEncodedFile.onError(new RuntimeException("failed to decode"));
                }
                System.out.println(opusenc);
            } catch (IOException e) {
                log("exception");
                CrashlyticsCore.getInstance().logException(e);
                recordedAndEncodedFile.onError(e);
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        CrashlyticsCore.getInstance().logException(e);
                    }
                }
            }
        }



        //        public void stop() {
//            stopped = true;
//        }
    }

    public static class Record {
        final File file;
        final float duration;//secs

        public Record(File file, float duration) {
            this.file = file;
            this.duration = duration;
        }
    }
    public static void log(String msg) {
//        Log.d("VoiceRecorder", msg);
    }

}
