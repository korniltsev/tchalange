package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Presenter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.mortar.ViewPresenterHolder;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.rx.RxChat;
import ru.korniltsev.telegram.core.utils.PhotoUtils;

import javax.inject.Inject;

//todo draw only bitmap
public class PhotoMessageView extends ImageView {
    public static final int ZERO_MEASURE_SPEC = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
    final RxGlide picasso;
    final Presenter presenter;//todo wtf, why presenter is here
    final DpCalculator calc;
    private final int horizontalWidth;
    private final int verticalWidth;
    private TdApi.Photo photo;
    private int width;
    private int height;

    public PhotoMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        presenter = (Presenter) ViewPresenterHolder.get(context);

        final MyApp app = MyApp.from(context);
        calc = app.calc;
        picasso = app.rxGlide;
        int spaceLeft = app.displayWidth - calc.dp(41 + 9 + 11 + 16);
        spaceLeft = Math.min(spaceLeft, calc.dp(300));
        horizontalWidth = spaceLeft;
        verticalWidth = (int) (spaceLeft * 0.7);
        setScaleType(ScaleType.CENTER_INSIDE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        log("measure " + MeasureSpec.toString(widthMeasureSpec) + " " + MeasureSpec.toString(heightMeasureSpec));

        if (width == 0) {
            super.onMeasure(ZERO_MEASURE_SPEC, ZERO_MEASURE_SPEC);
        } else {
            int w = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int h = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(w, h);
        }
    }

    public void load(final TdApi.Photo photo1, @Nullable TdApi.Message sentPhotoMessageHack) {

        //        if (this.photo == photo1) {
        //            return;
        //        }
        this.photo = photo1;

        setImageDrawable(null);

        float ratio;
        TdApi.File file;
        RxChat.SentImageInfo f = presenter.getRxChat()
                .getSentImage(sentPhotoMessageHack);
        if (f == null) {
            ratio = PhotoUtils.getPhotoRation(photo1);
            file = PhotoUtils.findSmallestBiggerThan(photo1, width, height);
        } else {
            //
            if (photo.photos.length == 1
                    && photo.photos[0].type.equals("i")
                    && photo.photos[0].photo.path.equals(f.f.path)) {
                final TdApi.PhotoSize p = this.photo.photos[0];
                int w = p.width;
                int h = p.height;
                ratio = (float) w / h;
                if ((f.exif == ExifInterface.ORIENTATION_ROTATE_90) || (f.exif == ExifInterface.ORIENTATION_ROTATE_270)) {
                    ratio = 1/ratio ;
                }
            } else {
                ratio = PhotoUtils.getPhotoRation(photo1);
            }
            //            Photo {
            //                id = 0
            //                photos = PhotoSize[] {[PhotoSize {
            //                    type = i
            //                    photo = File {
            //                        id = 1846
            //                        persistentId =
            //                                size = 1615702
            //                        path = /storage/emulated/0/temp.jpg
            //                    }
            //                    width = 3264
            //                    height = 2448
            //                }
            //                    ]}
            //            }

            file = f.f;
        }
        if (ratio > 1) {
            width = horizontalWidth;
        } else {
            width = verticalWidth;
        }
        height = (int) (width / ratio);

        picasso.loadPhoto(file, false)
                .resize(width, height)
                .into(this);
    }

    public void log(String msg) {
        Log.d("PhotoMessageView" + hashCode(), msg);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        log("set image drawable " + drawable);
    }
}
