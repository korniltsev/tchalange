package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.Presenter;
import ru.korniltsev.telegram.core.app.MyApp;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.mortar.ViewPresenterHolder;
import ru.korniltsev.telegram.core.picasso.RxGlide;
import ru.korniltsev.telegram.core.utils.PhotoUtils;

import javax.inject.Inject;

//todo draw only bitmap
public class PhotoMessageView extends ImageView {
    public static final int ZERO_MEASURE_SPEC = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
    //    private final int atmost;
    final  RxGlide picasso;
    final Presenter presenter;//todo wtf, why presenter is here
    final DpCalculator calc;
    private TdApi.Photo photo;
    private int dip207;
    private int dip154;
    private int width;
    private int height;

    public PhotoMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        presenter = (Presenter) ViewPresenterHolder.get(context);


        final MyApp app = MyApp.from(context);
        calc = app.calc;
        picasso = app.rxGlide;
        dip207 = calc.dp(207);
        dip154 = calc.dp(154);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (width == 0) {
            super.onMeasure(ZERO_MEASURE_SPEC, ZERO_MEASURE_SPEC);
        } else {
            int w = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int h = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(w, h);
        }
    }

    public void load(final TdApi.Photo photo1, @Nullable TdApi.Message sentPhotoMessageHack) {
        if (this.photo == photo1) {
            return;
        }
//        final TdApi.MessagePhoto photo = (TdApi.MessagePhoto) msg.message;
         this.photo = photo1;
//        this.msg = msg;
        setImageDrawable(null);

        float ratio = PhotoUtils.getPhotoRation(photo1);
        if (ratio > 1) {
            width = dip207;
        } else {
            width = dip154;
        }
        height = (int) ( width/ratio);


        TdApi.File f = presenter.getRxChat()
                .getSentImage(sentPhotoMessageHack);
        if (f == null){
            f = PhotoUtils.findSmallestBiggerThan(photo1, width, height);
        }

        picasso.loadPhoto(f, false)
//                .resize(width, height)
                .into(this);
    }
}
