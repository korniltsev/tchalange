package ru.korniltsev.telegram.core.rx;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.Utils;
import ru.korniltsev.telegram.core.utils.PhotoUtils;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;

import static ru.korniltsev.telegram.core.utils.Preconditions.checkMainThread;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

@Singleton
public class GalleryService {
    final Context ctx;
    final RxDownloadManager downloader;
    private final SharedPreferences prefs;
    private final String appName;

    @Inject
    public GalleryService(Context ctx, RxDownloadManager downloader) {
        this.ctx = ctx;
        appName = ctx.getApplicationInfo().loadLabel(ctx.getPackageManager()).toString();
        this.downloader = downloader;
        prefs = ctx.getSharedPreferences("GalleryService", Context.MODE_PRIVATE);
    }

    private int increaseCounter() {
        int counter = prefs.getInt("counter", 0);
        prefs.edit()
                .putInt("counter", counter + 1)
                .commit();
        return counter;
    }

    public Observable<File> saveToGallery(@NonNull final TdApi.Photo photo) {
        TdApi.PhotoSize biggestSize = PhotoUtils.findBiggestSize(photo);
        final TdApi.File photo1 = biggestSize.photo;
        return impl(photo1);
    }

    @NonNull
    private Observable<File> impl(TdApi.File photo1) {
        return downloader.download(photo1)
                .compose(RxDownloadManager.ONLY_RESULT)
                .observeOn(io())
                .flatMap(new Func1<TdApi.File, Observable<File>>() {
                    @Override
                    public Observable<File> call(TdApi.File fileLocal) {
                        return Observable.just(
                                copyFileToGallery(fileLocal));
                    }
                })
                .observeOn(mainThread())
                .map(new Func1<File, File>() {
                    @Override
                    public File call(File f) {
                        checkMainThread();
                        scanFile(f);
                        return f;
                    }
                });
    }

    private void scanFile(File f) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        ctx.sendBroadcast(mediaScanIntent);
    }

    private File copyFileToGallery(TdApi.File fileLocal) {
        File picturesDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                appName);
        picturesDir.mkdirs();
        File dst = new File(picturesDir, increaseCounter() + ".jpeg");//todo may be not jpeg
        File src = new File(fileLocal.path);
        try {
            Utils.copyFile(src, dst);
            return dst;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Observable<File> saveToGallery(TdApi.File big) {
        return impl(big);
    }
}
