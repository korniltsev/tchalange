package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import com.crashlytics.android.core.CrashlyticsCore;
import ru.korniltsev.telegram.core.audio.AudioPlayer;

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
    private int playerBufferSize;
    private AudioRecord audioRecord;
    private Reader reader;

    @Inject
    public VoiceRecorder(Context ctx) {
        this.ctx = ctx;
        tmpFilesDir = new File(ctx.getFilesDir(), "VoiceRecorder");
        tmpFilesDir.mkdir();
        playerBufferSize = AudioTrack.getMinBufferSize(AudioPlayer.SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }
    }

    public void record() {
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, AudioPlayer.SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, playerBufferSize);
        audioRecord.startRecording();
        reader = new Reader(audioRecord, getTemporaryFile(), playerBufferSize);
        new Thread(reader)
                .start();
    }

    int counter;

    private File getTemporaryFile() {
        String fileName = "tmp" + counter++;
        return new File(tmpFilesDir, fileName);
    }

    public void stop() {

        try {
            audioRecord.stop();
            audioRecord.release();
            reader.stop();
        } catch (IllegalStateException e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    static class Reader implements Runnable{
        final AudioRecord record;
        final File targetFile;
        final int bufferSize ;
        volatile boolean stopped = false;
        public Reader(AudioRecord record, File targetFile, int bufferSize) {
            this.record = record;
            this.targetFile = targetFile;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(targetFile);
                final byte[] bytes = new byte[bufferSize];
                while (!stopped) {
                    final int read = record.read(bytes, 0, bufferSize);
                    fos.write(bytes, 0, read);
                }
            } catch (IOException e) {
                CrashlyticsCore.getInstance().logException(e);
            } finally {
                if (fos != null){
                    try {
                        fos.close();
                    } catch (IOException e) {
                        CrashlyticsCore.getInstance().logException(e);
                    }
                }
            }
        }

        public void stop() {
            stopped = true;
        }
    }

}
