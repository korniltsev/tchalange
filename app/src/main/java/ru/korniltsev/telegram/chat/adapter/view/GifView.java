package ru.korniltsev.telegram.chat.adapter.view;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import pl.droidsonroids.gif.GifDrawable;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.views.DownloadView;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.views.DownloadView.Config.FINAL_ICON_EMPTY;

public class GifView extends FrameLayout {

    private final int dp207;
    private final int dp154;
    @Inject RxGlide picasso;
    @Inject DpCalculator calc;
    @Inject RxDownloadManager downloader;

//    private ImageView actionIcon;
    private ImageView preview;

//    private TdApi.Video msg;
    private DownloadView downloadView;
    private int width;
    private int height;
    private TdApi.PhotoSize thumb;
    private final BlurTransformation blur;

    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
        //207x165
        dp207 = calc.dp(207);
        dp154 = calc.dp(154);
        blur = new BlurTransformation(getContext().getApplicationContext(), 12);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        preview = ((ImageView) findViewById(R.id.preview));
        downloadView = ((DownloadView) findViewById(R.id.download_view));
    }

    private void playVideo(TdApi.File f) {
        assertTrue(f.isLocal());
        File src = new File(f.path);


        File exposed = downloader.exposeFile(src, Environment.DIRECTORY_DOWNLOADS, null);

        Uri uri = Uri.fromFile(exposed);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, "video/*");
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            //todo error
        }
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }



//    public void set(TdApi.Video msg) {
////        this.msg = msg;
//        TdApi.PhotoSize thumb = msg.thumb;
//        TdApi.File file = msg.video;
//        bindGeneral(thumb, file, false);
//    }

    public void set(TdApi.Document doc) {
        assertTrue("image/gif".equals(doc.mimeType));
        TdApi.PhotoSize thumb = doc.thumb;
        TdApi.File file = doc.document;
        bindGeneral(thumb, file);
    }

    private void bindGeneral(TdApi.PhotoSize thumb, TdApi.File file) {
        final boolean gif = true;
        this.thumb = thumb;
        float ratio = (float) thumb.width / thumb.height;
        if (ratio > 1) {
            width = dp207;
        } else {
            width = dp154;
        }
        height = (int) (width / ratio);
        showLowQualityThumb(thumb);
        requestLayout();

        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, FINAL_ICON_EMPTY, false, false, 48);
        downloadView.setVisibility(View.VISIBLE);
        downloadView.bind(file, cfg, new DownloadView.CallBack() {
            @Override
            public void onFinished(TdApi.File e, boolean justDownloaded) {
                assertTrue(e.isLocal());
                if (justDownloaded) {
                    setAndPlayGif(e);
                }
            }

            @Override
            public void play(TdApi.File e) {
                assertTrue(e.isLocal());
                Drawable drawable = preview.getDrawable();
                if (drawable instanceof GifDrawable){
                    ((GifDrawable) drawable).pause();
                    preview.setImageDrawable(null);
                    showLowQualityThumb(GifView.this.thumb);
                    downloadView.setVisibility(View.VISIBLE);
                } else {
                    setAndPlayGif(e);
                }
            }
        }, this);
    }

    private void setAndPlayGif(TdApi.File e) {
        assertTrue(e.isLocal());
        try {
            preview.setImageDrawable(new GifDrawable(e.path));
            downloadView.setVisibility(View.GONE);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }


    private boolean showLowQualityThumb(TdApi.PhotoSize thumb) {
        if (thumb.photo.id == 0) {
            picasso.getPicasso()
                    .cancelRequest(preview);
            return false;
        } else {
            picasso.loadPhoto(thumb.photo, false)
                    .transform(blur)
                    .into(preview);
            return true;
        }
    }
}
