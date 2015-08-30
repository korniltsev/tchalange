package ru.korniltsev.telegram.chat.adapter.view;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.common.AppUtils;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.rx.RxDownloadManager;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.views.DownloadView;

import java.io.File;

import static junit.framework.Assert.assertTrue;
import static ru.korniltsev.telegram.core.views.DownloadView.Config.FINAL_ICON_EMPTY;

public class VideoView extends FrameLayout {

//    private final int dp207;
//    private final int dp154;
    final RxGlide picasso;
    final DpCalculator calc;
    final RxDownloadManager downloader;
    private final int horizontalWidth;
    private final int verticalWidth;

    //    private ImageView actionIcon;
    private ImageView preview;

    //    private TdApi.Video msg;
    private DownloadView downloadView;
    private int width;
    private int height;
    private TdApi.PhotoSize thumb;
    private final BlurTransformation blur;

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final MyApp app = MyApp.from(context);
        calc = app.calc;
        picasso = app.rxGlide;
        downloader = app.downloadManager;


        //207x165
        int spaceLeft = app.displayWidth - calc.dp(41 + 9 + 11 + 16);
        spaceLeft = Math.max(spaceLeft, calc.dp(300));
        horizontalWidth = spaceLeft;
        verticalWidth = (int) (spaceLeft * 0.7);
        blur = new BlurTransformation(6);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        preview = ((ImageView) findViewById(R.id.preview));
        downloadView = ((DownloadView) findViewById(R.id.download_view));
    }

    public static void playVideo(Context ctx, RxDownloadManager downloader, TdApi.File f) {
        assertTrue(f.isLocal());
        File src = new File(f.path);

        File exposed = downloader.exposeFile(src, Environment.DIRECTORY_DOWNLOADS, null);

        Uri uri = Uri.fromFile(exposed);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setDataAndType(uri, "video/*");
        try {
            ctx.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            AppUtils.showNoActivityError(ctx);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    public void set(TdApi.Video msg) {
        TdApi.PhotoSize thumb = msg.thumb;
        TdApi.File file = msg.video;
        bindGeneral(thumb, file);
    }

    private void bindGeneral(TdApi.PhotoSize thumb, TdApi.File file) {
        this.thumb = thumb;
        float ratio = (float) thumb.width / thumb.height;
        if (ratio > 1) {
            width = horizontalWidth;
        } else {
            width = verticalWidth;
        }
        height = (int) (width / ratio);

        if (thumb.photo.id == 0) {
            picasso.getPicasso()
                    .cancelRequest(preview);
        } else {
            picasso.loadPhoto(thumb.photo, false)
                    .transform(blur)
                    .into(preview);
        }
        requestLayout();

        DownloadView.Config cfg = new DownloadView.Config(R.drawable.ic_play, FINAL_ICON_EMPTY, false, false, 48);
        downloadView.setVisibility(View.VISIBLE);
        downloadView.bind(file, cfg, new DownloadView.CallBack() {
            @Override
            public void onFinished(TdApi.File e, boolean justDownloaded) {
            }

            @Override
            public void play(TdApi.File e) {
                playVideo(getContext(), downloader, e);
            }
        }, this);
    }
}
