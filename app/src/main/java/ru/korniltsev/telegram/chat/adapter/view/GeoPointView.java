package ru.korniltsev.telegram.chat.adapter.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import mortar.dagger1support.ObjectGraphService;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.core.emoji.DpCalculator;
import ru.korniltsev.telegram.core.picasso.RxGlide;

import javax.inject.Inject;
import java.util.Locale;

import static android.view.View.MeasureSpec.makeMeasureSpec;

public class GeoPointView extends ImageView {
    @Inject DpCalculator calc;
    @Inject RxGlide picasso;

    public GeoPointView(Context context, AttributeSet attrs) {
        super(context, attrs);
        ObjectGraphService.inject(context, this);
    }

    public void set(TdApi.MessageLocation msg) {
        String url = urlFor(
                msg.location.latitude,
                msg.location.longitude
        );
        picasso.getPicasso()
                .load(url)
                .into(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final ViewGroup.LayoutParams lp = getLayoutParams();
        final int w = makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        final int h = makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        super.onMeasure(w, h);
    }

    private String urlFor(double latitude, double longitude) {

        return String.format(Locale.US, "https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=13&size=200x100&maptype=roadmap&scale=%d&markers=color:red|size:big|%f,%f&sensor=false", latitude, longitude, Math.min(2, (int) Math.ceil(calc.density)), latitude, longitude);
    }
}
