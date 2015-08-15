package ru.korniltsev.telegram.core.picasso;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.rx.RXClient;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.utils.bitmap.BitmapPool;
import webp.SupportBitmapFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.Assert.assertTrue;

public class TDFileRequestHandler extends RequestHandler {

    private static final long TIMEOUT = 25000;

    public static TDFileUri load(TdApi.File f, boolean webp, BitmapPool.Size size) {
        return new TDFileUri(f, webp, size);
    }

    final RxDownloadManager downloader;
    private final BitmapPool pool;

    public TDFileRequestHandler(RxDownloadManager downloader, BitmapPool pool) {
        this.downloader = downloader;
        this.pool = pool;
    }

    @Override
    public boolean canHandleRequest(Request data) {
        return data.customUri instanceof TDFileUri;
    }

    @Override
    public Result load(Request request, int networkPolicy) throws IOException {
        TDFileUri uri = (TDFileUri) request.customUri;
        boolean webp = uri.webp;
        String path;
        if (uri.file.isLocal()) {
            path = uri.file.path;
        } else {
            path = downloadAndGetPath(uri.file.id);
        }

        //        https://code.google.com/p/webp/issues/detail?id=147
        //        WebP support for transparent files was added in Android JB-MR2 (4.2) onwards.
        if (webp && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Bitmap bitmap = SupportBitmapFactory.decodeWebPBitmap(path);
            if (bitmap != null) {
                return new Result(
                        bitmap,
                        Picasso.LoadedFrom.NETWORK);
            } else {
                //may be it is not webp
                return new Result(new FileInputStream(path), Picasso.LoadedFrom.NETWORK);
            }
        } else {
            if (BitmapPool.BITMAP_REUSE_SUPPORTED && uri.size != null){
                final Bitmap inBitmap = pool.get();
                final BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inBitmap = inBitmap;
                final Bitmap decoded = BitmapFactory.decodeFile(path, opts);
                assertTrue(inBitmap == decoded);
                return new Result(decoded, Picasso.LoadedFrom.NETWORK);
            } else {
                return new Result(new FileInputStream(path), Picasso.LoadedFrom.NETWORK);
            }
        }
    }


    private String downloadAndGetPath(int id) throws IOException {
        try {
            TdApi.File first = downloader.download(id)
                    .compose(RxDownloadManager.ONLY_RESULT)
                    .first()
                    .toBlocking()
                    .toFuture()
                    .get(TIMEOUT, TimeUnit.MILLISECONDS);
            return first.path;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            throw new IOException(e);
        }catch (Throwable e) {
            Log.e("EmptyFileDataFetcher", "err", e);
            throw new IOException(e);
        }
    }

    public static class TDFileUri {
        final TdApi.File file;
        final boolean webp;
        @Nullable private final BitmapPool.Size size;

        public TDFileUri(TdApi.File file, boolean webp, @Nullable BitmapPool.Size size) {
            this.file = file;
            this.webp = webp;
            this.size = size;
        }
    }
}
