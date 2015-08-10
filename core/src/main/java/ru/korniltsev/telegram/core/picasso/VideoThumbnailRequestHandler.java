package ru.korniltsev.telegram.core.picasso;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.IOException;

public class VideoThumbnailRequestHandler extends RequestHandler {

    public static VideoThumbUri create(TdApi.File fileLocal) {
        return new VideoThumbUri(fileLocal.path);
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.customUri instanceof VideoThumbUri;
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        VideoThumbUri uri = (VideoThumbUri) request.customUri;

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();

        try {
            retriever.setDataSource(uri.filePath);
            final Bitmap frameAtTime = retriever.getFrameAtTime(0);
            return new Result(frameAtTime, Picasso.LoadedFrom.DISK);
        } catch (Throwable ex) {
            CrashlyticsCore.getInstance().logException(ex);
            return null;
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException ignored) {
            }
        }


    }


    public static final class VideoThumbUri {
        public final String filePath;

        public VideoThumbUri(String filePath) {
            this.filePath = filePath;
        }
    }
}
