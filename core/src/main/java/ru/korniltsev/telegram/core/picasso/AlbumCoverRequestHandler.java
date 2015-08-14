package ru.korniltsev.telegram.core.picasso;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaExtractor;
import android.media.MediaMetadataRetriever;
import android.support.annotation.Nullable;
import com.crashlytics.android.core.CrashlyticsCore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import org.drinkless.td.libcore.telegram.TdApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;

public class AlbumCoverRequestHandler  extends RequestHandler{
    @Override
    public boolean canHandleRequest(Request data) {
        return data.customUri instanceof Uri;
    }

    @Override
    @Nullable
    public Result load(Request request, int networkPolicy) throws IOException {
        final Uri customUri = (Uri) request.customUri;
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(customUri.fileLocal.path);
            final byte[] picBytes = r.getEmbeddedPicture();

            if (picBytes == null) {
                return null;
            }
            final Bitmap picture = BitmapFactory.decodeByteArray(picBytes, 0, picBytes.length);
            return new Result(picture, Picasso.LoadedFrom.DISK);
        } catch (IllegalArgumentException e) {
            CrashlyticsCore.getInstance().logException(e);
            return null;
        }
    }

    public static class Uri  {
        final TdApi.File fileLocal;

        public Uri(TdApi.File fileLocal) {
            this.fileLocal = fileLocal;
            assertTrue(fileLocal.isLocal());
        }
    }
}
