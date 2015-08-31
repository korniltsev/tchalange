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
import ru.korniltsev.telegram.core.audio.VoicePlayer;
import rx.Observable;
import rx.subjects.PublishSubject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class VoiceRecorder {
    private static final String TAG = "VoiceRecorder";
    private final Context ctx;
    private final File tmpFilesDir;
    private final Vibrator vibrator;
    private int playerBufferSize;

    @Nullable private AudioRecord audioRecord;
    @Nullable private Reader reader;

    public VoiceRecorder(Context ctx) {
        this.ctx = ctx;
        tmpFilesDir = new File(ctx.getFilesDir(), "VoiceRecorder");
        tmpFilesDir.mkdir();
        playerBufferSize = AudioTrack.getMinBufferSize(VoicePlayer.SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (playerBufferSize <= 0) {
            playerBufferSize = 3840;
        }
        vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
    }

    private static int[] mSampleRates = new int[]{VoicePlayer.SAMPLE_RATE_IN_HZ, 44100, 22050, 11025, 8000};

    @Nullable
    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT}) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        CrashlyticsCore.getInstance().logException(e);
                    }
                }
            }
        }
        return null;
    }

    public Observable<Double> record() {
        if (audioRecord != null) {
            return Observable.empty();
        }
        try {
            audioRecord = findAudioRecord();//new AudioRecord(MediaRecorder.AudioSource.DEFAULT, VoicePlayer.SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, playerBufferSize);
            if (audioRecord == null) {
                return Observable.empty();
            }
            audioRecord.startRecording();
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
            return Observable.empty();
        }
        reader = new Reader(audioRecord, getTemporaryFile(), playerBufferSize);
        new Thread(reader)
                .start();
        vibrate();
        return reader.amplitude;
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
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
            return Observable.empty();
        }
    }

    static class Reader implements Runnable {
        final AudioRecord record;
        final File targetFile;
        final int bufferSize;
        private final PublishSubject<Record> recordedAndEncodedFile = PublishSubject.create();
        private final PublishSubject<Double> amplitude = PublishSubject.create();

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
                final int shortBufSize = bufferSize / 2;
                final short[] shortBuff = new short[shortBufSize];
                int readTotal = 0;
                while (true) {
                    final int shortsRead = record.read(shortBuff, 0, shortBufSize);
                    if (shortsRead == 0) {
                        log("breal");
                        break;
                    } else if (shortsRead > 0) {
                        //write data
                        readTotal += shortsRead;
                        shortArrayToByteArray(shortBuff, bytes);
                        fos.write(bytes, 0, shortsRead * 2);
                        //calc amplitude
                        final double rms = rms(shortBuff, shortsRead);
                        this.amplitude.onNext(rms);
                    } else {
                        log("break because " + shortsRead);
                        break;
                    }
                }
                fos.close();

                int samples = readTotal;
                float duration = (float) samples / VoicePlayer.SAMPLE_RATE_IN_HZ;

                final File ogg = new File(targetFile.getParent(), "encoded_" + targetFile.getName() + ".ogg");
                ogg.delete();
                String[] opusEncArgs = new String[]{
                        "--raw", "--raw-chan", "1",
                        targetFile.getAbsolutePath(), ogg.getAbsolutePath()};
                final int sampleRate = record.getSampleRate();
                final int bits = record.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT ? 16 : 8;
                final int channelCount = record.getChannelConfiguration() == AudioFormat.CHANNEL_IN_MONO ? 1 : 2;
                final boolean opusenc = OpusToolsWrapper.encode(targetFile.getAbsolutePath(), ogg.getAbsolutePath(), bits, sampleRate, channelCount);
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

    private static double rms(short[] shortBuff, int shortsRead) {
        double sqrt;
        double sum = 0f;
        for (int i = 0; i < shortsRead; ++i) {
            sum += shortBuff[i] * shortBuff[i];
        }
        final double amplitude = sum / shortsRead;

        sqrt = Math.sqrt(amplitude);
        return sqrt;
    }

    private static byte[] shortArrayToByteArray(short[] sData, byte[] bytes) {
        for (int i = 0; i < sData.length; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
        }
        return bytes;
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
